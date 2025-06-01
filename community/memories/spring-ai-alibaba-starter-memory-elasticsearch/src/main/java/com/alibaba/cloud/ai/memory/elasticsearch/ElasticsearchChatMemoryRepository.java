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
package com.alibaba.cloud.ai.memory.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Elasticsearch implementation of ChatMemoryRepository
 */
public class ElasticsearchChatMemoryRepository implements ChatMemoryRepository, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchChatMemoryRepository.class);

	private static final String INDEX_NAME = "chat_memory";

	// private final ElasticsearchConfig config;

	private final ElasticsearchClient client;

	private final ObjectMapper objectMapper;

	public ElasticsearchChatMemoryRepository(ElasticsearchClient client) {
		this.objectMapper = new ObjectMapper();
		// Configure Jackson to ignore unknown properties to handle schema changes
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			this.client = client;
			createIndexIfNotExists();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to create Elasticsearch client", e);
		}
	}

	private void createIndexIfNotExists() throws IOException {
		if (!client.indices().exists(e -> e.index(INDEX_NAME)).value()) {
			createIndex();
		}
	}

	private void createIndex() throws IOException {
		client.indices()
			.create(c -> c.index(INDEX_NAME)
				.mappings(m -> m.properties("conversationId", p -> p.keyword(k -> k))
					.properties("messageType", p -> p.keyword(k -> k))
					.properties("messageText", p -> p.text(t -> t))
					.properties("timestamp", p -> p.date(d -> d))));
	}

	public void recreateIndex() throws IOException {
		if (client.indices().exists(e -> e.index(INDEX_NAME)).value()) {
			client.indices().delete(d -> d.index(INDEX_NAME));
		}
		createIndex();
	}

	// private ElasticsearchClient createClient(ElasticsearchConfig config)
	// throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
	// // Create HttpHosts for all nodes
	// HttpHost[] httpHosts;
	// if (!CollectionUtils.isEmpty(config.getNodes())) {
	// httpHosts = config.getNodes().stream().map(node -> {
	// String[] parts = node.split(":");
	// return new HttpHost(parts[0], Integer.parseInt(parts[1]), config.getScheme());
	// }).toArray(HttpHost[]::new);
	// }
	// else {
	// // Fallback to single node configuration
	// httpHosts = new HttpHost[] { new HttpHost(config.getHost(), config.getPort(),
	// config.getScheme()) };
	// }
	//
	// var restClientBuilder = RestClient.builder(httpHosts);
	//
	// // Add authentication if credentials are provided
	// if (StringUtils.hasText(config.getUsername()) &&
	// StringUtils.hasText(config.getPassword())) {
	// CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
	// credentialsProvider.setCredentials(AuthScope.ANY,
	// new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));
	//
	// // Create SSL context if using HTTPS
	// if ("https".equalsIgnoreCase(config.getScheme())) {
	// SSLContext sslContext = SSLContextBuilder.create()
	// .loadTrustMaterial(null, (chains, authType) -> true)
	// .build();
	//
	// restClientBuilder.setHttpClientConfigCallback(
	// httpClientBuilder ->
	// httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
	// .setSSLContext(sslContext)
	// .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE));
	// }
	// else {
	// restClientBuilder.setHttpClientConfigCallback(
	// httpClientBuilder ->
	// httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
	// }
	// }
	//
	// // Create the transport and client
	// ElasticsearchTransport transport = new
	// RestClientTransport(restClientBuilder.build(), new JacksonJsonpMapper());
	// return new ElasticsearchClient(transport);
	// }

	@Override
	public List<String> findConversationIds() {
		try {
			SearchResponse<ChatMessage> response = client
				.search(s -> s.index(INDEX_NAME).size(10000).query(q -> q.matchAll(m -> m)), ChatMessage.class);

			return response.hits()
				.hits()
				.stream()
				.map(hit -> hit.source().getConversationId())
				.distinct()
				.collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new RuntimeException("Error finding conversation IDs", e);
		}
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		try {
			logger.info("Finding messages for conversation: {}", conversationId);
			SearchResponse<ChatMessage> response = client.search(s -> s.index(INDEX_NAME)
				.query(q -> q.term(t -> t.field("conversationId").value(conversationId)))
				.sort(sort -> sort
					.field(f -> f.field("timestamp").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc))),
					ChatMessage.class);

			List<Message> messages = response.hits()
				.hits()
				.stream()
				.map(hit -> hit.source().toSpringMessage())
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

			logger.info("Found {} messages for conversation: {}", messages.size(), conversationId);
			return messages;
		}
		catch (IOException e) {
			logger.error("Error finding messages for conversation: {}", conversationId, e);
			throw new RuntimeException("Error finding messages for conversation: " + conversationId, e);
		}
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");

		try {
			// Delete existing messages first
			deleteByConversationId(conversationId);

			// Bulk insert new messages
			BulkRequest.Builder br = new BulkRequest.Builder();
			for (Message message : messages) {
				ChatMessage chatMessage = new ChatMessage(conversationId, message);
				logger.info("Saving message for {}: type={}, text={}", conversationId, chatMessage.getMessageType(),
						chatMessage.getMessageText());
				br.operations(op -> op.index(idx -> idx.index(INDEX_NAME).document(chatMessage)));
			}

			BulkResponse response = client.bulk(br.build());
			if (response.errors()) {
				logger.error("Error saving messages: {}",
						response.items()
							.stream()
							.filter(item -> item.error() != null)
							.map(item -> item.error().reason())
							.collect(Collectors.joining(", ")));
				throw new RuntimeException("Error saving messages to Elasticsearch");
			}

			// Ensure index is refreshed immediately
			client.indices().refresh(r -> r.index(INDEX_NAME));
			logger.info("Successfully saved {} messages for conversation {}", messages.size(), conversationId);
		}
		catch (IOException e) {
			logger.error("Error saving messages", e);
			throw new RuntimeException("Error saving messages", e);
		}
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		try {
			DeleteByQueryResponse response = client.deleteByQuery(
					d -> d.index(INDEX_NAME).query(q -> q.term(t -> t.field("conversationId").value(conversationId))));

			if (response.failures().size() > 0) {
				throw new RuntimeException("Error deleting messages for conversation: " + conversationId);
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Error deleting messages", e);
		}
	}

	public void clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		try {
			SearchResponse<ChatMessage> response = client.search(s -> s.index(INDEX_NAME)
				.query(q -> q.term(t -> t.field("conversationId").value(conversationId)))
				.sort(sort -> sort
					.field(f -> f.field("timestamp").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc))),
					ChatMessage.class);

			List<ChatMessage> messages = response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());

			if (messages.size() >= maxLimit) {
				// Delete all messages
				deleteByConversationId(conversationId);

				// Reinsert messages after skipping the oldest ones
				List<ChatMessage> messagesToKeep = messages.stream().skip(deleteSize).collect(Collectors.toList());

				BulkRequest.Builder br = new BulkRequest.Builder();
				for (ChatMessage message : messagesToKeep) {
					br.operations(op -> op.index(idx -> idx.index(INDEX_NAME).document(message)));
				}

				BulkResponse bulkResponse = client.bulk(br.build());
				if (bulkResponse.errors()) {
					throw new RuntimeException("Error saving messages to Elasticsearch");
				}

				// Force refresh the index to make changes available immediately
				client.indices().refresh(r -> r.index(INDEX_NAME));
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Error clearing over limit messages", e);
		}
	}

	@Override
	public void close() {
		// Elasticsearch client doesn't need explicit closing
		if (Objects.nonNull(this.client)) {
			this.client.shutdown();
		}
	}

	// Debug method to diagnose search issues
	public String rawSearchQuery(String conversationId) throws IOException {
		// Get the raw source field to inspect document structure
		var allResponse = client
			.search(s -> s.index(INDEX_NAME).query(q -> q.matchAll(m -> m)).size(100).source(src -> src.fetch(true)) // Explicitly
			// request
			// source
			// fields
					, Void.class);

		// Try both keyword and non-keyword field for conversationId
		var byIdResponseKeyword = client.search(s -> s.index(INDEX_NAME)
			.query(q -> q.term(t -> t.field("conversationId.keyword").value(conversationId)))
			.size(100)
			.source(src -> src.fetch(true)), Void.class);

		var byIdResponseNoKeyword = client.search(s -> s.index(INDEX_NAME)
			.query(q -> q.term(t -> t.field("conversationId").value(conversationId)))
			.size(100)
			.source(src -> src.fetch(true)), Void.class);

		StringBuilder sb = new StringBuilder();
		sb.append("=== All documents (").append(allResponse.hits().total().value()).append(") ===\n");
		sb.append(allResponse.toString()).append("\n\n");

		sb.append("=== Documents for conversation with keyword field ")
			.append(conversationId)
			.append(" (")
			.append(byIdResponseKeyword.hits().total().value())
			.append(") ===\n");
		sb.append(byIdResponseKeyword.toString()).append("\n\n");

		sb.append("=== Documents for conversation without keyword field ")
			.append(conversationId)
			.append(" (")
			.append(byIdResponseNoKeyword.hits().total().value())
			.append(") ===\n");
		sb.append(byIdResponseNoKeyword.toString());

		return sb.toString();
	}

	private static class ChatMessage {

		private String conversationId;

		private String messageType;

		private String messageText;

		private long timestamp;

		// For backward compatibility with existing data
		private Object message;

		public ChatMessage() {
		}

		public ChatMessage(String conversationId, Message message) {
			this.conversationId = conversationId;
			this.messageType = message.getMessageType().toString();
			this.messageText = message.getText();
			this.timestamp = System.currentTimeMillis();
		}

		public String getConversationId() {
			return conversationId;
		}

		public void setConversationId(String conversationId) {
			this.conversationId = conversationId;
		}

		public String getMessageType() {
			return messageType;
		}

		public void setMessageType(String messageType) {
			this.messageType = messageType;
		}

		public String getMessageText() {
			return messageText;
		}

		public void setMessageText(String messageText) {
			this.messageText = messageText;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		public Object getMessage() {
			return message;
		}

		public void setMessage(Object message) {
			this.message = message;
		}

		public Message toSpringMessage() {
			try {
				if (messageType != null && messageText != null) {
					switch (MessageType.valueOf(messageType)) {
						case USER:
							return new UserMessage(messageText);
						case ASSISTANT:
							return new AssistantMessage(messageText);
						case SYSTEM:
							return new SystemMessage(messageText);
						default:
							throw new IllegalStateException("Unknown message type: " + messageType);
					}
				}
				// Old format compatibility (using reflection or casting)
				else if (message != null) {
					// For debugging purposes
					logger.info("Using legacy message format: {}", message);
					// Just create a default message to avoid errors
					return new UserMessage("Legacy message - please reindex");
				}
				return null;
			}
			catch (Exception e) {
				logger.error("Error converting message", e);
				return new UserMessage("Error: " + e.getMessage());
			}
		}

	}

}
