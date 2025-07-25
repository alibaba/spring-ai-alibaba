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
package com.alibaba.cloud.ai.toolcalling.googlescholar;

import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google Scholar Search Service Provides academic paper search capabilities from Google
 * Scholar
 *
 * @author Makoto
 */
public class GoogleScholarService
		implements SearchService, Function<GoogleScholarService.Request, GoogleScholarService.Response> {

	private static final Logger log = LoggerFactory.getLogger(GoogleScholarService.class);

	private final GoogleScholarProperties properties;

	public GoogleScholarService(GoogleScholarProperties properties) {
		this.properties = properties;
	}

	@Override
	public SearchService.Response query(String query) {
		return this.apply(Request.simpleQuery(query));
	}

	@Override
	public Response apply(Request request) {
		if (!StringUtils.hasText(request.getQuery())) {
			throw new RuntimeException("Search query cannot be empty.");
		}

		try {
			String searchUrl = buildSearchUrl(request);
			log.debug("Google Scholar search URL: {}", searchUrl);

			Connection connection = Jsoup.connect(searchUrl)
				.userAgent(GoogleScholarConstants.USER_AGENT)
				.timeout(properties.getTimeout())
				.followRedirects(true)
				.ignoreHttpErrors(true);

			Document doc = connection.get();
			List<ScholarResult> results = parseSearchResults(doc);

			log.debug("Found {} search results", results.size());
			return new Response(results);

		}
		catch (IOException e) {
			log.error("Error searching Google Scholar: ", e);
			throw new RuntimeException("Failed to search Google Scholar: " + e.getMessage(), e);
		}
		catch (Exception e) {
			log.error("Unexpected error during Google Scholar search: ", e);
			throw new RuntimeException("Unexpected error during search: " + e.getMessage(), e);
		}
	}

	private String buildSearchUrl(Request request) {
		StringBuilder url = new StringBuilder(properties.getBaseUrl());
		url.append("/scholar?q=");
		url.append(URLEncoder.encode(request.getQuery(), StandardCharsets.UTF_8));

		if (StringUtils.hasText(request.author())) {
			url.append("&author=").append(URLEncoder.encode(request.author(), StandardCharsets.UTF_8));
		}

		if (StringUtils.hasText(request.publication())) {
			url.append("&publication=").append(URLEncoder.encode(request.publication(), StandardCharsets.UTF_8));
		}

		if (request.yearFrom() != null && request.yearFrom() > 0) {
			url.append("&as_ylo=").append(request.yearFrom());
		}

		if (request.yearTo() != null && request.yearTo() > 0) {
			url.append("&as_yhi=").append(request.yearTo());
		}

		url.append("&num=").append(Math.min(request.numResults() != null ? request.numResults() : 10, 20));
		url.append("&hl=").append(properties.getLanguage());

		return url.toString();
	}

	private List<ScholarResult> parseSearchResults(Document doc) {
		List<ScholarResult> results = new ArrayList<>();
		Elements resultElements = doc.select("div.gs_ri");

		for (Element element : resultElements) {
			try {
				ScholarResult result = parseScholarResult(element);
				if (result != null) {
					results.add(result);
				}
			}
			catch (Exception e) {
				log.warn("Error parsing scholar result: ", e);
			}
		}

		return results;
	}

	private ScholarResult parseScholarResult(Element element) {
		// Extract title and URL
		Element titleElement = element.selectFirst("h3.gs_rt a");
		String title = titleElement != null ? titleElement.text() : "";
		String url = titleElement != null ? titleElement.attr("href") : "";

		if (!StringUtils.hasText(title)) {
			return null;
		}

		// Extract authors and publication info
		Element authorsElement = element.selectFirst("div.gs_a");
		String authors = authorsElement != null ? authorsElement.text() : "";

		// Extract abstract/snippet
		Element snippetElement = element.selectFirst("div.gs_rs");
		String snippet = snippetElement != null ? snippetElement.text() : "";

		// Extract citation count
		Integer citationCount = null;
		Element citationElement = element.selectFirst("div.gs_fl a:contains(Cited by)");
		if (citationElement != null && properties.isIncludeCitations()) {
			String citationText = citationElement.text();
			Pattern pattern = Pattern.compile("Cited by (\\d+)");
			Matcher matcher = pattern.matcher(citationText);
			if (matcher.find()) {
				citationCount = Integer.parseInt(matcher.group(1));
			}
		}

		// Extract PDF link if available
		String pdfUrl = null;
		Element pdfElement = element.selectFirst("div.gs_ggs.gs_fl a");
		if (pdfElement != null) {
			pdfUrl = pdfElement.attr("href");
		}

		// Extract publication year
		Integer year = null;
		if (StringUtils.hasText(authors)) {
			Pattern yearPattern = Pattern.compile("(\\d{4})");
			Matcher yearMatcher = yearPattern.matcher(authors);
			if (yearMatcher.find()) {
				year = Integer.parseInt(yearMatcher.group(1));
			}
		}

		return new ScholarResult(title, url, authors, snippet, citationCount, pdfUrl, year);
	}

	public record ScholarResult(String title, String url, String authors, String snippet, Integer citationCount,
			String pdfUrl, Integer year) {

	}

	@JsonClassDescription("Google Scholar Search Request. Search for academic papers and publications.")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Request(
			@JsonProperty(required = true,
					value = "query") @JsonPropertyDescription("Search query for academic papers") String query,
			@JsonProperty(value = "author") @JsonPropertyDescription("Filter by author name") String author,
			@JsonProperty(
					value = "publication") @JsonPropertyDescription("Filter by publication name or journal") String publication,
			@JsonProperty(
					value = "yearFrom") @JsonPropertyDescription("Start year for publication date filter") Integer yearFrom,
			@JsonProperty(
					value = "yearTo") @JsonPropertyDescription("End year for publication date filter") Integer yearTo,
			@JsonProperty(value = "numResults",
					defaultValue = "10") @JsonPropertyDescription("Number of results to return (max 20)") Integer numResults)
			implements
				SearchService.Request {

		@Override
		public String getQuery() {
			return this.query();
		}

		public static Request simpleQuery(String query) {
			return new Request(query, null, null, null, null, 10);
		}
	}

	@JsonClassDescription("Google Scholar Search Response")
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(@JsonProperty("results") List<ScholarResult> results) implements SearchService.Response {

		@Override
		public SearchService.SearchResult getSearchResult() {
			return new SearchService.SearchResult(this.results()
				.stream()
				.map(item -> new SearchService.SearchContent(item.title(), item.snippet(), item.url(), null))
				.toList());
		}
	}

}
