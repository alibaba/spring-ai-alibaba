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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@JsonClassDescription("Search repositories operation")
public class SearchRepositoryService implements Function<Request, Response> {

	private static final String GITHUB_API_URL = "https://api.github.com";

	private static final String SEARCH_REPOS_ENDPOINT = "/search/repositories";

	private static final String DEFAULT_PER_PAGE = "10";

	private static final Logger logger = LoggerFactory.getLogger(SearchRepositoryService.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final WebClient webClient;

	private final GithubToolKitProperties properties;

	public SearchRepositoryService(GithubToolKitProperties properties) {
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
		String baseUrl = GITHUB_API_URL + SEARCH_REPOS_ENDPOINT;
		URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
			.queryParam("q", request.query())
			.queryParam("per_page", DEFAULT_PER_PAGE)
			.queryParam("sort", request.sort() != null ? request.sort() : "best match")
			.queryParam("order", request.order() != null ? request.order() : "desc")
			.build(properties.getOwner(), properties.getRepository());
		try {
			Mono<String> responseMono = webClient.get().uri(uri).retrieve().bodyToMono(String.class);
			String responseData = responseMono.block();
			logger.info("SearchRepositoriesOperation success");

			return new Response<>(parseRepositorySearchResults(responseData));
		}
		catch (IOException e) {
			logger.error("Error occurred while parsing response: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public static List<Repository> parseRepositorySearchResults(String json) throws IOException {
		JsonNode rootNode = objectMapper.readTree(json);
		JsonNode itemsNode = rootNode.get("items");
		List<Repository> repositories = new ArrayList<>();

		if (itemsNode != null && itemsNode.isArray()) {
			for (JsonNode itemNode : itemsNode) {
				long id = itemNode.get("id").asLong();
				String name = itemNode.get("name").asText();
				String fullName = itemNode.get("full_name").asText();
				String description = itemNode.has("description") && !itemNode.get("description").isNull()
						? itemNode.get("description").asText() : null;
				String htmlUrl = itemNode.get("html_url").asText();
				int stargazersCount = itemNode.get("stargazers_count").asInt();
				int forksCount = itemNode.get("forks_count").asInt();
				String language = itemNode.has("language") && !itemNode.get("language").isNull()
						? itemNode.get("language").asText() : null;

				repositories.add(new Repository(id, name, fullName, description, htmlUrl, stargazersCount, forksCount,
						language));
			}
		}

		return repositories;
	}

	public record Repository(long id, String name, String fullName, String description, String htmlUrl,
			int stargazersCount, int forksCount, String language) {
	}

}
