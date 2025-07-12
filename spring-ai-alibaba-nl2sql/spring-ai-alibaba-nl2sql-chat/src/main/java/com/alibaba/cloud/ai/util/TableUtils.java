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

package com.alibaba.cloud.ai.util;

import com.alibaba.cloud.ai.dbconnector.bo.ResultSetBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 表格工具类，用于处理SQL查询结果的表格格式化
 *
 * @author Makoto
 */
public class TableUtils {

	private static final Logger logger = LoggerFactory.getLogger(TableUtils.class);

	/**
	 * 默认显示的最大行数
	 */
	private static final int DEFAULT_ROW_LIMIT = 20;

	/**
	 * 格式化SQL查询结果为Markdown表格
	 * @param resultSetBO SQL查询结果对象
	 * @return 格式化后的Markdown表格字符串
	 */
	public static String formatSqlResultToMarkdown(ResultSetBO resultSetBO) {
		return formatSqlResultToMarkdown(resultSetBO, DEFAULT_ROW_LIMIT);
	}

	/**
	 * 格式化SQL查询结果为Markdown表格，指定最大显示行数
	 * @param resultSetBO SQL查询结果对象
	 * @param rowLimit 最大显示行数
	 * @return 格式化后的Markdown表格字符串
	 */
	public static String formatSqlResultToMarkdown(ResultSetBO resultSetBO, int rowLimit) {
		if (resultSetBO == null) {
			return "**查询结果**: 无数据\n\n";
		}

		List<String> columns = resultSetBO.getColumn();
		List<Map<String, String>> data = resultSetBO.getData();

		if (columns == null || columns.isEmpty() || data == null) {
			return "**查询结果**: 无数据\n\n";
		}

		StringBuilder tableBuilder = new StringBuilder();

		try {
			tableBuilder.append("**查询结果**:\n\n");
			tableBuilder.append("| ");
			for (String column : columns) {
				tableBuilder.append(column).append(" | ");
			}
			tableBuilder.append("\n| ");

			for (int i = 0; i < columns.size(); i++) {
				tableBuilder.append("--- | ");
			}
			tableBuilder.append("\n");

			int effectiveRowLimit = Math.min(data.size(), rowLimit);
			for (int i = 0; i < effectiveRowLimit; i++) {
				Map<String, String> row = data.get(i);
				tableBuilder.append("| ");
				for (String column : columns) {
					String value = row.get(column);
					value = (value == null || value.isEmpty()) ? "-" : value;
					tableBuilder.append(value).append(" | ");
				}
				tableBuilder.append("\n");
			}

			if (data.size() > effectiveRowLimit) {
				tableBuilder.append("\n*注: 共")
					.append(data.size())
					.append("行数据，仅显示前")
					.append(effectiveRowLimit)
					.append("行*\n");
			}

			tableBuilder.append("\n");
		}
		catch (Exception e) {
			logger.error("格式化SQL结果为Markdown表格时发生错误", e);
			return "**查询结果**: 格式化错误: " + e.getMessage() + "\n\n";
		}

		return tableBuilder.toString();
	}

	/**
	 * 将SQL查询结果添加到报告中
	 * @param reportData 报告构建器
	 * @param resultSetBO SQL查询结果对象
	 */
	public static void formatSqlResult(StringBuilder reportData, ResultSetBO resultSetBO) {
		if (reportData == null) {
			return;
		}

		String formattedTable = formatSqlResultToMarkdown(resultSetBO);
		reportData.append(formattedTable);
	}

}
