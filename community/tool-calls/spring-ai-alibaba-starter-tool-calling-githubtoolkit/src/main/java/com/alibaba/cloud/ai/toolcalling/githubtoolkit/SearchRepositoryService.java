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
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SearchRepositoryService implements Function<SearchRepositoryService.Request, Response> {

	private static final String SEARCH_REPOS_ENDPOINT = "/search/repositories";

	private static final String DEFAULT_PER_PAGE = "10";

	private static final Logger logger = LoggerFactory.getLogger(SearchRepositoryService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	public SearchRepositoryService(WebClientTool webClientTool, JsonParseTool jsonParseTool) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
	}

	@Override
	public Response apply(Request request) {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("q", request.query());
		params.add("per_page", DEFAULT_PER_PAGE);
		params.add("sort", request.sort() != null ? request.sort() : "best match");
		params.add("order", request.order() != null ? request.order() : "desc");

		try {
			String responseData = webClientTool.get(SEARCH_REPOS_ENDPOINT, params).block();
			logger.info("SearchRepositoriesOperation success");

			return new Response<>(parseRepositorySearchResults(responseData));
		}
		catch (IOException e) {
			logger.error("Error occurred while parsing response: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public List<Repository> parseRepositorySearchResults(String json) throws JsonProcessingException {
		List<Map<String, Object>> itemMaps = jsonParseTool.getFieldValue(json,
				new TypeReference<List<Map<String, Object>>>() {
				}, "items");

		return itemMaps.stream().map(itemMap -> {
			long id = ((Number) itemMap.get("id")).longValue();
			String name = (String) itemMap.get("name");
			String fullName = (String) itemMap.get("full_name");
			String description = (String) itemMap.get("description");
			String htmlUrl = (String) itemMap.get("html_url");
			int stargazersCount = ((Number) itemMap.get("stargazers_count")).intValue();
			int forksCount = ((Number) itemMap.get("forks_count")).intValue();
			String language = (String) itemMap.get("language");

			return new Repository(id, name, fullName, description, htmlUrl, stargazersCount, forksCount, language);
		}).collect(Collectors.toList());
	}

	public record Repository(long id, String name, String fullName, String description, String htmlUrl,
			int stargazersCount, int forksCount, String language) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("GitHub Repository search request")
	public record Request(@JsonProperty(required = true,
			value = "query") @JsonPropertyDescription("Keywords used for queries, useful for getting a list of repositories") String query,

			@JsonProperty(
					value = "sort") @JsonPropertyDescription("Sorts the results of your query by number of stars, forks, or help-wanted-issues or how recently the items were updated.") String sort,

			@JsonProperty(
					value = "order") @JsonPropertyDescription("Determines whether the first search result returned is the highest number of matches (desc) or lowest number of matches (asc). This parameter is ignored unless you provide sort.") String order) {
	}

}
