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
package com.alibaba.cloud.ai.vectorstore.opensearch;

import com.aliyun.ha3engine.vector.Client;
import com.aliyun.ha3engine.vector.models.*;
import com.aliyun.teautil.Common;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import java.util.*;

/**
 * Provides an API for interacting with the OpenSearch service, including uploading,
 * deleting, and searching documents.
 *
 * @author fuyou.lxm
 * @since 1.0.0-M6
 */
public class OpenSearchApi {

	private static final Logger logger = LoggerFactory.getLogger(OpenSearchApi.class);

	private final Client client;

	private final OpenSearchVectorStoreProperties properties;

	/**
	 * Initializes a new instance of the OpenSearchApi class.
	 * @param properties basic configuration for connecting to OpenSearch instance.
	 * @throws RuntimeException If the initialization fails due to an error in configuring
	 * or creating the OpenSearch client. The exception message will include details of
	 * the underlying error.
	 */
	public OpenSearchApi(OpenSearchVectorStoreProperties properties) {
		try {
			this.properties = properties;
			Config openSearchConfiguration = Config.build(properties.toClientParams());
			this.client = new Client(openSearchConfiguration);
		}
		catch (Exception exception) {
			logger.error("init OpenSearch client error", exception);
			throw new RuntimeException(exception);
		}
	}

	private String getFullTableName(String tableName) {
		return this.properties.getInstanceId() + "_" + tableName;
	}

	public void createCollectionAndIndex(String mappingJson) throws Exception {
		String queryUri = "/openapi/ha3/instances/" + this.properties.getInstanceId() + "/tables";
		Map<String, ?> ansMap = client._request("POST", queryUri, null, client.getHeadersFromRunTimeOption(),
				Common.toJSONString(mappingJson), client._runtimeOptions);

		if (null == ansMap.get("requestId")) {
			throw new RuntimeException(
					"OpenSearch autoInitializeIndex failed. Error message:{} " + ansMap.get("message"));
		}

		logger.info("OpenSearch autoInitializeIndex success. requestId:{}", ansMap.get("requestId"));
	}

	public List<Object> getIndexList(String tableName) throws Exception {
		String queryUri = "/openapi/ha3/instances/" + this.properties.getInstanceId() + "/tables" + "/" + tableName;
		Map<String, ?> ansMap = client._request("GET", queryUri, null, client.getHeadersFromRunTimeOption(), null,
				client._runtimeOptions);

		if (null == ansMap.get("requestId")) {
			throw new RuntimeException(
					"OpenSearch autoInitializeIndex failed. Error message:{} " + ansMap.get("message"));
		}
		return Collections.singletonList(ansMap.get("vectorIndex"));
	}

