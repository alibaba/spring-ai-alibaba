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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static com.alibaba.cloud.ai.toolcalling.yuque.YuqueProperties.BASE_URL;

/**
 * @author 北极星
 */
public class YuqueUpdateDocService
		implements Function<YuqueUpdateDocService.updateDocRequest, YuqueUpdateDocService.updateDocResponse> {

	private final WebClient webClient;

	public YuqueUpdateDocService(YuqueProperties yuqueProperties) {
		this.webClient = WebClient.builder()
			.baseUrl(BASE_URL)
			.defaultHeader("X-Auth-Token", yuqueProperties.getAuthToken())
			.build();

	}

	@Override
	public YuqueUpdateDocService.updateDocResponse apply(YuqueUpdateDocService.updateDocRequest updateDocRequest) {
		Mono<YuqueUpdateDocService.updateDocResponse> updateDocResponseMono = webClient.method(HttpMethod.PUT)
			.uri("/{book_id}/docs/{id}", updateDocRequest.bookId, updateDocRequest.id)
			.retrieve()
			.bodyToMono(YuqueUpdateDocService.updateDocResponse.class);

		return updateDocResponseMono.block();
	}

	protected record updateDocRequest(@JsonProperty("bookId") String bookId, @JsonProperty("id") String id) {
	}

	protected record updateDocResponse(@JsonProperty("id") String id, @JsonProperty("slug") String slug,
			@JsonProperty("type") String type, @JsonProperty("description") String description,
			@JsonProperty("cover") String cover, @JsonProperty("user_id") String userId,
			@JsonProperty("book_id") String bookId, @JsonProperty("last_editor_id") String lastEditorId,
			@JsonProperty("format") String format, @JsonProperty("body_draft") String bodyDraft,
			@JsonProperty("body_sheet") String bodySheet, @JsonProperty("body_table") String bodyTable,
			@JsonProperty("body_html") String bodyHtml, @JsonProperty("public") int isPublic,
			@JsonProperty("status") String status, @JsonProperty("likes_count") int likesCount,
			@JsonProperty("read_count") int readCount, @JsonProperty("comments_count") int commentsCount,
			@JsonProperty("word_count") int wordCount, @JsonProperty("created_at") String createdAt,
			@JsonProperty("updated_at") String updatedAt) {
	}

}
