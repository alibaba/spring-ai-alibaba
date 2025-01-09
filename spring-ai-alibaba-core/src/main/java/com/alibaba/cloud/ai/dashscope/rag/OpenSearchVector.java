/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.dashscope.rag;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.ha3engine.vector.Client;
import com.aliyun.ha3engine.vector.models.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the VectorStore interface for Alibaba OpenSearch.
 *
 * @author fuyou.lxm
 * @since 1.0.0-M3
 */
public class OpenSearchVector implements VectorStore {

	private static final Logger logger = LoggerFactory.getLogger(OpenSearchVector.class);

	private static final String ID_FIELD_NAME = "id";

	private static final String CONTENT_FIELD_NAME = "content";

	private static final String METADATA_FIELD_NAME = "metadata";

	private static final String EMPTY_TEXT = "";

	private final String tableName;

	private final String pKField;

	private final List<String> outputFields;

	private final OpenSearchClientWrapper openSearchClient;

	/**
	 * Constructs a new instance of OpenSearchVector with the specified table name and
	 * OpenSearch configuration. This constructor initializes the output fields to include
	 * CONTENT_FIELD_NAME and METADATA_FIELD_NAME.
	 * @param tableName The name of the table where documents will be stored.
	 * @param openSearchConfig The configuration for OpenSearch.
	 */
	public OpenSearchVector(String tableName, OpenSearchConfig openSearchConfig) {
		this(tableName, List.of(CONTENT_FIELD_NAME, METADATA_FIELD_NAME), openSearchConfig);
	}

