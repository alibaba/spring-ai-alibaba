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

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.models.RepositoryFile;
import org.gitlab4j.api.models.TreeItem;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * GitLab repository reader.
 * Reads files from GitLab repositories and converts them to documents.
 *
 * @author brianxiadong
 */
public class GitLabRepositoryReader extends AbstractGitLabReader {

    private final boolean useParser;

    /**
     * Constructor for GitLabRepositoryReader.
     *
     * @param gitLabApi GitLab API client
     * @param projectId Project ID
     * @param useParser Whether to use a parser for file content
     * @param verbose Whether to enable verbose logging
     */
    public GitLabRepositoryReader(GitLabApi gitLabApi, Integer projectId, boolean useParser, boolean verbose) {
        super(gitLabApi, projectId, verbose);
        this.useParser = useParser;
    }

    /**
     * Load a single file from the repository.
     *
     * @param filePath Path to the file
     * @param ref Branch name or commit ID
     * @return Document representation of the file
     * @throws GitLabApiException if there is an error accessing the GitLab API
     */
    private Document loadSingleFile(String filePath, String ref) throws GitLabApiException {
        RepositoryFile file = gitLabApi.getRepositoryFileApi().getFile(projectId, filePath, ref);
        byte[] content = Base64.getDecoder().decode(file.getContent());
        String fileContent = new String(content, StandardCharsets.UTF_8);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("file_path", file.getFilePath());
        metadata.put("file_name", file.getFileName());
        metadata.put("size", content.length);
        metadata.put("url", String.format("%s/repository/files/%s/raw", projectUrl, 
                                        StringUtils.replace(file.getFilePath(), "/", "%2F")));

        return new Document(file.getBlobId(), fileContent, metadata);
    }

    @Override
    public List<Document> get() {
        return loadData("main", null, null, false);
    }

    /**
     * Load data from a GitLab repository.
     *
     * @param ref Branch name or commit ID
     * @param filePath Path to a specific file (optional)
     * @param path Path to a directory (optional)
     * @param recursive Whether to load files recursively
     * @return List of documents
     */
    public List<Document> loadData(String ref, String filePath, String path, boolean recursive) {
        try {
            if (StringUtils.hasText(filePath)) {
                return Collections.singletonList(loadSingleFile(filePath, ref));
            }

            RepositoryApi repositoryApi = gitLabApi.getRepositoryApi();
            List<TreeItem> items = repositoryApi.getTree(projectId, path, ref, recursive);

            List<Document> documents = new ArrayList<>();
            for (TreeItem item : items) {
                if ("blob".equals(item.getType())) {
                    if (verbose) {
                        System.out.println("Loading file: " + item.getPath());
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
} 