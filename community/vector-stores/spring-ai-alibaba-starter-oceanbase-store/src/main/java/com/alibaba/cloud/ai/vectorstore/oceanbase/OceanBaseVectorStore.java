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
package com.alibaba.cloud.ai.vectorstore.oceanbase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.IntStream;

import static org.springframework.ai.vectorstore.SearchRequest.DEFAULT_TOP_K;

public class OceanBaseVectorStore extends AbstractObservationVectorStore implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(OceanBaseVectorStore.class);

	private static final String DATA_BASE_SYSTEM = "oceanbase";

	private static final String REF_DOC_NAME = "refDocId";

	private static final String METADATA_FIELD_NAME = "metadata";

	private static final String CONTENT_FIELD_NAME = "content";

	private static final String DOC_NAME = "docId";

	private static final Double DEFAULT_SIMILARITY_THRESHOLD = 0.0;

	private static final String CREATE_TABLE_SQL_TEMPLATE = "CREATE TABLE IF NOT EXISTS %s ("
			+ "id varchar(100) PRIMARY KEY, " + "vector VECTOR(384) NOT NULL, " + "description text, "
			+ "metadata text)";

	private static final String INSERT_DOC_SQL_TEMPLATE = "INSERT INTO %s (id, vector, description, metadata) VALUES (?, ?, ?, ?)";

	private static final String DELETE_DOC_SQL_TEMPLATE = "DELETE FROM %s WHERE id = ?";

	private static final String DELETE_DOC_BY_FILTER_SQL_TEMPLATE = "DELETE FROM %s WHERE %s";

	private static final String SIMILARITY_SEARCH_SQL_TEMPLATE = "SELECT id, vector, description, metadata, l2_distance(vector,?) as distance FROM %s "
			+ "ORDER BY vector_distance(vector, ?) ASC LIMIT ?";

	public final FilterExpressionConverter filterExpressionConverter = new OceanBaseVectorFilterExpressionConverter();

	private final String tableName;

	private final Integer defaultTopK;

	private final Double defaultSimilarityThreshold;

	private final DataSource dataSource;

	private final ObjectMapper objectMapper;

	protected OceanBaseVectorStore(Builder builder) {
		super(builder);
		this.tableName = builder.tableName;
		this.dataSource = builder.dataSource;
		this.objectMapper = JsonMapper.builder().addModules(JacksonUtils.instantiateAvailableModules()).build();
		this.defaultSimilarityThreshold = builder.defaultSimilarityThreshold;
		this.defaultTopK = builder.defaultTopK;
	}

	public static Builder builder(String tableName, DataSource dataSource, EmbeddingModel embeddingModel) {
		return new Builder(tableName, dataSource, embeddingModel);
	}

	@Override
	public void afterPropertiesSet() {
		initializeDatabase();
	}

	private void initializeDatabase() {
		executeUpdate(String.format(CREATE_TABLE_SQL_TEMPLATE, tableName));
		logger.debug("Successfully created or verified table: {}", tableName);
	}

	@Override
	public void doAdd(List<Document> documents) {
		Assert.notNull(documents, "The document list should not be null.");
		if (CollectionUtils.isEmpty(documents)) {
			return;
		}
		List<float[]> embeddings = this.embeddingModel.embed(documents, EmbeddingOptionsBuilder.builder().build(),
				this.batchingStrategy);
		String sql = String.format(INSERT_DOC_SQL_TEMPLATE, tableName);
		try (Connection connection = dataSource.getConnection();
				PreparedStatement pstmt = connection.prepareStatement(sql)) {
			for (int i = 0; i < documents.size(); i++) {
				Document doc = documents.get(i);
				Map<String, String> metadata = createMetadata(doc);
				String vectorString = convertEmbeddingToString(embeddings.get(i));
				pstmt.setString(1, doc.getId());
				pstmt.setString(2, vectorString);
				pstmt.setString(3, doc.getText());
				pstmt.setString(4, objectMapper.writeValueAsString(metadata));
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		}
		catch (Exception e) {
			logger.error("Failed to add documents", e);
			throw new RuntimeException("Failed to add documents to OceanBase", e);
		}
	}

	private Map<String, String> createMetadata(Document doc) throws JsonProcessingException {
		Map<String, String> metadata = new HashMap<>();
		String refDocId = Optional.ofNullable(doc.getMetadata().get(DOC_NAME))
			.map(Object::toString)
			.orElse(doc.getId());
		metadata.put(REF_DOC_NAME, refDocId);
		metadata.put(CONTENT_FIELD_NAME, doc.getText());
		metadata.put(METADATA_FIELD_NAME, objectMapper.writeValueAsString(doc.getMetadata()));
		return metadata;
	}

	private String convertEmbeddingToString(float[] embedding) {
		return Arrays.toString(IntStream.range(0, embedding.length).mapToObj(i -> embedding[i]).toArray());
	}

	@Override
	public void doDelete(List<String> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return;
		}
		executeBatchUpdate(String.format(DELETE_DOC_SQL_TEMPLATE, tableName), ids);
	}

	@Override
	public void doDelete(Filter.Expression filterExpression) {
		String nativeFilterExpression = filterExpressionConverter.convertExpression(filterExpression);
		executeUpdate(String.format(DELETE_DOC_BY_FILTER_SQL_TEMPLATE, tableName, nativeFilterExpression));
	}

	@Override
	public List<Document> similaritySearch(String query) {
		return this.similaritySearch(SearchRequest.builder()
			.query(query)
			.topK(this.defaultTopK)
			.similarityThreshold(this.defaultSimilarityThreshold)
			.build());
	}

	@Override
	public List<Document> doSimilaritySearch(SearchRequest searchRequest) {
		String sql = String.format(SIMILARITY_SEARCH_SQL_TEMPLATE, tableName);
		List<Document> similarDocuments = new ArrayList<>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement pstmt = connection.prepareStatement(sql)) {
			String vector = convertQueryToVectorBytes(searchRequest.getQuery());
			pstmt.setString(1, vector);
			pstmt.setString(2, vector);
			pstmt.setInt(3, searchRequest.getTopK());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				Document doc = extractDocumentFromResultSet(rs);
				similarDocuments.add(doc);
			}
		}
		catch (Exception e) {
			logger.error("Failed to perform similarity search", e);
			throw new RuntimeException("Failed to perform similarity search in OceanBase", e);
		}
		return similarDocuments;
	}

	private Document extractDocumentFromResultSet(ResultSet rs) throws SQLException, JsonProcessingException {
		String id = rs.getString("id");
		String vectorMetadata = rs.getString("metadata");
		String distance = rs.getString("distance");
		Map<String, String> metadata = extractMetadata(vectorMetadata);
		String pageContent = metadata.get(CONTENT_FIELD_NAME);
		Map<String, Object> metadataJson = objectMapper.readValue(metadata.get(METADATA_FIELD_NAME),
				new TypeReference<Map<String, Object>>() {
				});
		metadataJson.put("distance", distance);
		return new Document(String.valueOf(id), pageContent, metadataJson);
	}

	private Map<String, String> extractMetadata(String vectorStr) throws JsonProcessingException {
		return objectMapper.readValue(vectorStr, Map.class);
	}

	private String convertQueryToVectorBytes(String query) {
		return Arrays.toString(this.embeddingModel.embed(query));
	}

	private void executeUpdate(String sql) {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.execute();
		}
		catch (SQLException e) {
			logger.error("SQL execution failed", e);
			throw new RuntimeException("Failed to execute SQL", e);
		}
	}

	private void executeBatchUpdate(String sql, List<String> params) {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement pstmt = connection.prepareStatement(sql)) {
			for (String param : params) {
				pstmt.setLong(1, Long.parseLong(param));
				pstmt.addBatch();
			}
			pstmt.executeBatch();
		}
		catch (SQLException e) {
			logger.error("Batch SQL execution failed", e);
			throw new RuntimeException("Failed to execute batch SQL", e);
		}
	}

	@Override
	public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
		return VectorStoreObservationContext.builder(DATA_BASE_SYSTEM, operationName)
			.collectionName(this.tableName)
			.dimensions(this.embeddingModel.dimensions());
	}

	public static class Builder extends AbstractVectorStoreBuilder<Builder> {

		private final String tableName;

		private final DataSource dataSource;

		private int defaultTopK = DEFAULT_TOP_K;

		private Double defaultSimilarityThreshold = DEFAULT_SIMILARITY_THRESHOLD;

		private Builder(String tableName, DataSource dataSource, EmbeddingModel embeddingModel) {
			super(embeddingModel);
			Assert.notNull(tableName, "Table name must not be null");
			Assert.notNull(dataSource, "Data source must not be null");
			this.tableName = tableName.toLowerCase();
			this.dataSource = dataSource;
		}

		public Builder defaultTopK(int defaultTopK) {
			Assert.isTrue(defaultTopK >= 0, "The topK should be positive value.");
			this.defaultTopK = defaultTopK;
			return this;
		}

		public Builder defaultSimilarityThreshold(Double defaultSimilarityThreshold) {
			Assert.isTrue(defaultSimilarityThreshold >= 0.0 && defaultSimilarityThreshold <= 1.0,
					"The similarity threshold must be in range [0.0:1.0].");
			this.defaultSimilarityThreshold = defaultSimilarityThreshold;
			return this;
		}

		@Override
		public OceanBaseVectorStore build() {
			try {
				return new OceanBaseVectorStore(this);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to build OceanBaseVectorStore: " + e.getMessage(), e);
			}
		}

	}

}
