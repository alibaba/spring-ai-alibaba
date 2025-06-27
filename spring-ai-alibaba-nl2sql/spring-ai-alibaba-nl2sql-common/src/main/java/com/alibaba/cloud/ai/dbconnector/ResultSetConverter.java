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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 JDBC ResultSet 转换为结构化数据
 */
public class ResultSetConverter {

	public static List<String[]> convert(ResultSet rs) throws SQLException {
		ResultSetMetaData data = rs.getMetaData();
		int columnsCount = data.getColumnCount();
		List<String[]> list = new ArrayList<>();
		String[] rowHead = new String[columnsCount];

		for (int i = 1; i <= columnsCount; i++) {
			rowHead[i - 1] = data.getColumnLabel(i);
		}

		list.add(rowHead);

		while (rs.next()) {
			String[] rowData = new String[columnsCount];
			int idx = 0;
			for (String head : rowHead) {
				rowData[idx++] = rs.getString(head) == null ? "" : rs.getString(head);
			}
			list.add(rowData);
		}

		return list;
	}

}
