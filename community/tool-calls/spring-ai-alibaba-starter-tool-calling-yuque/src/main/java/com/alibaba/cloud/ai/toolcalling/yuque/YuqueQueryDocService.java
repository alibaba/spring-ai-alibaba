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
public class YuqueQueryDocService
		implements Function<YuqueQueryDocService.queryDocRequest, YuqueQueryDocService.queryDocResponse> {

	/**
	 * Applies this function to the given argument.
	 * @param queryDocRequest the function argument
	 * @return the function result
	 */
	private static final Logger logger = LoggerFactory.getLogger(YuqueQueryDocService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public YuqueQueryDocService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public queryDocResponse apply(queryDocRequest queryDocRequest) {
		if (queryDocRequest.bookId == null || queryDocRequest.id == null) {
			return null;
		}
		String uri = "/repos/" + queryDocRequest.bookId + "/docs/" + queryDocRequest.id;
		try {
			String json = webClientTool.get(uri).block();
			return jsonParseTool.jsonToObject(json, queryDocResponse.class);
		}
		catch (Exception e) {
			logger.error("Failed to query the Yuque document.", e);
			return null;
		}
	}

	public record queryDocRequest(String bookId, String id) {
	}

	public record queryDocResponse(@JsonProperty("data") YuqueConstants.DocSerializer data) {
	}

}
