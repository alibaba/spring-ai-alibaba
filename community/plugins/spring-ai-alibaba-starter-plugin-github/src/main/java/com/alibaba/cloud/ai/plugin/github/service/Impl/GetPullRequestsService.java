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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@JsonClassDescription("Get all pull request information for the repository.")
public class GetPullRequestsService extends GithubService {

	private static final Logger logger = LoggerFactory.getLogger(GetPullRequestsService.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public GetPullRequestsService(GithubProperties properties) {
		super(properties);
	}

	@Override
	public Response apply(Request request) {
		String baseUrl = GITHUB_API_URL + REPO_ENDPOINT + PULL_REQUESTS_ENDPOINT;
		URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
			.queryParam("state", request.pullRequestState())
			.queryParam("per_page", DEFAULT_PER_PAGE)
			.build(properties.getOwner(), properties.getRepository());
		try {
			Mono<String> responseMono = webClient.get().uri(uri).retrieve().bodyToMono(String.class);
			String responseData = responseMono.block();
			logger.info("Response: {}", responseData);
			return new Response<>(parsePullRequests(responseData));
		}
		catch (IOException e) {
			logger.error("Error occurred while parsing response: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public static List<PullRequest> parsePullRequests(String json) throws IOException {
		JsonNode pullsNode = objectMapper.readTree(json);
		List<PullRequest> pullRequests = new ArrayList<>();

		for (JsonNode pullNode : pullsNode) {
			long id = pullNode.get("id").asLong();
			Integer number = pullNode.get("number").asInt();
			String title = pullNode.get("title").asText();
			String state = pullNode.get("state").asText();
			String userLogin = pullNode.get("user").get("login").asText();
			String createdAt = pullNode.get("created_at").asText();
			String updatedAt = pullNode.get("updated_at").asText();

			PullRequest pullRequest = new PullRequest(id, number, title, state, userLogin, createdAt, updatedAt);
			pullRequests.add(pullRequest);
		}

		return pullRequests;
	}

	public record PullRequest(long id, Integer pullRequestNumber, String title, String state, String userLogin,
			String createdAt, String updatedAt) {
	}

}
