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
package com.alibaba.cloud.ai.graph.checkpoint.savers;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.ClientSessionOptions;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.lang.String.format;

public class MongoSaver implements BaseCheckpointSaver {

	private static final Logger logger = LoggerFactory.getLogger(MongoSaver.class);

	private MongoClient client;

	private MongoDatabase database;

	private TransactionOptions txnOptions;

	private final ObjectMapper objectMapper;

	private static final String DB_NAME = "check_point_db";

	private static final String COLLECTION_NAME = "checkpoint_collection";

	private static final String DOCUMENT_PREFIX = "mongo:checkpoint:document:";

	private static final String DOCUMENT_CONTENT_KEY = "checkpoint_content";

	/**
	 * Instantiates a new Mongo saver.
	 * @param client the client
	 */
	public MongoSaver(MongoClient client) {
		this.client = client;
		this.database = client.getDatabase(DB_NAME);
		this.txnOptions = TransactionOptions.builder().writeConcern(WriteConcern.MAJORITY).build();
		this.objectMapper = new ObjectMapper();
		Runtime.getRuntime().addShutdownHook(new Thread(client::close));
	}

	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			// Sets transaction options
			ClientSession clientSession = this.client
				.startSession(ClientSessionOptions.builder().defaultTransactionOptions(txnOptions).build());
			clientSession.startTransaction();
			List<Checkpoint> checkpoints = null;
			try {
				MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
				BasicDBObject dbObject = new BasicDBObject("_id", DOCUMENT_PREFIX + configOption.get());
				Document document = collection.find(dbObject).first();
				if (document == null)
					return Collections.emptyList();
				String checkpointsStr = document.getString(DOCUMENT_CONTENT_KEY);
				checkpoints = objectMapper.readValue(checkpointsStr, new TypeReference<>() {
				});
				clientSession.commitTransaction();
			}
			catch (Exception e) {
				clientSession.abortTransaction();
				throw new RuntimeException(e);
			}
			finally {
				clientSession.close();
			}
			return checkpoints;
		}
		else {
			throw new IllegalArgumentException("threadId is not allow null");
		}
	}

	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			// lock
			ClientSession clientSession = this.client
				.startSession(ClientSessionOptions.builder().defaultTransactionOptions(txnOptions).build());
			List<Checkpoint> checkpoints = null;
			try {
				clientSession.startTransaction();
				MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
				BasicDBObject dbObject = new BasicDBObject("_id", DOCUMENT_PREFIX + configOption.get());
				Document document = collection.find(dbObject).first();
				if (document == null)
					return Optional.empty();
				String checkpointsStr = document.getString(DOCUMENT_CONTENT_KEY);
				checkpoints = objectMapper.readValue(checkpointsStr, new TypeReference<>() {
				});
				clientSession.commitTransaction();
				if (config.checkPointId().isPresent()) {
					List<Checkpoint> finalCheckpoints = checkpoints;
					return config.checkPointId()
						.flatMap(id -> finalCheckpoints.stream()
							.filter(checkpoint -> checkpoint.getId().equals(id))
							.findFirst());
				}
				return getLast(getLinkedList(checkpoints), config);
			}
			catch (Exception e) {
				clientSession.abortTransaction();
				throw new RuntimeException(e);
			}
			finally {
				clientSession.close();
			}
		}
		else {
			throw new IllegalArgumentException("threadId is not allow null");
		}
	}

	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			// lock
			ClientSession clientSession = this.client
				.startSession(ClientSessionOptions.builder().defaultTransactionOptions(txnOptions).build());
			clientSession.startTransaction();
			try {
				MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
				BasicDBObject dbObject = new BasicDBObject("_id", DOCUMENT_PREFIX + configOption.get());
				Document document = collection.find(dbObject).first();
				LinkedList<Checkpoint> checkpointLinkedList = null;
				if (Objects.nonNull(document)) {
					String checkpointsStr = document.getString(DOCUMENT_CONTENT_KEY);
					List<Checkpoint> checkpoints = objectMapper.readValue(checkpointsStr, new TypeReference<>() {
					});
					checkpointLinkedList = getLinkedList(checkpoints);
					if (config.checkPointId().isPresent()) { // Replace Checkpoint
						String checkPointId = config.checkPointId().get();
						int index = IntStream.range(0, checkpoints.size())
							.filter(i -> checkpoints.get(i).getId().equals(checkPointId))
							.findFirst()
							.orElseThrow(() -> (new NoSuchElementException(
									format("Checkpoint with id %s not found!", checkPointId))));
						checkpointLinkedList.set(index, checkpoint);
						Document tempDocument = new Document().append("_id", DOCUMENT_PREFIX + configOption.get())
							.append(DOCUMENT_CONTENT_KEY, objectMapper.writeValueAsString(checkpointLinkedList));
						collection.replaceOne(Filters.eq("_id", DOCUMENT_PREFIX + configOption.get()), tempDocument);
						clientSession.commitTransaction();
						clientSession.close();
						return config;
					}
				}
				if (checkpointLinkedList == null) {
					checkpointLinkedList = new LinkedList<>();
					checkpointLinkedList.push(checkpoint); // Add Checkpoint
					Document tempDocument = new Document().append("_id", DOCUMENT_PREFIX + configOption.get())
						.append(DOCUMENT_CONTENT_KEY, objectMapper.writeValueAsString(checkpointLinkedList));
					InsertOneResult insertOneResult = collection.insertOne(tempDocument);
					insertOneResult.wasAcknowledged();
				}
				else {
					checkpointLinkedList.push(checkpoint); // Add Checkpoint
					Document tempDocument = new Document().append("_id", DOCUMENT_PREFIX + configOption.get())
						.append(DOCUMENT_CONTENT_KEY, objectMapper.writeValueAsString(checkpointLinkedList));
					ReplaceOptions opts = new ReplaceOptions().upsert(true);
					collection.replaceOne(Filters.eq("_id", DOCUMENT_PREFIX + configOption.get()), tempDocument, opts);
				}
				clientSession.commitTransaction();
			}
			catch (Exception e) {
				clientSession.abortTransaction();
				throw new RuntimeException(e);
			}
			finally {
				clientSession.close();
			}
			return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
		}
		else {
			throw new IllegalArgumentException("threadId is not allow null");
		}
	}

	@Override
	public boolean clear(RunnableConfig config) {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			ClientSession clientSession = this.client
				.startSession(ClientSessionOptions.builder().defaultTransactionOptions(txnOptions).build());
			clientSession.startTransaction();
			try {
				MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
				BasicDBObject dbObject = new BasicDBObject("_id", DOCUMENT_PREFIX + configOption.get());
				collection.findOneAndDelete(dbObject);
				clientSession.commitTransaction();
				return true;
			}
			catch (Exception e) {
				clientSession.abortTransaction();
				throw new RuntimeException(e);
			}
			finally {
				clientSession.close();
			}
		}
		else {
			throw new IllegalArgumentException("threadId is not allow null");
		}
	}

}
