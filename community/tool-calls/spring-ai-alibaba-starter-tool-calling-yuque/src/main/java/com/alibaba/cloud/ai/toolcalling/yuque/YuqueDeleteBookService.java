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
 * @author 北极星
 */
public class YuqueDeleteBookService
		implements Function<YuqueDeleteBookService.DeleteBookRequest, YuqueDeleteBookService.DeleteBookResponse> {

	private static final Logger logger = LoggerFactory.getLogger(YuqueDeleteBookService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public YuqueDeleteBookService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public YuqueDeleteBookService.DeleteBookResponse apply(YuqueDeleteBookService.DeleteBookRequest request) {
		if (request == null || request.bookId == null) {
			return null;
		}
		String uri = "/repos/" + request.bookId;
		try {
			String json = webClientTool.delete(uri).block();
			return jsonParseTool.jsonToObject(json, DeleteBookResponse.class);
		}
		catch (Exception e) {
			logger.error("Failed to delete the Yuque book.", e);
			return null;
		}
	}

	public record DeleteBookRequest(@JsonProperty("bookId") String bookId) {
	}

	public record DeleteBookResponse(@JsonProperty("data") YuqueConstants.BookSerializer data) {
	}

}
