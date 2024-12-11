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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@JsonClassDescription("Get Issues operation")
public class GetIssuesService extends GithubService {

	private static final Logger logger = LoggerFactory.getLogger(GetIssuesService.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public GetIssuesService(GithubProperties properties) {
		super(properties);
	}

	@Override
	public Response apply(Request request) {
		String baseUrl = GITHUB_API_URL + REPO_ENDPOINT + ISSUES_ENDPOINT;
		URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
			.queryParam("per_page", DEFAULT_PER_PAGE)
			.build(properties.getOwner(), properties.getRepository());
		try {
			Mono<String> responseMono = webClient.get().uri(uri).retrieve().bodyToMono(String.class);
			String responseData = responseMono.block();
			logger.info("Response: {}", responseData);
			return new Response<>(parseIssues(responseData));
		}
		catch (IOException e) {
			logger.error("Error parsing response: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public static List<Issues> parseIssues(String json) throws IOException {
		JsonNode issuesNode = objectMapper.readTree(json);
		List<GetIssuesService.Issues> issues = new ArrayList<>();

		for (JsonNode issueNode : issuesNode) {
			long id = issueNode.get("id").asLong();
			Integer number = issueNode.get("number").asInt();
			String title = issueNode.get("title").asText();
			String state = issueNode.get("state").asText();
			String userLogin = issueNode.get("user").get("login").asText();
			String createdAt = issueNode.get("created_at").asText();
			int comments = issueNode.get("comments").asInt();

			GetIssuesService.Issues issue = new GetIssuesService.Issues(id, number, title, state, userLogin, createdAt,
					comments);
			issues.add(issue);
		}

		return issues;
	}

	public record Issues(long id, Integer issueNumber, String title, String state, String userLogin, String createdAt,
			int comments) {
	}

}
