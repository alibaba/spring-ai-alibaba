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
import org.springframework.ai.document.DocumentReader;

/**
 * Abstract base class for GitLab document readers.
 * Provides common functionality for GitLab API interactions.
 *
 * @author brianxiadong
 */
public abstract class AbstractGitLabReader implements DocumentReader, AutoCloseable {

    protected final GitLabApi gitLabApi;
    protected final Integer projectId;
    protected final boolean verbose;
    protected final String projectUrl;

    /**
     * Constructor for AbstractGitLabReader.
     *
     * @param gitLabApi GitLab API client
     * @param projectId Project ID (optional)
     * @param verbose Whether to enable verbose logging
     */
    protected AbstractGitLabReader(GitLabApi gitLabApi, Integer projectId, boolean verbose) {
        this.gitLabApi = gitLabApi;
        this.projectId = projectId;
        this.verbose = verbose;
        this.projectUrl = projectId != null ? String.format("%s/projects/%d", gitLabApi.getGitLabServerUrl(), projectId) : null;
    }

    /**
     * Get the GitLab API client.
     *
     * @return GitLab API client
     */
    protected GitLabApi getGitLabApi() {
        return gitLabApi;
    }

    /**
     * Get the project URL.
     *
     * @return Project URL
     */
    protected String getProjectUrl() {
        return projectUrl;
    }

    @Override
    public void close() {
        // Do not close GitLabApi here as it is managed by the factory
    }
} 