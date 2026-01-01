///*
// * Copyright 2024-2026 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.alibaba.cloud.ai.graph.node;
//
//import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
//import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
//import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
//import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
//import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
//import com.alibaba.cloud.ai.graph.OverAllState;
//import com.alibaba.cloud.ai.model.RerankModel;
//
//import org.springframework.ai.document.Document;
//import org.springframework.ai.document.MetadataMode;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.vectorstore.SimpleVectorStore;
//import org.springframework.ai.vectorstore.filter.Filter;
//import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class KnowledgeRetrievalNodeTest {
//
//	private static final Logger logger = LoggerFactory.getLogger(KnowledgeRetrievalNode.class);
//
//	List<Document> documents = List.of(new Document(
//			"产品说明�?产品名称：智能机器人\n" + "产品描述：智能机器人是一个智能设备，能够自动完成各种任务。\n" + "功能：\n" + "1. 自动导航：机器人能够自动导航到指定位置。\n"
//					+ "2. 自动抓取：机器人能够自动抓取物品。\n" + "3. 自动放置：机器人能够自动放置物品。\n",
//			Map.of("type", "instruction", // 文档类型
//					"year", "2023", // 年份
//					"month", "06" // 月份
//			)),
//			new Document(
//					"产品说明�?产品名称：智能家居控制器\n" + "产品描述：智能家居控制器是一款集成化设备，可远程控制多种智能家电。\n" + "功能：\n"
//							+ "1. 远程控制：通过手机APP远程控制家电开关和调节。\n" + "2. 定时任务：设置家电定时开启或关闭。\n" + "3. 场景模式：支持多种场景模式一键切换。\n"
//							+ "4. 能耗统计：实时监控并统计家电能耗数据。\n",
//
//					Map.of("type", "instruction", // 文档类型
//							"year", "2024", // 年份
//							"month", "02" // 月份
//
//					)));
//
//	String apiKey = System.getenv().getOrDefault("AI_DASHSCOPE_API_KEY", "test-api-key");
//
//	DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
//
//	;
//
//	EmbeddingModel embeddingModel = new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED,
//			DashScopeEmbeddingOptions.builder().withModel("text-embedding-v2").build());
//
//	SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
//
//	RerankModel rerankModel = new DashScopeRerankModel(dashScopeApi);
//
//	Filter.Expression filterExpression = new FilterExpressionBuilder().eq("type", "instruction").build();
//
//	DashScopeRerankOptions rerankOptions = new DashScopeRerankOptions();
//
//	Map<String, Object> initStateMap() {
//		Map<String, Object> modifiableMap = new HashMap<>();
//		modifiableMap.put("user_prompt", "你将作为一名机器人产品的专家，对于用户的使用需求作出解�?);
//		modifiableMap.put("top_k", 5);
//		modifiableMap.put("similarity_threshold", 0.1);
//		modifiableMap.put("filter_expression", filterExpression);
//		modifiableMap.put("enable_ranker", true);
//		modifiableMap.put("rerank_model", rerankModel);
//		modifiableMap.put("rerank_options", rerankOptions);
//		modifiableMap.put("vector_store", simpleVectorStore);
//		return modifiableMap;
//	}
//
//	KnowledgeRetrievalNode.Builder initNodeBuilder() {
//		return KnowledgeRetrievalNode.builder()
//			.userPromptKey("user_prompt")
//			.topKKey("top_k")
//			.similarityThresholdKey("similarity_threshold")
//			.filterExpressionKey("filter_expression")
//			.enableRankerKey("enable_ranker")
//			.rerankModelKey("rerank_model")
//			.rerankOptionsKey("rerank_options")
//			.vectorStoreKey("vector_store");
//	}
//
//	@Test
//	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
//	void testValueFirst() throws Exception {
//		simpleVectorStore.add(documents);
//
//		KnowledgeRetrievalNode node = initNodeBuilder().topK(5).isKeyFirst(false).build();
//		Map<String, Object> stateMap = initStateMap();
//		// 修改topk
//		stateMap.put("top_k", 1);
//		Map<String, Object> newState = node.apply(new OverAllState(stateMap));
//		logger.info("文档检索结果加入prompt为{}", newState.get("user_prompt"));
//		assertEquals(2, node.documents.size());
//	}
//
//	@Test
//	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
//	void testTopK() throws Exception {
//
//		simpleVectorStore.add(documents);
//
//		KnowledgeRetrievalNode node = initNodeBuilder().build();
//		Map<String, Object> stateMap = initStateMap();
//		// 原本topk�?
//		Map<String, Object> newState = node.apply(new OverAllState(stateMap));
//		logger.info("文档检索结果加入prompt为{}", newState.get("user_prompt"));
//		assertEquals(2, node.documents.size());
//		// 修改topk
//		stateMap.put("top_k", 1);
//		newState = node.apply(new OverAllState(stateMap));
//		logger.info("文档检索结果加入prompt为{}", newState.get("user_prompt"));
//		assertEquals(1, node.documents.size());
//
//	}
//
//	@Test
//	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
//	void testSimilarityThreshold() throws Exception {
//
//		simpleVectorStore.add(documents);
//
//		KnowledgeRetrievalNode node = initNodeBuilder().build();
//		Map<String, Object> stateMap = initStateMap();
//		// 原本similarity_threshold�?�?
//		Map<String, Object> newState = node.apply(new OverAllState(stateMap));
//		logger.info("文档检索结果加入prompt为{}", newState.get("user_prompt"));
//		assertEquals(2, node.documents.size());
//		// 修改�?.5
//		stateMap.put("similarity_threshold", 0.5);
//		newState = node.apply(new OverAllState(stateMap));
//		logger.info("文档检索结果加入prompt为{}", newState.get("user_prompt"));
//		assertEquals(1, node.documents.size());
//
//	}
//
//	@Test
//	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
//	void testFilterExpression() throws Exception {
//
//		simpleVectorStore.add(documents);
//
//		KnowledgeRetrievalNode node = initNodeBuilder().build();
//		Map<String, Object> stateMap = initStateMap();
//		// 原本筛选条件是eq("type", "instruction")
//		Map<String, Object> newState = node.apply(new OverAllState(stateMap));
//		logger.info("文档检索结果加入prompt为{}", newState.get("user_prompt"));
//		assertEquals(2, node.documents.size());
//		// 现修改为eq("type", "book")
//		stateMap.put("filter_expression", new FilterExpressionBuilder().eq("type", "book").build());
//		newState = node.apply(new OverAllState(stateMap));
//		logger.info("文档检索结果加入prompt为{}", newState.get("user_prompt"));
//		assertEquals(0, node.documents.size());
//
//	}
//
//	@Test
//	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
//	void testRerank() throws Exception {
//
//		simpleVectorStore.add(documents);
//
//		KnowledgeRetrievalNode node = initNodeBuilder().build();
//		Map<String, Object> stateMap = initStateMap();
//
//		// rerankOptions原本默认topN�?
//		Map<String, Object> newState = node.apply(new OverAllState(stateMap));
//		logger.info("文档检索结果加入prompt为{}", newState.get("user_prompt"));
//		assertEquals(2, node.documents.size());
//		// 重设topN数量
//		rerankOptions.setTopN(1);
//		stateMap.put("rerank_options", rerankOptions);
//		newState = node.apply(new OverAllState(stateMap));
//		logger.info("文档检索结果加入prompt为{}", newState.get("user_prompt"));
//		assertEquals(1, node.documents.size());
//		// 使重排序失效
//		stateMap.put("enable_ranker", false);
//		newState = node.apply(new OverAllState(stateMap));
//		logger.info("文档检索结果加入prompt为{}", newState.get("user_prompt"));
//		assertEquals(2, node.documents.size());
//
//	}
//
//}
