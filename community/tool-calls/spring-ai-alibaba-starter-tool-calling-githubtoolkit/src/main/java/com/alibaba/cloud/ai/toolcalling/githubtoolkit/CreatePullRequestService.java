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
package com.alibaba.cloud.ai.toolcalling.githubtoolkit;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@JsonClassDescription("Create a pull request operation")
public class CreatePullRequestService implements Function<Request, Response> {

	private static final String GITHUB_API_URL = "https://api.github.com";

	private static final String REPO_ENDPOINT = "/repos/{owner}/{repo}";

	protected static final String PULL_REQUESTS_ENDPOINT = "/pulls";

	private static final Logger logger = LoggerFactory.getLogger(CreatePullRequestService.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final WebClient webClient;

	private final GithubToolKitProperties properties;

	public CreatePullRequestService(GithubToolKitProperties properties) {
		assert properties.getToken() != null && properties.getToken().length() == 40;
		this.properties = properties;
		this.webClient = WebClient.builder()
			.defaultHeader(HttpHeaders.USER_AGENT, HttpHeaders.USER_AGENT)
			.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
			.defaultHeader("X-GitHub-Api-Version", GithubToolKitProperties.X_GitHub_Api_Version)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
			.build();
	}

	@Override
	public Response apply(Request request) {
		try {
			String url = GITHUB_API_URL + REPO_ENDPOINT + PULL_REQUESTS_ENDPOINT;
			Map<String, Object> body = new HashMap<>();
			addIfNotNull(body, "title", request.pullRequestTitle());
			addIfNotNull(body, "body", request.pullRequestBody());
			addIfNotNull(body, "head", request.pullRequestHead());
			addIfNotNull(body, "base", request.pullRequestBase());
			addIfNotNull(body, "head_repo", request.headRepo());
			addIfNotNull(body, "issue", request.issue());
			addIfNotNull(body, "draft", request.draft());

			Mono<String> responseMono = webClient.post()
				.uri(url, properties.getOwner(), properties.getRepository())
				.bodyValue(body)
				.retrieve()
				.bodyToMono(String.class);
			String responseData = responseMono.block();
			logger.info("Pull request created successfully.");
			return new Response<>(parsePullRequest(responseData));
		}
		catch (WebClientResponseException e) {
			logger.error("GitHub API error: Status {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new RuntimeException("GitHub API error", e);
		}
		catch (IOException e) {
			logger.error("Error parsing pull request data: {}", e.getMessage());
			throw new RuntimeException("Error parsing response", e);
		}
		catch (Exception e) {
			logger.error("Unexpected error: {}", e.getMessage());
			throw new RuntimeException("Unexpected error", e);
		}
	}

	private void addIfNotNull(Map<String, Object> map, String key, Object value) {
		if (value != null) {
			map.put(key, value);
		}
	}

	public static PullRequest parsePullRequest(String json) throws IOException {
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

		PullRequest pullRequest = new PullRequest(id, title, state, prNumber, userLogin, body, htmlUrl, headRef,
				baseRef);

		return pullRequest;
	}

	public record PullRequest(long id, String title, String state, Integer prNumber, String userLogin, String body,
			String htmlUrl,

			String headRef,

			String baseRef) {
	}

}
