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
package com.alibaba.cloud.ai.service.dsl.serialize;

import com.alibaba.cloud.ai.exception.SerializationException;
import com.alibaba.cloud.ai.service.dsl.Serializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("json")
public class JsonSerializer implements Serializer {

	private ObjectMapper objectMapper;

	public JsonSerializer() {
		objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Override
	public Map<String, Object> load(String s) {
		return objectMapper.convertValue(s, new TypeReference<>() {
		});
	}

	@Override
	public String dump(Map<String, Object> data) {
		String result;
		try {
			result = objectMapper.writeValueAsString(data);
		}
		catch (JsonProcessingException e) {
			throw new SerializationException("failed to dump data" + data, e);
		}
		return result;
	}

}
