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
package com.alibaba.cloud.ai.example.manus.dynamic.model.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dahua
 * @date 2025/7/12 13:02
 */
@Converter
public class MapToStringConverter implements AttributeConverter<Map<String, String>, String> {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Map<String, String> attribute) {
		try {
			return objectMapper.writeValueAsString(attribute);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Error converting map to string", e);
		}
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String dbData) {
		// 增加一个 null 或空字符串的检查
		if (dbData == null || dbData.isEmpty()) {
			// 返回一个空的 Map 或者 null，根据业务逻辑决定
			return new HashMap<>();
		}
		try {
			return objectMapper.readValue(dbData, new TypeReference<>() {
			});
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Error converting string to map", e);
		}
	}

}
