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
package com.alibaba.cloud.ai.reader.bilibili;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class for reading and parsing video information and subtitles from Bilibili.
 * Implements the DocumentReader interface to provide methods for obtaining document
 * content.
 */
public class BilibiliDocumentReader implements DocumentReader {

	private static final Logger logger = LoggerFactory.getLogger(BilibiliDocumentReader.class);

	private static final String API_BASE_URL = "https://api.bilibili.com/x/web-interface/view?bvid=";

	private final String resourcePath;

	private final ObjectMapper objectMapper;

	private static final WebClient WEB_CLIENT = WebClient.builder()
		.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
		.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
		.build();

	public BilibiliDocumentReader(String resourcePath) {
		Assert.hasText(resourcePath, "Query string must not be empty");
		this.resourcePath = resourcePath;
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public List<Document> get() {
		List<Document> documents = new ArrayList<>();
		try {
			String bvid = extractBvid(resourcePath);
			String videoInfoResponse = fetchVideoInfo(bvid);
			JsonNode videoData = parseJson(videoInfoResponse).path("data");
			String title = videoData.path("title").asText();
			String description = videoData.path("desc").asText();
			Document infoDoc = new Document("Video information", Map.of("title", title, "description", description));
			documents.add(infoDoc);
			String documentContent = fetchAndProcessSubtitles(videoData, title, description);
			documents.add(new Document(documentContent));
		}
		catch (IllegalArgumentException e) {
			logger.error("Invalid input: {}", e.getMessage());
			documents.add(new Document("Error: Invalid input"));
		}
		catch (IOException e) {
			logger.error("Error parsing JSON: {}", e.getMessage(), e);
			documents.add(new Document("Error parsing JSON: " + e.getMessage()));
		}
		catch (Exception e) {
			logger.error("Unexpected error: {}", e.getMessage(), e);
			documents.add(new Document("Unexpected error: " + e.getMessage()));
		}
		return documents;
	}

	private String extractBvid(String resourcePath) {
		return resourcePath.replaceAll(".*(BV\\w+).*", "$1");
	}

	private String fetchVideoInfo(String bvid) {
		return WEB_CLIENT.get().uri(API_BASE_URL + bvid).retrieve().bodyToMono(String.class).block();
	}

	private JsonNode parseJson(String jsonResponse) throws IOException {
		return objectMapper.readTree(jsonResponse);
	}

	private String fetchAndProcessSubtitles(JsonNode videoData, String title, String description) throws IOException {
		JsonNode subtitleList = videoData.path("subtitle").path("list");
		if (subtitleList.isArray() && subtitleList.size() > 0) {
			String subtitleUrl = subtitleList.get(0).path("subtitle_url").asText();
			String subtitleResponse = WEB_CLIENT.get().uri(subtitleUrl).retrieve().bodyToMono(String.class).block();

			JsonNode subtitleJson = parseJson(subtitleResponse);
			StringBuilder rawTranscript = new StringBuilder();
			subtitleJson.path("body").forEach(node -> rawTranscript.append(node.path("content").asText()).append(" "));

			return String.format("Video Title: %s, Description: %s\nTranscript: %s", title, description,
					rawTranscript.toString().trim());
		}
		else {
			return String.format("No subtitles found for video: %s. Returning an empty transcript.", resourcePath);
		}
	}

}
