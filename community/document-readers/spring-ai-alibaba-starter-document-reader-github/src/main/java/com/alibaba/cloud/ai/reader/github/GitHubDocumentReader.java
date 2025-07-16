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
package com.alibaba.cloud.ai.reader.github;

import org.kohsuke.github.GHContent;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.alibaba.cloud.ai.document.DocumentParser;

/**
 * @author HeYQ
 * @since 1.0.0
 */
public class GitHubDocumentReader implements DocumentReader {

	private final DocumentParser parser;

	private GitHubResource gitHubResource;

	private List<GitHubResource> gitHubResourceList;

	public GitHubDocumentReader(GitHubResource gitHubResource, DocumentParser parser) {
		this.gitHubResource = gitHubResource;
		this.parser = parser;
	}

	public GitHubDocumentReader(List<GitHubResource> gitHubResourceList, DocumentParser parser) {
		this.gitHubResourceList = gitHubResourceList;
		this.parser = parser;
	}

	@Override
	public List<Document> get() {
		List<Document> documents = new ArrayList<>();
		if (!Objects.isNull(gitHubResourceList) && !gitHubResourceList.isEmpty()) {
			processResourceList(documents);
		}
		else if (gitHubResource != null) {
			loadDocuments(documents, gitHubResource);
		}

		return documents;
	}

	private void processResourceList(List<Document> documents) {
		for (GitHubResource resource : gitHubResourceList) {
			loadDocuments(documents, resource);
		}
	}

	private void loadDocuments(List<Document> documents, GitHubResource gitHubResource) {
		try {
			List<Document> documentList = parser.parse(gitHubResource.getInputStream());
			for (Document document : documentList) {
				GHContent ghContent = gitHubResource.getText();
				Map<String, Object> metadata = document.getMetadata();
				metadata.put("github_git_url", ghContent.getGitUrl());
				metadata.put("github_download_url", ghContent.getDownloadUrl());
				metadata.put("github_html_url", ghContent.getHtmlUrl());
				metadata.put("github_url", ghContent.getUrl());
				metadata.put("github_file_name", ghContent.getName());
				metadata.put("github_file_path", ghContent.getPath());
				metadata.put("github_file_sha", ghContent.getSha());
				metadata.put("github_file_size", Long.toString(ghContent.getSize()));
				metadata.put("github_file_encoding", ghContent.getEncoding());
				documents.add(document);
			}
		}
		catch (IOException ioException) {
			throw new RuntimeException("Failed to load document from GitHub: {}", ioException);
		}
	}

}
