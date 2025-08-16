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
package com.alibaba.cloud.ai.toolcalling.openalex;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * OpenAlex Academic Search Service Provides access to OpenAlex API for searching
 * scholarly works, authors, institutions, etc.
 *
 * @author Makoto
 */
public class OpenAlexService implements SearchService, Function<OpenAlexService.Request, OpenAlexService.Response> {

	private static final Logger log = LoggerFactory.getLogger(OpenAlexService.class);

	private final OpenAlexProperties properties;

	private final WebClientTool webClientTool;

	private final ObjectMapper objectMapper;

	public OpenAlexService(OpenAlexProperties properties, JsonParseTool jsonParseTool, WebClientTool webClientTool) {
		this.properties = properties;
		this.webClientTool = webClientTool;
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(Request.simpleQuery(query));
	}

	@Override
	public Response apply(Request request) {
		if (!StringUtils.hasText(request.getQuery())) {
			return Response.errorResponse(request.getQuery(), "Search query cannot be empty.");
		}

		try {
			String searchUrl = buildSearchUrl(request);
			log.debug("OpenAlex search URL: {}", searchUrl);

			String responseBody = webClientTool.get(searchUrl).block();

			if (responseBody == null) {
				return Response.errorResponse(request.getQuery(), "Empty response from OpenAlex API");
			}

			return parseResponse(responseBody, request);

		}
		catch (Exception e) {
			log.error("Error searching OpenAlex: ", e);
			return Response.errorResponse(request.getQuery(), "Failed to search OpenAlex: " + e.getMessage());
		}
	}

	private String buildSearchUrl(Request request) {
		StringBuilder url = new StringBuilder();

		// Determine endpoint based on entity type
		switch (request.entityType().toLowerCase()) {
			case "author", "authors" -> url.append(OpenAlexConstants.AUTHORS_ENDPOINT);
			case "source", "sources", "journal", "journals" -> url.append(OpenAlexConstants.SOURCES_ENDPOINT);
			case "institution", "institutions" -> url.append(OpenAlexConstants.INSTITUTIONS_ENDPOINT);
			case "topic", "topics" -> url.append(OpenAlexConstants.TOPICS_ENDPOINT);
			case "publisher", "publishers" -> url.append(OpenAlexConstants.PUBLISHERS_ENDPOINT);
			case "funder", "funders" -> url.append(OpenAlexConstants.FUNDERS_ENDPOINT);
			default -> url.append(OpenAlexConstants.WORKS_ENDPOINT);
		}

		url.append("?filter=");

		// Build search filter based on query
		if (request.entityType().equals("works") || request.entityType().equals("work")) {
			url.append("title.search:");
		}
		else if (request.entityType().equals("authors") || request.entityType().equals("author")) {
			url.append("display_name.search:");
		}
		else {
			url.append("display_name.search:");
		}

		url.append(URLEncoder.encode(request.getQuery(), StandardCharsets.UTF_8));

		// Add additional filters
		if (StringUtils.hasText(request.author())) {
			url.append(",author.id:").append(URLEncoder.encode(request.author(), StandardCharsets.UTF_8));
		}

		if (StringUtils.hasText(request.institution())) {
			url.append(",institutions.id:").append(URLEncoder.encode(request.institution(), StandardCharsets.UTF_8));
		}

		if (request.fromYear() != null && request.fromYear() > 0) {
			url.append(",from_publication_date:").append(request.fromYear()).append("-01-01");
		}

		if (request.toYear() != null && request.toYear() > 0) {
			url.append(",to_publication_date:").append(request.toYear()).append("-12-31");
		}

		if (request.isOpenAccess() != null && request.isOpenAccess()) {
			url.append(",is_oa:true");
		}

		// Add pagination and other parameters
		url.append("&per-page=")
			.append(Math.min(request.perPage() != null ? request.perPage() : properties.getPerPage(), 200));
		url.append("&sort=").append(request.sortBy() != null ? request.sortBy() : "cited_by_count:desc");

		return url.toString();
	}

	private Response parseResponse(String responseBody, Request request) {
		try {
			JsonNode rootNode = objectMapper.readTree(responseBody);
			JsonNode resultsNode = rootNode.get("results");

			List<OpenAlexResult> results = new ArrayList<>();
			if (resultsNode != null && resultsNode.isArray()) {
				for (JsonNode resultNode : resultsNode) {
					try {
						OpenAlexResult result = parseResult(resultNode, request.entityType());
						if (result != null) {
							results.add(result);
						}
					}
					catch (Exception e) {
						log.warn("Error parsing OpenAlex result: ", e);
					}
				}
			}

			log.debug("Found {} search results", results.size());
			return new Response(request.getQuery(), results, null);
		}
		catch (Exception e) {
			log.error("Error parsing OpenAlex response: ", e);
			return Response.errorResponse(request.getQuery(), "Failed to parse response: " + e.getMessage());
		}
	}

