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
import java.util.List;
import java.util.function.Function;

/**
 * @author 北极星
 */
public class YuqueQueryBookService
		implements Function<YuqueQueryBookService.queryBookRequest, YuqueQueryBookService.queryBookResponse> {

	private static final Logger logger = LoggerFactory.getLogger(YuqueQueryBookService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public YuqueQueryBookService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public queryBookResponse apply(queryBookRequest queryBookRequest) {
		if (queryBookRequest == null || queryBookRequest.bookId == null) {
			return null;
		}
		String uri = "/repos/" + queryBookRequest.bookId + "/docs";
		try {
			String json = webClientTool.get(uri).block();
			return jsonParseTool.jsonToObject(json, queryBookResponse.class);
		}
		catch (Exception e) {
			logger.error("Failed to query the Yuque book.", e);
			return null;
		}
	}

	protected record queryBookRequest(String bookId) {
	}

	protected record queryBookResponse(@JsonProperty("meta") meta meta,
			@JsonProperty("data") List<YuqueQueryBookService.data> data) {
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	protected record data(@JsonProperty("id") String id, @JsonProperty("type") String type,
			@JsonProperty("slug") String slug, @JsonProperty("title") String title,
			@JsonProperty("description") String description, @JsonProperty("cover") String cover,
			@JsonProperty("user_id") String userId, @JsonProperty("user") YuqueQueryDocService.UserSerializer user,
			@JsonProperty("book_id") String bookId, @JsonProperty("last_editor_id") String lastEditorId,
			@JsonProperty("public") int isPublic, @JsonProperty("status") String status,
			@JsonProperty("likes_count") String likesCount, @JsonProperty("read_count") String readCount,
			@JsonProperty("comments_count") String commentsCount, @JsonProperty("word_count") String wordCount,
			@JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
			@JsonProperty("published_at") String publishedAt,
			@JsonProperty("first_published_at") String firstPublishedAt) {
	}

	protected record meta(@JsonProperty("total") String total) {
	}

}
