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
package com.alibaba.cloud.ai.reader.arxiv;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ArXiv resource class for managing queries and resource access
 *
 * @author brianxiadong
 */
public class ArxivResource {

	public static final String SOURCE = "source";

	public static final String TITLE = "title";

	public static final String AUTHORS = "authors";

	public static final String SUMMARY = "summary";

	public static final String CATEGORIES = "categories";

	public static final String PRIMARY_CATEGORY = "primary_category";

	public static final String PUBLISHED = "published";

	public static final String UPDATED = "updated";

	public static final String DOI = "doi";

	public static final String JOURNAL_REF = "journal_ref";

	public static final String COMMENT = "comment";

	public static final String ENTRY_ID = "entry_id";

	public static final String PDF_URL = "pdf_url";

	private final String queryString;

	private final int maxDocuments;

	private Path tempFilePath;

	public ArxivResource(String queryString, int maxDocuments) {
		this.queryString = queryString;
		this.maxDocuments = maxDocuments;
	}

	public String getQueryString() {
		return queryString;
	}

	public int getMaxDocuments() {
		return maxDocuments;
	}

	public void setTempFilePath(Path tempFilePath) {
		this.tempFilePath = tempFilePath;
	}

	public InputStream getInputStream() throws IOException {
		if (tempFilePath == null || !Files.exists(tempFilePath)) {
			throw new IOException("Temporary PDF file not found or not set");
		}
		return Files.newInputStream(tempFilePath);
	}

	public void cleanup() {
		if (tempFilePath != null) {
			try {
				Files.deleteIfExists(tempFilePath);
			}
			catch (IOException e) {
				// Log error but don't throw exception since this is just cleanup
			}
		}
	}

}
