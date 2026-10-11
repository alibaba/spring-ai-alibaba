/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.multiagents.qa.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Map;

/**
 * Stub web search API tools for the QA assistant example.
 * In production these would call a search engine API like Google, Bing, or Brave Search.
 */
public class WebSearchTools {

	private static final Map<String, String> WEB_RESULTS = Map.of(
			"ai agent frameworks", """
					Top AI Agent Frameworks in 2026:
					1. LangChain / LangGraph - Most widely adopted, strong ecosystem
					2. Spring AI Alibaba - Enterprise-focused, Java-native agent framework
					3. CrewAI - Role-based multi-agent orchestration
					4. AutoGen - Microsoft's conversational agent framework
					5. MetaGPT - Multi-agent software development
					Source: https://github.com/topics/ai-agent
					""",
			"competitors", """
					Market Analysis - AI Agent Platform Competitors:
					- Vendor A: Strong in NLP, weak in multi-agent orchestration
					- Vendor B: Good UI, limited enterprise integration
					- Vendor C: Open-source, smaller community
					Our platform leads in enterprise integration and governance features.
					Source: Industry Report 2026
					"""
	);

	@Tool(name = "search_web", description = "Search the web for up-to-date information using a search engine.")
	public String searchWeb(
			@ToolParam(description = "The search query") String query,
			@ToolParam(description = "Maximum number of results to return", required = false) Integer maxResults) {

		int limit = maxResults != null ? maxResults : 5;
		String lowerQuery = query.toLowerCase();

		for (Map.Entry<String, String> entry : WEB_RESULTS.entrySet()) {
			if (lowerQuery.contains(entry.getKey())) {
				return String.format("Web search results (top %d):\n%s", limit, entry.getValue());
			}
		}

		return String.format("Web search for '%s': No specific results found. Try rephrasing your query.", query);
	}

	@Tool(name = "fetch_web_page", description = "Fetch and extract content from a specific URL.")
	public String fetchWebPage(
			@ToolParam(description = "The URL to fetch") String url) {
		return String.format("Fetched content from %s: [Page content would be extracted here]", url);
	}
}
