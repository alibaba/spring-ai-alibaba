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
 * Stub knowledge base API tools for the QA assistant example.
 * In production these would call a vector database or enterprise search service.
 */
public class KnowledgeBaseTools {

	private static final Map<String, String> KNOWLEDGE_BASE = Map.of(
			"remote work policy", """
					Remote Work Policy: Employees may work remotely up to 3 days per week with manager approval.
					Core collaboration hours are 10:00-15:00. Remote work requests must be submitted
					through the HR system at least 48 hours in advance.
					""",
			"refund policy", """
					Refund Policy: Customers may request a full refund within 30 days of purchase.
					After 30 days, store credit may be issued at the discretion of customer service.
					Refunds are processed within 5-7 business days.
					""",
			"product features", """
					Product Features: Our platform supports multi-agent orchestration, real-time monitoring,
					auto-scaling, and integration with 50+ enterprise systems. Key differentiators include
					low-latency inference and built-in governance controls.
					"""
	);

	@Tool(name = "search_knowledge_base", description = "Search the enterprise knowledge base for relevant documents and answers.")
	public String searchKnowledgeBase(
			@ToolParam(description = "The search query to look up in the knowledge base") String query) {

		// Simulate knowledge base search with keyword matching
		String lowerQuery = query.toLowerCase();
		for (Map.Entry<String, String> entry : KNOWLEDGE_BASE.entrySet()) {
			if (lowerQuery.contains(entry.getKey())) {
				return String.format("Found in knowledge base:\n%s", entry.getValue());
			}
		}

		return "No relevant documents found in the knowledge base for: " + query;
	}

	@Tool(name = "list_knowledge_categories", description = "List all available categories in the knowledge base.")
	public String listKnowledgeCategories() {
		return "Available categories: " + String.join(", ", KNOWLEDGE_BASE.keySet());
	}
}
