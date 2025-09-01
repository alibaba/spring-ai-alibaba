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
package com.alibaba.cloud.ai.memory.mem0.advisor;

import com.alibaba.cloud.ai.memory.mem0.model.Mem0ServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Memory is retrieved from a Mem0 added into the prompt's system text. user text.
 *
 * @author Morain Miao
 * @since 1.0.0
 */
public class Mem0ChatMemoryAdvisor implements BaseChatMemoryAdvisor {

	private static final Logger logger = LoggerFactory.getLogger(Mem0ChatMemoryAdvisor.class);

	public static final String USER_ID = "user_id";

	public static final String AGENT_ID = "agent_id";

	public static final String RUN_ID = "run_id";

	public static final String FILTERS = "filters";

	private static final PromptTemplate DEFAULT_SYSTEM_PROMPT_TEMPLATE = new PromptTemplate(
			"""
					         ---------------------
					         USER_INPUT_MESSAGE:
					         {query}
					         ---------------------
					         Use the long term conversation memory from the LONG_TERM_MEMORY section to provide accurate answers.

					         LONG_TERM_MEMORY is a dictionary containing the search results, typically under a "results" type, and potentially "relations" type if graph store is enabled.
					         Example:
					         ```text
					         \\[
					         	 \\{
					         	  "type": "results", e.g.: vector store
					               "id": "...", e.g.: memory id
					               "memory": "...", e.g.: memory text
					               "hash": "...",  e.g.: memory hash value
					               "metadata": "...", e.g.: user custom dict
					               "score": 0.3,   e.g.: relevance score: the higher the score, the more relevant.
					               "created_at": "...", e.g.: created time
					               "updated_at": null, e.g.: updated time
					               "user_id": "...",
					               "agent_id": "...",
					               "run_id": "...",
					               "role": "..."
					             \\},
					         	\\{
					         	  "type": "relations", e.g.: graph store
					               "source": "...", e.g.: graph store source
					               "relationship": "...", e.g.: value is loves means hobby
					               "destination": "...",
					               "target": "..."
					             \\}
					         ]
					         ```

					         ---------------------
					         LONG_TERM_MEMORY:
					         {long_term_memory}
					         ---------------------
					""");

	private final PromptTemplate systemPromptTemplate;

	private final int order;

	private final Scheduler scheduler;

	private final VectorStore vectorStore;

	public Mem0ChatMemoryAdvisor(PromptTemplate systemPromptTemplate, int order, Scheduler scheduler,
			VectorStore vectorStore) {
		this.systemPromptTemplate = systemPromptTemplate;
		this.order = order;
		this.scheduler = scheduler;
		this.vectorStore = vectorStore;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
		// 1. Search for similar documents in the vector store.
		Assert.notNull(request.prompt().getUserMessage(), "User message cannot be null");
		Assert.notEmpty(request.prompt().getUserMessage().getMetadata(), "Metadata cannot contain null elements");
		Assert.isTrue(request.context().containsKey(USER_ID) || request.context().containsKey(AGENT_ID)
				|| request.context().containsKey(RUN_ID), "user_id, agent_id, and run_id cannot all be null");

		UserMessage userMessage = request.prompt().getUserMessage();
		String query = userMessage != null ? userMessage.getText() : "";

		Map<String, Object> params = request.context();
		SearchRequest searchRequest = Mem0ServerRequest.SearchRequest.builder()
			.query(query)
			.userId(params.containsKey(USER_ID) ? params.get(USER_ID).toString() : null)
			.agentId(params.containsKey(AGENT_ID) ? params.get(AGENT_ID).toString() : null)
			.runId(params.containsKey(RUN_ID) ? params.get(RUN_ID).toString() : null)
			.filters(params.containsKey(FILTERS) && params.get(FILTERS) instanceof Map
					? (Map<String, Object>) params.get(FILTERS) : null)
			.build();

		List<Document> documents = this.vectorStore.similaritySearch(searchRequest);

		String documentContext = documents == null ? ""
				: documents.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));

		// 3. Augment the user prompt with the document context.
		String augmentedUserText = this.systemPromptTemplate
			.render(Map.of("query", query, "long_term_memory", documentContext));

		Map<String, Object> metadata = userMessage.getMetadata();
		metadata.putAll(params);

		if (StringUtils.hasText(query)) {
			this.vectorStore.add(toDocuments(java.util.List.of(userMessage)));
		}
		// 4. Update ChatClientRequest with augmented prompt.
		return request.mutate().prompt(request.prompt().augmentUserMessage(augmentedUserText)).context(params).build();
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		List<Message> assistantMessages = new ArrayList<>();
		if (chatClientResponse.chatResponse() != null) {
			assistantMessages = chatClientResponse.chatResponse().getResults().stream().map(generation -> {
				Message message = generation.getOutput();
				message.getMetadata().putAll(chatClientResponse.context());
				return message;
			}).toList();
		}

		// write mem0 memory
		if (!assistantMessages.isEmpty()) {
			logger.debug("before add assistant messages to mem0 , assistantMessages: {}", assistantMessages);
			this.vectorStore.add(toDocuments(assistantMessages));
		}
		return chatClientResponse;
	}

	private List<Document> toDocuments(List<Message> messages) {
		List<Document> docs = messages.stream()
			.filter((m) -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
			.map(message -> {
				HashMap<String, Object> metadata = new HashMap<>(
						message.getMetadata() != null ? message.getMetadata() : new HashMap());
				if (message instanceof UserMessage userMessage) {
					metadata.put("role", userMessage.getMessageType().getValue());
				}
				else if (message instanceof AssistantMessage assistantMessage) {
					metadata.put("role", assistantMessage.getMessageType().getValue());
				}
				else {
					throw new RuntimeException("Unknown message type: " + message.getMessageType().getValue());
				}
				metadata.putAll(message.getMetadata());

				return Document.builder().text(message.getText()).metadata(metadata).build();
			})
			.toList();
		return docs;
	}

	private Mem0ServerRequest.SearchRequest getConversationId(Map<String, Object> context) {
		Mem0ServerRequest.SearchRequest build = Mem0ServerRequest.SearchRequest.builder()
			.userId(context.getOrDefault(USER_ID, "").toString())
			.build();
		return build;
	}

	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public static Mem0ChatMemoryAdvisor.Builder builder(VectorStore chatMemory) {
		return new Mem0ChatMemoryAdvisor.Builder(chatMemory);
	}

	public static class Builder {

		private PromptTemplate systemPromptTemplate = DEFAULT_SYSTEM_PROMPT_TEMPLATE;

		private String defaultConversationId;

		private int order;

		private Scheduler scheduler;

		private final VectorStore vectorStore;

		protected Builder(VectorStore vectorStore) {
			this.defaultConversationId = "default";
			this.scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
			this.order = -2147482648;
			this.vectorStore = vectorStore;
		}

		public Mem0ChatMemoryAdvisor.Builder systemPromptTemplate(PromptTemplate systemPromptTemplate) {
			this.systemPromptTemplate = systemPromptTemplate;
			return this;
		}

		public Mem0ChatMemoryAdvisor.Builder conversationId(String conversationId) {
			this.defaultConversationId = conversationId;
			return this;
		}

		public Mem0ChatMemoryAdvisor.Builder scheduler(Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		public Mem0ChatMemoryAdvisor.Builder order(int order) {
			this.order = order;
			return this;
		}

		public Mem0ChatMemoryAdvisor build() {
			return new Mem0ChatMemoryAdvisor(this.systemPromptTemplate, this.order, this.scheduler, this.vectorStore);
		}

	}

}
