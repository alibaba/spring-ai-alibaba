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

import java.time.LocalDateTime;
import java.util.List;

/**
 * Configuration class for GitLab issue reader. Contains all parameters for filtering and
 * retrieving issues.
 *
 * @author brianxiadong
 */
public class GitLabIssueConfig {

	// Assignee username to filter issues
	private String assignee;

	// Author username to filter issues
	private String author;

	// Whether to return only confidential issues
	private Boolean confidential;

	// Return issues created after this date
	private LocalDateTime createdAfter;

	// Return issues created before this date
	private LocalDateTime createdBefore;

	// List of issue IIDs to filter
	private List<Integer> iids;

	// Type of issues to return (issue, incident, test_case)
	private GitLabIssueType issueType;

	// Labels to filter issues
	private List<String> labels;

	// Milestone title to filter issues
	private String milestone;

	// Whether to return only non-archived issues
	private Boolean nonArchived;

	// Scope of issues to return (created_by_me, assigned_to_me, all)
	private GitLabScope scope;

	// Search query to filter issues
	private String search;

	// State of issues to return (opened, closed, all)
	private GitLabIssueState state;

	// Return issues updated after this date
	private LocalDateTime updatedAfter;

	// Return issues updated before this date
	private LocalDateTime updatedBefore;

	private GitLabIssueConfig() {
		// Use builder pattern to create instances
	}

	public String getAssignee() {
		return assignee;
	}

	public String getAuthor() {
		return author;
	}

	public Boolean getConfidential() {
		return confidential;
	}

	public LocalDateTime getCreatedAfter() {
		return createdAfter;
	}

	public LocalDateTime getCreatedBefore() {
		return createdBefore;
	}

	public List<Integer> getIids() {
		return iids;
	}

	public GitLabIssueType getIssueType() {
		return issueType;
	}

	public List<String> getLabels() {
		return labels;
	}

	public String getMilestone() {
		return milestone;
	}

	public Boolean getNonArchived() {
		return nonArchived;
	}

	public GitLabScope getScope() {
		return scope;
	}

	public String getSearch() {
		return search;
	}

	public GitLabIssueState getState() {
		return state;
	}

	public LocalDateTime getUpdatedAfter() {
		return updatedAfter;
	}

	public LocalDateTime getUpdatedBefore() {
		return updatedBefore;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final GitLabIssueConfig config;

		private Builder() {
			config = new GitLabIssueConfig();
		}

		public Builder assignee(String assignee) {
			config.assignee = assignee;
			return this;
		}

		public Builder author(String author) {
			config.author = author;
			return this;
		}

		public Builder confidential(Boolean confidential) {
			config.confidential = confidential;
			return this;
		}

		public Builder createdAfter(LocalDateTime createdAfter) {
			config.createdAfter = createdAfter;
			return this;
		}

		public Builder createdBefore(LocalDateTime createdBefore) {
			config.createdBefore = createdBefore;
			return this;
		}

		public Builder iids(List<Integer> iids) {
			config.iids = iids;
			return this;
		}

		public Builder issueType(GitLabIssueType issueType) {
			config.issueType = issueType;
			return this;
		}

		public Builder labels(List<String> labels) {
			config.labels = labels;
			return this;
		}

		public Builder milestone(String milestone) {
			config.milestone = milestone;
			return this;
		}

		public Builder nonArchived(Boolean nonArchived) {
			config.nonArchived = nonArchived;
			return this;
		}

		public Builder scope(GitLabScope scope) {
			config.scope = scope;
			return this;
		}

		public Builder search(String search) {
			config.search = search;
			return this;
		}

		public Builder state(GitLabIssueState state) {
			config.state = state != null ? state : GitLabIssueState.OPEN;
			return this;
		}

		public Builder updatedAfter(LocalDateTime updatedAfter) {
			config.updatedAfter = updatedAfter;
			return this;
		}

		public Builder updatedBefore(LocalDateTime updatedBefore) {
			config.updatedBefore = updatedBefore;
			return this;
		}

		public GitLabIssueConfig build() {
			return config;
		}

	}

}
