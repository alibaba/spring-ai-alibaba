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
package com.alibaba.cloud.ai.reader.obsidian;

import com.alibaba.cloud.ai.parser.markdown.MarkdownDocumentParser;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Document reader for Obsidian vault Reads all markdown files in the vault and parses
 * them using MarkdownDocumentParser
 *
 * @author xiadong
 * @since 2024-01-06
 */
public class ObsidianDocumentReader implements DocumentReader {

	private final Path vaultPath;

	private final MarkdownDocumentParser parser;

	/**
	 * Constructor for reading all files in vault
	 * @param vaultPath Path to Obsidian vault
	 */
	public ObsidianDocumentReader(Path vaultPath) {
		this.vaultPath = vaultPath;
		this.parser = new MarkdownDocumentParser();
	}

	@Override
	public List<Document> get() {
		List<Document> allDocuments = new ArrayList<>();

		// Find all markdown files in vault
		List<ObsidianResource> resources = ObsidianResource.findAllMarkdownFiles(vaultPath);

		// Parse each file
		for (ObsidianResource resource : resources) {
			try {
				List<Document> documents = parser.parse(resource.getInputStream());
				String source = resource.getSource();

				// Add metadata to each document
				for (Document doc : documents) {
					doc.getMetadata().put(ObsidianResource.SOURCE, source);
				}

				allDocuments.addAll(documents);
			}
			catch (IOException e) {
				throw new RuntimeException("Failed to read Obsidian file: " + resource.getFilePath(), e);
			}
		}

		return allDocuments;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private Path vaultPath;

		public Builder vaultPath(Path vaultPath) {
			this.vaultPath = vaultPath;
			return this;
		}

		public ObsidianDocumentReader build() {
			return new ObsidianDocumentReader(vaultPath);
		}

	}

}
