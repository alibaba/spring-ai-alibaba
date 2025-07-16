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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX;

public final class YuqueConstants {

	public static final String CONFIG_PREFIX = TOOL_CALLING_CONFIG_PREFIX + ".yuque";

	public static final String TOKEN_ENV = "YUQUE_TOKEN";

	public static final String CREATE_BOOK_TOOL_NAME = "createYuqueBook";

	public static final String QUERY_BOOK_TOOL_NAME = "queryYuqueBook";

	public static final String UPDATE_BOOK_TOOL_NAME = "updateBookService";

	public static final String DELETE_BOOK_TOOL_NAME = "deleteBookService";

	public static final String CREATE_DOC_TOOL_NAME = "createYuqueDoc";

	public static final String QUERY_DOC_TOOL_NAME = "queryYuqueDoc";

	public static final String UPDATE_DOC_TOOL_NAME = "updateDocService";

	public static final String DELETE_DOC_TOOL_NAME = "deleteDocService";

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DocSerializer(@JsonProperty("id") String id, @JsonProperty("type") String type,
			@JsonProperty("slug") String slug, @JsonProperty("title") String title,
			@JsonProperty("description") String description, @JsonProperty("cover") String cover,
			@JsonProperty("user_id") String userId, @JsonProperty("book_id") String bookId,
			@JsonProperty("last_editor_id") String lastEditorId, @JsonProperty("format") String format,
			@JsonProperty("body_draft") String bodyDraft, @JsonProperty("body") String body,
			@JsonProperty("body_sheet") String bodySheet, @JsonProperty("body_table") String bodyTable,
			@JsonProperty("body_html") String bodyHtml, @JsonProperty("body_lake") String bodyLake,
			@JsonProperty("public") Integer isPublic, @JsonProperty("status") String status,
			@JsonProperty("likes_count") String likesCount, @JsonProperty("read_count") String readCount,
			@JsonProperty("comments_count") String commentsCount, @JsonProperty("word_count") String wordCount,
			@JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
			@JsonProperty("content_updated_at") String contentUpdatedAt, @JsonProperty("_serializer") String serializer,
			@JsonProperty("published_at") String publishedAt,
			@JsonProperty("first_published_at") String firstPublishedAt, @JsonProperty("user") UserSerializer user,
			@JsonProperty("book") BookSerializer book, @JsonProperty("creator") UserSerializer creator,
			@JsonProperty("tags") List<TagSerializer> tags, @JsonProperty("latest_version_id") String latestVersionId) {
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	protected record UserSerializer(@JsonProperty("id") String id, @JsonProperty("login") String login,
			@JsonProperty("name") String name, @JsonProperty("avatar_url") String avatarUrl,
			@JsonProperty("books_count") String booksCount, @JsonProperty("public_books_count") String publicBooksCount,
			@JsonProperty("followers_count") String followersCount,
			@JsonProperty("following_count") String following_count, @JsonProperty("public") Integer isPublic,
			@JsonProperty("description") String description, @JsonProperty("created_at") String createdAt,
			@JsonProperty("updated_at") String updatedAt) {
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	protected record BookSerializer(@JsonProperty("id") String id, @JsonProperty("type") String type,
			@JsonProperty("slug") String slug, @JsonProperty("name") String name,
			@JsonProperty("description") String description, @JsonProperty("creator_id") String creatorId,
			@JsonProperty("user_id") String userId, @JsonProperty("public") Integer isPublic,
			@JsonProperty("items_count") String itemsCount, @JsonProperty("user") UserSerializer user,
			@JsonProperty("namespace") String namespace, @JsonProperty("likes_count") String likesCount,
			@JsonProperty("watches_count") String watchesCount,
			@JsonProperty("content_updated_at") String contentUpdatedAt, @JsonProperty("created_at") String createdAt,
			@JsonProperty("updated_at") String updatedAt) {
	}

	// Used to retrieve specific property values from JSON.
	@JsonIgnoreProperties(ignoreUnknown = true)
	protected record TagSerializer(@JsonProperty("id") String id, @JsonProperty("title") String title,
			@JsonProperty("doc_id") String docId, @JsonProperty("book_id") String bookId,
			@JsonProperty("user_id") String userId, @JsonProperty("created_at") String createdAt,
			@JsonProperty("updated_at") String updatedAt) {
	}

}
