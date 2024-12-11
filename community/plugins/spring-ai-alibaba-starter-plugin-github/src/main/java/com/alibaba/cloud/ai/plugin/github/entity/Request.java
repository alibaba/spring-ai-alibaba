/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.plugin.github.entity;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonClassDescription("GitHub API request")
public record Request(@JsonProperty(
		value = "query") @JsonPropertyDescription("Keywords used for queries, useful for getting a list of issues") String query,

		@JsonProperty(
				value = "issueNumber") @JsonPropertyDescription("The number of the issue, which is used to get details about the issue or to leave a comment") Integer issueNumber,

		@JsonProperty(
				value = "comment") @JsonPropertyDescription("Comment content, which is suitable for commenting on an issue") String comment,

		@JsonProperty(
				value = "pullRequestNumber") @JsonPropertyDescription("The number of the pullRequest, which is used to get details about the pullRequest or to other operations") Integer pullRequestNumber,

		@JsonProperty(
				value = "pullRequestState") @JsonPropertyDescription("Pull Request state，open、closed、merged等状态") String pullRequestState,

		@JsonProperty(value = "pullRequestTitle") @JsonPropertyDescription("Pull Request 的标题") String pullRequestTitle,

		@JsonProperty(value = "pullRequestBody") @JsonPropertyDescription("Pull Request 的描述") String pullRequestBody,

		@JsonProperty(
				value = "pullRequestHead") @JsonPropertyDescription("The name of the branch where your changes are implemented.") String pullRequestHead,

		@JsonProperty(
				value = "pullRequestBase") @JsonPropertyDescription("The name of the branch you want the changes pulled into.") String pullRequestBase) {
}