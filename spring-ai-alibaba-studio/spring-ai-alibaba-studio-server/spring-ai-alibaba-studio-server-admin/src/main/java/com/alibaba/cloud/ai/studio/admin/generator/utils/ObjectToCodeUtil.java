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

package com.alibaba.cloud.ai.studio.admin.generator.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 定义一些常用对象的构造方法代码
 *
 * @author vlsmb
 * @since 2025/9/5
 */
public final class ObjectToCodeUtil {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private ObjectToCodeUtil() {

	}

	private static String mapToCode(Map<?, ?> map) {
		String elements = map.entrySet()
			.stream()
			.flatMap(e -> Stream.of(e.getKey(), e.getValue()))
			.map(ObjectToCodeUtil::toCode)
			.collect(Collectors.joining(", "));
		return "Map.of(" + elements + ")";
	}

	private static String listToCode(List<?> list) {
		String elements = list.stream().map(ObjectToCodeUtil::toCode).collect(Collectors.joining(", "));
		return "List.of(" + elements + ")";
	}

	public static String toCode(Object object) {
		if (object == null) {
			return "null";
		}
		else if (object instanceof String) {
			try {
				// 尝试使用Jackson打印字符串，以便转义特殊字符，如果失败则进行简单处理
				return objectMapper.writeValueAsString(object.toString());
			}
			catch (Exception e) {
				return "\"" + object.toString()
					.replace("\"", "\\")
					.replace("\n", "\\n")
					.replace("\r", "\\r")
					.replace("\t", "\\t")
					.replace("\b", "\\b") + "\"";
			}
		}
		else if (object instanceof List<?>) {
			return listToCode((List<?>) object);
		}
		else if (object instanceof Map<?, ?>) {
			return mapToCode((Map<?, ?>) object);
		}
		else {
			// 默认使用 toString() 方法
			return object.toString();
		}
	}

}
