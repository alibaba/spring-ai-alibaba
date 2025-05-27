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

package com.alibaba.cloud.ai.mcp.nacos.registry.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Sunrisea
 */
public class JsonSchemaUtils {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static boolean compare(String origin, String target) {
		try {
			JsonNode originNode = objectMapper.readTree(origin);
			JsonNode targetNode = objectMapper.readTree(target);
			return compare(originNode, targetNode);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean compare(JsonNode originNode, JsonNode targetNode) {
		if (originNode == null && targetNode == null) {
			return true;
		}
		if (originNode == null || targetNode == null) {
			return false;
		}
		JsonNode originProperties = originNode.get("properties");
		JsonNode targetProperties = targetNode.get("properties");

		if (targetProperties == null && originProperties != null) {
			return false;
		}
		else if (targetProperties != null && originProperties == null) {
			return false;
		}
		else if (originProperties != null && targetProperties != null) {
			// 遍历原始 properties
			for (Iterator<Map.Entry<String, JsonNode>> it = originProperties.fields(); it.hasNext();) {
				Map.Entry<String, JsonNode> entry = it.next();
				String key = entry.getKey();
				JsonNode valueNode = entry.getValue();
				if (!valueNode.isObject()) {
					// 只处理 object 类型
					continue;
				}
				JsonNode typeNode = valueNode.get("type");
				if (typeNode == null || !typeNode.isTextual()) {
					continue;
				}
				String type = typeNode.asText();

				if (!targetProperties.has(key)) {
					return false;
				}

				JsonNode targetValueNode = targetProperties.get(key);
				JsonNode targetTypeNode = targetValueNode.get("type");
				String targetType = targetTypeNode != null && targetTypeNode.isTextual() ? targetTypeNode.asText() : "";

				if (!type.equals(targetType)) {
					return false;
				}

				// 如果是 object 类型，递归比较
				if ("object".equals(type)) {
					if (!compare(valueNode, targetValueNode)) {
						return false;
					}
				}
				// 如果是 array 类型，比较 items 内容
				else if ("array".equals(type)) {
					JsonNode originItems = valueNode.get("items");
					JsonNode targetItems = targetValueNode.get("items");
					if (originItems != null && targetItems != null) {
						if (!compare(originItems, targetItems)) {
							return false;
						}
					}
				}
			}

			// 检查新增字段
			for (Iterator<Map.Entry<String, JsonNode>> it = targetProperties.fields(); it.hasNext();) {
				Map.Entry<String, JsonNode> entry = it.next();
				String key = entry.getKey();
				if (!originProperties.has(key)) {
					return false;
				}
			}
		}

		JsonNode originRequired = originNode.get("required");
		JsonNode targetRequired = targetNode.get("required");

		// 一方存在 required，另一方不存在
		if (originRequired != null && targetRequired != null) {
			if (!originRequired.isArray() || !targetRequired.isArray()) {
				// 类型不对
				return false;
			}
			if (originRequired.size() != targetRequired.size()) {
				// 数量不同
				return false;
			}
			// 使用 Set 确保字段顺序不影响比较结果
			Set<String> originSet = new HashSet<>();
			for (JsonNode node : originRequired) {
				if (node.isTextual()) {
					originSet.add(node.asText());
				}
				else {
					// 非字符串类型视为不匹配
					return false;
				}
			}
			Set<String> targetSet = new HashSet<>();
			for (JsonNode node : targetRequired) {
				if (node.isTextual()) {
					targetSet.add(node.asText());
				}
				else {
					// 非字符串类型视为不匹配
					return false;
				}
			}
			return originSet.equals(targetSet);
		}
		else {
			return originRequired == null && targetRequired == null;
		}
	}

}