	/**
	 * Uploads a document to the specified OpenSearch index.
	 * @param tableName The name of the OpenSearch index to which the document should be
	 * uploaded.
	 * @param pKField The primary key field name for the document in the index.
	 * @param document A list containing a map that represents the document to be
	 * uploaded. Each key-value pair in the map corresponds to a field and its value in
	 * the document.
	 * @throws RuntimeException If the upload fails due to an error in the OpenSearch
	 * service. The exception message will include the error code and error message from
	 * the service response, or a generic message if no specific details are available.
	 */
	public void uploadDocument(String tableName, String pKField, List<Map<String, ?>> document) {
		PushDocumentsRequest request = new PushDocumentsRequest();
		request.setBody(document);

		try {
			PushDocumentsResponse response = client.pushDocuments(getFullTableName(tableName), pKField, request);
			ResponseBody responseBody = new ResponseBody(response.getBody());

			if (!responseBody.isSuccess()) {
				String errorCode = responseBody.errorCode;
				String errorMsg = Optional.ofNullable(responseBody.errorMessage).orElse("No error message provided");
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
	 * @param tableName The name of the OpenSearch table from which to delete the
	 * document.
	 * @param pKField The primary key field of the document to be deleted.
	 * @param document A list of maps representing the document(s) to be deleted. Each map
	 * should contain the primary key field and its corresponding value.
	 * @throws RuntimeException If the deletion fails due to an error in the OpenSearch
	 * service.
	 */
	public void deleteDocument(String tableName, String pKField, List<Map<String, ?>> document) {
		PushDocumentsRequest request = new PushDocumentsRequest();
		request.setBody(document);

		try {
			PushDocumentsResponse response = client.pushDocuments(getFullTableName(tableName), pKField, request);
			ResponseBody responseBody = new ResponseBody(response.getBody());

			if (!responseBody.isSuccess()) {
				String errorCode = responseBody.errorCode;
				String errorMsg = Optional.ofNullable(responseBody.errorMessage).orElse("No error message provided");
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
	 * Executes a search query and returns a list of objects of the specified type.
	 * @param queryRequest The query request containing the search parameters.
	 * @param itemConverter The converter used to transform each result item (JSONObject)
	 * into an object of the specified type.
	 * @return A list of objects of the specified type, extracted from the search
	 * response.
	 * @throws RuntimeException if an exception occurs during the search query execution.
	 */
	public <T> List<T> search(QueryRequest queryRequest, Converter<JsonNode, T> itemConverter) {
		try {
			SearchResponse searchResponse = client.inferenceQuery(queryRequest);
			SearchResponseBody responseBody = getSearchResponseBody(searchResponse);
			List<T> documents = new ArrayList<>();
			Integer totalCount = responseBody.totalCount;

			// Return an empty list if totalCount is null or less than or equal to zero
			if (totalCount == null || totalCount <= 0) {
				return new ArrayList<>();
			}

			ArrayNode resultArray = responseBody.result;
			if (resultArray != null && !resultArray.isEmpty()) {
				for (JsonNode item : resultArray) {
					documents.add(itemConverter.convert(item));
				}
			}
			return documents;
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
	private SearchResponseBody getSearchResponseBody(SearchResponse searchResponse) {
		SearchResponseBody responseBody = new SearchResponseBody(searchResponse.getBody());

		if (responseBody.hasError()) {
			String errorCode = responseBody.errorCode;
			String errorMsg = Optional.ofNullable(responseBody.errorMessage).orElse("No error message provided");
			throw new RuntimeException(String.format(
					"OpenSearch inferenceQuery failed. " + "Error code: %s. Error message: %s", errorCode, errorMsg));
		}

		return responseBody;
	}

	/**
	 * Represents the response body from the OpenSearch API.
	 */
	private record ResponseBody(

			Integer code,

			String status,

			String errorCode,

			String errorMessage) {

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
			this(parseJson(pushDocumentsResponseBodyString));
		}

		/**
		 * Constructs a ResponseBody instance from a JSONObject.
		 * @param jsonObject The JSONObject representing the response body.
		 */
		public ResponseBody(JsonNode jsonObject) {
			this(jsonObject.path(CODE_KEY).asInt(), jsonObject.path(STATUS_KEY).asText(),
					jsonObject.path(ERROR_CODE_KEY).asText(), jsonObject.path(ERROR_MESSAGE_KEY).asText());
		}

		private static JsonNode parseJson(String jsonString) {
			try {
				return new ObjectMapper().readTree(jsonString);
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException("Failed to parse JSON", e);
			}
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
	public record SimilarityResult(

			String id,

			double score,

			String content,

			Map<String, Object> metadata) {
	}

	/**
	 * Represents the response body from a search operation.
	 */
	private record SearchResponseBody(

			String errorCode,

			String errorMessage,

			Integer totalCount,

			ArrayNode result) {

		private static final String TOTAL_COUNT_KEY = "totalCount";

		private static final String ERROR_CODE_KEY = "errorCode";

		private static final String ERROR_MESSAGE_KEY = "errorMsg";

		private static final String RESULT_KEY = "result";

		public SearchResponseBody(String searchResponseBodyString) {
			this(parseJson(searchResponseBodyString));
		}

		public SearchResponseBody(JsonNode jsonObject) {
			this(jsonObject.path(ERROR_CODE_KEY).asText(), jsonObject.path(ERROR_MESSAGE_KEY).asText(),
					jsonObject.path(TOTAL_COUNT_KEY).asInt(), (ArrayNode) jsonObject.path(RESULT_KEY));
		}

		private static JsonNode parseJson(String jsonString) {
			try {
				return new ObjectMapper().readTree(jsonString);
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException("Failed to parse JSON", e);
			}
		}

		public boolean hasError() {
			return errorCode != null && !errorCode.isEmpty();
		}
	}

}
