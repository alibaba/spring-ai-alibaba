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

	protected record queryDocRequest(String bookId, String id) {
	}

	protected record queryDocResponse(@JsonProperty("data") YuqueQueryDocService.data data) {
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	protected record data(@JsonProperty("id") String id, @JsonProperty("type") String type,
			@JsonProperty("slug") String slug, @JsonProperty("title") String title,
			@JsonProperty("description") String description, @JsonProperty("cover") String cover,
			@JsonProperty("user_id") String userId, @JsonProperty("book_id") String bookId,
			@JsonProperty("last_editor_id") String lastEditorId, @JsonProperty("format") String format,
			@JsonProperty("body_draft") String bodyDraft, @JsonProperty("body") String body,
			@JsonProperty("body_html") String bodyHtml, @JsonProperty("body_lake") String bodyLake,
			@JsonProperty("public") int isPublic, @JsonProperty("status") String status,
			@JsonProperty("likes_count") String likesCount, @JsonProperty("read_count") String readCount,
			@JsonProperty("comments_count") String commentsCount, @JsonProperty("word_count") String wordCount,
			@JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
			@JsonProperty("content_updated_at") String contentUpdatedAt, @JsonProperty("_serializer") String serializer,
			@JsonProperty("published_at") String publishedAt,
			@JsonProperty("first_published_at") String firstPublishedAt, @JsonProperty("user") UserSerializer user,
			@JsonProperty("book") BookSerializer book) {
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	protected record UserSerializer(@JsonProperty("id") String id, @JsonProperty("type") String type,
			@JsonProperty("login") String login, @JsonProperty("name") String name,
			@JsonProperty("avatar_url") String avatarUrl, @JsonProperty("followers_count") int followersCount,
			@JsonProperty("following_count") int following_count, @JsonProperty("public") int isPublic,
			@JsonProperty("description") String description, @JsonProperty("created_at") String createdAt,
			@JsonProperty("updated_at") String updatedAt, @JsonProperty("work_id") String workId,
			@JsonProperty("_serializer") String serializer) {
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	protected record BookSerializer(@JsonProperty("id") String id, @JsonProperty("type") String type,
			@JsonProperty("slug") String slug, @JsonProperty("name") String name,
			@JsonProperty("description") String description, @JsonProperty("creator_id") String creatorId,
			@JsonProperty("user_id") String userId, @JsonProperty("public") int isPublic,
			@JsonProperty("items_count") int itemsCount, @JsonProperty("user") UserSerializer user,
			@JsonProperty("namespace") String namespace, @JsonProperty("likes_count") String likesCount,
			@JsonProperty("watches_count") String watchesCount, @JsonProperty("_serializer") String serializer) {
	}

}
