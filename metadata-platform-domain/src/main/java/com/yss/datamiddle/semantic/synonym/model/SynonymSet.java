package com.yss.datamiddle.semantic.synonym.model;

import com.yss.datamiddle.semantic.term.exception.BusinessValidationException;
import com.yss.datamiddle.semantic.term.exception.StateConflictException;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 同义词组聚合根（SynonymSetAggregate：synonym_set + synonym_word）。
 */
@Getter
@Setter
public class SynonymSet implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String canonical;
    private List<String> words = new ArrayList<>();
    private Long termId;
    private Boolean enabled;
    private Integer version;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SynonymSet create(String name, String canonical, List<String> words, Long termId, String operator) {
        validateCanonicalInWords(canonical, words);

        SynonymSet s = new SynonymSet();
        s.name = name;
        s.canonical = canonical;
        s.words = words == null ? new ArrayList<>() : new ArrayList<>(words);
        s.termId = termId;
        s.enabled = true;
        s.version = 0;
        s.createdBy = operator;
        s.createdAt = LocalDateTime.now();
        s.updatedAt = s.createdAt;
        return s;
    }

    public void update(String name, String canonical, List<String> words, Long termId, Integer expectedVersion, String operator) {
        if (!Objects.equals(this.version, expectedVersion)) {
            throw new StateConflictException("VERSION_CONFLICT: 版本已过期");
        }
        validateCanonicalInWords(canonical, words);

        this.name = name;
        this.canonical = canonical;
        this.words = words == null ? new ArrayList<>() : new ArrayList<>(words);
        this.termId = termId;
        this.version = (this.version == null ? 0 : this.version) + 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void toggleStatus(boolean enabled, String operator) {
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    public void addWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            String trimmed = word.trim();
            if (this.words == null) {
                this.words = new ArrayList<>();
            }
            if (!this.words.contains(trimmed)) {
                this.words.add(trimmed);
                this.version = (this.version == null ? 0 : this.version) + 1;
                this.updatedAt = LocalDateTime.now();
            }
        }
    }

    private static void validateCanonicalInWords(String canonical, List<String> words) {
        if (canonical == null || canonical.trim().isEmpty()) {
            throw new BusinessValidationException("PARAM_VALIDATION_ERROR", "canonical", "REQUIRED", "主词不能为空");
        }
        if (words == null || !words.contains(canonical)) {
            throw new BusinessValidationException("PARAM_VALIDATION_ERROR", "canonical", "REQUIRED_IN_WORDS", "主词必须包含在同义词条列表中");
        }
    }
}
