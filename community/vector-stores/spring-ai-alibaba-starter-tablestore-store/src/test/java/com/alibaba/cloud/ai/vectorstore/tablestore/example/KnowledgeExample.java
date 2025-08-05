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
package com.alibaba.cloud.ai.vectorstore.tablestore.example;

import com.aliyun.openservices.tablestore.agent.knowledge.KnowledgeSearchRequest;
import com.aliyun.openservices.tablestore.agent.knowledge.KnowledgeStoreImpl;
import com.aliyun.openservices.tablestore.agent.model.Document;
import com.aliyun.openservices.tablestore.agent.model.DocumentHit;
import com.aliyun.openservices.tablestore.agent.model.Response;
import com.aliyun.openservices.tablestore.agent.model.filter.Filter;
import com.aliyun.openservices.tablestore.agent.model.filter.Filters;
import com.aliyun.openservices.tablestore.agent.model.sort.FieldSort;
import com.aliyun.openservices.tablestore.agent.model.sort.Order;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KnowledgeExample {

	public void example(KnowledgeStoreImpl store) throws Exception {

		/*
		 * Create an Embedding model for subsequent use. Dimension 768.
		 */
		FakedEmbeddingService fakedEmbeddingService = new FakedEmbeddingService(768);

		// Create a document
		String tenantId = "user_小明"; // If not involving multi-tenant scenarios, don't
										// pass tenantId related parameters in other
										// places. If involving multi-tenant, what value
										// to fill for tenantId is decided by each
										// business itself, typically using user ID or
										// knowledge base ID as tenant ID has generality.
		Document document = new Document("文档id_a", tenantId);
		document.setText("你好，世界");
		float[] embedding = fakedEmbeddingService.embed(document.getText());
		document.setEmbedding(embedding);
		document.getMetadata().put("meta_example_string", "abc");
		document.getMetadata().put("meta_example_text", "abc");
		document.getMetadata().put("meta_example_long", 1);
		document.getMetadata().put("meta_example_double", 0.5);
		document.getMetadata().put("meta_example_boolean", true);
		document.getMetadata().put("meta_example_bytes", "test".getBytes(StandardCharsets.UTF_8));

		// Store document into knowledge base (if existed before then overwrite)
		store.putDocument(document);

		// Read document
		Document documentByRead = store.getDocument("文档id_a", tenantId);

		// Batch read documents
		store.getDocuments(Arrays.asList("文档id_a", "文档id_b", "文档id_c"), tenantId);

		// Update document
		store.updateDocument(document);

		// Delete document
		store.deleteDocument("文档id_a", tenantId);
		// Delete all documents under a tenant
		store.deleteDocumentByTenant(tenantId);
		// Delete documents under a tenant that meet conditions: city=="shanghai" && year
		// >= 2005
		store.deleteDocument(new HashSet<>(Collections.singletonList(tenantId)),
				Filters.and(Filters.eq("city", "shanghai"), Filters.gte("year", 2005)));

		// Query: documents related to "你好"
		String queryText = "你好";
		// Tenant ID in multi-tenant scenarios. If not involving multi-tenant scenarios,
		// pass null or empty collection.
		Set<String> tenantIds = new HashSet<>(Collections.singletonList(tenantId));
		Filter metadataFilter1 = null; // Filter condition.
		Filter metadataFilter2 = Filters.and(Filters.eq("city", "shanghai"), Filters.gte("year", 2005)); // Filter
																											// condition.
																											// city=="shanghai"
																											// &&
																											// year
																											// >=
																											// 2005
		{
			// 1. Using vector retrieval for documents
			{
				float[] queryVector = fakedEmbeddingService.embed(queryText);
				int topK = 20;
				Float minScore = 0.0f; // 0.0f or null indicates no limit

				// If this parameter is null or empty collection, it will return all
				// non-vector fields defined in KnowledgeStore initialization parameters
				// by default.
				List<String> columnsToGet = new ArrayList<>();
				Response<DocumentHit> response = store.vectorSearch(queryVector, topK, minScore, tenantIds,
						metadataFilter1, columnsToGet);
				List<DocumentHit> hits = response.getHits();
				for (DocumentHit hit : hits) {
					// Get document
					Document doc = hit.getDocument();
					// Get score
					Double score = hit.getScore();
				}
			}
			// 2. Using full-text retrieval (For details on other parameters, please refer
			// to the documentation example of vector retrieval above)
			{
				int limit = 50;
				// Here shows traversing all documents, if only need first few, just
				// perform first query.
				String nextToken = null;
				do {
					Response<DocumentHit> response = store.fullTextSearch(queryText, tenantIds, limit, metadataFilter1,
							nextToken, null);
					List<DocumentHit> hits = response.getHits();
					for (DocumentHit hit : hits) {
						// Get document
						Document doc = hit.getDocument();
						// Get score
						Double score = hit.getScore();
					}
					nextToken = response.getNextToken();
				}
				while (nextToken != null);

			}
			// 3. Relatively flexible custom queries: below is an example of filtering
			// data based on meta information only.
			{
				// Query text field of document. Can also query other text fields in
				// metaData.
				Filter textQuery = Filters.textMatch(store.getTextField(), queryText);
				// Perform full-text search while needing to meet metadataFilter2, can
				// freely combine with any other conditions here.
				Filter finalFilter = Filters.and(textQuery, metadataFilter2);

				// Here shows traversing all documents, if only need first few, just
				// perform first query.
				String nextToken = null;
				do {
					KnowledgeSearchRequest searchRequest = KnowledgeSearchRequest.builder()
						// Setting tenant ID in multi-tenant scenarios can improve
						// performance. If not involving multi-tenant scenarios, pass null
						// or empty collection.
						.tenantIds(tenantIds)
						.limit(10)
						.metadataFilter(finalFilter) // No need to add extra multi-tenant
														// conditions. Already added in
														// constructor above.
														// Extra addition has no impact on
														// functionality, very small
														// impact on performance.
						.nextToken(nextToken)
						.sort(new FieldSort("city", Order.ASC)) // Sort by city field
																// ascending.
																// Not setting sorting
																// usually has faster
																// performance.
						.build();
					Response<DocumentHit> response = store.searchDocuments(searchRequest);
					List<DocumentHit> hits = response.getHits();
					for (DocumentHit hit : hits) {
						// Get document
						Document doc = hit.getDocument();
						// Get score(When querying metaData only, the score is not
						// meaningful.)
						Double score = hit.getScore();
					}
					nextToken = response.getNextToken();
				}
				while (nextToken != null);
			}
		}

	}

}
