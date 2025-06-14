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
public class YuqueDeleteDocService
		implements Function<YuqueDeleteDocService.DeleteDocRequest, YuqueDeleteDocService.DeleteDocResponse> {

	private static final Logger logger = LoggerFactory.getLogger(YuqueDeleteDocService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public YuqueDeleteDocService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public YuqueDeleteDocService.DeleteDocResponse apply(YuqueDeleteDocService.DeleteDocRequest deleteDocRequest) {
		if (deleteDocRequest == null || deleteDocRequest.bookId == null) {
			return null;
		}
		String uri = "/repos/" + deleteDocRequest.bookId + "/docs/" + deleteDocRequest.id;
		try {
			String json = webClientTool.delete(uri).block();
			return jsonParseTool.jsonToObject(json, DeleteDocResponse.class);
		}
		catch (Exception e) {
			logger.error("Failed to delete the Yuque document.", e);
			return null;
		}
	}

	public record DeleteDocRequest(@JsonProperty("bookId") String bookId, @JsonProperty("id") String id) {
	}

	public record DeleteDocResponse(@JsonProperty("data") YuqueConstants.DocSerializer data) {
	}

}
