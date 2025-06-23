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

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GetIssueService implements Function<GetIssueService.Request, Response> {

	private static final String REPO_ENDPOINT = "/repos/{owner}/{repo}";

	private static final String ISSUES_ENDPOINT = "/issues";

	private static final Logger logger = LoggerFactory.getLogger(GetIssueService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final GithubToolKitProperties properties;

	public GetIssueService(GithubToolKitProperties properties, WebClientTool webClientTool,
			JsonParseTool jsonParseTool) {
		assert properties.getToken() != null && properties.getToken().length() == 40;
		this.properties = properties;
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public Response apply(Request request) {
		try {
			String endpoint = REPO_ENDPOINT + ISSUES_ENDPOINT + "/{issueNumber}";
			String responseData = webClientTool.get(endpoint, Map.of("owner", properties.getOwner(), "repo",
					properties.getRepository(), "issueNumber", request.issueNumber()))
				.block();
			logger.info("GetIssueOperation response: {}", responseData);
			return new Response<>(parseIssueDetails(responseData));
		}
		catch (IOException e) {
			logger.error("Error occurred while parsing response: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public Issue parseIssueDetails(String json) throws JsonProcessingException {
		long id = Long.parseLong(jsonParseTool.getFieldValueAsString(json, "id"));
		String title = jsonParseTool.getFieldValueAsString(json, "title").replaceAll("\"", "");
		String state = jsonParseTool.getFieldValueAsString(json, "state").replaceAll("\"", "");
		String createdAt = jsonParseTool.getFieldValueAsString(json, "created_at").replaceAll("\"", "");
		String updatedAt = jsonParseTool.getFieldValueAsString(json, "updated_at").replaceAll("\"", "");
		int comments = Integer.parseInt(jsonParseTool.getFieldValueAsString(json, "comments"));
		String htmlUrl = jsonParseTool.getFieldValueAsString(json, "html_url").replaceAll("\"", "");
		String body = jsonParseTool.getFieldValueAsString(json, "body").replaceAll("\"", "");
		String closedAt = jsonParseTool.getFieldValueAsString(json, "closed_at").replaceAll("\"", "");

		String userLogin = jsonParseTool.getDepthFieldValueAsString(json, "user", "login").replaceAll("\"", "");
		String closedBy = jsonParseTool.getDepthFieldValueAsString(json, "closed_by", "login").replaceAll("\"", "");

		List<String> labels = new ArrayList<>();
		try {
			List<Map<String, Object>> labelObjects = jsonParseTool.getFieldValue(json,
					new TypeReference<List<Map<String, Object>>>() {
					}, "labels");

			if (labelObjects != null) {
				labels = labelObjects.stream()
					.map(labelMap -> (String) labelMap.get("name"))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			}
		}
		catch (Exception e) {
			logger.warn("Failed to parse labels from JSON: {}", e.getMessage());
			labels = new ArrayList<>();
		}

		List<String> assignees = new ArrayList<>();
		try {
			List<Map<String, Object>> assigneeObjects = jsonParseTool.getFieldValue(json,
					new TypeReference<List<Map<String, Object>>>() {
					}, "assignees");

			if (assigneeObjects != null) {
				assignees = assigneeObjects.stream()
					.map(assigneeMap -> (String) assigneeMap.get("login"))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			}
		}
		catch (Exception e) {
			logger.warn("Failed to parse assignees from JSON: {}", e.getMessage());
			assignees = new ArrayList<>();
		}

		return new Issue(id, title, body, state, userLogin, labels, assignees, createdAt, updatedAt, closedAt, closedBy,
				comments, htmlUrl);
	}

	public record Issue(long id, String title, String body, String state, String userLogin, List<String> labels,
			List<String> assignees, String createdAt, String updatedAt, String closedAt, String closedBy, int comments,
			String htmlUrl) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("GitHub Issue request")
	public record Request(@JsonProperty(required = true,
			value = "issueNumber") @JsonPropertyDescription("The number of the issue, which is used to get details about the issue or to leave a comment") Integer issueNumber) {
	}

}
