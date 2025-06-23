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
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultSetBuilder {

	public static ResultSetBO buildFrom(ResultSet rs, String schema) throws SQLException {
		ResultSetMetaData data = rs.getMetaData();
		int columnsCount = data.getColumnCount();
		ResultSetBO resultSetBO = new ResultSetBO();
		String[] rowHead = new String[columnsCount];

		for (int i = 1; i <= columnsCount; i++) {
			rowHead[i - 1] = data.getColumnLabel(i);
		}

		List<Map<String, String>> resultSetData = Lists.newArrayList();
		int count = 0;

		while (rs.next() && count < SqlExecutor.RESULT_SET_LIMIT) {
			Map<String, String> kv = new HashMap<>();
			for (String h : rowHead) {
				kv.put(h, rs.getString(h) == null ? "" : rs.getString(h));
			}
			resultSetData.add(kv);
			count++;
		}

		// 清洗列名
		List<String> cleanedHead = cleanColumnNames(Arrays.asList(rowHead));
		List<Map<String, String>> cleanedData = cleanResultSet(resultSetData);

		resultSetBO.setColumn(cleanedHead);
		resultSetBO.setData(cleanedData);

		return resultSetBO;
	}

	private static List<String> cleanColumnNames(List<String> columnNames) {
		return columnNames.stream().map(name -> StringUtils.remove(StringUtils.remove(name, "`"), "\"")).toList();
	}

	private static List<Map<String, String>> cleanResultSet(List<Map<String, String>> data) {
		return data.stream().map(row -> {
			Map<String, String> cleanedRow = new HashMap<>();
			row.forEach((k, v) -> {
				String cleanedKey = StringUtils.remove(k, "`");
				cleanedKey = StringUtils.remove(cleanedKey, "\"");
				cleanedRow.put(cleanedKey, v);
			});
			return cleanedRow;
		}).toList();
	}

}
