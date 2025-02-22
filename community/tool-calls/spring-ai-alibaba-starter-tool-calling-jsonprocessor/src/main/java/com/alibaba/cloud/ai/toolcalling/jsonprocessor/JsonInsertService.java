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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.util.Assert;

import java.util.function.Function;

/**
 * @author 北极星
 */
public class JsonInsertService implements Function<JsonInsertService.JsonInsertRequest, Object> {

	@Override
	public Object apply(JsonInsertRequest request) {
		String content = request.content;
		String field = request.field;
		JsonElement value = request.value;
		JsonElement jsonElement = JsonParser.parseString(content);
		if (!jsonElement.isJsonObject())
			throw new IllegalArgumentException("Content is not a valid JSON object .");
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		Assert.notNull(field, "insert json field can not be null");
		Assert.notNull(value, "insert json fieldValue can not be null");
		jsonObject.add(field, value);
		return jsonObject;
	}

	record JsonInsertRequest(@JsonProperty("content") String content, @JsonProperty("field") String field,
			@JsonProperty("value") JsonElement value) {
	}

}
