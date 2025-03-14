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
import org.gitlab4j.api.IssuesApi;
import org.gitlab4j.api.models.Issue;
import org.gitlab4j.api.models.IssueFilter;
import org.gitlab4j.models.Constants;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * GitLab issues reader. Reads issues from GitLab projects or groups and converts them to
 * documents. Only supports public repositories.
 *
 * @author brianxiadong
 */
public class GitLabIssueReader extends AbstractGitLabReader {

	// GitLab group path for filtering issues by group
	private final String groupPath;

	// Configuration object containing all issue filtering parameters
	private final GitLabIssueConfig config;

	/**
	 * Constructor for GitLabIssueReader with default configuration (open issues).
	 * @param hostUrl GitLab host URL
	 * @param namespace Project namespace (e.g. "spring-ai")
	 * @param projectName Project name (e.g. "spring-ai")
	 * @throws GitLabApiException if project cannot be found
	 */
	public GitLabIssueReader(String hostUrl, String namespace, String projectName) throws GitLabApiException {
		this(hostUrl, namespace, projectName, null, null);
	}

	/**
	 * Constructor for GitLabIssueReader.
	 * @param hostUrl GitLab host URL
	 * @param namespace Project namespace (e.g. "spring-ai")
	 * @param projectName Project name (e.g. "spring-ai")
	 * @param groupPath Group path (optional)
	 * @param config Issue configuration (optional, defaults to open issues)
	 * @throws GitLabApiException if project cannot be found
	 */
	public GitLabIssueReader(String hostUrl, String namespace, String projectName, String groupPath,
			GitLabIssueConfig config) throws GitLabApiException {
		super(hostUrl, namespace, projectName);
		this.groupPath = groupPath;
		this.config = config != null ? config : GitLabIssueConfig.builder().state(GitLabIssueState.OPEN).build();
	}

	/**
	 * Convert a GitLab issue to a Document.
	 * @param issue GitLab issue
	 * @return Document representation of the issue
	 */
	private Document buildDocumentFromIssue(Issue issue) {
		String title = issue.getTitle();
		String description = issue.getDescription();

		Map<String, Object> metadata = new HashMap<>();

		// Required fields
		metadata.put("state", issue.getState());
		metadata.put("url", issue.getWebUrl());

		// Optional fields, only add if not empty
		if (issue.getLabels() != null && !issue.getLabels().isEmpty()) {
			metadata.put("labels", issue.getLabels());
		}

		if (issue.getCreatedAt() != null) {
			metadata.put("created_at", issue.getCreatedAt());
		}

		if (issue.getClosedAt() != null) {
			metadata.put("closed_at", issue.getClosedAt());
		}

		if (issue.getAssignee() != null && issue.getAssignee().getUsername() != null) {
			metadata.put("assignee", issue.getAssignee().getUsername());
		}

		if (issue.getAuthor() != null && issue.getAuthor().getUsername() != null) {
			metadata.put("author", issue.getAuthor().getUsername());
		}

		return new Document(String.valueOf(issue.getIid()),
				String.format("%s\n%s", title, description != null ? description : ""), metadata);
	}

	/**
	 * Convert LocalDateTime to ISO string format for GitLab API.
	 * @param dateTime LocalDateTime to convert
	 * @return ISO formatted string or null
	 */
	private Date toGitLabDateFormat(LocalDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	@Override
	public List<Document> get() {
		try {
			IssuesApi issuesApi = gitLabApi.getIssuesApi();

			// Convert Integer iids to Long iids
			List<Long> longIids = config.getIids() != null ? config.getIids().stream().map(Long::valueOf).toList()
					: null;

			// Build the filter parameters
			IssueFilter filter = new IssueFilter().withIids(longIids)
				.withState(config.getState() != null
						? Constants.IssueState.valueOf(config.getState().getValue().toUpperCase())
						: Constants.IssueState.OPENED)
				.withLabels(config.getLabels())
				.withMilestone(config.getMilestone())
				.withScope(config.getScope() != null
						? Constants.IssueScope.valueOf(config.getScope().getValue().toUpperCase()) : null)
				.withSearch(config.getSearch())
				.withCreatedAfter(
						config.getCreatedAfter() != null ? toGitLabDateFormat(config.getCreatedAfter()) : null)
				.withCreatedBefore(
						config.getCreatedBefore() != null ? toGitLabDateFormat(config.getCreatedBefore()) : null)
				.withUpdatedAfter(
						config.getUpdatedAfter() != null ? toGitLabDateFormat(config.getUpdatedAfter()) : null)
				.withUpdatedBefore(
						config.getUpdatedBefore() != null ? toGitLabDateFormat(config.getUpdatedBefore()) : null);

			// Handle assignee and author
			String assignee = config.getAssignee();
			if (assignee != null) {
				try {
					Long assigneeId = Long.parseLong(assignee);
					filter.withAssigneeId(assigneeId);
				}
				catch (NumberFormatException e) {
					// If not a number, treat as username
					filter.withoutAssigneeUsername(assignee);
				}
			}

			String author = config.getAuthor();
			if (author != null) {
				try {
					Long authorId = Long.parseLong(author);
					filter.withAuthorId(authorId);
				}
				catch (NumberFormatException e) {
					// If not a number, treat as username
					filter.withoutAuthorUsername(author);
				}
			}

			List<Issue> issues;
			if (groupPath != null) {
				// Get group issues using IssuesApi.getGroupIssues(groupPath, filter)
				issues = issuesApi.getGroupIssues(groupPath, filter);
			}
			else {
				// Get project issues using IssuesApi.getIssues(projectId, filter)
				issues = issuesApi.getIssues(project.getId(), filter);
			}

			return issues.stream().map(this::buildDocumentFromIssue).toList();

		}
		catch (GitLabApiException e) {
			throw new RuntimeException("Failed to load issues from GitLab", e);
		}
	}

}
