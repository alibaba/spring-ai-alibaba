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
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@JsonClassDescription("Get issue operation")
public class GetIssueService implements Function<Request, Response> {

	private static final String GITHUB_API_URL = "https://api.github.com";

	private static final String REPO_ENDPOINT = "/repos/{owner}/{repo}";

	private static final String ISSUES_ENDPOINT = "/issues";

	private static final Logger logger = LoggerFactory.getLogger(GetIssueService.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final WebClient webClient;

	private final GithubToolKitProperties properties;

	public GetIssueService(GithubToolKitProperties properties) {
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
			String url = GITHUB_API_URL + REPO_ENDPOINT + ISSUES_ENDPOINT + "/{issueNumber}";
			Mono<String> responseMono = webClient.get()
				.uri(url, properties.getOwner(), properties.getRepository(), request.issueNumber())
				.retrieve()
				.bodyToMono(String.class);
			String responseData = responseMono.block();
			logger.info("GetIssueOperation response: {}", responseData);
			return new Response<>(parseIssueDetails(responseData));
		}
		catch (IOException e) {
			logger.error("Error occurred while parsing response: {}", e.getMessage());
			throw new RuntimeException(e);
		}

	}

	public static Issue parseIssueDetails(String json) throws IOException {
		JsonNode issueNode = objectMapper.readTree(json);

		long id = issueNode.get("id").asLong();
		String title = issueNode.get("title").asText();
		String state = issueNode.get("state").asText();
		String userLogin = issueNode.get("user").get("login").asText();

		JsonNode labelsNode = issueNode.get("labels");
		List<String> labels = new ArrayList<>();
		if (labelsNode != null && labelsNode.isArray()) {
			for (JsonNode labelNode : labelsNode) {
				String name = labelNode.get("name").asText();
				labels.add(name);
			}
		}
		JsonNode assigneesNode = issueNode.get("assignees");
		List<String> assignees = new ArrayList<>();
		if (assigneesNode != null && assigneesNode.isArray()) {
			for (JsonNode assigneeNode : assigneesNode) {
				String assigneeLogin = assigneeNode.get("login").asText();
				assignees.add(assigneeLogin);
			}
		}
		String createdAt = issueNode.get("created_at").asText();
		String updatedAt = issueNode.get("updated_at").asText();
		String closedAt = issueNode.has("closed_at") && !issueNode.get("closed_at").isNull()
				? issueNode.get("closed_at").asText() : null;
		String closedBy = issueNode.has("closed_by") && !issueNode.get("closed_by").isNull()
				? issueNode.get("closed_by").get("login").asText() : null;
		int comments = issueNode.get("comments").asInt();
		String htmlUrl = issueNode.get("html_url").asText();
		String body = issueNode.has("body") && !issueNode.get("body").isNull() ? issueNode.get("body").asText() : null;
		return new Issue(id, title, body, state, userLogin, labels, assignees, createdAt, updatedAt, closedAt, closedBy,
				comments, htmlUrl);
	}

	public record Issue(long id, String title, String body, String state, String userLogin, List<String> labels,
			List<String> assignees, String createdAt, String updatedAt, String closedAt, String closedBy, int comments,
			String htmlUrl

	) {
	}

}
