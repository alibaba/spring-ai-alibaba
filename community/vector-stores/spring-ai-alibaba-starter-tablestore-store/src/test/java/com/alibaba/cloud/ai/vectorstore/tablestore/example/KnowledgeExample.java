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
		 * 创建一个Embedding模型供后续使用。维度768。
		 */
		FakedEmbeddingService fakedEmbeddingService = new FakedEmbeddingService(768);

		// 创建一个 document
		String tenantId = "user_小明"; // 如果不涉及多租户场景，后续其它地方的tenantId相关参数不传即可。如果涉及多租户，该租户id填什么值由各个业务自己决定，通常来说使用用户id或者知识库id当做租户id有通用性。
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

		// 将 document 存入知识库(如果之前存在则覆盖)
		store.putDocument(document);

		// 读取 document
		Document documentByRead = store.getDocument("文档id_a", tenantId);

		// 批量读取 document
		store.getDocuments(Arrays.asList("文档id_a", "文档id_b", "文档id_c"), tenantId);

		// 更新 document
		store.updateDocument(document);

		// 删除 document
		store.deleteDocument("文档id_a", tenantId);
		// 删除一个租户下所有 document
		store.deleteDocumentByTenant(tenantId);
		// 删除一个租户下满足条件的 document, 条件：city=="shanghai" && year >= 2005
		store.deleteDocument(new HashSet<>(Collections.singletonList(tenantId)),
				Filters.and(Filters.eq("city", "shanghai"), Filters.gte("year", 2005)));

		// 查询："你好" 相关的文档
		String queryText = "你好";
		Set<String> tenantIds = new HashSet<>(Collections.singletonList(tenantId)); // 多租户场景下租户id。如果不涉及多租户场景，则传null或空集合即可
		Filter metadataFilter1 = null; // 过滤条件.
		Filter metadataFilter2 = Filters.and(Filters.eq("city", "shanghai"), Filters.gte("year", 2005)); // 过滤条件.
																											// city=="shanghai"
																											// &&
																											// year
																											// >=
																											// 2005
		{
			// 1. 使用 向量检索 document
			{
				float[] queryVector = fakedEmbeddingService.embed(queryText);
				int topK = 20;
				Float minScore = 0.0f; // 0.0f或者null表示不限制

				List<String> columnsToGet = new ArrayList<>(); // 该参数填null或空集合会默认返回初始化KnowledgeStore时候的参数里定义的所有非向量字段。
				Response<DocumentHit> response = store.vectorSearch(queryVector, topK, minScore, tenantIds,
						metadataFilter1, columnsToGet);
				List<DocumentHit> hits = response.getHits();
				for (DocumentHit hit : hits) {
					// 获取到文档
					Document doc = hit.getDocument();
					// 获取到分数
					Double score = hit.getScore();
				}
			}
			// 2. 使用全文检索 (其它参数不再细节讲解，请参考上面的向量检索的文档示例)
			{
				int limit = 50;
				// 这里展示遍历获取所有文档，如果仅需要前面几个，则只进行首次查询即可。
				String nextToken = null;
				do {
					Response<DocumentHit> response = store.fullTextSearch(queryText, tenantIds, limit, metadataFilter1,
							nextToken, null);
					List<DocumentHit> hits = response.getHits();
					for (DocumentHit hit : hits) {
						// 获取到文档
						Document doc = hit.getDocument();
						// 获取到分数
						Double score = hit.getScore();
					}
					nextToken = response.getNextToken();
				}
				while (nextToken != null);

			}
			// 3. 相对灵活的自定义查询: 下面只根据meta进行过滤数据为例。
			{
				Filter textQuery = Filters.textMatch(store.getTextField(), queryText); // 查询文档的text文本字段。也可以查询其它metaData里的文本字段
				// 进行全文检索同时需要满足metadataFilter2，这里可以自由组合其它任意条件。
				Filter finalFilter = Filters.and(textQuery, metadataFilter2);

				// 这里展示遍历获取所有文档，如果仅需要前面几个，则只进行首次查询即可。
				String nextToken = null;
				do {
					KnowledgeSearchRequest searchRequest = KnowledgeSearchRequest.builder()
						.tenantIds(tenantIds) // 多租户场景下设置租户id可提高性能。如果不涉及多租户场景，则传null或空集合即可
						.limit(10)
						.metadataFilter(finalFilter) // 不需要额外添加多租户的条件。上面已经在构造函数里添加了.
														// 额外添加对功能没影响，对性能影响很小。
						.nextToken(nextToken)
						.sort(new FieldSort("city", Order.ASC)) // 按照city字段升序。
																// 不设置排序通常有更快的性能。
						.build();
					Response<DocumentHit> response = store.searchDocuments(searchRequest);
					List<DocumentHit> hits = response.getHits();
					for (DocumentHit hit : hits) {
						// 获取到文档
						Document doc = hit.getDocument();
						// 获取到分数(只查询metaData元数据时，score无意义)
						Double score = hit.getScore();
					}
					nextToken = response.getNextToken();
				}
				while (nextToken != null);
			}
		}

	}

}
