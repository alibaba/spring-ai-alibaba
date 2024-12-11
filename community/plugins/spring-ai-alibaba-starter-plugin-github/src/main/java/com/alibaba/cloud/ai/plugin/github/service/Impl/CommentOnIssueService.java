/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.plugin.github.service.Impl;

import com.alibaba.cloud.ai.plugin.github.GithubProperties;
import com.alibaba.cloud.ai.plugin.github.entity.Request;
import com.alibaba.cloud.ai.plugin.github.entity.Response;
import com.alibaba.cloud.ai.plugin.github.service.GithubService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

@JsonClassDescription("comment on issue operation")
public class CommentOnIssueService extends GithubService {

	private static final Logger logger = LoggerFactory.getLogger(CommentOnIssueService.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public CommentOnIssueService(GithubProperties properties) {
		super(properties);
	}

	@Override
	public Response apply(Request request) {
		String url = GITHUB_API_URL + REPO_ENDPOINT + ISSUES_ENDPOINT + "/{issueNumber}/comments";
		Map<String, Object> body = Map.of("body", request.comment());

		Mono<String> responseMono = webClient.post()
			.uri(url, properties.getOwner(), properties.getRepository(), request.issueNumber())
			.bodyValue(body)
			.retrieve()
			.bodyToMono(String.class);
		String responseData = responseMono.block();
		logger.info("Comment on issue response: {}", responseData);
		try {
			return new Response<>(parseComment(responseData));
		}
		catch (IOException e) {
			logger.error("Error parsing comment on issue response: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public static CommentOnIssueService.Comment parseComment(String json) throws IOException {
		JsonNode commentNode = objectMapper.readTree(json);

		String htmlUrl = commentNode.get("html_url").asText();
		String body = commentNode.get("body").asText();

		CommentOnIssueService.Comment comment = new CommentOnIssueService.Comment(htmlUrl, body);

		return comment;
	}

	public record Comment(String htmlUrl, String body) {
	}

}
