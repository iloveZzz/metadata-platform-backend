package com.yss.metadata.domain.governance.model;

import com.yss.metadata.domain.governance.exception.ClassificationStateConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 分级分类结果状态机测试（WU-04-02 候选确认/修正，TDD 红→绿）。
 *
 * <p>覆盖：待确认→已确认（幂等）、修正覆盖名并流转已修正、
 * 空修正名冲突（409 语义）、确认后修正仍可修正。</p>
 */
class ClassificationTest {

    @Test
    @DisplayName("候选确认：pending → confirmed；重复确认幂等无操作")
    void confirmTransitionsAndIsIdempotent() {
        Classification classification = pending();

        classification.confirm();

        assertThat(classification.getStatus()).isEqualTo(ClassificationStatus.CONFIRMED);

        classification.confirm();

        assertThat(classification.getStatus()).isEqualTo(ClassificationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("修正候选：分类名覆盖 + 流转已修正")
    void correctOverridesNameAndTransitions() {
        Classification classification = pending();

        classification.correct("内部受限");

        assertThat(classification.getName()).isEqualTo("内部受限");
        assertThat(classification.getStatus()).isEqualTo(ClassificationStatus.CORRECTED);
    }

    @Test
    @DisplayName("修正名空白抛状态冲突（409 语义）")
    void correctWithBlankNameThrows() {
        Classification classification = pending();

        assertThatThrownBy(() -> classification.correct("   "))
                .isInstanceOf(ClassificationStateConflictException.class)
                .hasMessageContaining("不能为空");
        assertThatThrownBy(() -> classification.correct(null))
                .isInstanceOf(ClassificationStateConflictException.class);
        // 状态未被污染
        assertThat(classification.getStatus()).isEqualTo(ClassificationStatus.PENDING);
    }

    @Test
    @DisplayName("已确认后仍可修正（覆盖为已修正）")
    void correctAfterConfirmAllowed() {
        Classification classification = pending();
        classification.confirm();

        classification.correct("受限");

        assertThat(classification.getName()).isEqualTo("受限");
        assertThat(classification.getStatus()).isEqualTo(ClassificationStatus.CORRECTED);
    }

    private Classification pending() {
        return Classification.builder()
                .id("c-1").assetId("a-1").columnId("col-1")
                .name("敏感-PII").level("PII").source("auto")
                .status(ClassificationStatus.PENDING)
                .build();
    }
}
