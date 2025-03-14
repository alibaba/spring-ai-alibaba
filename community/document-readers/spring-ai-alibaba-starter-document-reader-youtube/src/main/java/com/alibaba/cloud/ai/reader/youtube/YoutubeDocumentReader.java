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
package com.alibaba.cloud.ai.reader.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for reading and parsing video information and subtitles from Youtube.
 * Implements the DocumentReader interface to provide methods for obtaining document
 * content.
 */
public class YoutubeDocumentReader implements DocumentReader {

	private static final String WATCH_URL = "https://www.youtube.com/watch?v=%s";

	private final ObjectMapper objectMapper;

	private static final List<String> YOUTUBE_URL_PATTERNS = List.of("youtube\\.com/watch\\?v=([^&]+)",
			"youtu\\.be/([^?&]+)");

	private final String resourcePath;

	private static final WebClient WEB_CLIENT = WebClient.builder()
		.defaultHeader("Accept-Language", "en-US")
		.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
		.build();

	public YoutubeDocumentReader(String resourcePath) {
		Assert.hasText(resourcePath, "Query string must not be empty");
		this.resourcePath = resourcePath;
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public List<Document> get() {
		List<Document> documents = new ArrayList<>();
		try {
			String videoId = extractVideoIdFromUrl(resourcePath);
			String subtitleContent = getSubtitleInfo(videoId);
			documents.add(new Document(StringEscapeUtils.unescapeHtml4(subtitleContent)));
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to load document from Youtube: {}", e);
		}
		return documents;
	}

	// Method to extract the videoId from the resourcePath
	public String extractVideoIdFromUrl(String resourcePath) {
		for (String pattern : YOUTUBE_URL_PATTERNS) {
			Pattern regexPattern = Pattern.compile(pattern);
			Matcher matcher = regexPattern.matcher(resourcePath);
			if (matcher.find()) {
				return matcher.group(1); // Extract the videoId (captured group)
			}
		}
		throw new IllegalArgumentException("Invalid YouTube URL: Unable to extract videoId.");
	}

	public String getSubtitleInfo(String videoId) throws IOException {
		// Step 1: Fetch the HTML content of the YouTube video page
		String url = String.format(WATCH_URL, videoId);
		String htmlContent = fetchHtmlContent(url).block(); // Blocking for simplicity in
															// this example

		// Step 2: Extract the subtitle tracks from the HTML
		String captionsJsonString = extractCaptionsJson(htmlContent);
		if (captionsJsonString != null) {
			JsonNode captionsJson = objectMapper.readTree(captionsJsonString);
			JsonNode captionTracks = captionsJson.path("playerCaptionsTracklistRenderer").path("captionTracks");

			// Check if captionTracks exists and is an array
			if (captionTracks.isArray()) {
				// Step 3: Extract and decode each subtitle track's URL
				StringBuilder subtitleInfo = new StringBuilder();
				JsonNode captionTrack = captionTracks.get(0);
				// Safely access languageCode and baseUrl with null checks
				String language = captionTrack.path("languageCode").asText("Unknown");
				String urlEncoded = captionTrack.path("baseUrl").asText("");

				// Decode the URL to avoid \u0026 issues
				String decodedUrl = URLDecoder.decode(urlEncoded, StandardCharsets.UTF_8);

				String subtitleText = fetchSubtitleText(decodedUrl);
				subtitleInfo.append("Language: ").append(language).append("\n").append(subtitleText).append("\n\n");

				return subtitleInfo.toString();
			}
			else {
				return "No captions available.";
			}
		}
		else {
			return "No captions data found.";
		}
	}

	private Mono<String> fetchHtmlContent(String url) {
		// Use WebClient to fetch HTML content asynchronously
		return WEB_CLIENT.get().uri(url).retrieve().bodyToMono(String.class);
	}

	private String extractCaptionsJson(String htmlContent) {
		// Extract the captions JSON from the HTML content
		String marker = "\"captions\":";
		int startIndex = htmlContent.indexOf(marker);
		if (startIndex != -1) {
			int endIndex = htmlContent.indexOf("\"videoDetails", startIndex);
			if (endIndex != -1) {
				String captionsJsonString = htmlContent.substring(startIndex + marker.length(), endIndex);
				return captionsJsonString.trim();
			}
		}
		return null;
	}

	private String fetchSubtitleText(String decodedUrl) throws IOException {
		// Fetch the subtitle text by making a request to the decoded subtitle URL
		org.jsoup.nodes.Document doc = Jsoup.connect(decodedUrl).get();

		// Assuming the subtitle text is inside <transcript> tags, extract the text
		StringBuilder subtitleText = new StringBuilder();
		doc.select("text").forEach(textNode -> {
			String text = textNode.text();
			subtitleText.append(text).append("\n");
		});

		return subtitleText.toString();
	}

}
