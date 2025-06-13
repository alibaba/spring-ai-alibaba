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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * @author 北极星
 */
public class YuqueDeleteDocService
		implements Function<YuqueDeleteDocService.deleteDocRequest, YuqueDeleteDocService.deleteDocResponse> {

	private static final Logger logger = LoggerFactory.getLogger(YuqueDeleteDocService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public YuqueDeleteDocService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public YuqueDeleteDocService.deleteDocResponse apply(YuqueDeleteDocService.deleteDocRequest deleteDocRequest) {
		if (deleteDocRequest == null || deleteDocRequest.bookId == null) {
			return null;
		}
		String uri = "/repos/" + deleteDocRequest.bookId + "/docs/" + deleteDocRequest.id;
		try {
			String json = webClientTool.delete(uri).block();
			return jsonParseTool.jsonToObject(json, deleteDocResponse.class);
		}
		catch (Exception e) {
			logger.error("Failed to delete the Yuque document.", e);
			return null;
		}
	}

	protected record deleteDocRequest(@JsonProperty("bookId") String bookId, @JsonProperty("id") String id) {
	}

	protected record deleteDocResponse(@JsonProperty("data") YuqueDeleteDocService.data data) {
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	protected record data(@JsonProperty("id") String id, @JsonProperty("slug") String slug,
			@JsonProperty("type") String type, @JsonProperty("description") String description,
			@JsonProperty("cover") String cover, @JsonProperty("user_id") String user_id,
			@JsonProperty("book_id") String book_id, @JsonProperty("last_editor_id") String last_editor_id,
			@JsonProperty("format") String format, @JsonProperty("body_draft") String body_draft,
			@JsonProperty("body_sheet") String body_sheet, @JsonProperty("body_table") String body_table,
			@JsonProperty("body_html") String body_html, @JsonProperty("public") int isPublic,
			@JsonProperty("status") String status, @JsonProperty("likes_count") int likes_count,
			@JsonProperty("read_count") int read_count, @JsonProperty("comments_count") int comments_count,
			@JsonProperty("word_count") int word_count, @JsonProperty("created_at") String createdAt,
			@JsonProperty("updated_at") String updatedAt) {
	}

}
