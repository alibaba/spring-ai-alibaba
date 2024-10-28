package com.alibaba.cloud.ai.dashscope.rag;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.gpdb20160503.Client;
import com.aliyun.gpdb20160503.models.CreateCollectionRequest;
import com.aliyun.gpdb20160503.models.CreateNamespaceRequest;
import com.aliyun.gpdb20160503.models.DeleteCollectionDataRequest;
import com.aliyun.gpdb20160503.models.DeleteCollectionDataResponse;
import com.aliyun.gpdb20160503.models.DescribeCollectionRequest;
import com.aliyun.gpdb20160503.models.DescribeNamespaceRequest;
import com.aliyun.gpdb20160503.models.InitVectorDatabaseRequest;
import com.aliyun.gpdb20160503.models.InitVectorDatabaseResponse;
import com.aliyun.gpdb20160503.models.QueryCollectionDataRequest;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponse;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponseBody;
import com.aliyun.gpdb20160503.models.UpsertCollectionDataRequest;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.tea.TeaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author HeYQ
 * @version 1.0
 * @since 2024-10-23 20:29
 */
public class AnalyticdbVector implements VectorStore {

	private static final Logger logger = LoggerFactory.getLogger(AnalyticdbVector.class);

	private final String collectionName;

	private AnalyticdbConfig config;

	private Client client;

	public AnalyticdbVector(String collectionName, AnalyticdbConfig config) throws Exception {
		// collection_name must be updated every time
		this.collectionName = collectionName.toLowerCase();
		this.config = config;
		Config clientConfig = Config.build(this.config.toAnalyticdbClientParams());
		this.client = new Client(clientConfig);
		initialize();
		logger.debug("created AnalyticdbVector client success");
	}

	/**
	 * initialize vector db
	 */
	private void initialize() throws Exception {
		initializeVectorDataBase();
		createNameSpaceIfNotExists();
		createCollectionIfNotExists(this.config.getEmbeddingDimension());
	}

	private void initializeVectorDataBase() throws Exception {
		InitVectorDatabaseRequest request = new InitVectorDatabaseRequest().setDBInstanceId(config.getDBInstanceId())
			.setRegionId(config.getRegionId())
			.setManagerAccount(config.getManagerAccount())
			.setManagerAccountPassword(config.getManagerAccountPassword());
		InitVectorDatabaseResponse initVectorDatabaseResponse = client.initVectorDatabase(request);
		logger.debug("successfully initialize vector database, response body:{}", initVectorDatabaseResponse.getBody());

	}

	private void createNameSpaceIfNotExists() throws Exception {
		try {
			DescribeNamespaceRequest request = new DescribeNamespaceRequest()
				.setDBInstanceId(this.config.getDBInstanceId())
				.setRegionId(this.config.getRegionId())
				.setNamespace(this.config.getNamespace())
				.setManagerAccount(this.config.getManagerAccount())
				.setManagerAccountPassword(this.config.getManagerAccountPassword());
			this.client.describeNamespace(request);
		}
		catch (TeaException e) {
			if (Objects.equals(e.getStatusCode(), 404)) {
				CreateNamespaceRequest request = new CreateNamespaceRequest()
					.setDBInstanceId(this.config.getDBInstanceId())
					.setRegionId(this.config.getRegionId())
					.setNamespace(this.config.getNamespace())
					.setManagerAccount(this.config.getManagerAccount())
					.setManagerAccountPassword(this.config.getManagerAccountPassword())
					.setNamespacePassword(this.config.getNamespacePassword());
				this.client.createNamespace(request);
			}
			else {
				throw new Exception("failed to create namespace:{}", e);
			}
		}
	}

	private void createCollectionIfNotExists(Long embeddingDimension) throws Exception {
		try {
			// Describe the collection to check if it exists
			DescribeCollectionRequest describeRequest = new DescribeCollectionRequest()
				.setDBInstanceId(this.config.getDBInstanceId())
				.setRegionId(this.config.getRegionId())
				.setNamespace(this.config.getNamespace())
				.setNamespacePassword(this.config.getNamespacePassword())
				.setCollection(this.collectionName);
			this.client.describeCollection(describeRequest);
			logger.debug("collection" + this.collectionName + "already exists");
		}
		catch (TeaException e) {
			if (Objects.equals(e.getStatusCode(), 404)) {
				// Collection does not exist, create it
				String metadata = JSON.toJSONString(new JSONObject().fluentPut("refDocId", "text")
					.fluentPut("content", "text")
					.fluentPut("metadata", "jsonb"));
				String fullTextRetrievalFields = "content";
				CreateCollectionRequest createRequest = new CreateCollectionRequest()
					.setDBInstanceId(this.config.getDBInstanceId())
					.setRegionId(this.config.getRegionId())
					.setManagerAccount(this.config.getManagerAccount())
					.setManagerAccountPassword(this.config.getManagerAccountPassword())
					.setNamespace(this.config.getNamespace())
					.setCollection(this.collectionName)
					.setDimension(embeddingDimension)
					.setMetrics(this.config.getMetrics())
					.setMetadata(metadata)
					.setFullTextRetrievalFields(fullTextRetrievalFields);
				this.client.createCollection(createRequest);
				logger.debug("collection" + this.collectionName + "created");
			}
			else {
				throw new RuntimeException(
						"Failed to create collection " + this.collectionName + ": " + e.getMessage());
			}
		}
	}

