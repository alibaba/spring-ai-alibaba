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

import com.alibaba.cloud.ai.vectorstore.tablestore.TablestoreVectorStore;
import com.aliyun.openservices.tablestore.agent.knowledge.KnowledgeStoreImpl;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TablestoreVectorStoreExample {

	/**
	 * 因为 Spring AI Vector Store 兼容版基于 `Knowledge Store` 实现，所以需要先初始化 KnowledgeStore
	 * @param knowledgeStoreImpl 如何初始化 KnowledgeStore 请参考
	 * {@link KnowledgeStoreInitExample}
	 */
	public void example(KnowledgeStoreImpl knowledgeStoreImpl) throws Exception {

		/*
		 * 创建一个Embedding模型供后续使用。维度768。
		 */
		FakedEmbeddingService embeddingService = new FakedEmbeddingService(768);

		TablestoreVectorStore store = TablestoreVectorStore.builder(knowledgeStoreImpl, embeddingService)
			.initializeTable(true) // 首次使用可将该参数设置为 true，进行初始化表。后续不需要设置。
			.build();

		/*
		 * 初始化表.spring内部会自动初始化表，如果非spring场景，可以人工调用。或者直接使用
		 * knowledgeStoreImpl.initTable()完成初始化。
		 */
		store.afterPropertiesSet();

		/*
		 * 声明文档
		 */
		Map<String, Object> metadata = new HashMap();
		metadata.put("city", "hangzhou");
		metadata.put("year", 2005);
		// 因 KnowledgeStoreImpl 内部有多租户优化，而spring不支持多租户优化，这里我们将多租户设置到metadata中
		metadata.put(com.aliyun.openservices.tablestore.agent.model.Document.DOCUMENT_TENANT_ID, "租户id_user小明");
		Document document = new Document("文档id_001", "The World is Big and Salvation Lurks Around the Corner",
				metadata);

		// 添加文档
		store.add(List.of(document));

		// 删除文档
		store.delete(List.of("文档id_001"));

		/*
		 * 搜索文档。
		 *
		 * 为了兼容KnowledgeStoreImpl的多租户能力，请在查询条件里设置: tenant_id == 'user3' 或者 tenant_id IN
		 * ['user1', 'user2']
		 *
		 * <p> tenant_id 的字段名是固定的，来自常量 {@link
		 * com.aliyun.openservices.tablestore.agent.model.Document.DOCUMENT_TENANT_ID}</p>
		 */
		String filterExpression = " (city == 'hangzhou' || year < 2025) && tenant_id == 'user3' ";
		List<Document> results = store.similaritySearch(SearchRequest.builder()
			.filterExpression(filterExpression)
			.query("The World")
			.topK(5)
			.similarityThreshold(0.0f)
			.build());
		for (Document result : results) {
			String docId = result.getId();
			String docText = result.getText();
			Map<String, Object> docMetadata = result.getMetadata();
			Double score = result.getScore();
		}
	}

}
