package com.yss.datamiddle.semantic.metric.batch;

import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetricCsvParserTest {

    private final MetricCsvParser parser = new MetricCsvParser();

    @Test
    public void testParseCsvWithHeader() {
        String csv = "指标名称,指标分组,业务口径,负责人,计算公式,逻辑描述\n" +
                "日活跃用户数,用户域,当日有登录的用户唯一去重数,admin,count(distinct user_id),按天聚合\n" +
                "\"月营收,总额\",财务域,\"本月确认营收,含税\",finance,sum(amount),按月结算";

        List<MetricImportItem> list = parser.parseCsv(csv);
        Assertions.assertEquals(2, list.size());

        Assertions.assertEquals("日活跃用户数", list.get(0).getName());
        Assertions.assertEquals("用户域", list.get(0).getMetricGroup());
        Assertions.assertEquals("count(distinct user_id)", list.get(0).getExpression());

        Assertions.assertEquals("月营收,总额", list.get(1).getName());
        Assertions.assertEquals("本月确认营收,含税", list.get(1).getDescription());
    }

    @Test
    public void testExportCsv() {
        MetricDefinition m = MetricDefinition.create("净利润", "财务域", "营业收入减去营业成本及税费", "cfo", "admin");
        m.addVersion("income - cost - tax", "标准口径", Collections.singletonList("dept"), null, "admin");

        String csv = parser.exportToCsv(Collections.singletonList(m));
        Assertions.assertTrue(csv.contains("指标名称,指标分组"));
        Assertions.assertTrue(csv.contains("净利润,财务域"));
        Assertions.assertTrue(csv.contains("income - cost - tax"));
    }
}
