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
public class YuqueUpdateDocService
		implements Function<YuqueUpdateDocService.updateDocRequest, YuqueUpdateDocService.updateDocResponse> {

	private static final Logger logger = LoggerFactory.getLogger(YuqueUpdateDocService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public YuqueUpdateDocService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public updateDocResponse apply(updateDocRequest updateDocRequest) {
		if (updateDocRequest.bookId == null || updateDocRequest.id == null) {
			return null;
		}
		String uri = "/repos/" + updateDocRequest.bookId + "/docs/" + updateDocRequest.id;
		try {
			String json = webClientTool.put(uri, updateDocRequest).block();
			return jsonParseTool.jsonToObject(json, updateDocResponse.class);
		}
		catch (Exception e) {
			logger.error("Failed to update the Yuque document.", e);
			return null;
		}
	}

	public record updateDocRequest(@JsonProperty("bookId") String bookId, @JsonProperty("id") String id,
			@JsonProperty("slug") String slug, @JsonProperty("title") String title,
			@JsonProperty("public") Integer isPublic, @JsonProperty("format") String format,
			@JsonProperty("body") String body) {
	}

	public record updateDocResponse(@JsonProperty("data") YuqueConstants.DocSerializer data) {
	}

}
