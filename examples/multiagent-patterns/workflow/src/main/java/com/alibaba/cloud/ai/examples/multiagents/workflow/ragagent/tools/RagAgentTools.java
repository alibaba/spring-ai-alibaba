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
package com.alibaba.cloud.ai.examples.multiagents.workflow.ragagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Tools for the RAG agent. Enables fetching live news alongside retrieved context.
 */
public class RagAgentTools {

	@Tool(name = "get_latest_news", description = "Get the latest WNBA news and updates.")
	public String getLatestNews(@ToolParam(description = "Query for news") String query) {
		return "Latest: The WNBA announced expanded playoff format for 2025. Caitlin Clark breaks rookie assist record.";
	}
}
