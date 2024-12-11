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

package com.alibaba.cloud.ai.plugin.github.service;

import com.alibaba.cloud.ai.plugin.github.GithubProperties;
import com.alibaba.cloud.ai.plugin.github.entity.Request;
import com.alibaba.cloud.ai.plugin.github.entity.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.function.Function;

public abstract class GithubService implements Function<Request, Response> {

	protected static final String GITHUB_API_URL = "https://api.github.com";

	protected static final String REPO_ENDPOINT = "/repos/{owner}/{repo}";

	protected static final String ISSUES_ENDPOINT = "/issues";

	protected static final String PULL_REQUESTS_ENDPOINT = "/pulls";

	protected static final String DEFAULT_PER_PAGE = "10";

	protected final WebClient webClient;

	protected final GithubProperties properties;

	public GithubService(GithubProperties properties) {
		assert properties.getToken() != null && properties.getToken().length() == 40;
		this.properties = properties;
		this.webClient = WebClient.builder()
			.defaultHeader(HttpHeaders.USER_AGENT, HttpHeaders.USER_AGENT)
			.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
			.defaultHeader("X-GitHub-Api-Version", GithubProperties.X_GitHub_Api_Version)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
			.build();
	}

	public abstract Response apply(Request request);

}
