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
package com.alibaba.cloud.ai.memory.mongodb;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.*;
import com.mongodb.client.model.Indexes;
import io.micrometer.common.util.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.orderBy;

/**
 * MongoDB implementation of ChatMemoryRepository
 */
public class MongoDBChatMemoryRepository implements ChatMemoryRepository, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(MongoDBChatMemoryRepository.class);

	private static final String COLLECTION_NAME = "chat_memory";

	private static final String CONVERSATION_ID_FIELD = "conversationId";

	private static final String MESSAGE_TYPE_FIELD = "messageType";

	private static final String MESSAGE_TEXT_FIELD = "messageText";

	private static final String TIMESTAMP_FIELD = "timestamp";

	private final MongoClient mongoClient;

	private final MongoCollection<Document> collection;

	private final ObjectMapper objectMapper;

	private final String databaseName;

	public MongoDBChatMemoryRepository(MongoClient mongoClient, String databaseName) {
		this.databaseName = databaseName;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			this.mongoClient = mongoClient;
			MongoDatabase database = mongoClient.getDatabase(databaseName);
			this.collection = database.getCollection(COLLECTION_NAME);
			createIndexesIfNotExists();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to create MongoDB client", e);
		}
	}

	public static MongoDBBuilder builder() {
		return new MongoDBBuilder();
	}

	private void createIndexesIfNotExists() {
		collection.createIndex(Indexes.ascending(CONVERSATION_ID_FIELD, TIMESTAMP_FIELD));
		collection.createIndex(Indexes.ascending(CONVERSATION_ID_FIELD));
	}

	public void recreateCollection() {
		MongoDatabase database = mongoClient.getDatabase(databaseName);
		if (database.listCollectionNames().into(new ArrayList<>()).contains(COLLECTION_NAME)) {
			collection.drop();
		}
		database.createCollection(COLLECTION_NAME);
		createIndexesIfNotExists();
	}

	@Override
	public List<String> findConversationIds() {
		try {
			DistinctIterable<String> distinctIds = collection.distinct(CONVERSATION_ID_FIELD, String.class);
			return distinctIds.into(new ArrayList<>());
		}
		catch (Exception e) {
			throw new RuntimeException("Error finding conversation IDs", e);
		}
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		try {
			logger.info("Finding messages for conversation: {}", conversationId);
			Bson filter = eq(CONVERSATION_ID_FIELD, conversationId);
			FindIterable<Document> documents = collection.find(filter).sort(orderBy(ascending(TIMESTAMP_FIELD)));

			List<Message> messages = new ArrayList<>();
			for (Document doc : documents) {
				Message message = documentToMessage(doc);
				if (message != null) {
					messages.add(message);
				}
			}
			logger.info("Found {} messages for conversation: {}", messages.size(), conversationId);
			return messages;
		}
		catch (Exception e) {
			logger.error("Error finding messages for conversation:{} ", conversationId, e);
			throw new RuntimeException("Error finding messages for conversation: " + conversationId, e);
		}
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");
		try {
			deleteByConversationId(conversationId);
			List<Document> documents = messages.stream()
				.map(message -> messageToDocument(conversationId, message))
				.collect(Collectors.toList());
			if (!documents.isEmpty()) {
				collection.insertMany(documents);
				logger.info("Successfully saved {} messages for conversation {}", messages.size(), conversationId);
			}
		}
		catch (Exception e) {
			logger.error("Error saving messages", e);
			throw new RuntimeException("Error saving messages", e);
		}
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		try {
			Bson filter = eq(CONVERSATION_ID_FIELD, conversationId);
			collection.deleteMany(filter);
		}
		catch (Exception e) {
			throw new RuntimeException("Error deleting messages", e);
		}
	}

	public void clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		try {
			Bson filter = eq(CONVERSATION_ID_FIELD, conversationId);
			FindIterable<Document> documents = collection.find(filter).sort(orderBy(ascending(TIMESTAMP_FIELD)));
			List<Document> messages = documents.into(new ArrayList<>());
			if (messages.size() >= maxLimit) {
				deleteByConversationId(conversationId);
				if (deleteSize < messages.size()) {
					List<Document> messagesToKeep = messages.subList(deleteSize, messages.size());
					if (!messagesToKeep.isEmpty()) {
						collection.insertMany(messagesToKeep);
					}
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Error clearing over limit messages", e);
		}
	}

	@Override
	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	private Message documentToMessage(Document doc) {
		try {
			String messageType = doc.getString(MESSAGE_TYPE_FIELD);
			String messageText = doc.getString(MESSAGE_TEXT_FIELD);

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
			return null;
		}
		catch (Exception e) {
			logger.error("Error converting message", e);
			return new UserMessage("Error: " + e.getMessage());
		}
	}

	private Document messageToDocument(String conversationId, Message message) {
		Document doc = new Document();
		doc.put(CONVERSATION_ID_FIELD, conversationId);
		doc.put(MESSAGE_TYPE_FIELD, message.getMessageType().toString());
		doc.put(MESSAGE_TEXT_FIELD, message.getText());
		doc.put(TIMESTAMP_FIELD, System.currentTimeMillis());
		return doc;
	}

	public static class MongoDBBuilder {

		private String host = "127.0.0.1";

		private int port = 27017;

		private String userName;

		private String password;

		private String authDatabaseName = "admin";

		private String databaseName = "spring_ai";

		public MongoDBBuilder host(String host) {
			this.host = host;
			return this;
		}

		public MongoDBBuilder port(int port) {
			this.port = port;
			return this;
		}

		public MongoDBBuilder userName(String userName) {
			this.userName = userName;
			return this;
		}

		public MongoDBBuilder password(String password) {
			this.password = password;
			return this;
		}

		public MongoDBBuilder authDatabaseName(String authDatabaseName) {
			this.authDatabaseName = authDatabaseName;
			return this;
		}

		public MongoDBBuilder databaseName(String databaseName) {
			this.databaseName = databaseName;
			return this;
		}

		public MongoDBChatMemoryRepository build() {
			ServerAddress serverAddress = new ServerAddress(host, port);
			MongoClientSettings.Builder build = MongoClientSettings.builder();
			if (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password)) {
				MongoCredential credential = MongoCredential.createCredential(userName, authDatabaseName,
						password.toCharArray());
				build.credential(credential);
			}
			MongoClientSettings settings = build
				.applyToClusterSettings(builder -> builder.hosts(List.of(serverAddress)))
				.build();
			MongoClient mongoClient = MongoClients.create(settings);
			return new MongoDBChatMemoryRepository(mongoClient, databaseName);
		}

	}

}
