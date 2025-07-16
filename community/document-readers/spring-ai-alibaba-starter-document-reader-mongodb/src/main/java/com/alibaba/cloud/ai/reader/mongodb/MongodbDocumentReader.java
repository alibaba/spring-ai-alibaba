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
package com.alibaba.cloud.ai.reader.mongodb;

import com.alibaba.cloud.ai.reader.mongodb.converter.DefaultDocumentConverter;
import com.alibaba.cloud.ai.reader.mongodb.converter.DocumentConverter;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.DocumentReader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * MongoDB Document Reader Implementation Class
 *
 * @author Yongtao Tan
 * @version 1.0.0
 *
 */
public class MongodbDocumentReader implements DocumentReader, Closeable {

	private static final Logger log = LoggerFactory.getLogger(MongodbDocumentReader.class);

	private final MongoTemplate mongoTemplate;

	private final MongodbResource properties;

	private final MongoClient mongoClient;

	private final boolean shouldCloseClient;

	private volatile boolean closed = false;

	/**
	 * Document Converter Interface
	 */
	private final DocumentConverter documentConverter;

	// MongoDB URI format validation
	private static final Pattern MONGODB_URI_PATTERN = Pattern.compile("mongodb(?:\\+srv)?://[^/]+(/[^?]+)?(\\?.*)?");

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder Pattern Constructor
	 */
	public static class Builder {

		private MongoTemplate mongoTemplate;

		private MongodbResource resource;

		private MongoClient mongoClient;

		private DocumentConverter converter;

		public Builder withMongoTemplate(MongoTemplate mongoTemplate) {
			this.mongoTemplate = mongoTemplate;
			return this;
		}

		public Builder withResource(MongodbResource resource) {
			this.resource = resource;
			return this;
		}

		public Builder withMongoClient(MongoClient mongoClient) {
			this.mongoClient = mongoClient;
			return this;
		}

		public Builder withDocumentConverter(DocumentConverter converter) {
			this.converter = converter;
			return this;
		}

		/**
		 * Create MongoDB Client Creates a MongoDB client with connection pool and timeout
		 * settings based on configuration
		 * @param resource MongoDB configuration resource
		 * @return Configured MongoDB client instance
		 */
		private static MongoClient createMongoClient(MongodbResource resource) {
			Assert.notNull(resource, "MongodbResource must not be null");
			Assert.hasText(resource.getUri(), "MongoDB URI must not be empty");

			MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(resource.getUri()))
				.applyToConnectionPoolSettings(builder -> builder.maxSize(resource.getPoolSize())
					.minSize(1)
					.maxWaitTime(2000, TimeUnit.MILLISECONDS)
					.maxConnectionLifeTime(30, TimeUnit.MINUTES))
				.applyToSocketSettings(
						builder -> builder.connectTimeout(resource.getConnectTimeout(), TimeUnit.MILLISECONDS)
							.readTimeout(resource.getConnectTimeout(), TimeUnit.MILLISECONDS))
				.applyToServerSettings(builder -> builder.heartbeatFrequency(10000, TimeUnit.MILLISECONDS))
				.build();

			return MongoClients.create(settings);
		}

