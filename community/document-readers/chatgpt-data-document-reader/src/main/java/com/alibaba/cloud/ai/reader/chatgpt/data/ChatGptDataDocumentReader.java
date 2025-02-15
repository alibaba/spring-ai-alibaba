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
package com.alibaba.cloud.ai.reader.chatgpt.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

/**
 * Document reader for loading exported ChatGPT conversation data
 *
 * @author brianxiadong
 */
public class ChatGptDataDocumentReader implements DocumentReader {

	private final String logFilePath;

	private final int numLogs;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public ChatGptDataDocumentReader(String logFilePath, int numLogs) {
		this.logFilePath = logFilePath;
		this.numLogs = numLogs;
	}

	public ChatGptDataDocumentReader(String logFilePath) {
		this(logFilePath, 0);
	}

	private String concatenateRows(JsonNode message, String title) {
		if (message == null || message.isEmpty()) {
			return "";
		}

		JsonNode author = message.get("author");
		String sender = author != null ? author.get("role").asText() : "unknown";

		JsonNode content = message.get("content");
		JsonNode parts = content.get("parts");
		String text = parts.get(0).asText();

		long createTime = message.get("create_time").asLong();
		LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(createTime), ZoneId.systemDefault());
		String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

		return String.format("%s - %s on %s: %s\n\n", title, sender, formattedDate, text);
	}

	@Override
	public List<Document> get() {
		try {
			String jsonContent = Files.readString(Paths.get(logFilePath), StandardCharsets.UTF_8);
			JsonNode data = objectMapper.readTree(jsonContent);
			List<Document> documents;

			int limit = numLogs > 0 ? Math.min(numLogs, data.size()) : data.size();

			documents = IntStream.range(0, limit).mapToObj(i -> {
				JsonNode conversation = data.get(i);
				String title = conversation.get("title").asText();
				JsonNode messages = conversation.get("mapping");

				String text = StreamSupport
					.stream(Spliterators.spliteratorUnknownSize(messages.fieldNames(), Spliterator.ORDERED), false)
					.map(key -> {
						JsonNode messageWrapper = messages.get(key);
						JsonNode message = messageWrapper.get("message");

						if ("0".equals(key) && "system".equals(message.get("author").get("role").asText())) {
							return "";
						}
						return concatenateRows(message, title);
					})
					.filter(s -> !s.isEmpty())
					.collect(Collectors.joining());

				// Create document metadata
				Map<String, Object> metadata = new HashMap<>();
				metadata.put("source", logFilePath);
				// Return new Document object
				return new Document(text, metadata);
			}).collect(Collectors.toList());

			return documents;
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to load ChatGPT data from file: " + logFilePath, e);
		}
	}

}
