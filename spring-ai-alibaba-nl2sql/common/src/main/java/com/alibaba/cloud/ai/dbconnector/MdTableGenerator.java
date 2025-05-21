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