	private OpenAlexResult parseResult(JsonNode node, String entityType) {
		String id = node.has("id") ? node.get("id").asText() : "";
		String displayName = node.has("display_name") ? node.get("display_name").asText() : "";
		String title = node.has("title") ? node.get("title").asText() : displayName;

		// Extract description/snippet based on entity type
		String description = "";
		if (entityType.equals("works") || entityType.equals("work")) {
			// For works, try to get abstract or use host venue info
			if (node.has("host_venue") && node.get("host_venue").has("display_name")) {
				description = "发表于: " + node.get("host_venue").get("display_name").asText();
			}
			if (node.has("publication_year")) {
				description += " (" + node.get("publication_year").asText() + ")";
			}
		}
		else if (entityType.equals("authors") || entityType.equals("author")) {
			// For authors, include affiliation info
			if (node.has("last_known_institutions")) {
				JsonNode institutions = node.get("last_known_institutions");
				if (institutions.isArray() && institutions.size() > 0) {
					JsonNode firstInst = institutions.get(0);
					if (firstInst.has("display_name")) {
						description = "隶属于: " + firstInst.get("display_name").asText();
					}
				}
			}
			if (node.has("works_count")) {
				description += " | 作品数: " + node.get("works_count").asText();
			}
		}

		// Get URL
		String url = node.has("id") ? node.get("id").asText() : "";

		// Get citation count - safely handle missing values
		Integer citationCount = null;
		if (node.has("cited_by_count") && !node.get("cited_by_count").isNull()) {
			citationCount = node.get("cited_by_count").asInt();
		}

		// Get publication year - safely handle missing values
		Integer year = null;
		if (node.has("publication_year") && !node.get("publication_year").isNull()) {
			year = node.get("publication_year").asInt();
		}

		// Get DOI for works
		String doi = null;
		if (node.has("doi") && !node.get("doi").isNull()) {
			doi = node.get("doi").asText();
		}

		return new OpenAlexResult(id, title, displayName, description, url, doi, citationCount, year, entityType);
	}

	public record OpenAlexResult(String id, String title, String displayName, String description, String url,
			String doi, Integer citationCount, Integer year, String entityType) {
	}

	@JsonClassDescription("OpenAlex Search Request. Search for scholarly works, authors, institutions, etc.")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Request(
			@JsonProperty(required = true, value = "query") @JsonPropertyDescription("Search query terms") String query,

			@JsonProperty(value = "entity_type",
					defaultValue = "works") @JsonPropertyDescription("Type of entity to search: works, authors, sources, institutions, topics, publishers, funders") String entityType,

			@JsonProperty(value = "author") @JsonPropertyDescription("Filter by author ID or name") String author,

			@JsonProperty(
					value = "institution") @JsonPropertyDescription("Filter by institution ID or name") String institution,

			@JsonProperty(
					value = "from_year") @JsonPropertyDescription("Start year for publication date filter") Integer fromYear,

			@JsonProperty(
					value = "to_year") @JsonPropertyDescription("End year for publication date filter") Integer toYear,

			@JsonProperty(
					value = "is_open_access") @JsonPropertyDescription("Filter for open access works only") Boolean isOpenAccess,

			@JsonProperty(value = "per_page",
					defaultValue = "25") @JsonPropertyDescription("Number of results per page (max 200)") Integer perPage,

			@JsonProperty(value = "sort_by",
					defaultValue = "cited_by_count:desc") @JsonPropertyDescription("Sort order: cited_by_count:desc, publication_date:desc, relevance_score:desc") String sortBy)
			implements
				SearchService.Request {

		@Override
		public String getQuery() {
			return this.query();
		}

		public static Request simpleQuery(String query) {
			return new Request(query, "works", null, null, null, null, null, 25, "cited_by_count:desc");
		}

		public static Request authorSearch(String query) {
			return new Request(query, "authors", null, null, null, null, null, 25, "works_count:desc");
		}

		public static Request institutionSearch(String query) {
			return new Request(query, "institutions", null, null, null, null, null, 25, "works_count:desc");
		}
	}

	@JsonClassDescription("OpenAlex Search Response")
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(@JsonProperty("query") String query, @JsonProperty("results") List<OpenAlexResult> results,
			@JsonProperty("error") String error) implements SearchService.Response {

		public static Response errorResponse(String query, String errorMsg) {
			return new Response(query, null, errorMsg);
		}

		@Override
		public SearchService.SearchResult getSearchResult() {
			if (results == null) {
				return new SearchService.SearchResult(List.of());
			}
			return new SearchService.SearchResult(this.results()
				.stream()
				.map(item -> new SearchService.SearchContent(item.title(), item.description(), item.url(), null))
				.toList());
		}
	}

}
