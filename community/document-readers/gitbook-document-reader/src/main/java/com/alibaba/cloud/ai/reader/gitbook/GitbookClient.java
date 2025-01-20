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
package com.alibaba.cloud.ai.reader.gitbook;

import com.alibaba.cloud.ai.reader.gitbook.model.GitbookPage;
import com.alibaba.cloud.ai.reader.gitbook.model.GitbookSpace;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A client for interacting with the Gitbook API. This client provides methods to fetch
 * spaces, pages, and their content from Gitbook. It handles authentication, request
 * formatting, and response parsing.
 *
 * <p>
 * The client supports:
 * <ul>
 * <li>Custom API endpoints</li>
 * <li>Authentication via API token</li>
 * <li>JSON response parsing</li>
 * <li>Recursive page structure traversal</li>
 * </ul>
 *
 * @author brianxiadong
 */
public class GitbookClient {

	private static final String DEFAULT_API_URL = "https://api.gitbook.com/v1";

	private final String apiToken;

	private final String baseUrl;

	private final HttpClient httpClient;

	private final ObjectMapper objectMapper;

	/**
	 * Creates a new GitbookClient with the specified API token and optional custom API
	 * URL.
	 * @param apiToken The API token for authentication with Gitbook
	 * @param apiUrl Optional custom API URL (if null, uses default Gitbook API endpoint)
	 */
	public GitbookClient(String apiToken, String apiUrl) {
		this.apiToken = apiToken;
		this.baseUrl = apiUrl != null ? apiUrl : DEFAULT_API_URL;
		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Retrieves information about a specific Gitbook space.
	 * @param spaceId The ID of the space to fetch
	 * @return A GitbookSpace object containing the space information
	 * @throws RuntimeException if the request fails or the response cannot be parsed
	 */
	public GitbookSpace getSpace(String spaceId) {
		String url = baseUrl + "/spaces/" + spaceId;
		return makeRequest(url, GitbookSpace.class);
	}

	/**
	 * Lists all pages in a Gitbook space, including their metadata. This method
	 * recursively traverses the page structure to build a flat list of all pages,
	 * maintaining their hierarchical relationships in the metadata.
	 * @param spaceId The ID of the space to list pages from
	 * @return A list of GitbookPage objects
	 * @throws RuntimeException if the request fails or the response cannot be parsed
	 */
	public List<GitbookPage> listPages(String spaceId) {
		GitbookSpace space = getSpace(spaceId);
		String url = baseUrl + "/spaces/" + spaceId + "/content";
		GitbookSpace content = makeRequest(url, GitbookSpace.class);

		List<GitbookPage> pagesInfo = new ArrayList<>();
		if (content.getPages() != null) {
			for (GitbookPage page : content.getPages()) {
				extractPageInfo(pagesInfo, page, space.getTitle(), null);
			}
		}

		return pagesInfo;
	}

	/**
	 * Retrieves the markdown content of a specific page.
	 * @param spaceId The ID of the space containing the page
	 * @param pageId The ID of the page to fetch
	 * @return The markdown content of the page
	 * @throws RuntimeException if the request fails or the response cannot be parsed
	 */
	public String getPageMarkdown(String spaceId, String pageId) {
		String url = baseUrl + "/spaces/" + spaceId + "/content/page/" + pageId;
		URI uri = URI.create(url + "?format=markdown");

		Map<String, Object> response = makeRequest(uri.toString(), new TypeReference<Map<String, Object>>() {
		});
		return (String) response.get("markdown");
	}

	/**
	 * Makes an HTTP request to the Gitbook API and parses the response.
	 * @param url The URL to make the request to
	 * @param responseType The class to parse the response into
	 * @return The parsed response
	 * @throws RuntimeException if the request fails or the response cannot be parsed
	 */
	private <T> T makeRequest(String url, Class<T> responseType) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Authorization", "Bearer " + apiToken)
				.header("Content-Type", "application/json")
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new RuntimeException(
						"Failed to make request to Gitbook API. Status code: " + response.statusCode());
			}

			return objectMapper.readValue(response.body(), responseType);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to make request to Gitbook API", e);
		}
	}

	/**
	 * Overloaded version of makeRequest that accepts a TypeReference for complex types.
	 */
	private <T> T makeRequest(String url, TypeReference<T> typeReference) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Authorization", "Bearer " + apiToken)
				.header("Content-Type", "application/json")
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new RuntimeException(
						"Failed to make request to Gitbook API. Status code: " + response.statusCode());
			}

			return objectMapper.readValue(response.body(), typeReference);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to make request to Gitbook API", e);
		}
	}

	/**
	 * Recursively extracts page information and builds the page hierarchy. This method
	 * processes both document pages and group pages, maintaining the hierarchical
	 * structure in the page metadata.
	 * @param pages The list to add extracted page information to
	 * @param page The current page to process
	 * @param prevTitle The title path up to this point
	 * @param parent The ID of the parent page
	 */
	private void extractPageInfo(List<GitbookPage> pages, GitbookPage page, String prevTitle, String parent) {
		String title = buildTitle(prevTitle, page.getTitle());
		page.setParent(parent);

		if (page.isDocument()) {
			pages.add(page);
		}

		if (page.hasSubPages()) {
			for (GitbookPage subPage : page.getSubPages()) {
				extractPageInfo(pages, subPage, title, page.getId());
			}
		}
	}

	/**
	 * 构建标题路径
	 */
	private String buildTitle(String prevTitle, String currentTitle) {
		return prevTitle + " > " + currentTitle;
	}

}
