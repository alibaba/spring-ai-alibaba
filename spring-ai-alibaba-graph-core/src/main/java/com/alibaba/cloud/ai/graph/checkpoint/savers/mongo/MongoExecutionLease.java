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
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.ExecutionLease;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;

final class MongoExecutionLease implements ExecutionLease {

	static final String CONFIG_KEY = "_MONGO_EXECUTION_LEASE_";

	private static final Logger logger = LoggerFactory.getLogger(MongoExecutionLease.class);
	private static final Duration DURATION = Duration.ofSeconds(90);

	private final MongoSaver saver;
	private final MongoCollection<Document> collection;
	private final MongoCollection<Document> durable;
	private final String threadName;
	private final String token = UUID.randomUUID().toString();
	private final Disposable renewal;

	MongoExecutionLease(MongoSaver saver, MongoCollection<Document> collection, String threadName) {
		this.saver = saver;
		this.collection = collection;
		this.durable = collection.withWriteConcern(WriteConcern.MAJORITY);
		this.threadName = threadName;
		var available = new Document("$lte", List.of(new Document("$ifNull", List.of("$expiresAt", new Date(0))), "$$NOW"));
		var changes = new Document("owner", new Document("$cond", List.of(available, token, "$owner")))
				.append("expiresAt", new Document("$cond", List.of(available, expiration(), "$expiresAt")));
		var update = List.of(new Document("$set", changes));
		var options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);
		Document lease;
		try {
			lease = durable.findOneAndUpdate(Filters.eq("_id", threadName), update, options.upsert(true));
		}
		catch (MongoException ex) {
			if (ex.getCode() != 11000) {
				throw ex;
			}
			lease = durable.findOneAndUpdate(Filters.eq("_id", threadName), update, options.upsert(false));
		}
		if (lease == null || !token.equals(lease.getString("owner"))) {
			throw new IllegalStateException("A durable graph execution is already active for thread '" + threadName + "'");
		}
		renewal = Flux.interval(DURATION.dividedBy(3)).onBackpressureDrop()
				.concatMap(ignored -> Mono.fromCallable(this::renew).subscribeOn(Schedulers.boundedElastic()))
				.takeWhile(Boolean::booleanValue)
				.subscribe(ignored -> { }, error -> logger.warn("Graph execution lease renewal stopped", error));
	}

	@Override
	public RunnableConfig configure(RunnableConfig config) {
		return RunnableConfig.builder(config).addMetadata(CONFIG_KEY, this).build();
	}

	boolean belongsTo(MongoSaver owner, String thread) {
		return saver == owner && threadName.equals(thread);
	}

	void fence(ClientSession session) {
		// A write in the same transaction forces a conflict if a successor acquired the lease.
		if (collection.updateOne(session, activeOwner(), Updates.inc("writes", 1)).getMatchedCount() != 1) {
			throw new IllegalStateException("The graph execution lease for thread '" + threadName + "' has expired or been replaced");
		}
	}

	private synchronized boolean renew() {
		try {
			var update = List.of(new Document("$set", new Document("expiresAt", expiration())));
			return durable.updateOne(activeOwner(), update).getMatchedCount() == 1;
		}
		catch (MongoException ex) {
			logger.warn("Could not renew the graph execution lease for thread '{}'", threadName, ex);
			return true;
		}
	}

	private Bson owner() {
		return Filters.and(Filters.eq("_id", threadName), Filters.eq("owner", token));
	}

	private Bson activeOwner() {
		return Filters.and(owner(), Filters.expr(new Document("$gt", List.of("$expiresAt", "$$NOW"))));
	}

	private static Document expiration() {
		return new Document("$add", List.of("$$NOW", DURATION.toMillis()));
	}

	@Override
	public void close() {
		renewal.dispose();
		synchronized (this) {
			try {
				durable.deleteOne(owner());
			}
			catch (MongoException ex) {
				logger.warn("Could not release the graph execution lease for thread '{}'; it will expire", threadName, ex);
			}
		}
	}

}
