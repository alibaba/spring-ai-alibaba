/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.studio.admin.builder.generator.utils;

/**
 * 代码生成工具类，提供代码生成过程中常用的字符串处理和类型转换方法
 *
 * @author yHong
 * @version 1.0
 * @since 2025/9/10 21:30
 */
public final class CodeGenUtils {

	private CodeGenUtils() {
	}

	/**
	 * null 值转空字符串
	 * @param s 输入字符�?
	 * @return 非空字符�?
	 */
	public static String nvl(String s) {
		return s == null ? "" : s;
	}

	/**
	 * 转义字符串用�?Java 代码生成�?
	 * 主要用于生成 Java 字符串字面量，会转义反斜杠和双引号�?
	 *
	 * @param s 输入字符�?
	 * @return 转义后的字符�?
	 */
	public static String esc(String s) {
		return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	/**
	 * 对象转字符串
	 * @param o 输入对象
	 * @return 字符串表示，null 对象返回 null
	 */
	public static String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}

	/**
	 * 对象转整数（安全转换）�?
	 *
	 * @param v 输入�?
	 * @return 整数值，无法转换时返�?null
	 */
	public static Integer toInt(Object v) {
		if (v instanceof Integer i) {
			return i;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof String s) {
			try {
				return Integer.parseInt(s.trim());
			}
			catch (Exception ignore) {
				// 忽略解析异常，返�?null
			}
		}
		return null;
	}

	/**
	 * 检查字符串是否为空（null �?空白�?
	 * @param s 输入字符�?
	 * @return true 如果字符串为 null 或只包含空白字符
	 */
	public static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

}
