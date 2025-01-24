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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Document reader for loading exported ChatGPT conversation data
 *
 * @author brianxiadong
 */
public class ChatGptDataDocumentReader implements DocumentReader {

	private final String logFilePath;

	private final int numLogs;

	/**
	 * Initialize ChatGPT data loader with file path
	 * @param logFilePath The path to the log file
	 * @param numLogs Number of logs to load, load all if 0
	 */
	public ChatGptDataDocumentReader(String logFilePath, int numLogs) {
		this.logFilePath = logFilePath;
		this.numLogs = numLogs;
	}

	/**
	 * Initialize ChatGPT data loader with file path, load all logs
	 * @param logFilePath The path to the log file
	 */
	public ChatGptDataDocumentReader(String logFilePath) {
		this(logFilePath, 0);
	}

	/**
	 * Format message content into readable string
	 * @param message Message object to format
	 * @param title Conversation title
	 * @return Formatted message string
	 */
	private String concatenateRows(JSONObject message, String title) {
		if (message == null || message.isEmpty()) {
			return "";
		}

		// Get sender role
		JSONObject author = message.getJSONObject("author");
		String sender = author != null ? author.getString("role") : "unknown";

		// Get message content
		JSONObject content = message.getJSONObject("content");
		JSONArray parts = content.getJSONArray("parts");
		String text = parts.getString(0);

		// Get and format timestamp
		long createTime = message.getLongValue("create_time");
		LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(createTime), ZoneId.systemDefault());
		String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

		return String.format("%s - %s on %s: %s\n\n", title, sender, formattedDate, text);
	}

	@Override
	public List<Document> get() {
		try {
			// Read JSON file content
			String jsonContent = Files.readString(Paths.get(logFilePath), StandardCharsets.UTF_8);
			JSONArray data = JSON.parseArray(jsonContent);
			List<Document> documents = new ArrayList<>();

			// Limit number of logs if specified
			int limit = numLogs > 0 ? Math.min(numLogs, data.size()) : data.size();

			// Process first limit entries using Stream API
			documents = IntStream.range(0, limit).mapToObj(i -> {
				// Get conversation data
				JSONObject conversation = data.getJSONObject(i);
				String title = conversation.getString("title");
				JSONObject messages = conversation.getJSONObject("mapping");

				// Process messages using Stream
				String text = messages.keySet().stream().map(key -> {
					JSONObject messageWrapper = messages.getJSONObject(key);
					JSONObject message = messageWrapper.getJSONObject("message");

					// Skip first system role message
					if ("0".equals(key) && "system".equals(message.getJSONObject("author").getString("role"))) {
						return "";
					}
					return concatenateRows(message, title);
				}).filter(s -> !s.isEmpty()).collect(Collectors.joining());

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