	@Override
	public void add(List<Document> documents) {
		List<UpsertCollectionDataRequest.UpsertCollectionDataRequestRows> rows = new ArrayList<>(10);
		for (Document doc : documents) {
			float[] floatEmbeddings = doc.getEmbedding();
			List<Double> embedding = new ArrayList<>(floatEmbeddings.length);
			for (float floatEmbedding : floatEmbeddings) {
				embedding.add((double) floatEmbedding);
			}
			Map<String, String> metadata = new HashMap<>();
			metadata.put("refDocId", (String) doc.getMetadata().get("docId"));
			metadata.put("content", doc.getContent());
			metadata.put("metadata", JSON.toJSONString(doc.getMetadata()));
			rows.add(new UpsertCollectionDataRequest.UpsertCollectionDataRequestRows().setVector(embedding)
				.setMetadata(metadata));
		}
		UpsertCollectionDataRequest request = new UpsertCollectionDataRequest()
			.setDBInstanceId(this.config.getDBInstanceId())
			.setRegionId(this.config.getRegionId())
			.setNamespace(this.config.getNamespace())
			.setNamespacePassword(this.config.getNamespacePassword())
			.setCollection(this.collectionName)
			.setRows(rows);
		try {
			this.client.upsertCollectionData(request);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to add collection data by IDs: " + e.getMessage(), e);
		}
	}

	@Override
	public Optional<Boolean> delete(List<String> ids) {
		if (ids.isEmpty()) {
			return Optional.of(false);
		}
		String idsStr = ids.stream().map(id -> "'" + id + "'").collect(Collectors.joining(", ", "(", ")"));
		DeleteCollectionDataRequest request = new DeleteCollectionDataRequest()
			.setDBInstanceId(this.config.getDBInstanceId())
			.setRegionId(this.config.getRegionId())
			.setNamespace(this.config.getNamespace())
			.setNamespacePassword(this.config.getNamespacePassword())
			.setCollection(this.collectionName)
			.setCollectionData(null)
			.setCollectionDataFilter("refDocId IN " + idsStr);
		try {
			DeleteCollectionDataResponse deleteCollectionDataResponse = this.client.deleteCollectionData(request);
			return deleteCollectionDataResponse.statusCode.equals(200) ? Optional.of(true) : Optional.of(false);
			// Handle response if needed
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to delete collection data by IDs: " + e.getMessage(), e);
		}
	}

	@Override
	public List<Document> similaritySearch(String query) {

		return similaritySearch(SearchRequest.query(query));

	}

	@Override
	public List<Document> similaritySearch(SearchRequest searchRequest) {
		double scoreThreshold = searchRequest.getSimilarityThreshold();
		boolean includeValues = searchRequest.hasFilterExpression();
		int topK = searchRequest.getTopK();

		QueryCollectionDataRequest request = new QueryCollectionDataRequest()
			.setDBInstanceId(this.config.getDBInstanceId())
			.setRegionId(this.config.getRegionId())
			.setNamespace(this.config.getNamespace())
			.setNamespacePassword(this.config.getNamespacePassword())
			.setCollection(this.collectionName)
			.setIncludeValues(includeValues)
			.setMetrics(this.config.getMetrics())
			.setVector(null)
			.setContent(searchRequest.query)
			.setTopK((long) topK)
			.setFilter(null);
		try {
			QueryCollectionDataResponse response = this.client.queryCollectionData(request);
			List<Document> documents = new ArrayList<>();
			for (QueryCollectionDataResponseBody.QueryCollectionDataResponseBodyMatchesMatch match : response.getBody()
				.getMatches()
				.getMatch()) {
				if (match.getScore() != null && match.getScore() > scoreThreshold) {
					Map<String, String> metadata = match.getMetadata();
					String pageContent = metadata.get("content");
					Map<String, Object> metadataJson = JSONObject.parseObject(metadata.get("metadata"), HashMap.class);
					Document doc = new Document(pageContent, metadataJson);
					documents.add(doc);
				}
			}
			return documents;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to search by full text: " + e.getMessage(), e);
		}
	}

}
