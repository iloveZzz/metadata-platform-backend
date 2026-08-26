package com.yss.metadata.repository;

import com.yss.metadata.domain.rbac.gateway.RoleGateway;
import com.yss.metadata.domain.rbac.model.Role;
import com.yss.metadata.domain.rbac.model.RoleSummary;
import com.yss.metadata.infrastructure.convertor.RoleConvertor;
import com.yss.metadata.repository.entity.DataDomainPO;
import com.yss.metadata.repository.entity.RoleDomainPO;
import com.yss.metadata.repository.entity.RolePO;
import com.yss.metadata.repository.gateway.impl.RoleGatewayImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 角色仓储 H2 持久化测试（WU-06-01；role + role_domain + data_domain）。
 *
 * <p>覆盖：创建含域绑定（data_domain 幂等 upsert）、列表 refs=绑定数、
 * name 唯一、删除级联清理 role_domain（data_domain 行保留）、域绑定替换。</p>
 */
class RoleGatewayImplH2Test extends H2MapperTestSupport {

    private RoleGateway roleGateway;
    private RoleRepository roleMapper;
    private RoleDomainRepository roleDomainMapper;
    private DataDomainRepository dataDomainMapper;

    @BeforeEach
    void setUp() {
        roleMapper = sqlSession.getMapper(RoleRepository.class);
        roleDomainMapper = sqlSession.getMapper(RoleDomainRepository.class);
        dataDomainMapper = sqlSession.getMapper(DataDomainRepository.class);
        roleGateway = new RoleGatewayImpl(roleMapper, roleDomainMapper, dataDomainMapper,
                Mappers.getMapper(RoleConvertor.class));
    }

    @Test
    @DisplayName("创建含域绑定：role + data_domain 幂等 upsert + role_domain 绑定；列表 refs=绑定数")
    void saveWithDomainsAndListRefs() {
        Role role = roleGateway.save(Role.builder().id("r-1").name("数据工程师").scope("交易/客户域").build());
        roleGateway.replaceDomains(role.getId(), Arrays.asList("交易域", "客户域", "交易域"));

        // data_domain 幂等 upsert：3 次录入只落 2 行
        assertThat(dataDomainMapper.selectList(null)).hasSize(2);
        // role_domain 绑定 2 行
        assertThat(roleDomainMapper.selectList(null)).hasSize(2);

        List<RoleSummary> list = roleGateway.listAll();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getRefs()).isEqualTo(2);
        assertThat(list.get(0).getScope()).isEqualTo("交易/客户域");
    }

    @Test
    @DisplayName("name 唯一：findByName 命中；role 表唯一约束")
    void findByName() {
        roleGateway.save(Role.builder().id("r-1").name("平台管理员").build());

        Optional<Role> found = roleGateway.findByName("平台管理员");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("r-1");
        assertThat(roleGateway.findByName("不存在")).isEmpty();
    }

    @Test
    @DisplayName("删除：级联清理 role_domain，data_domain 行保留（避免孤儿引用）")
    void deleteCascadesBindingsKeepsDomains() {
        roleGateway.save(Role.builder().id("r-1").name("数据工程师").build());
        roleGateway.replaceDomains("r-1", Collections.singletonList("交易域"));

        roleGateway.deleteById("r-1");

        assertThat(roleMapper.selectById("r-1")).isNull();
        assertThat(roleDomainMapper.selectList(null)).isEmpty();
        // data_domain 行保留
        assertThat(dataDomainMapper.selectList(null)).hasSize(1);
    }

    @Test
    @DisplayName("域绑定替换：全量替换（旧绑定清除 + 新绑定写入）")
    void replaceDomainsFullReplace() {
        roleGateway.save(Role.builder().id("r-1").name("治理专员").build());
        roleGateway.replaceDomains("r-1", Arrays.asList("交易域", "客户域"));
        assertThat(roleGateway.countDomains("r-1")).isEqualTo(2);

        roleGateway.replaceDomains("r-1", Collections.singletonList("财务域"));

        assertThat(roleGateway.countDomains("r-1")).isEqualTo(1);
        List<RoleDomainPO> bindings = roleDomainMapper.selectList(null);
        assertThat(bindings).hasSize(1);
        DataDomainPO bound = dataDomainMapper.selectById(bindings.get(0).getDomainId());
        assertThat(bound.getName()).isEqualTo("财务域");
    }

    @Test
    @DisplayName("countDomains：无绑定返回 0")
    void countDomainsEmpty() {
        roleGateway.save(Role.builder().id("r-1").name("只读角色").build());
        assertThat(roleGateway.countDomains("r-1")).isZero();
        assertThat(roleGateway.countDomains("not-exist")).isZero();
    }
}
