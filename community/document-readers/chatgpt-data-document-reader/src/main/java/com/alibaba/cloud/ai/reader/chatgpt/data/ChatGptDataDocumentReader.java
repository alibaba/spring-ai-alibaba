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

	/**
	 * Constructor with file path and limit
	 * @param logFilePath path to the JSON file containing ChatGPT conversations
	 * @param numLogs maximum number of conversations to load (0 for all)
	 */
	public ChatGptDataDocumentReader(String logFilePath, int numLogs) {
		this.logFilePath = logFilePath;
		this.numLogs = numLogs;
	}

	/**
	 * Constructor with file path only
	 * @param logFilePath path to the JSON file containing ChatGPT conversations
	 */
	public ChatGptDataDocumentReader(String logFilePath) {
		this(logFilePath, 0);
	}

	/**
	 * Process a message from the conversation
	 * @param messageNode the JSON node containing the message
	 * @param title the title of the conversation
	 * @return formatted message text
	 */
	private String processMessage(JsonNode messageNode, String title) {
		if (messageNode == null || messageNode.isEmpty()) {
			return "";
		}

		String role = messageNode.get("role").asText();

		JsonNode content = messageNode.get("content");
		JsonNode parts = content.get("parts");
		String text = parts.get(0).asText();

		String createTimeStr = messageNode.get("create_time").asText();
		LocalDateTime dateTime = LocalDateTime.parse(createTimeStr, DateTimeFormatter.ISO_DATE_TIME);
		String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

		return String.format("%s - %s on %s: %s\n\n", title, role, formattedDate, text);
	}

	@Override
	public List<Document> get() {
		try {
			String jsonContent = Files.readString(Paths.get(logFilePath), StandardCharsets.UTF_8);
			JsonNode data = objectMapper.readTree(jsonContent);

			// Determine how many conversations to process
			int limit = numLogs > 0 ? Math.min(numLogs, data.size()) : data.size();

			// Process each conversation
			return IntStream.range(0, limit).mapToObj(i -> {
				JsonNode conversation = data.get(i);
				String title = conversation.get("title").asText();
				JsonNode mapping = conversation.get("mapping");

				// Build the conversation text by processing each message
				StringBuilder conversationText = new StringBuilder();

				// Iterate through all messages in the mapping
				StreamSupport
					.stream(Spliterators.spliteratorUnknownSize(mapping.fieldNames(), Spliterator.ORDERED), false)
					.forEach(key -> {
						JsonNode messageNode = mapping.get(key);
						String messageText = processMessage(messageNode, title);
						if (!messageText.isEmpty()) {
							conversationText.append(messageText);
						}
					});

				// Create document metadata
				Map<String, Object> metadata = new HashMap<>();
				metadata.put("source", logFilePath);
				metadata.put("title", title);
				metadata.put("id", conversation.get("id").asText());
				metadata.put("create_time", conversation.get("create_time").asText());

				// Return new Document object
				return new Document(conversationText.toString(), metadata);
			}).collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to load ChatGPT data from file: " + logFilePath, e);
		}
	}

}
