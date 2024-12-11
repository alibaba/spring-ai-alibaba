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

@JsonClassDescription("Create a pull request operation")
public class CreatePullRequestService extends GithubService {

	private static final Logger logger = LoggerFactory.getLogger(CreatePullRequestService.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public CreatePullRequestService(GithubProperties properties) {
		super(properties);
	}

	@Override
	public Response apply(Request request) {
		String url = GITHUB_API_URL + REPO_ENDPOINT + PULL_REQUESTS_ENDPOINT;
		Map<String, Object> body = Map.of("title", request.pullRequestTitle(), "body", request.pullRequestBody(),
				"head", request.pullRequestHead(), "base", request.pullRequestBase());

		Mono<String> responseMono = webClient.post()
			.uri(url, properties.getOwner(), properties.getRepository())
			.bodyValue(body)
			.retrieve()
			.bodyToMono(String.class);
		String responseData = responseMono.block();
		logger.info("Pull request created successfully.");
		try {
			return new Response<>(parsePullRequest(responseData));
		}
		catch (IOException e) {
			logger.error("Error parsing pull request data: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public static CreatePullRequestService.PullRequest parsePullRequest(String json) throws IOException {
		JsonNode pullRequestNode = objectMapper.readTree(json);

		long id = pullRequestNode.get("id").asLong();
		String title = pullRequestNode.get("title").asText();
		String state = pullRequestNode.get("state").asText();
		int prNumber = pullRequestNode.get("number").asInt();
		String userLogin = pullRequestNode.get("user").get("login").asText();
		String body = pullRequestNode.get("body").asText();
		String htmlUrl = pullRequestNode.get("html_url").asText();
		String headRef = pullRequestNode.get("head").get("ref").asText();
		String baseRef = pullRequestNode.get("base").get("ref").asText();

		CreatePullRequestService.PullRequest pullRequest = new CreatePullRequestService.PullRequest(id, title, state,
				prNumber, userLogin, body, htmlUrl, headRef, baseRef);

		return pullRequest;
	}

	public record PullRequest(long id, String title, String state, Integer prNumber, String userLogin, String body,
			String htmlUrl,

			String headRef,

			String baseRef) {
	}

}
