/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.routing.simple.tools;

import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Stub tools for the GitHub vertical (code, issues, PRs) using @Tool annotation.
 * In production these would call real APIs.
 */
@Component
public class GitHubStubTools {

	@Tool(name = "search_code", description = "Search code in GitHub repositories.")
	public String searchCode(
			@ToolParam(description = "Search query") String query,
			@ToolParam(description = "Repository name", required = false) String repo) {
		String rep = repo != null ? repo : "main";
		return "Found code matching '" + query + "' in " + rep + ": authentication middleware in src/auth.py";
	}

	@Tool(name = "search_issues", description = "Search GitHub issues and pull requests.")
	public String searchIssues(@ToolParam(description = "Search query") String query) {
		return "Found 3 issues matching '" + query + "': #142 (API auth docs), #89 (OAuth flow), #203 (token refresh)";
	}

	@Tool(name = "search_prs", description = "Search pull requests for implementation details.")
	public String searchPrs(@ToolParam(description = "Search query") String query) {
		return "PR #156 added JWT authentication, PR #178 updated OAuth scopes";
	}
}
