/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.dbconnector;

import com.alibaba.cloud.ai.dbconnector.bo.ResultSetBO;

import java.util.List;
import java.util.Map;

public class MdTableGenerator {

	/**
	 * 将二维数组转为 Markdown 表格
	 * @param resultArr 二维数组
	 * @return Markdown 表格字符串
	 */
	public static String generateTable(String[][] resultArr) {
		if (resultArr == null || resultArr.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		// 头部
		sb.append("| ");
		for (String col : resultArr[0]) {
			sb.append(col).append(" | ");
		}
		sb.append("\n");

		// 分隔线
		sb.append("|---".repeat(resultArr[0].length)).append("|\n");

		// 数据行
		for (int i = 1; i < resultArr.length; i++) {
			sb.append("| ");
			for (String cell : resultArr[i]) {
				sb.append(cell).append(" | ");
			}
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * 将 ResultSetBO 转为 Markdown 表格
	 * @param resultSetBO 结构化数据
	 * @return Markdown 表格字符串
	 */
	public static String generateTable(ResultSetBO resultSetBO) {
		List<String> column = resultSetBO.getColumn();
		List<Map<String, String>> data = resultSetBO.getData();

		String[][] resultArr = new String[data.size() + 1][column.size()];
		int idxR = 0;

		resultArr[idxR++] = column.toArray(new String[0]);

		for (Map<String, String> kv : data) {
			String[] row = new String[column.size()];
			int idxC = 0;
			for (String c : column) {
				row[idxC++] = kv.get(c);
			}
			resultArr[idxR++] = row;
		}

		return generateTable(resultArr);
	}

}
