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
package com.alibaba.cloud.ai.toolcalling.jsonprocessor;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * @author 北极星
 */
public class JsonProcessorRemoveService implements Function<JsonProcessorRemoveService.JsonRemoveRequest, Object> {

	private final JsonParseTool jsonParseTool;

	private static final Logger logger = LoggerFactory.getLogger(JsonProcessorRemoveService.class);

	public JsonProcessorRemoveService(JsonParseTool jsonParseTool) {
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public Object apply(JsonRemoveRequest request) {
		String content = request.content;
		String field = request.field;
		try {
			return jsonParseTool.removeFieldValue(content, field);
		}
		catch (JsonProcessingException e) {
			logger.error("Error occurred while json processing: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@JsonClassDescription("JsonProcessorRemoveService request")
	record JsonRemoveRequest(@JsonProperty("content") String content, @JsonProperty("value") String field) {
	}

}
