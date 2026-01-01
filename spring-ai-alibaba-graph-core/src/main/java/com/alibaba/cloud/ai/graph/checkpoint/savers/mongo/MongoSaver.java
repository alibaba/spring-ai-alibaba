/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.checkpoint.savers.mongo;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.check_point.CheckPointSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import com.mongodb.BasicDBObject;
import com.mongodb.ClientSessionOptions;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class MongoSaver implements BaseCheckpointSaver {

	private static final Logger logger = LoggerFactory.getLogger(MongoSaver.class);
	private static final String DB_NAME = "check_point_db";
	private static final String THREAD_META_COLLECTION = "thread_meta";
	private static final String CHECKPOINT_COLLECTION = "checkpoint_collection";
	private static final String THREAD_META_PREFIX = "mongo:thread:meta:";
	private static final String CHECKPOINT_PREFIX = "mongo:checkpoint:content:";
	private static final String DOCUMENT_CONTENT_KEY = "checkpoint_content";
	// Thread meta document field names
	private static final String FIELD_THREAD_ID = "thread_id";
	private static final String FIELD_IS_RELEASED = "is_released";
	private static final String FIELD_THREAD_NAME = "thread_name";
	private final Serializer<Checkpoint> checkpointSerializer;
	private MongoClient client;
	private MongoDatabase database;
	private TransactionOptions txnOptions;

	/**
	 * Protected constructor for MongoSaver.
	 * Use {@link #builder()} to create instances.
	 *
	 * @param client the client
	 * @param stateSerializer the state serializer
	 */
	protected MongoSaver(MongoClient client, StateSerializer stateSerializer) {
		Objects.requireNonNull(client, "client cannot be null");
		Objects.requireNonNull(stateSerializer, "stateSerializer cannot be null");
		this.client = client;
		this.database = client.getDatabase(DB_NAME);
		this.txnOptions = TransactionOptions.builder().writeConcern(WriteConcern.MAJORITY).build();
		this.checkpointSerializer = new CheckPointSerializer(stateSerializer);
		Runtime.getRuntime().addShutdownHook(new Thread(client::close));
	}

	/**
	 * Creates a new builder for MongoSaver.
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	private String serializeCheckpoints(List<Checkpoint> checkpoints) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeInt(checkpoints.size());
			for (Checkpoint checkpoint : checkpoints) {
				checkpointSerializer.write(checkpoint, oos);
			}
			oos.flush();
			byte[] bytes = baos.toByteArray();
			return Base64.getEncoder().encodeToString(bytes);
		}
	}

	private LinkedList<Checkpoint> deserializeCheckpoints(String content) throws IOException, ClassNotFoundException {
		if (content == null || content.isEmpty()) {
			return new LinkedList<>();
		}
		byte[] bytes = Base64.getDecoder().decode(content);
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			 ObjectInputStream ois = new ObjectInputStream(bais)) {
			int size = ois.readInt();
			LinkedList<Checkpoint> checkpoints = new LinkedList<>();
			for (int i = 0; i < size; i++) {
				checkpoints.add(checkpointSerializer.read(ois));
			}
			return checkpoints;
		}
	}

	/**
	 * Gets or creates a thread_id for the given thread_name.
	 * If an active thread exists, returns its thread_id.
	 * If no active thread exists or the thread is released, creates a new thread_id.
	 *
	 * This method uses atomic operations to prevent race conditions in concurrent scenarios.
	 * Uses findOneAndUpdate with conditional logic to ensure thread-safe creation.
	 *
	 * @param threadName the thread name
	 * @param clientSession the MongoDB client session for transaction
	 * @return the thread_id (UUID string)
	 */
	private String getOrCreateThreadId(String threadName, ClientSession clientSession) {
		MongoCollection<Document> threadMetaCollection = database.getCollection(THREAD_META_COLLECTION);
		String metaId = THREAD_META_PREFIX + threadName;

		// Step 1: Try to atomically get an active thread
		// Filter: _id matches AND is_released != true
		Document activeThreadFilter = new Document("_id", metaId)
				.append(FIELD_IS_RELEASED, new Document("$ne", true));

		FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions()
				.returnDocument(ReturnDocument.AFTER);

		// Atomically get an active thread (using a no-op update to ensure atomic read)
		Document existingDoc = threadMetaCollection.findOneAndUpdate(
				clientSession,
				activeThreadFilter,
				Updates.currentDate("_lastAccessed"), // No-op update for atomic read
				findOptions
		);

		if (existingDoc != null) {
			String threadId = existingDoc.getString(FIELD_THREAD_ID);
			if (threadId != null) {
				// Active thread exists, return its thread_id
				return threadId;
			}
		}

		// Step 2: No active thread exists, create a new one atomically
		// Strategy: Use findOneAndUpdate with upsert, but handle two cases:
		// a) Document doesn't exist - upsert will create it
		// b) Document exists but is_released == true - update it conditionally

		String newThreadId = UUID.randomUUID().toString();
		FindOneAndUpdateOptions upsertOptions = new FindOneAndUpdateOptions()
				.upsert(true)
				.returnDocument(ReturnDocument.AFTER);

		// First, try to create if document doesn't exist (using upsert)
		// This will create the document if it doesn't exist
		Document createResult = threadMetaCollection.findOneAndUpdate(
				clientSession,
				Filters.eq("_id", metaId), // This matches if exists, or creates if not (with upsert)
				Updates.combine(
						// Only set these when inserting (document doesn't exist)
						Updates.setOnInsert(FIELD_THREAD_ID, newThreadId),
						Updates.setOnInsert(FIELD_IS_RELEASED, false)
				),
				upsertOptions
		);

		if (createResult != null) {
			Boolean isReleased = createResult.getBoolean(FIELD_IS_RELEASED, false);
			String existingThreadId = createResult.getString(FIELD_THREAD_ID);

			// If document was just created or was already active, return the thread_id
			if (existingThreadId != null && !Boolean.TRUE.equals(isReleased)) {
				return existingThreadId;
			}

			// If document exists but is released, update it atomically
			// Use conditional update to ensure we only update if still released
			if (Boolean.TRUE.equals(isReleased)) {
				Document updateResult = threadMetaCollection.findOneAndUpdate(
						clientSession,
						Filters.and(
								Filters.eq("_id", metaId),
								Filters.eq(FIELD_IS_RELEASED, true) // Only update if still released
						),
						Updates.combine(
								Updates.set(FIELD_THREAD_ID, newThreadId),
								Updates.set(FIELD_IS_RELEASED, false)
						),
						new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
				);

				if (updateResult != null) {
					return updateResult.getString(FIELD_THREAD_ID);
				}

				// If update failed (another thread already updated it), query again
				Document finalDoc = threadMetaCollection.find(clientSession, new BasicDBObject("_id", metaId)).first();
				if (finalDoc != null) {
					String finalThreadId = finalDoc.getString(FIELD_THREAD_ID);
					Boolean finalIsReleased = finalDoc.getBoolean(FIELD_IS_RELEASED, false);
					if (finalThreadId != null && !Boolean.TRUE.equals(finalIsReleased)) {
						return finalThreadId;
					}
				}
			}
		}

		// Final fallback: query again to get the current state
		Document finalDoc = threadMetaCollection.find(clientSession, new BasicDBObject("_id", metaId)).first();
		if (finalDoc != null) {
			String finalThreadId = finalDoc.getString(FIELD_THREAD_ID);
			if (finalThreadId != null) {
				return finalThreadId;
			}
		}

		return newThreadId;
	}

	/**
	 * Gets the active thread_id for the given thread_name.
	 * Returns null if no active thread exists.
	 *
	 * @param threadName the thread name
	 * @param clientSession the MongoDB client session for transaction
	 * @return the active thread_id, or null if not found
	 */
	private String getActiveThreadId(String threadName, ClientSession clientSession) {
		MongoCollection<Document> threadMetaCollection = database.getCollection(THREAD_META_COLLECTION);
		String metaId = THREAD_META_PREFIX + threadName;

		Document metaDoc = threadMetaCollection.find(clientSession, new BasicDBObject("_id", metaId)).first();

		if (metaDoc != null) {
			String threadId = metaDoc.getString(FIELD_THREAD_ID);
			Boolean isReleased = metaDoc.getBoolean(FIELD_IS_RELEASED, false);

			if (threadId != null && !Boolean.TRUE.equals(isReleased)) {
				return threadId;
			}
		}

		return null; // No active thread exists
	}

	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		Optional<String> threadNameOpt = config.threadId();
		if (!threadNameOpt.isPresent()) {
			throw new IllegalArgumentException("threadId is not allowed to be null");
		}

		String threadName = threadNameOpt.get();
		ClientSession clientSession = this.client
				.startSession(ClientSessionOptions.builder().defaultTransactionOptions(txnOptions).build());
		clientSession.startTransaction();
		List<Checkpoint> checkpoints = null;
		try {
			// Get active thread_id for the thread_name
			String threadId = getActiveThreadId(threadName, clientSession);
			if (threadId == null) {
				clientSession.commitTransaction();
				return Collections.emptyList();
			}

			// Use thread_id to query checkpoints
			MongoCollection<Document> collection = database.getCollection(CHECKPOINT_COLLECTION);
			String checkpointId = CHECKPOINT_PREFIX + threadId;
			Document document = collection.find(clientSession, new BasicDBObject("_id", checkpointId)).first();
			if (document == null) {
				clientSession.commitTransaction();
				return Collections.emptyList();
			}
			String checkpointsStr = document.getString(DOCUMENT_CONTENT_KEY);
			checkpoints = deserializeCheckpoints(checkpointsStr);
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

	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		Optional<String> threadNameOpt = config.threadId();
		if (!threadNameOpt.isPresent()) {
			throw new IllegalArgumentException("threadId is not allow null");
		}

		String threadName = threadNameOpt.get();
		ClientSession clientSession = this.client
				.startSession(ClientSessionOptions.builder().defaultTransactionOptions(txnOptions).build());
		LinkedList<Checkpoint> checkpoints = null;
		try {
			clientSession.startTransaction();

			// Get active thread_id for the thread_name
			String threadId = getActiveThreadId(threadName, clientSession);
			if (threadId == null) {
				clientSession.commitTransaction();
				return Optional.empty();
			}

			// Use thread_id to query checkpoints
			MongoCollection<Document> collection = database.getCollection(CHECKPOINT_COLLECTION);
			String checkpointId = CHECKPOINT_PREFIX + threadId;
			Document document = collection.find(clientSession, new BasicDBObject("_id", checkpointId)).first();
			if (document == null) {
				clientSession.commitTransaction();
				return Optional.empty();
			}
			String checkpointsStr = document.getString(DOCUMENT_CONTENT_KEY);
			checkpoints = deserializeCheckpoints(checkpointsStr);
			clientSession.commitTransaction();

			if (config.checkPointId().isPresent()) {
				List<Checkpoint> finalCheckpoints = checkpoints;
				return config.checkPointId()
						.flatMap(id -> finalCheckpoints.stream()
								.filter(checkpoint -> checkpoint.getId().equals(id))
								.findFirst());
			}
			return getLast(checkpoints, config);
		}
		catch (Exception e) {
			clientSession.abortTransaction();
			throw new RuntimeException(e);
		}
		finally {
			clientSession.close();
		}
	}

	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		Optional<String> threadNameOpt = config.threadId();
		if (!threadNameOpt.isPresent()) {
			throw new IllegalArgumentException("threadId is not allow null");
		}

		String threadName = threadNameOpt.get();
		ClientSession clientSession = this.client
				.startSession(ClientSessionOptions.builder().defaultTransactionOptions(txnOptions).build());
		clientSession.startTransaction();
		try {
			// Get or create thread_id
			String threadId = getOrCreateThreadId(threadName, clientSession);

			// Use thread_id as key for checkpoint storage
			MongoCollection<Document> collection = database.getCollection(CHECKPOINT_COLLECTION);
			String checkpointDocId = CHECKPOINT_PREFIX + threadId;
			Document document = collection.find(clientSession, new BasicDBObject("_id", checkpointDocId)).first();
			LinkedList<Checkpoint> checkpointLinkedList = null;

			if (Objects.nonNull(document)) {
				String checkpointsStr = document.getString(DOCUMENT_CONTENT_KEY);
				checkpointLinkedList = deserializeCheckpoints(checkpointsStr);
				LinkedList<Checkpoint> finalCheckpointLinkedList = checkpointLinkedList;
				if (config.checkPointId().isPresent()) { // Replace Checkpoint
					String checkPointId = config.checkPointId().get();
					int index = IntStream.range(0, checkpointLinkedList.size())
							.filter(i -> finalCheckpointLinkedList.get(i).getId().equals(checkPointId))
							.findFirst()
							.orElseThrow(() -> (new NoSuchElementException(
									format("Checkpoint with id %s not found!", checkPointId))));
					finalCheckpointLinkedList.set(index, checkpoint);
					Document tempDocument = new Document().append("_id", checkpointDocId)
							.append(DOCUMENT_CONTENT_KEY, serializeCheckpoints(finalCheckpointLinkedList));
					collection.replaceOne(clientSession, Filters.eq("_id", checkpointDocId), tempDocument);
					clientSession.commitTransaction();
					return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
				}
			}

			if (checkpointLinkedList == null) {
				checkpointLinkedList = new LinkedList<>();
				checkpointLinkedList.push(checkpoint); // Add Checkpoint
				Document tempDocument = new Document().append("_id", checkpointDocId)
						.append(DOCUMENT_CONTENT_KEY, serializeCheckpoints(checkpointLinkedList));
				collection.insertOne(clientSession, tempDocument);
			}
			else {
				checkpointLinkedList.push(checkpoint); // Add Checkpoint
				Document tempDocument = new Document().append("_id", checkpointDocId)
						.append(DOCUMENT_CONTENT_KEY, serializeCheckpoints(checkpointLinkedList));
				ReplaceOptions opts = new ReplaceOptions().upsert(true);
				collection.replaceOne(clientSession, Filters.eq("_id", checkpointDocId), tempDocument, opts);
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

	@Override
	public Tag release(RunnableConfig config) throws Exception {
		Optional<String> threadNameOpt = config.threadId();
		if (!threadNameOpt.isPresent()) {
			throw new IllegalArgumentException("threadId is not allow null");
		}

		String threadName = threadNameOpt.get();
		ClientSession clientSession = this.client
				.startSession(ClientSessionOptions.builder().defaultTransactionOptions(txnOptions).build());
		clientSession.startTransaction();
		try {
			MongoCollection<Document> threadMetaCollection = database.getCollection(THREAD_META_COLLECTION);
			String metaId = THREAD_META_PREFIX + threadName;

			Document metaDoc = threadMetaCollection.find(clientSession, new BasicDBObject("_id", metaId)).first();
			if (metaDoc == null) {
				clientSession.abortTransaction();
				throw new IllegalStateException("Thread not found: " + threadName);
			}

			String threadId = metaDoc.getString(FIELD_THREAD_ID);
			if (threadId == null) {
				clientSession.abortTransaction();
				throw new IllegalStateException("Thread not found: " + threadName);
			}

			// Mark thread as released atomically
			// Use findOneAndUpdate with condition to ensure we only release active threads
			Document releaseFilter = new Document("_id", metaId)
					.append(FIELD_IS_RELEASED, false); // Only release if not already released

			Document updatedDoc = threadMetaCollection.findOneAndUpdate(
					clientSession,
					releaseFilter,
					Updates.set(FIELD_IS_RELEASED, true),
					new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
			);

			if (updatedDoc == null) {
				// Thread was already released or doesn't exist
				clientSession.abortTransaction();
				throw new IllegalStateException("Thread is not active or already released: " + threadName);
			}

			// Get checkpoints for Tag (using thread_id)
			MongoCollection<Document> checkpointCollection = database.getCollection(CHECKPOINT_COLLECTION);
			String checkpointDocId = CHECKPOINT_PREFIX + threadId;
			Document checkpointDoc = checkpointCollection.find(clientSession, new BasicDBObject("_id", checkpointDocId))
					.first();

			Collection<Checkpoint> checkpoints = Collections.emptyList();
			if (checkpointDoc != null) {
				String checkpointsStr = checkpointDoc.getString(DOCUMENT_CONTENT_KEY);
				if (checkpointsStr != null) {
					checkpoints = deserializeCheckpoints(checkpointsStr);
				}
			}

			clientSession.commitTransaction();
			return new Tag(threadName, checkpoints);

		}
		catch (Exception e) {
			clientSession.abortTransaction();
			throw new RuntimeException(e);
		}
		finally {
			clientSession.close();
		}
	}

	/**
	 * Builder class for MongoSaver.
	 */
	public static class Builder {
		private MongoClient client;
		private StateSerializer stateSerializer;

		public Builder client(MongoClient client) {
			this.client = client;
			return this;
		}

		public Builder stateSerializer(StateSerializer stateSerializer) {
			this.stateSerializer = stateSerializer;
			return this;
		}

		/**
		 * Builds a new MongoSaver instance.
		 * @return a new MongoSaver instance
		 * @throws IllegalArgumentException if client or stateSerializer is null
		 */
		public MongoSaver build() {
			if (client == null) {
				throw new IllegalArgumentException("client cannot be null");
			}
			if (stateSerializer == null) {
				this.stateSerializer = StateGraph.DEFAULT_JACKSON_SERIALIZER;
			}
			return new MongoSaver(client, stateSerializer);
		}
	}

}
