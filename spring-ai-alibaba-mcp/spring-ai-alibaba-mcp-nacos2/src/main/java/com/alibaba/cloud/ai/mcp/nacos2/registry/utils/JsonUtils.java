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

package com.alibaba.cloud.ai.mcp.nacos2.registry.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Sunrisea
 */
public class JsonUtils {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	static {
		OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public static String serialize(Object obj) throws JsonProcessingException {
		return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
	}

	/**
	 * 将JSON字符串转换为对象
	 * @param json JSON字符串
	 * @param clazz 目标对象的类类型
	 * @param <T> 目标对象的类型
	 * @return 转换后的对象
	 * @throws JsonProcessingException 如果转换过程中发生错误
	 */
	public static <T> T deserialize(String json, Class<T> clazz) throws JsonProcessingException {
		return OBJECT_MAPPER.readValue(json, clazz);
	}

}
