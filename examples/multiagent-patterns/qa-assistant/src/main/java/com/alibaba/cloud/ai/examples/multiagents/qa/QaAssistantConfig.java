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
package com.alibaba.cloud.ai.examples.multiagents.qa;

import com.alibaba.cloud.ai.examples.multiagents.qa.tools.KnowledgeBaseTools;
import com.alibaba.cloud.ai.examples.multiagents.qa.tools.WebSearchTools;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the QA assistant: stub tools, knowledge base agent, web search agent,
 * and supervisor agent. Specialized agents (knowledge base, web search) are wrapped as tools
 * via {@link AgentTool} and configured with {@link ReactAgent#systemPrompt(String)} and
 * {@link ReactAgent.Builder#inputType(java.lang.reflect.Type)}.
 */
@Configuration
public class QaAssistantConfig {

	private static final String KB_AGENT_PROMPT = """
			You are a knowledge base assistant. \
			Search the enterprise knowledge base to find accurate answers to user questions. \
			Use search_knowledge_base to look up relevant documents. \
			When you find relevant information, synthesize a clear and concise answer. \
			If the knowledge base doesn't contain the answer, clearly state that you couldn't find relevant information.
			""";

	private static final String WEB_AGENT_PROMPT = """
			You are a web search assistant. \
			Search the web to find up-to-date information for user questions. \
			Use search_web to look up relevant information from the internet. \
			When you find relevant results, synthesize a clear and concise answer with source references. \
			Always cite the sources you found.
			""";

	private static final String SUPERVISOR_PROMPT = """
			You are a helpful QA assistant. \
			You can search the enterprise knowledge base and the web to answer user questions. \
			Break down user queries and decide which tool(s) to use. \
			For company-internal questions, prefer the knowledge base. \
			For general or up-to-date information, use web search. \
			Combine results from both sources when appropriate to provide comprehensive answers.
			""";

	private static final String KB_SEARCH_DESCRIPTION = """
			Search the enterprise knowledge base for relevant documents and answers.
			Use this when the user asks company-specific questions, product documentation,
			internal policies, or any information that may exist in the knowledge base.
			Input: Natural language query (e.g., 'What is our refund policy?')
			""";

	private static final String WEB_SEARCH_DESCRIPTION = """
			Search the web for up-to-date information.
			Use this when the user asks general knowledge questions, current events,
			or when the knowledge base doesn't have the answer.
			Input: Natural language query (e.g., 'What are the latest AI trends in 2026?')
			""";

	@Bean
	public MemorySaver memorySaver() {
		return new MemorySaver();
	}

	@Bean
	public KnowledgeBaseTools knowledgeBaseTools() {
		return new KnowledgeBaseTools();
	}

	@Bean
	public WebSearchTools webSearchTools() {
		return new WebSearchTools();
	}

	@Bean
	public ReactAgent kbAgent(ChatModel chatModel, KnowledgeBaseTools knowledgeBaseTools) {
		return ReactAgent.builder()
				.name("search_knowledge_base")
				.description(KB_SEARCH_DESCRIPTION)
				.systemPrompt(KB_AGENT_PROMPT)
				.model(chatModel)
				.methodTools(knowledgeBaseTools)
				.inputType(String.class)
				.build();
	}

	@Bean
	public ReactAgent webAgent(ChatModel chatModel, WebSearchTools webSearchTools) {
		return ReactAgent.builder()
				.name("search_web")
				.description(WEB_SEARCH_DESCRIPTION)
				.systemPrompt(WEB_AGENT_PROMPT)
				.model(chatModel)
				.methodTools(webSearchTools)
				.inputType(String.class)
				.build();
	}

	@Bean
	public ReactAgent qaSupervisorAgent(ChatModel chatModel, ReactAgent kbAgent, ReactAgent webAgent, MemorySaver memorySaver) {
		return ReactAgent.builder()
				.name("qa_assistant")
				.systemPrompt(SUPERVISOR_PROMPT)
				.model(chatModel)
				.saver(memorySaver)
				.tools(
						AgentTool.getFunctionToolCallback(kbAgent),
						AgentTool.getFunctionToolCallback(webAgent))
				.build();
	}
}
