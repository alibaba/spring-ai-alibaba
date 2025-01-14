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
package com.alibaba.cloud.ai.reader.huggingface.fs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Hugging Face File System reader. Uses the Hugging Face Hub client library to read files
 * from Hugging Face repositories.
 *
 * @author brianxiadong
 **/
public class HuggingFaceFSDocumentReader implements DocumentReader {

	public static final String SOURCE = "source";

	private final String resourcePath;

	private final ObjectMapper objectMapper;

	/**
	 * Create a new HuggingFaceFSDocumentReader instance.
	 * @param resourcePath the path to the resource
	 */
	public HuggingFaceFSDocumentReader(String resourcePath) {
		Assert.notNull(resourcePath, "Resource path must not be null");
		this.resourcePath = resourcePath;
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public List<Document> get() {
		try {
			List<Map<String, Object>> jsonDicts = loadDicts();
			List<Document> documents = new ArrayList<>();

			for (Map<String, Object> dict : jsonDicts) {
				Document document = new Document(dict.toString());
				document.getMetadata().put(SOURCE, resourcePath);
				documents.add(document);
			}

			return documents;
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to load documents from HuggingFace: " + e.getMessage(), e);
		}
	}

	/**
	 * Parse file and load as list of dictionaries
	 *
	 */
	public List<Map<String, Object>> loadDicts() throws IOException {
		Path path = Paths.get(resourcePath);
		byte[] content = Files.readAllBytes(path);
		String data;

		// Handle gzip compressed files
		if (resourcePath.endsWith(".gz")) {
			try (InputStream inputStream = new ByteArrayInputStream(content);
					GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
				data = new String(gzipInputStream.readAllBytes());
			}
		}
		else {
			data = new String(content);
		}

		List<Map<String, Object>> jsonDicts = new ArrayList<>();
		String[] lines = data.split("\n");

		for (String line : lines) {
			try {
				if (!line.trim().isEmpty()) {
					@SuppressWarnings("unchecked")
					Map<String, Object> jsonDict = objectMapper.readValue(line, Map.class);
					jsonDicts.add(jsonDict);
				}
			}
			catch (Exception e) {
				// Skip invalid JSON lines
				continue;
			}
		}

		return jsonDicts;
	}

	/**
	 * Get the resource path.
	 * @return the resource path
	 */
	public String getResourcePath() {
		return this.resourcePath;
	}

}
