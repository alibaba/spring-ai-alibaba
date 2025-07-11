/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.util.ChartUtils;
import com.alibaba.cloud.ai.util.TableUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReportGeneratorNode的单元测试
 *
 * 注意：此测试类仅测试工具类方法，不测试实际的Node功能， 因为Node依赖于Spring AI和多个外部组件
 */
class ReportGeneratorNodeTest {

	@Test
	void testTableFormatting() {
		String testTable = "| Column1 | Column2 |\n| --- | --- |\n| Value1 | Value2 |\n| Value3 | Value4 |";
		String formatted = TableUtils.formatTable(testTable);

		assertThat(formatted).isNotNull();
		assertThat(formatted).contains("Column1");
		assertThat(formatted).contains("Column2");
		assertThat(formatted).contains("Value1");
	}

	@Test
	void testChartExtraction() {
		String text = "这是一个图表引用 ![图表](path/to/chart.png) 测试";
		List<String> paths = ChartUtils.extractChartPaths(text);

		assertThat(paths).isNotNull();
		assertThat(paths).hasSize(1);
		assertThat(paths.get(0)).isEqualTo("path/to/chart.png");
	}

	/**
	 * 测试表格内容提取
	 */
	@Test
	void testTableExtraction() {
		String content = "这是一段文本，下面包含表格：\n\n" + "| 列1 | 列2 | 列3 |\n" + "| --- | --- | --- |\n" + "| 数据1 | 数据2 | 数据3 |\n"
				+ "| 数据4 | 数据5 | 数据6 |\n\n" + "表格后的文本";

		List<String> tables = TableUtils.extractTables(content);

		assertThat(tables).isNotNull();
		assertThat(tables).hasSize(1);
		assertThat(tables.get(0)).contains("列1");
		assertThat(tables.get(0)).contains("数据1");
	}

	/**
	 * 测试表格合并功能
	 */
	@Test
	void testTableMerging() {
		String table1 = "| 字段 | 类型 |\n| --- | --- |\n| id | int |\n| name | string |";
		String table2 = "| 字段 | 类型 |\n| --- | --- |\n| age | int |\n| email | string |";

		List<String> tables = new ArrayList<>();
		tables.add(table1);
		tables.add(table2);

		String merged = TableUtils.mergeTables(tables);

		assertThat(merged).isNotNull();
		assertThat(merged).isNotEmpty();
		assertThat(merged).contains("id");
		assertThat(merged).contains("email");
	}

	/**
	 * 测试创建表格摘要
	 */
	@Test
	void testTableSummary() {
		String table = "| 产品 | 销量 | 价格 |\n" + "| --- | --- | --- |\n" + "| 手机 | 1200 | 4999 |\n"
				+ "| 电脑 | 800 | 8999 |\n" + "| 耳机 | 2500 | 999 |";

		String summary = TableUtils.generateTableSummary(table, 2);

		assertThat(summary).isNotNull();
		assertThat(summary).contains("3 行数据");
		assertThat(summary).contains("产品, 销量, 价格");
	}

	/**
	 * 测试ChartUtils创建图表引用
	 */
	@Test
	void testChartReference() {
		String chartPath = "path/to/chart.png";
		String reference = ChartUtils.createChartReference(chartPath, "销售趋势");

		assertThat(reference).isEqualTo("![销售趋势](path/to/chart.png)");
	}

}
