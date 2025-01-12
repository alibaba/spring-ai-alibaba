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
import org.gitlab4j.api.models.User;

/**
 * Factory class for creating GitLab readers.
 * Supports both public and private repository access.
 *
 * @author brianxiadong
 */
public class GitLabReaderFactory {

    private final String hostUrl;
    private GitLabApi gitLabApi;

    /**
     * Create a factory for public repositories.
     *
     * @param hostUrl GitLab host URL (e.g. "https://gitlab.com")
     */
    public GitLabReaderFactory(String hostUrl) {
        this.hostUrl = hostUrl;
        this.gitLabApi = new GitLabApi(hostUrl);
    }

    /**
     * Create a factory with authentication for private repositories.
     *
     * @param hostUrl GitLab host URL (e.g. "https://gitlab.com")
     * @param personalAccessToken GitLab personal access token
     * @throws GitLabApiException if authentication fails
     */
    public GitLabReaderFactory(String hostUrl, String personalAccessToken) throws GitLabApiException {
        this.hostUrl = hostUrl;
        this.gitLabApi = new GitLabApi(hostUrl, personalAccessToken);
        // Test the connection and token
        User user = this.gitLabApi.getUserApi().getCurrentUser();
        if (user == null) {
            throw new GitLabApiException("Failed to authenticate with GitLab");
        }
    }

    /**
     * Create a repository reader.
     *
     * @param projectId Project ID
     * @param useParser Whether to use a parser for file content
     * @param verbose Whether to enable verbose logging
     * @return GitLabRepositoryReader instance
     */
    public GitLabRepositoryReader createRepositoryReader(Integer projectId, boolean useParser, boolean verbose) {
        return new GitLabRepositoryReader(gitLabApi, projectId, useParser, verbose);
    }

    /**
     * Create an issue reader.
     *
     * @param projectId Project ID (optional)
     * @param groupId Group ID (optional)
     * @param verbose Whether to enable verbose logging
     * @return GitLabIssueReader instance
     */
    public GitLabIssueReader createIssueReader(Integer projectId, Integer groupId, boolean verbose) {
        return new GitLabIssueReader(gitLabApi, projectId, groupId, verbose);
    }

    /**
     * Close all resources associated with this factory.
     */
    public void close() {
        if (gitLabApi != null) {
            gitLabApi.close();
            gitLabApi = null;
        }
    }
} 