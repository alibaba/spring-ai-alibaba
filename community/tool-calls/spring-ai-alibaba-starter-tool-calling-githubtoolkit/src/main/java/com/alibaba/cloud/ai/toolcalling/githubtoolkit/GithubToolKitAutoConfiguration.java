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

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpHeaders;

import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX;

/**
 * @author Yeaury
 */
@Configuration
@EnableConfigurationProperties(GithubToolKitProperties.class)
@ConditionalOnClass(GithubToolKitProperties.class)
@ConditionalOnProperty(prefix = TOOL_CALLING_CONFIG_PREFIX + ".githubtoolkit", name = "enabled", havingValue = "true")
public class GithubToolKitAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("implement the function of get a GitHub issue operation")
	public GetIssueService getIssueFunction(GithubToolKitProperties properties, JsonParseTool jsonParseTool) {
		WebClientTool githubWebClientTool = githubWebClientTool(properties, jsonParseTool);
		return new GetIssueService(properties, githubWebClientTool, jsonParseTool);
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("implement the function of create GitHub pull request operation")
	public CreatePullRequestService createPullRequestFunction(GithubToolKitProperties properties,
			JsonParseTool jsonParseTool) {
		WebClientTool githubWebClientTool = githubWebClientTool(properties, jsonParseTool);
		return new CreatePullRequestService(properties, githubWebClientTool, jsonParseTool);
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("implement the function of search the list of repositories operation")
	public SearchRepositoryService SearchRepositoryFunction(GithubToolKitProperties properties,
			JsonParseTool jsonParseTool) {
		WebClientTool githubWebClientTool = githubWebClientTool(properties, jsonParseTool);
		return new SearchRepositoryService(githubWebClientTool, jsonParseTool);
	}

	public WebClientTool githubWebClientTool(GithubToolKitProperties properties, JsonParseTool jsonParseTool) {
		return WebClientTool.builder(jsonParseTool, properties).httpHeadersConsumer(headers -> {
			headers.set(HttpHeaders.USER_AGENT, HttpHeaders.USER_AGENT);
			headers.set(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");
			headers.set("X-GitHub-Api-Version", GithubToolKitProperties.X_GitHub_Api_Version);
			headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken());
		}).build();
	}

}
