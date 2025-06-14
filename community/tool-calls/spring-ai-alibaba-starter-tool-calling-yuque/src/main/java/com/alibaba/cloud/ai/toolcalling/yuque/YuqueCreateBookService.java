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
package com.alibaba.cloud.ai.toolcalling.yuque;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * @author hiriki
 */
public class YuqueCreateBookService
		implements Function<YuqueCreateBookService.CreateBookRequest, YuqueCreateBookService.CreateBookResponse> {

	private static final Logger logger = LoggerFactory.getLogger(YuqueCreateBookService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public YuqueCreateBookService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public CreateBookResponse apply(CreateBookRequest request) {
		if (request.login == null || request.name == null || request.slug == null) {
			return null;
		}
		String uri = "/groups/" + request.login + "/repos";
		try {
			String json = webClientTool.post(uri, request).block();
			return jsonParseTool.jsonToObject(json, CreateBookResponse.class);
		}
		catch (Exception e) {
			logger.error("Failed to create the Yuque book.", e);
			return null;
		}
	}

	public record CreateBookRequest(@JsonProperty("login") String login, @JsonProperty("name") String name,
			@JsonProperty("slug") String slug, @JsonProperty("description") String description,
			@JsonProperty("public") Integer isPublic, @JsonProperty("enhancedPrivacy") Boolean enhancedPrivacy) {
	}

	public record CreateBookResponse(@JsonProperty("data") YuqueConstants.BookSerializer data) {
	}

}
