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
package com.alibaba.cloud.ai.functioncalling.githubtoolkit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author Yeaury
 */
@EnableConfigurationProperties(GithubToolKitProperties.class)
public class GithubToolKitAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("implement the function of get a GitHub issue operation")
	public GetIssueService getIssueService(GithubToolKitProperties properties) {
		return new GetIssueService(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("implement the function of create GitHub pull request operation")
	public CreatePullRequestService createPullRequestService(GithubToolKitProperties properties) {
		return new CreatePullRequestService(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("implement the function of search the list of repositories operation")
	public SearchRepositoryService SearchRepositoryService(GithubToolKitProperties properties) {
		return new SearchRepositoryService(properties);
	}

}
