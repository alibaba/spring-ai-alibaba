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
package com.alibaba.cloud.ai.memory.tablestore;

import com.alicloud.openservices.tablestore.SyncClient;
import com.aliyun.openservices.tablestore.agent.memory.MemoryStore;
import com.aliyun.openservices.tablestore.agent.memory.MemoryStoreImpl;
import com.aliyun.openservices.tablestore.agent.model.MetaType;
import com.aliyun.openservices.tablestore.agent.model.Session;
import com.aliyun.openservices.tablestore.agent.util.Pair;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Tablestore implementation of ChatMemoryRepository
 */
public class TablestoreChatMemoryRepository implements ChatMemoryRepository {

	private SyncClient client;

	private String sessionTableName = "session";

	private String sessionSecondaryIndexName = "session_secondary_index";

	private List<Pair<String, MetaType>> sessionSecondaryIndexMeta = Collections.emptyList();

	private String messageTableName = "message";

	private String messageSecondaryIndexName = "message_secondary_index";

	private MemoryStoreImpl store;

	public TablestoreChatMemoryRepository(MemoryStoreImpl store) {
		this.store = store;
	}

	public TablestoreChatMemoryRepository(SyncClient client, String sessionTableName, String sessionSecondaryIndexName,
			List<Pair<String, MetaType>> sessionSecondaryIndexMeta, String messageTableName,
			String messageSecondaryIndexName) {
		this.client = client;
		this.sessionTableName = sessionTableName;
		this.sessionSecondaryIndexName = sessionSecondaryIndexName;
		this.sessionSecondaryIndexMeta = sessionSecondaryIndexMeta;
		this.messageTableName = messageTableName;
		this.messageSecondaryIndexName = messageSecondaryIndexName;
	}

	public TablestoreChatMemoryRepository(SyncClient client) {
		this.client = client;
	}

	public MemoryStoreImpl getStore() {
		if (store == null) {
			synchronized (TablestoreChatMemoryRepository.class) {
				if (store == null) {
					store = MemoryStoreImpl.builder()
						.client(client)
						.sessionTableName(sessionTableName)
						.sessionSecondaryIndexName(sessionSecondaryIndexName)
						.sessionSecondaryIndexMeta(sessionSecondaryIndexMeta)
						.messageTableName(messageTableName)
						.messageSecondaryIndexName(messageSecondaryIndexName)
						.build();
					store.initTable();
				}
			}
		}
		return store;
	}

	@Override
	public List<String> findConversationIds() {
		MemoryStore store = getStore();
		Iterator<Session> iterator = store.listAllSessions();
		List<String> list = new ArrayList<>();
		while (iterator != null && iterator.hasNext() && list.size() < 10000) {
			Session next = iterator.next();
			String sessionId = next.getSessionId();
			list.add(sessionId);
		}
		return list;
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Iterator<com.aliyun.openservices.tablestore.agent.model.Message> iterator = getStore()
			.listMessages(conversationId);
		List<Message> messages = new ArrayList<>();
		while (iterator != null && iterator.hasNext()) {
			com.aliyun.openservices.tablestore.agent.model.Message next = iterator.next();
			Message message = MessageUtils.toSpringMessage(next);
			messages.add(message);
		}
		return messages;
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		deleteByConversationId(conversationId);
		String md5UserId = MessageUtils.getMD5UserId(conversationId);
		Session session = new Session(md5UserId, conversationId);
		getStore().putSession(session);
		session.getMetadata().put("messagesCount", messages.size());
		for (Message message : messages) {
			com.aliyun.openservices.tablestore.agent.model.Message tablestoreMessage = MessageUtils
				.toTablestoreMessage(conversationId, message);
			getStore().putMessage(tablestoreMessage);
		}
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		String md5UserId = MessageUtils.getMD5UserId(conversationId);
		getStore().deleteSessionAndMessages(md5UserId, conversationId);
	}

	public SyncClient getClient() {
		return client;
	}

	public String getSessionTableName() {
		return sessionTableName;
	}

	public String getSessionSecondaryIndexName() {
		return sessionSecondaryIndexName;
	}

	public List<Pair<String, MetaType>> getSessionSecondaryIndexMeta() {
		return sessionSecondaryIndexMeta;
	}

	public String getMessageTableName() {
		return messageTableName;
	}

	public String getMessageSecondaryIndexName() {
		return messageSecondaryIndexName;
	}

}
