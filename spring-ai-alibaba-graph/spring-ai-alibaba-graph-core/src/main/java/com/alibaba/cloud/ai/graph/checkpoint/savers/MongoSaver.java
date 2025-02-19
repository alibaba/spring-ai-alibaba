package com.alibaba.cloud.ai.graph.checkpoint.savers;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.fastjson.JSON;
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
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import static java.lang.String.format;

/**
 * The type Mongo saver.
 *
 * @author disaster
 * @since 1.0.0-M2
 */
@Slf4j
public class MongoSaver implements BaseCheckpointSaver {

	private MongoClient client;

	private MongoDatabase database;

	private TransactionOptions txnOptions;

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
				checkpoints = JSON.parseArray(checkpointsStr, Checkpoint.class);
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
				checkpoints = JSON.parseArray(checkpointsStr, Checkpoint.class);
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
					List<Checkpoint> checkpoints = JSON.parseArray(checkpointsStr, Checkpoint.class);
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
							.append(DOCUMENT_CONTENT_KEY, JSON.toJSONString(checkpointLinkedList));
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
						.append(DOCUMENT_CONTENT_KEY, JSON.toJSONString(checkpointLinkedList));
					InsertOneResult insertOneResult = collection.insertOne(tempDocument);
					insertOneResult.wasAcknowledged();
				}
				else {
					checkpointLinkedList.push(checkpoint); // Add Checkpoint
					Document tempDocument = new Document().append("_id", DOCUMENT_PREFIX + configOption.get())
						.append(DOCUMENT_CONTENT_KEY, JSON.toJSONString(checkpointLinkedList));
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
