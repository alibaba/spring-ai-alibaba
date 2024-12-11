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

package com.alibaba.cloud.ai.plugin.github.service;

import com.alibaba.cloud.ai.plugin.github.GithubProperties;
import com.alibaba.cloud.ai.plugin.github.entity.Request;
import com.alibaba.cloud.ai.plugin.github.entity.Response;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonClassDescription("Get issue operation")
public class GetIssueService extends GithubService {

	private static final Logger logger = LoggerFactory.getLogger(GetIssueService.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public GetIssueService(GithubProperties properties) {
		super(properties);
	}

	@Override
	public Response apply(Request request) {
		String url = GITHUB_API_URL + REPO_ENDPOINT + ISSUES_ENDPOINT + "/{issueNumber}";
		Mono<String> responseMono = webClient.get()
			.uri(url, properties.getOwner(), properties.getRepository(), request.issueNumber())
			.retrieve()
			.bodyToMono(String.class);
		String responseData = responseMono.block();
		logger.info("GetIssueOperation response: {}", responseData);
		try {
			return new Response<>(parseIssueDetails(responseData));
		}
		catch (IOException e) {
			logger.error("Error occurred while parsing response: {}", e.getMessage());
			throw new RuntimeException(e);
		}

	}

	public static GetIssueService.Issue parseIssueDetails(String json) throws IOException {
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
		return new GetIssueService.Issue(id, title, body, state, userLogin, labels, assignees, createdAt, updatedAt,
				closedAt, closedBy, comments, htmlUrl);
	}

	public record Issue(long id, String title, String body, String state, String userLogin, List<String> labels,
			List<String> assignees, String createdAt, String updatedAt, String closedAt, String closedBy, int comments,
			String htmlUrl

	) {
	}

}