		public MongodbDocumentReader build() {
			Assert.notNull(resource, "MongodbResource must not be null");

			if (mongoTemplate == null && mongoClient == null) {
				mongoClient = createMongoClient(resource);
				mongoTemplate = new MongoTemplate(mongoClient, resource.getDatabase());
			}
			else if (mongoTemplate == null) {
				mongoTemplate = new MongoTemplate(mongoClient, resource.getDatabase());
			}
			return new MongodbDocumentReader(this);
		}

	}

	private MongodbDocumentReader(Builder builder) {
		this.properties = builder.resource;
		this.mongoTemplate = builder.mongoTemplate;
		this.mongoClient = builder.mongoClient;
		this.documentConverter = Objects.isNull(builder.converter) ? new DefaultDocumentConverter() : builder.converter;
		this.shouldCloseClient = builder.mongoClient == null;

		validateConfiguration();
	}

	/**
	 * Validate configuration validity
	 */
	private void validateConfiguration() {
		validateMongoDbUri(properties.getUri());
		validatePoolSettings(properties);
	}

	private void validateMongoDbUri(String uri) {
		Assert.hasText(uri, "MongoDB URI must not be empty");
		if (!MONGODB_URI_PATTERN.matcher(uri).matches()) {
			throw new IllegalArgumentException("Invalid MongoDB URI format");
		}
	}

	private void validatePoolSettings(MongodbResource resource) {
		if (resource.getPoolSize() <= 0) {
			throw new IllegalArgumentException("Pool size must be greater than 0");
		}
		if (resource.getConnectTimeout() <= 0) {
			throw new IllegalArgumentException("Connect timeout must be greater than 0");
		}
	}

	/**
	 * Execute query and record performance metrics
	 */
	private <T> T executeWithMetrics(String operation, Supplier<T> query) {
		checkState();
		try {
			log.debug("Executing operation: {}", operation);
			T result = query.get();
			log.debug("Operation completed successfully: {}", operation);
			return result;
		}
		catch (MongoException e) {
			log.error("Operation failed: {}", operation, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Check reader state
	 */
	private void checkState() {
		if (closed) {
			throw new IllegalStateException("MongodbDocumentReader has been closed");
		}
	}

	@Override
	public void close() {
		if (!closed) {
			synchronized (this) {
				if (!closed) {
					try {
						log.info("Closing MongodbDocumentReader...");
						if (shouldCloseClient && mongoClient != null) {
							mongoClient.close();
						}

						log.info("MongodbDocumentReader closed successfully");
					}
					catch (Exception e) {
						log.error("Error while closing MongodbDocumentReader", e);
					}
					finally {
						closed = true;
					}
				}
			}
		}
	}

	/**
	 * Query documents based on criteria definition
	 * @param criteriaDefinition MongoDB query criteria definition for specifying query
	 * rules
	 * @return List of documents matching the criteria, returns empty list if criteria is
	 * null
	 */
	public List<org.springframework.ai.document.Document> findByCriteriaDefinition(
			CriteriaDefinition criteriaDefinition) {
		if (criteriaDefinition == null) {
			return Collections.emptyList();
		}
		// 校验集合
		validateConfiguration();

		return processDocuments(new Query(criteriaDefinition));
	}

	/**
	 * Query documents using Query object
	 * @param query MongoDB query object that can contain various query conditions
	 * @return List of documents matching the query conditions, returns empty list if
	 * query is null
	 */
	public List<org.springframework.ai.document.Document> findByQuery(Query query) {
		if (query == null) {
			return Collections.emptyList();
		}
		return processDocuments(query);
	}

	/**
	 * Paginated document query
	 * @param query MongoDB query object
	 * @param page Page number, starting from 0
	 * @param size Page size
	 * @return Paginated document list
	 */
	public List<org.springframework.ai.document.Document> findWithPagination(Query query, int page, int size) {
		query.skip((long) page * size).limit(size);
		return processDocuments(query);
	}

	/**
	 * Implement DocumentReader interface's get method Get documents based on configured
	 * query conditions
	 * @return List of queried documents
	 * @throws RuntimeException when MongoDB operation or other exceptions occur
	 */
	@Override
	public List<org.springframework.ai.document.Document> get() {
		validateConfiguration();
		Query query = buildQuery();
		return processDocuments(query);
	}

	/**
	 * Build MongoDB query object based on configuration
	 * @return Built query object
	 * @throws RuntimeException when query string parsing fails
	 */
	private Query buildQuery() {
		Query query = new Query();
		if (StringUtils.hasText(properties.getQuery())) {
			try {
				Document queryDoc = Document.parse(properties.getQuery());
				query = new BasicQuery(queryDoc);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to parse query string: " + e.getMessage(), e);
			}
		}
		return query;
	}

	/**
	 * Process MongoDB query and convert document format
	 */
	private List<org.springframework.ai.document.Document> processDocuments(Query query) {
		return executeWithMetrics("processDocuments",
				() -> StreamSupport
					.stream(mongoTemplate.find(query, Document.class, properties.getCollection()).spliterator(), false)
					.map(doc -> documentConverter.convert(doc, properties.getDatabase(), properties.getCollection(),
							properties))
					.collect(Collectors.toList()));
	}

	/**
	 * Execute query in specified database and collection
	 * @param database Database name to query
	 * @param collection Collection name to query
	 * @param query Query conditions
	 * @return List of queried documents
	 */
	public List<org.springframework.ai.document.Document> findInDatabaseAndCollection(String database,
			String collection, Query query) {
		Assert.hasText(collection, "Collection name must not be empty");
		Assert.hasText(database, "Database name must not be empty");
		Assert.notNull(query, "Query must not be null");

		return executeWithMetrics("findInDatabaseAndCollection", () -> {
			List<org.springframework.ai.document.Document> results = StreamSupport
				.stream(mongoTemplate.getMongoDatabaseFactory()
					.getMongoDatabase(database)
					.getCollection(collection, Document.class)
					.find(query.getQueryObject())
					.spliterator(), false)
				.map(doc -> documentConverter.convert(doc, database, collection, properties))
				.collect(Collectors.toList());

			return results;
		});
	}

	/**
	 * Parallel query across multiple collections
	 */
	public List<org.springframework.ai.document.Document> findInDatabaseAndCollectionParallel(String database,
			List<String> collections, Query query) {
		Assert.notEmpty(collections, "Collections list must not be empty");
		Assert.hasText(database, "Database name must not be empty");
		Assert.notNull(query, "Query must not be null");

		return collections.parallelStream()
			.map(collection -> findInDatabaseAndCollection(database, collection, query))
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	/**
	 * Execute paginated query in specified collection
	 * @param collection Collection name to query
	 * @param query Query conditions
	 * @param page Page number (starting from 0)
	 * @param size Page size
	 * @return Paginated query result document list
	 */
	public List<org.springframework.ai.document.Document> findInDatabaseAndCollectionWithPagination(String collection,
			Query query, int page, int size) {
		return findInDatabaseAndCollectionWithPagination(properties.getDatabase(), collection, query, page, size);
	}

	/**
	 * Execute paginated query in specified database and collection
	 * @param database Database name to query
	 * @param collection Collection name to query
	 * @param query Query conditions
	 * @param page Page number (starting from 0)
	 * @param size Page size
	 * @return Paginated query result document list
	 */
	public List<org.springframework.ai.document.Document> findInDatabaseAndCollectionWithPagination(String database,
			String collection, Query query, int page, int size) {
		Assert.hasText(collection, "Collection name must not be empty");
		Assert.hasText(database, "Database name must not be empty");
		Assert.notNull(query, "Query must not be null");
		Assert.isTrue(page >= 0, "Page index must not be negative");
		Assert.isTrue(size > 0, "Page size must be greater than 0");

		return executeWithMetrics("findInDatabaseAndCollectionWithPagination", () -> {
			List<org.springframework.ai.document.Document> results = StreamSupport
				.stream(mongoTemplate.getMongoDatabaseFactory()
					.getMongoDatabase(database)
					.getCollection(collection, Document.class)
					.find(query.getQueryObject())
					.skip(page * size)
					.limit(size)
					.spliterator(), false)
				.map(doc -> documentConverter.convert(doc, database, collection, properties))
				.collect(Collectors.toList());

			return results;
		});
	}

}
