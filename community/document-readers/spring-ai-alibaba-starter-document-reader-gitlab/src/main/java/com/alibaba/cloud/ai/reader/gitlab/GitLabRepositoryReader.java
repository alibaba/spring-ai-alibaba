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
package com.alibaba.cloud.ai.reader.gitlab;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.models.RepositoryFile;
import org.gitlab4j.api.models.TreeItem;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * GitLab repository reader. Reads files from GitLab repositories and converts them to
 * documents. Only supports public repositories.
 *
 * @author brianxiadong
 */
public class GitLabRepositoryReader extends AbstractGitLabReader {

	private String ref;

	private String filePath;

	private String pattern;

	private boolean recursive;

	/**
	 * Constructor for GitLabRepositoryReader.
	 * @param hostUrl GitLab host URL
	 * @param namespace Project namespace (e.g. "spring-ai")
	 * @param projectName Project name (e.g. "spring-ai")
	 * @throws GitLabApiException if project cannot be found
	 */
	public GitLabRepositoryReader(String hostUrl, String namespace, String projectName) throws GitLabApiException {
		super(hostUrl, namespace, projectName);
		this.ref = "main"; // Default branch
	}

	/**
	 * Set the Git reference (branch, tag, or commit) to read from.
	 * @param ref Git reference
	 * @return this reader instance
	 */
	public GitLabRepositoryReader setRef(String ref) {
		this.ref = ref;
		return this;
	}

	/**
	 * Set the file path to read. If null, will read all files in the repository.
	 * @param filePath File path relative to repository root
	 * @return this reader instance
	 */
	public GitLabRepositoryReader setFilePath(String filePath) {
		this.filePath = filePath;
		return this;
	}

	/**
	 * Set the file pattern to filter files. Supports glob patterns like: - "*.md" for all
	 * markdown files - "docs/*.txt" for all text files in docs directory -
	 * "src/**\/*.java" for all Java files in src directory and subdirectories
	 * @param pattern File pattern in glob format
	 * @return this reader instance
	 */
	public GitLabRepositoryReader setPattern(String pattern) {
		this.pattern = pattern;
		return this;
	}

	/**
	 * Set whether to recursively read files in subdirectories.
	 * @param recursive Whether to read recursively
	 * @return this reader instance
	 */
	public GitLabRepositoryReader setRecursive(boolean recursive) {
		this.recursive = recursive;
		return this;
	}

	@Override
	public List<Document> get() {
		try {
			return loadData(ref, filePath, pattern, recursive);
		}
		catch (GitLabApiException e) {
			throw new RuntimeException("Failed to load files from GitLab", e);
		}
	}

	/**
	 * Load files from GitLab repository.
	 * @param ref Git reference (branch, tag, or commit)
	 * @param filePath File path to load (optional)
	 * @param pattern File pattern to filter (optional)
	 * @param recursive Whether to read recursively
	 * @return List of documents
	 * @throws GitLabApiException if API call fails
	 */
	List<Document> loadData(String ref, String filePath, String pattern, boolean recursive) throws GitLabApiException {
		try {
			if (StringUtils.hasText(filePath)) {
				return Collections.singletonList(loadSingleFile(filePath, ref));
			}

			RepositoryApi repositoryApi = gitLabApi.getRepositoryApi();
			List<TreeItem> items = repositoryApi.getTree(project.getId(), filePath, ref, recursive);

			List<Document> documents = new ArrayList<>();
			for (TreeItem item : items) {
				if (TreeItem.Type.BLOB.equals(item.getType())) {
					// Apply pattern filter if specified
					if (pattern != null && !pattern.isEmpty()) {
						String path = item.getPath();
						// Convert glob pattern to regex pattern
						String regexPattern = pattern.replace(".", "\\.") // Escape dots
							.replace("**", ".*") // Match any characters across
													// directories
							.replace("*", "[^/]*") // Match any characters except
													// directory separator
							.replace("?", "."); // Match single character
						if (!path.matches(regexPattern)) {
							continue;
						}
					}
					documents.add(loadSingleFile(item.getPath(), ref));
				}
			}

			return documents;
		}
		catch (GitLabApiException e) {
			throw new RuntimeException("Failed to load repository data from GitLab", e);
		}
	}

	/**
	 * Load a single file from the repository.
	 * @param filePath Path to the file
	 * @param ref Branch name or commit ID
	 * @return Document representation of the file
	 * @throws GitLabApiException if there is an error accessing the GitLab API
	 */
	private Document loadSingleFile(String filePath, String ref) throws GitLabApiException {
		RepositoryFile file = gitLabApi.getRepositoryFileApi().getFile(project.getId(), filePath, ref);
		byte[] content = Base64.getDecoder().decode(file.getContent());
		String fileContent = new String(content, StandardCharsets.UTF_8);

		Map<String, Object> metadata = new HashMap<>();

		// Required fields
		metadata.put("file_path", file.getFilePath());
		metadata.put("file_name", file.getFileName());
		metadata.put("size", content.length);
		metadata.put("url", String.format("%s/repository/files/%s/raw", projectUrl,
				StringUtils.replace(file.getFilePath(), "/", "%2F")));

		// Optional fields, only add if not empty
		if (file.getLastCommitId() != null) {
			metadata.put("last_commit_id", file.getLastCommitId());
		}

		if (file.getRef() != null) {
			metadata.put("ref", file.getRef());
		}

		if (file.getContentSha256() != null) {
			metadata.put("content_sha256", file.getContentSha256());
		}

		return new Document(file.getBlobId(), fileContent, metadata);
	}

}
