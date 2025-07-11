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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表格工具类，用于处理和格式化报告中的表格数据
 *
 * @author zhangshenghang
 */
public class TableUtils {

	private static final Logger logger = LoggerFactory.getLogger(TableUtils.class);

	// Markdown表格匹配模式
	private static final Pattern TABLE_PATTERN = Pattern.compile("\\|[^\\|\\n]*\\|[^\\|\\n]*\\|(?:[^\\|\\n]*\\|)*\\n" + // 表头行
			"\\|\\s*[-:]+\\s*\\|(?:\\s*[-:]+\\s*\\|)*\\n" + // 分隔行
			"(?:\\|[^\\|\\n]*\\|(?:[^\\|\\n]*\\|)*\\n)+" // 一行或多行数据
	);

	// 表格行分隔符匹配模式
	private static final Pattern TABLE_SEPARATOR_ROW_PATTERN = Pattern.compile("\\|\\s*[-:]+\\s*\\|");

	/**
	 * 从文本中提取所有Markdown表格
	 * @param text 包含表格的文本内容
	 * @return 表格列表
	 */
	public static List<String> extractTables(String text) {
		if (text == null || text.isEmpty()) {
			return new ArrayList<>();
		}

		List<String> tables = new ArrayList<>();
		Matcher matcher = TABLE_PATTERN.matcher(text);
		while (matcher.find()) {
			String table = matcher.group();
			tables.add(table.trim());
		}
		return tables;
	}

	/**
	 * 检查文本是否为有效的Markdown表格
	 * @param text 待检查的文本
	 * @return 是否为有效表格
	 */
	public static boolean isValidTable(String text) {
		if (text == null || text.trim().isEmpty()) {
			return false;
		}

		String[] lines = text.split("\\n");
		if (lines.length < 2) {
			return false;
		}

		// 检查是否包含分隔行（第二行应该是 |---|---|...）
		Matcher separatorMatcher = TABLE_SEPARATOR_ROW_PATTERN.matcher(lines[1]);
		return separatorMatcher.find();
	}

	/**
	 * 解析Markdown表格成二维数组
	 * @param tableText Markdown格式表格文本
	 * @return 表格数据的二维数组，第一行为表头
	 */
	public static String[][] parseTable(String tableText) {
		if (!isValidTable(tableText)) {
			logger.warn("无效的表格格式");
			return new String[0][0];
		}

		String[] lines = tableText.trim().split("\\n");
		List<String[]> rows = new ArrayList<>();

		String[] headers = parseTableRow(lines[0]);
		rows.add(headers);

		for (int i = 2; i < lines.length; i++) {
			String[] row = parseTableRow(lines[i]);
			rows.add(row);
		}

		return rows.toArray(new String[0][0]);
	}

	/**
	 * 解析表格行
	 * @param line 表格行文本
	 * @return 单元格数组
	 */
	private static String[] parseTableRow(String line) {
		String trimmed = line.trim();
		if (trimmed.startsWith("|")) {
			trimmed = trimmed.substring(1);
		}
		if (trimmed.endsWith("|")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}

		return Arrays.stream(trimmed.split("\\|")).map(String::trim).toArray(String[]::new);
	}

	/**
	 * 创建Markdown格式表格
	 * @param headers 表头
	 * @param rows 数据行
	 * @return Markdown格式的表格文本
	 */
	public static String createMarkdownTable(String[] headers, List<String[]> rows) {
		if (headers == null || headers.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("|");
		for (String header : headers) {
			sb.append(" ").append(header).append(" |");
		}
		sb.append("\n");

		sb.append("|");
		for (int i = 0; i < headers.length; i++) {
			sb.append(" --- |");
		}
		sb.append("\n");

		if (rows != null) {
			for (String[] row : rows) {
				sb.append("|");
				for (int i = 0; i < Math.min(row.length, headers.length); i++) {
					sb.append(" ").append(row[i]).append(" |");
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	/**
	 * 合并多个表格
	 * @param tables 表格列表
	 * @return 合并后的表格文本，如果不能合并则返回空字符串
	 */
	public static String mergeTables(List<String> tables) {
		if (tables == null || tables.isEmpty()) {
			return "";
		}

		if (tables.size() == 1) {
			return tables.get(0);
		}

		try {
			String[][] firstTable = parseTable(tables.get(0));
			if (firstTable.length == 0) {
				return "";
			}

			String[] headers = firstTable[0];
			List<String[]> allRows = new ArrayList<>();

			for (int i = 1; i < firstTable.length; i++) {
				allRows.add(firstTable[i]);
			}

			for (int t = 1; t < tables.size(); t++) {
				String[][] otherTable = parseTable(tables.get(t));
				if (otherTable.length > 1) {
					for (int i = 1; i < otherTable.length; i++) {
						allRows.add(otherTable[i]);
					}
				}
			}

			return createMarkdownTable(headers, allRows);
		}
		catch (Exception e) {
			logger.error("合并表格失败: {}", e.getMessage());
			return "";
		}
	}

	/**
	 * 格式化美化表格
	 * @param tableText 原始表格文本
	 * @return 格式化后的表格文本
	 */
	public static String formatTable(String tableText) {
		if (!isValidTable(tableText)) {
			return tableText;
		}

		try {
			String[][] table = parseTable(tableText);
			if (table.length == 0) {
				return tableText;
			}

			String[] headers = table[0];
			List<String[]> rows = new ArrayList<>();

			for (int i = 1; i < table.length; i++) {
				rows.add(table[i]);
			}

			return createMarkdownTable(headers, rows);
		}
		catch (Exception e) {
			logger.error("格式化表格失败: {}", e.getMessage());
			return tableText;
		}
	}

	/**
	 * 提取表格摘要信息
	 * @param tableText 表格文本
	 * @param maxRows 最大行数
	 * @return 表格摘要
	 */
	public static String generateTableSummary(String tableText, int maxRows) {
		if (!isValidTable(tableText)) {
			return "非有效表格数据";
		}

		try {
			String[][] table = parseTable(tableText);
			if (table.length <= 1) {
				return "空表格";
			}

			String[] headers = table[0];
			StringBuilder summary = new StringBuilder();

			summary.append("表格包含 ").append(table.length - 1).append(" 行数据，");
			summary.append("列名为: ").append(String.join(", ", headers)).append("。");

			if (table.length > 1) {
				int rowsToShow = Math.min(table.length - 1, maxRows);
				if (rowsToShow > 0) {
					summary.append("前 ").append(rowsToShow).append(" 行数据摘要: ");
					for (int i = 1; i <= rowsToShow; i++) {
						summary.append("\n行 ").append(i).append(": ");
						for (int j = 0; j < Math.min(headers.length, table[i].length); j++) {
							summary.append(headers[j]).append("=").append(table[i][j]).append(", ");
						}
						summary.setLength(summary.length() - 2);
					}
				}
			}

			return summary.toString();
		}
		catch (Exception e) {
			logger.error("生成表格摘要失败: {}", e.getMessage());
			return "无法解析表格内容";
		}
	}

}
