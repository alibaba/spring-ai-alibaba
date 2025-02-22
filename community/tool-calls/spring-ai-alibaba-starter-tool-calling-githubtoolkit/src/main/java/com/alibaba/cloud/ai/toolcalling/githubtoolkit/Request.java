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
package com.alibaba.cloud.ai.toolcalling.githubtoolkit;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonClassDescription("GitHub API request")
public record Request(@JsonProperty(required = true,
		value = "query") @JsonPropertyDescription("Keywords used for queries, useful for getting a list of repositories") String query,
		@JsonProperty(
				value = "sort") @JsonPropertyDescription("Sorts the results of your query by number of stars, forks, or help-wanted-issues or how recently the items were updated. ") String sort,
		@JsonProperty(
				value = "order") @JsonPropertyDescription("Determines whether the first search result returned is the highest number of matches (desc) or lowest number of matches (asc). This parameter is ignored unless you provide sort.") String order,

		@JsonProperty(required = true,
				value = "issueNumber") @JsonPropertyDescription("The number of the issue, which is used to get details about the issue or to leave a comment") Integer issueNumber,

		@JsonProperty(required = true,
				value = "pullRequestTitle") @JsonPropertyDescription("the title of the Pull Request") String pullRequestTitle,

		@JsonProperty(required = true,
				value = "pullRequestBody") @JsonPropertyDescription("the description of the Pull Request") String pullRequestBody,

		@JsonProperty(required = true,
				value = "pullRequestHead") @JsonPropertyDescription("The name of the branch where your changes are implemented.") String pullRequestHead,

		@JsonProperty(required = true,
				value = "pullRequestBase") @JsonPropertyDescription("The name of the branch you want the changes pulled into.") String pullRequestBase,

		@JsonProperty(
				value = "headRepo") @JsonPropertyDescription("The name of the repository where the changes in the pull request were made. This field is required for cross-repository pull requests if both repositories are owned by the same organization.") String headRepo,
		@JsonProperty(
				value = "issue") @JsonPropertyDescription("An issue in the repository to convert to a pull request. The issue title, body, and comments will become the title, body, and comments on the new pull request. Required unless title is specified.") String issue,
		@JsonProperty(
				value = "draft") @JsonPropertyDescription("Indicates whether the pull request is a draft. ") boolean draft

) {
}
