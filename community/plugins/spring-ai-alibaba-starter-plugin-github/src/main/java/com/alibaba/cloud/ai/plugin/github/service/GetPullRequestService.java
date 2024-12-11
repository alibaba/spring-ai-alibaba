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

@JsonClassDescription("get more information about a specific pull request")
public class GetPullRequestService extends GithubService {

	private static final Logger logger = LoggerFactory.getLogger(GetPullRequestService.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public GetPullRequestService(GithubProperties properties) {
		super(properties);
	}

	@Override
	public Response apply(Request request) {
		String url = GITHUB_API_URL + REPO_ENDPOINT + PULL_REQUESTS_ENDPOINT + "/{pullRequestNumber}";
		Mono<String> responseMono = webClient.get()
			.uri(url, properties.getOwner(), properties.getRepository(), request.pullRequestNumber())
			.retrieve()
			.bodyToMono(String.class);
		String responseData = responseMono.block();
		logger.info("GetPullRequestOperation response: {}", responseData);
		try {
			return new Response<>(parsePullRequestDetails(responseData));
		}
		catch (IOException e) {
			logger.error("Error parsing GetPullRequestOperation response: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public static GetPullRequestService.PullRequest parsePullRequestDetails(String json) throws IOException {
		JsonNode pullRequestNode = objectMapper.readTree(json);

		long id = pullRequestNode.get("id").asLong();
		String title = pullRequestNode.get("title").asText();
		String state = pullRequestNode.get("state").asText();
		String userLogin = pullRequestNode.get("user").get("login").asText();

		JsonNode labelsNode = pullRequestNode.get("labels");
		List<String> labels = new ArrayList<>();
		if (labelsNode != null && labelsNode.isArray()) {
			for (JsonNode labelNode : labelsNode) {
				String name = labelNode.get("name").asText();
				labels.add(name);
			}
		}

		JsonNode assigneesNode = pullRequestNode.get("assignees");
		List<String> assignees = new ArrayList<>();
		if (assigneesNode != null && assigneesNode.isArray()) {
			for (JsonNode assigneeNode : assigneesNode) {
				String assigneeLogin = assigneeNode.get("login").asText();
				assignees.add(assigneeLogin);
			}
		}

		String createdAt = pullRequestNode.get("created_at").asText();
		String updatedAt = pullRequestNode.get("updated_at").asText();
		String closedAt = pullRequestNode.has("closed_at") && !pullRequestNode.get("closed_at").isNull()
				? pullRequestNode.get("closed_at").asText() : null;
		String mergedAt = pullRequestNode.has("merged_at") && !pullRequestNode.get("merged_at").isNull()
				? pullRequestNode.get("merged_at").asText() : null;
		String mergeCommitSha = pullRequestNode.has("merge_commit_sha")
				&& !pullRequestNode.get("merge_commit_sha").isNull() ? pullRequestNode.get("merge_commit_sha").asText()
						: null;

		int comments = pullRequestNode.get("comments").asInt();
		String htmlUrl = pullRequestNode.get("html_url").asText();
		String body = pullRequestNode.has("body") && !pullRequestNode.get("body").isNull()
				? pullRequestNode.get("body").asText() : null;

		return new GetPullRequestService.PullRequest(id, title, body, state, userLogin, labels, assignees, createdAt,
				updatedAt, closedAt, mergedAt, mergeCommitSha, comments, htmlUrl);
	}

	public record PullRequest(long id, String title, String body, String state, String userLogin, List<String> labels,
			List<String> assignees, String createdAt, String updatedAt, String closedAt, String mergedAt,
			String mergeCommitSha, int comments, String htmlUrl) {
	}

}
