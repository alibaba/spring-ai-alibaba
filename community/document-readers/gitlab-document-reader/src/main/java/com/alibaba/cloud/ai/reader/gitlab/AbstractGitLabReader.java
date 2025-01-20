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
import org.gitlab4j.api.models.Project;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.Assert;

/**
 * Abstract base class for GitLab document readers. Provides common functionality for
 * GitLab API access. Only supports public repositories.
 *
 * @author brianxiadong
 */
public abstract class AbstractGitLabReader implements DocumentReader {

	// GitLab API client for interacting with GitLab server
	protected final GitLabApi gitLabApi;

	// GitLab project object containing project details
	protected final Project project;

	// Web URL of the GitLab project
	protected final String projectUrl;

	/**
	 * Constructor for accessing public GitLab repositories.
	 * @param hostUrl GitLab host URL
	 * @param namespace Project namespace (e.g. "spring-ai")
	 * @param projectName Project name (e.g. "spring-ai")
	 * @throws GitLabApiException if project cannot be found
	 */
	protected AbstractGitLabReader(String hostUrl, String namespace, String projectName) throws GitLabApiException {
		Assert.hasText(hostUrl, "Host URL must not be empty");
		Assert.hasText(namespace, "Namespace must not be empty");
		Assert.hasText(projectName, "Project name must not be empty");

		this.gitLabApi = new GitLabApi(hostUrl, ""); // Empty token for public access
		this.project = gitLabApi.getProjectApi().getProject(namespace, projectName);
		this.projectUrl = project.getWebUrl();
	}

	/**
	 * Get the GitLab API client.
	 * @return GitLab API client
	 */
	protected GitLabApi getGitLabApi() {
		return gitLabApi;
	}

	/**
	 * Get the project.
	 * @return GitLab project
	 */
	protected Project getProject() {
		return project;
	}

	/**
	 * Get the project URL.
	 * @return Project URL
	 */
	protected String getProjectUrl() {
		return projectUrl;
	}

}