	/**
	 * Constructs a new instance of OpenSearchVector with the specified table name, output
	 * fields, and OpenSearch configuration. Initializes the OpenSearch client using the
	 * provided configuration.
	 * @param tableName The name of the table where documents will be stored.
	 * @param outputFields A list of field names that should be included in the output.
	 * @param openSearchConfig The configuration for OpenSearch.
	 * @throws RuntimeException If there is an error initializing OpenSearch client.
	 */
	public OpenSearchVector(String tableName, List<String> outputFields, OpenSearchConfig openSearchConfig) {
		this.tableName = tableName;
		this.outputFields = outputFields;
		this.pKField = ID_FIELD_NAME;

		try {
			// init OpenSearch client
			Config config = Config.build(openSearchConfig.toClientParams());
			var instanceId = config.getInstanceId();
			var client = new Client(config);

			this.openSearchClient = new OpenSearchClientWrapper(client, instanceId, tableName, this.pKField);
		}
		catch (Exception e) {
			logger.error("init OpenSearch client error", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void add(List<Document> documents) {
		for (Document document : documents) {
			/*
			 * Document push outer structure, can add document operation structures. The
			 * structure supports one or more document operations.
			 */
			List<Map<String, ?>> documentToAdd = new ArrayList<>();
			Map<String, Object> documentMap = new HashMap<>();
			Map<String, Object> documentFields = new HashMap<>();

			// Insert document content information, key-value pairs matching.
			// The field_pk field must be consistent with the pkField configuration.
			documentFields.put(ID_FIELD_NAME, document.getId());
			documentFields.put(CONTENT_FIELD_NAME, document.getText());
			// Convert metadata to JSON
			documentFields.put(METADATA_FIELD_NAME, JSON.toJSONString(document.getMetadata()));

			// Add document content to documentEntry structure.
			documentMap.put("fields", documentFields);
			// New document command: add
			documentMap.put("cmd", "add");
			documentToAdd.add(documentMap);

			openSearchClient.uploadDocument(documentToAdd);
		}
	}

	@Override
	public Optional<Boolean> delete(List<String> idList) {
		for (String id : idList) {
			List<Map<String, ?>> documentToDelete = new ArrayList<>();
			Map<String, Object> documentMap = new HashMap<>();
			Map<String, Object> documentFields = new HashMap<>();

			documentFields.put(this.pKField, id);
			documentMap.put("fields", documentFields);
			documentMap.put("cmd", "delete");
			documentToDelete.add(documentMap);

			openSearchClient.deleteDocument(documentToDelete);
		}

		return Optional.of(true);
	}

	@Override
	public List<Document> similaritySearch(String query) {
		return this.similaritySearch(SearchRequest.builder().query(query).build());
	}

	@Override
	public List<Document> similaritySearch(SearchRequest searchRequest) {
		Assert.notNull(searchRequest, "The search request must not be null.");

		// set similarityThreshold
		var similarityThreshold = searchRequest.getSimilarityThreshold();

		QueryRequest queryRequest = new QueryRequest();
		queryRequest.setTableName(this.tableName); // Required, the name of the table to
													// query
		queryRequest.setContent(searchRequest.getQuery());
		queryRequest.setModal("text"); // Required, used for vectorizing the query term
		queryRequest.setTopK(searchRequest.getTopK()); // number of results to return
		queryRequest.setOutputFields(this.outputFields);

		try {
			List<SimilarityResult> similarityResults = openSearchClient.search(queryRequest);
			return similarityResults.stream()
				.filter(result -> result.score >= similarityThreshold)
				.map(result -> new Document(result.id, result.content, result.metadata))
				.collect(Collectors.toList());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Represents the response body from a search operation.
	 */
	private record SearchResponseBody(String errorCode, String errorMessage,

			Integer totalCount, JSONArray result) {

		private static final String TOTAL_COUNT_KEY = "totalCount";

		private static final String ERROR_CODE_KEY = "errorCode";

		private static final String ERROR_MESSAGE_KEY = "errorMsg";

		private static final String RESULT_KEY = "result";

		public SearchResponseBody(String searchResponseBodyString) {
			this(JSON.parseObject(searchResponseBodyString));
		}

		public SearchResponseBody(JSONObject jsonObject) {
			this(jsonObject.getString(ERROR_CODE_KEY), jsonObject.getString(ERROR_MESSAGE_KEY),
					jsonObject.getInteger(TOTAL_COUNT_KEY), jsonObject.getJSONArray(RESULT_KEY));
		}

		public boolean hasError() {
			return errorCode != null && !errorCode.isEmpty();
		}
	}

	/**
	 * Represents the response body from the OpenSearch API.
	 */
	private record ResponseBody(Integer code, String status, String errorCode, String errorMessage) {

		private static final String CODE_KEY = "code";

		private static final String STATUS_KEY = "status";

		private static final String ERROR_CODE_KEY = "errorCode";

		private static final String ERROR_MESSAGE_KEY = "errorMsg";

		private static final Integer SUCCESS_CODE = 200;

		/**
		 * Constructs a ResponseBody instance from a JSON string.
		 * @param pushDocumentsResponseBodyString The JSON string representing the
		 * response body.
		 */
		public ResponseBody(String pushDocumentsResponseBodyString) {
			this(JSON.parseObject(pushDocumentsResponseBodyString));
		}

		/**
		 * Constructs a ResponseBody instance from a JSONObject.
		 * @param jsonObject The JSONObject representing the response body.
		 */
		public ResponseBody(JSONObject jsonObject) {
			this(jsonObject.getInteger(CODE_KEY), jsonObject.getString(STATUS_KEY),
					jsonObject.getString(ERROR_CODE_KEY), jsonObject.getString(ERROR_MESSAGE_KEY));
		}

		/**
		 * Checks if the response is successful based on the code.
		 * @return true if the code is equal to SUCCESS_CODE, false otherwise.
		 */
		public boolean isSuccess() {
			return SUCCESS_CODE.equals(code);
		}
	}

	/**
	 * A record class representing the result of a similarity search. It contains the
	 * document ID, similarity score, content, and metadata.
	 *
	 * @param id The unique identifier of the document.
	 * @param score The similarity score of the document.
	 * @param content The content of the document.
	 * @param metadata Additional metadata associated with the document.
	 */
	public record SimilarityResult(String id, double score,

			String content,

			Map<String, Object> metadata) {
	}

	/**
	 * Wrapper class for interacting with OpenSearch, providing methods to upload
	 * documents, delete documents, and execute search queries.
	 */
	public static class OpenSearchClientWrapper {

		private final Client client;

		private final String fullTableName;

		private final String pKField;

		/**
		 * Constructs a new OpenSearchClientWrapper instance.
		 * @param client The Ha3 client used for communication with the OpenSearch
		 * service.
		 * @param instanceId The ID of the OpenSearch instance.
		 * @param tableName The name of the table within the OpenSearch instance.
		 * @param pKField The primary key field of the table.
		 */
		public OpenSearchClientWrapper(Client client, String instanceId, String tableName, String pKField) {
			this.client = client;
			this.pKField = pKField;
			this.fullTableName = instanceId + "_" + tableName;
		}

		/**
		 * Uploads document to the specified OpenSearch table.
		 * @param document A map wrapped in a list representing the document to be
		 * uploaded.
		 * @throws RuntimeException If the upload fails due to an error in the OpenSearch
		 * service.
		 */
		public void uploadDocument(List<Map<String, ?>> document) {
			PushDocumentsRequest request = new PushDocumentsRequest();
			request.setBody(document);

			try {
				PushDocumentsResponse response = client.pushDocuments(this.fullTableName, this.pKField, request);
				ResponseBody responseBody = new ResponseBody(response.getBody());

				if (!responseBody.isSuccess()) {
					String errorCode = responseBody.errorCode;
					String errorMsg = Optional.ofNullable(responseBody.errorMessage)
						.orElse("No error message provided");
					throw new RuntimeException(
							String.format("OpenSearch upload Document failed. " + "Error code: %s. Error message: %s",
									errorCode, errorMsg));
				}
			}
			catch (Exception e) {
				throw new RuntimeException("OpenSearch upload Document failed.Error message:" + e.getMessage(), e);
			}
		}

		/**
		 * Deletes a document from the specified OpenSearch table.
		 * @param document A map wrapped in a list representing the document to be
		 * deleted.
		 * @throws RuntimeException If the deletion fails due to an error in the
		 * OpenSearch service.
		 */
		public void deleteDocument(List<Map<String, ?>> document) {
			PushDocumentsRequest request = new PushDocumentsRequest();
			request.setBody(document);

			try {
				PushDocumentsResponse response = client.pushDocuments(this.fullTableName, this.pKField, request);
				ResponseBody responseBody = new ResponseBody(response.getBody());

				if (!responseBody.isSuccess()) {
					String errorCode = responseBody.errorCode;
					String errorMsg = Optional.ofNullable(responseBody.errorMessage)
						.orElse("No error message provided");
					throw new RuntimeException(
							String.format("OpenSearch delete Documents failed. " + "Error code: %s. Error message: %s",
									errorCode, errorMsg));
				}
			}
			catch (Exception e) {
				throw new RuntimeException("OpenSearch delete Documents failed. Error message:" + e.getMessage(), e);
			}
		}

		/**
		 * Executes a search query using the provided query request and returns the
		 * similarity results.
		 * @param queryRequest The query request containing the search parameters.
		 * @return A list of similarity results based on the search query.
		 * @throws RuntimeException If an exception occurs during the search process.
		 */
		public List<SimilarityResult> search(QueryRequest queryRequest) {
			try {
				SearchResponse searchResponse = client.inferenceQuery(queryRequest);
				SearchResponseBody responseBody = getSearchResponseBody(searchResponse);
				return SearchResultParser.parse(responseBody);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * Extracts and processes the search response body from a SearchResponse object.
		 * Throws a RuntimeException if the response contains an error.
		 * @param searchResponse The SearchResponse object containing the search results.
		 * @return The processed SearchResponseBody object.
		 * @throws RuntimeException If the search response contains an error.
		 */
		@NotNull
		private SearchResponseBody getSearchResponseBody(SearchResponse searchResponse) {
			SearchResponseBody responseBody = new SearchResponseBody(searchResponse.getBody());

			if (responseBody.hasError()) {
				String errorCode = responseBody.errorCode;
				String errorMsg = Optional.ofNullable(responseBody.errorMessage).orElse("No error message provided");
				throw new RuntimeException(
						String.format("OpenSearch inferenceQuery failed. " + "Error code: %s. Error message: %s",
								errorCode, errorMsg));
			}

			return responseBody;
		}

	}

	/**
	 * A utility class for parsing search results from a response body into a list of
	 * {@link SimilarityResult} objects. This class handles the extraction of relevant
	 * information such as document ID, content, score, and metadata from the JSON
	 * response.
	 */
	private static class SearchResultParser {

		private static final Logger logger = LoggerFactory.getLogger(SearchResultParser.class);

		private static final String FIELDS_KEY = "fields";

		private static final String SCORE_KEY = "score";

		/**
		 * Parses the search response body and returns a list of {@link SimilarityResult}
		 * objects.
		 * @param responseBody The search response body containing the results.
		 * @return A list of {@link SimilarityResult} objects extracted from the response
		 * body.
		 */
		private static List<SimilarityResult> parse(SearchResponseBody responseBody) {
			List<SimilarityResult> documents = new ArrayList<>();
			Integer totalCount = responseBody.totalCount;

			// Return an empty list if totalCount is null or less than or equal to zero
			if (totalCount == null || totalCount <= 0) {
				return documents;
			}

			JSONArray resultArray = responseBody.result;
			if (resultArray != null && !resultArray.isEmpty()) {
				for (Object item : resultArray) {
					documents.add(parse((JSONObject) item));
				}
			}
			return documents;
		}

		/**
		 * Parses a single JSON object representing a document into a
		 * {@link SimilarityResult} object.
		 * @param jsonDocument The JSON object containing the document details.
		 * @return A {@link SimilarityResult} object extracted from the JSON document.
		 */
		private static SimilarityResult parse(JSONObject jsonDocument) {
			String id = extractId(jsonDocument);
			String content = extractContent(jsonDocument);
			double score = extractScore(jsonDocument);
			Map<String, Object> metadata = extractMetadata(jsonDocument);

			return new SimilarityResult(id, score, content, metadata);
		}

		/**
		 * Extracts the content from the JSON document.
		 * @param jsonDocument The JSON object containing the document details.
		 * @return The content of the document, or empty string if not found or empty.
		 */
		private static String extractContent(JSONObject jsonDocument) {
			if (jsonDocument.containsKey(FIELDS_KEY)) {
				JSONObject fields = jsonDocument.getJSONObject(FIELDS_KEY);
				String content = fields.getString(CONTENT_FIELD_NAME);
				if (content == null || content.isEmpty()) {
					return EMPTY_TEXT;
				}
				return content;
			}

			return EMPTY_TEXT;
		}

		/**
		 * Extracts the ID from the JSON document.
		 * @param jsonDocument The JSON object containing the document details.
		 * @return The ID of the document, or empty string if not found or empty.
		 */
		private static String extractId(JSONObject jsonDocument) {
			String id = jsonDocument.getString(ID_FIELD_NAME);
			if (id == null || id.isEmpty()) {
				return EMPTY_TEXT;
			}
			return id;
		}

		/**
		 * Extracts the score from the JSON document.
		 * @param jsonDocument The JSON object containing the document details.
		 * @return The score of the document.
		 */
		private static double extractScore(JSONObject jsonDocument) {
			return jsonDocument.getDouble(SCORE_KEY);
		}

		/**
		 * Extracts the metadata from the JSON document.
		 * @param jsonDocument The JSON object containing the document details.
		 * @return A map of metadata extracted from the document, or an empty map if not
		 * found.
		 */
		@SuppressWarnings("unchecked")
		private static Map<String, Object> extractMetadata(JSONObject jsonDocument) {
			if (jsonDocument.containsKey(FIELDS_KEY)) {
				JSONObject fields = jsonDocument.getJSONObject(FIELDS_KEY);
				String metadataStr = fields.getString(METADATA_FIELD_NAME);

				return JSONObject.parseObject(metadataStr, HashMap.class);
			}
			else {
				return new HashMap<>();
			}
		}

	}

}
