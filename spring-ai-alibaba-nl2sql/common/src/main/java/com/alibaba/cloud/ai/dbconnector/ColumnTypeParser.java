package com.alibaba.cloud.ai.dbconnector;

public class ColumnTypeParser {

	public static String wrapType(String s) {
		if (s.equalsIgnoreCase("decimal") || s.equalsIgnoreCase("int") || s.equalsIgnoreCase("bigint")
				|| s.equalsIgnoreCase("bool") || s.equalsIgnoreCase("bit") || s.equalsIgnoreCase("boolean")
				|| s.equalsIgnoreCase("double")) {
			return "number";
		}
		else if (s.startsWith("varchar") || s.startsWith("char")) {
			return "text";
		}
		return s;
	}

}