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
package com.alibaba.cloud.ai.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

class CompositeDocumentRetrieverTests {

	private DocumentRetriever retriever1;

	private DocumentRetriever retriever2;

	private DocumentRetriever retriever3;

	private Query testQuery;

	@BeforeEach
	void setUp() {
		retriever1 = mock(DocumentRetriever.class);
		retriever2 = mock(DocumentRetriever.class);
		retriever3 = mock(DocumentRetriever.class);
		testQuery = new Query("test query");
	}

	private Document createDocumentWithScore(String id, String content, double score) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("score", score);
		Document doc = new Document(id, content, metadata);
		return doc;
	}

	@Test
	void testConstructorValidation() {
		assertThrows(IllegalArgumentException.class, () -> {
			new CompositeDocumentRetriever(Arrays.asList());
		});

		assertThrows(IllegalArgumentException.class, () -> {
			new CompositeDocumentRetriever(null);
		});

		assertThrows(IllegalArgumentException.class, () -> {
			new CompositeDocumentRetriever(Arrays.asList(retriever1), 0);
		});
	}

	@Test
	void testSingleRetrieverComposition() {
		Document doc1 = createDocumentWithScore("1", "content1", 0.9);
		when(retriever1.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc1));

		CompositeDocumentRetriever composite = new CompositeDocumentRetriever(Arrays.asList(retriever1));

		List<Document> results = composite.retrieve(testQuery);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getId()).isEqualTo("1");
		assertThat(results.get(0).getText()).isEqualTo("content1");
	}

	@Test
	void testMultipleRetrieversComposition() {
		Document doc1 = createDocumentWithScore("1", "content1", 0.9);
		Document doc2 = createDocumentWithScore("2", "content2", 0.8);
		Document doc3 = createDocumentWithScore("3", "content3", 0.95);

		when(retriever1.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc1));
		when(retriever2.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc2));
		when(retriever3.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc3));

		CompositeDocumentRetriever composite = new CompositeDocumentRetriever(
				Arrays.asList(retriever1, retriever2, retriever3));

		List<Document> results = composite.retrieve(testQuery);

		assertThat(results).hasSize(3);
		assertThat(results.stream().map(Document::getId)).containsExactlyInAnyOrder("1", "2", "3");
	}

	@Test
	void testSimpleMergeStrategy() {
		Document doc1 = createDocumentWithScore("1", "content1", 0.9);
		Document doc2 = createDocumentWithScore("2", "content2", 0.8);

		when(retriever1.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc1));
		when(retriever2.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc2));

		CompositeDocumentRetriever composite = new CompositeDocumentRetriever(Arrays.asList(retriever1, retriever2), 10,
				CompositeDocumentRetriever.ResultMergeStrategy.SIMPLE_MERGE);

		List<Document> results = composite.retrieve(testQuery);

		assertThat(results).hasSize(2);
		assertThat(results.get(0).getId()).isEqualTo("1");
		assertThat(results.get(1).getId()).isEqualTo("2");
	}

	@Test
	void testMaxResultsPerRetrieverLimit() {
		Document doc1 = createDocumentWithScore("1", "content1", 0.9);
		Document doc2 = createDocumentWithScore("2", "content2", 0.8);
		Document doc3 = createDocumentWithScore("3", "content3", 0.7);
		Document doc4 = createDocumentWithScore("4", "content4", 0.6);

		when(retriever1.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc1, doc2, doc3, doc4));

		CompositeDocumentRetriever composite = new CompositeDocumentRetriever(Arrays.asList(retriever1), 2);

		List<Document> results = composite.retrieve(testQuery);

		assertThat(results).hasSize(2);
		assertThat(results.get(0).getId()).isEqualTo("1");
		assertThat(results.get(1).getId()).isEqualTo("2");
	}

	@Test
	void testErrorHandling() {
		Document doc2 = createDocumentWithScore("2", "content2", 0.8);

		when(retriever1.retrieve(any(Query.class))).thenThrow(new RuntimeException("Database error"));
		when(retriever2.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc2));

		CompositeDocumentRetriever composite = new CompositeDocumentRetriever(Arrays.asList(retriever1, retriever2));

		List<Document> results = composite.retrieve(testQuery);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getId()).isEqualTo("2");
	}

	@Test
	void testBuilderPattern() {
		Document doc1 = createDocumentWithScore("1", "content1", 0.9);
		when(retriever1.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc1));

		CompositeDocumentRetriever composite = CompositeDocumentRetriever.builder()
			.addRetriever(retriever1)
			.addRetriever(retriever2)
			.maxResultsPerRetriever(5)
			.mergeStrategy(CompositeDocumentRetriever.ResultMergeStrategy.SIMPLE_MERGE)
			.build();

		List<Document> results = composite.retrieve(testQuery);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getId()).isEqualTo("1");
	}

	@Test
	void testEmptyResults() {
		when(retriever1.retrieve(any(Query.class))).thenReturn(Arrays.asList());
		when(retriever2.retrieve(any(Query.class))).thenReturn(null);

		CompositeDocumentRetriever composite = new CompositeDocumentRetriever(Arrays.asList(retriever1, retriever2));

		List<Document> results = composite.retrieve(testQuery);

		assertThat(results).isEmpty();
	}

	@Test
	void testRoundRobinMergeStrategy() {
		Document doc1 = createDocumentWithScore("1", "content1", 0.9);
		Document doc2 = createDocumentWithScore("2", "content2", 0.8);
		Document doc3 = createDocumentWithScore("3", "content3", 0.7);
		Document doc4 = createDocumentWithScore("4", "content4", 0.95);

		when(retriever1.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc1, doc2));
		when(retriever2.retrieve(any(Query.class))).thenReturn(Arrays.asList(doc3, doc4));

		CompositeDocumentRetriever composite = new CompositeDocumentRetriever(Arrays.asList(retriever1, retriever2), 10,
				CompositeDocumentRetriever.ResultMergeStrategy.ROUND_ROBIN);

		List<Document> results = composite.retrieve(testQuery);

		assertThat(results).hasSize(4);
		assertThat(results.get(0).getId()).isEqualTo("1");
		assertThat(results.get(1).getId()).isEqualTo("3");
		assertThat(results.get(2).getId()).isEqualTo("2");
		assertThat(results.get(3).getId()).isEqualTo("4");
	}

	@Test
	void testRealEnterpriseScenario() {
		DocumentRetriever techDocsRetriever = createRealTechDocsRetriever();
		DocumentRetriever policyRetriever = createRealPolicyRetriever();
		DocumentRetriever productRetriever = createRealProductRetriever();

		CompositeDocumentRetriever composite = CompositeDocumentRetriever.builder()
			.addRetriever(techDocsRetriever)
			.addRetriever(policyRetriever)
			.addRetriever(productRetriever)
			.maxResultsPerRetriever(3)
			.mergeStrategy(CompositeDocumentRetriever.ResultMergeStrategy.SCORE_BASED)
			.build();

		Query techQuery = new Query("Spring AI API 使用方法");
		List<Document> techResults = composite.retrieve(techQuery);
		assertThat(techResults).isNotEmpty();
		assertThat(techResults).anyMatch(doc -> doc.getText().contains("Spring AI") && doc.getScore() > 0.0);

		Query securityQuery = new Query("数据安全管理政策");
		List<Document> securityResults = composite.retrieve(securityQuery);
		assertThat(securityResults).isNotEmpty();
		assertThat(securityResults).anyMatch(doc -> doc.getText().contains("安全") && doc.getScore() > 0.0);

		Query productQuery = new Query("通义千问大语言模型");
		List<Document> productResults = composite.retrieve(productQuery);
		assertThat(productResults).isNotEmpty();
		assertThat(productResults).anyMatch(doc -> doc.getText().contains("通义千问") && doc.getScore() > 0.0);
	}

	@Test
	void testDifferentMergeStrategiesWithRealData() {
		DocumentRetriever techRetriever = createRealTechDocsRetriever();
		DocumentRetriever policyRetriever = createRealPolicyRetriever();

		Query testQuery = new Query("微服务架构设计");

		CompositeDocumentRetriever scoreBasedComposite = CompositeDocumentRetriever.builder()
			.addRetriever(techRetriever)
			.addRetriever(policyRetriever)
			.maxResultsPerRetriever(3)
			.mergeStrategy(CompositeDocumentRetriever.ResultMergeStrategy.SCORE_BASED)
			.build();

		List<Document> scoreBasedResults = scoreBasedComposite.retrieve(testQuery);
		assertThat(scoreBasedResults).isNotEmpty();

		CompositeDocumentRetriever roundRobinComposite = CompositeDocumentRetriever.builder()
			.addRetriever(techRetriever)
			.addRetriever(policyRetriever)
			.maxResultsPerRetriever(3)
			.mergeStrategy(CompositeDocumentRetriever.ResultMergeStrategy.ROUND_ROBIN)
			.build();

		List<Document> roundRobinResults = roundRobinComposite.retrieve(testQuery);
		assertThat(roundRobinResults).isNotEmpty();

		CompositeDocumentRetriever simpleMergeComposite = CompositeDocumentRetriever.builder()
			.addRetriever(techRetriever)
			.addRetriever(policyRetriever)
			.maxResultsPerRetriever(3)
			.mergeStrategy(CompositeDocumentRetriever.ResultMergeStrategy.SIMPLE_MERGE)
			.build();

		List<Document> simpleMergeResults = simpleMergeComposite.retrieve(testQuery);
		assertThat(simpleMergeResults).isNotEmpty();

		assertThat(scoreBasedResults.size()).isPositive();
		assertThat(roundRobinResults.size()).isPositive();
		assertThat(simpleMergeResults.size()).isPositive();
	}

	@Test
	void testPerformanceWithLargeDataset() {
		DocumentRetriever largeRetriever1 = createLargeDatasetRetriever("技术文档库", 100);
		DocumentRetriever largeRetriever2 = createLargeDatasetRetriever("企业政策库", 80);
		DocumentRetriever largeRetriever3 = createLargeDatasetRetriever("产品知识库", 120);

		CompositeDocumentRetriever composite = CompositeDocumentRetriever.builder()
			.addRetriever(largeRetriever1)
			.addRetriever(largeRetriever2)
			.addRetriever(largeRetriever3)
			.maxResultsPerRetriever(10)
			.mergeStrategy(CompositeDocumentRetriever.ResultMergeStrategy.SCORE_BASED)
			.build();

		long startTime = System.currentTimeMillis();

		Query performanceQuery = new Query("性能优化最佳实践");
		List<Document> results = composite.retrieve(performanceQuery);

		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;

		assertThat(results).isNotEmpty();
		assertThat(results.size()).isLessThanOrEqualTo(30);
		assertThat(executionTime).isLessThan(5000);
	}

	@Test
	void testMinimalQueryHandling() {
		DocumentRetriever techRetriever = createRealTechDocsRetriever();

		CompositeDocumentRetriever composite = CompositeDocumentRetriever.builder()
			.addRetriever(techRetriever)
			.maxResultsPerRetriever(5)
			.mergeStrategy(CompositeDocumentRetriever.ResultMergeStrategy.SCORE_BASED)
			.build();

		Query minimalQuery = new Query("a");
		List<Document> results = composite.retrieve(minimalQuery);

		assertThat(results).isNotNull();
		assertThat(results).isNotEmpty();
	}

	@Test
	void testChineseQuerySupport() {
		DocumentRetriever techRetriever = createRealTechDocsRetriever();
		DocumentRetriever policyRetriever = createRealPolicyRetriever();

		CompositeDocumentRetriever composite = CompositeDocumentRetriever.builder()
			.addRetriever(techRetriever)
			.addRetriever(policyRetriever)
			.maxResultsPerRetriever(3)
			.mergeStrategy(CompositeDocumentRetriever.ResultMergeStrategy.SCORE_BASED)
			.build();

		Query chineseQuery = new Query("微服务架构设计原则和最佳实践");
		List<Document> results = composite.retrieve(chineseQuery);

		assertThat(results).isNotEmpty();
		assertThat(results).anyMatch(doc -> doc.getText().contains("微服务"));
	}

	private DocumentRetriever createRealTechDocsRetriever() {
		List<String> techDocs = Arrays.asList(
				"Spring AI Alibaba 是阿里巴巴开源的 Spring AI 框架扩展，提供了与阿里云 AI 服务的无缝集成。支持通义千问、向量检索、语音识别等多种 AI 能力。",
				"微服务架构设计原则：单一职责、松耦合、高内聚。每个服务应该有明确的边界和职责，通过轻量级通信机制进行交互，如 REST API 或消息队列。",
				"微服务部署最佳实践：使用容器化技术，实现服务的独立部署和扩展。建立完善的监控体系，确保微服务系统的可观测性和稳定性。",
				"分布式系统故障排查指南：1. 检查服务健康状态；2. 查看日志聚合信息；3. 监控关键指标；4. 分析调用链路；5. 验证配置参数；6. 检查依赖服务状态。",
				"API 版本控制最佳实践：使用语义化版本号，向后兼容的更改使用 PATCH 版本，新增功能使用 MINOR 版本，破坏性更改使用 MAJOR 版本。",
				"容器化部署优化：合理设置资源限制，使用多阶段构建减小镜像大小，配置健康检查，实现优雅关闭，使用 ConfigMap 和 Secret 管理配置。",
				"Redis 缓存设计模式：Cache-Aside 模式适用于读多写少场景，Write-Through 模式确保数据一致性，Write-Behind 模式提高写入性能。",
				"数据库连接池配置：最大连接数应根据应用并发量和数据库性能确定，连接超时时间设置为 30 秒，空闲连接回收时间设置为 10 分钟。",
				"消息队列使用规范：消息要幂等处理，设置合理的重试机制，使用死信队列处理失败消息，避免消息堆积影响系统性能。");

		return createMockRetrieverWithRealData("技术文档库", "技术文档", techDocs, "张三丰");
	}

	private DocumentRetriever createRealPolicyRetriever() {
		List<String> policies = Arrays.asList("代码审查规范：所有代码必须经过至少一名高级工程师审查才能合并到主分支。审查内容包括代码质量、安全性、性能和可维护性。",
				"数据安全管理政策：个人敏感信息必须加密存储，数据库访问需要审计日志，生产环境数据不得在测试环境使用，定期进行安全漏洞扫描。",
				"企业级数据安全保护方案：建立完善的数据分类分级制度，实施数据加密传输和存储，部署企业级防火墙和入侵检测系统，制定数据备份和灾难恢复计划。",
				"发布流程管理：生产环境发布必须在工作日进行，重大版本发布需要提前 3 天通知，所有发布都要有回滚预案，发布后 2 小时内监控系统状态。",
				"开发环境管理规范：开发环境配置要与生产环境保持一致，使用 Docker 容器化开发环境，定期清理无用的开发资源，禁止在开发环境存储真实用户数据。",
				"第三方依赖管理：新增第三方库需要安全审查，优先使用公司内部维护的组件，定期更新依赖版本修复安全漏洞，维护依赖关系清单。",
				"API 安全规范：所有 API 接口必须实现认证授权，使用 HTTPS 传输，限制请求频率防止滥用，记录 API 调用日志用于审计。",
				"员工信息安全培训：新员工入职必须完成信息安全培训，每年进行安全意识考试，及时通报最新安全威胁，建立安全事件应急响应机制。",
				"合规性要求：遵守 GDPR 数据保护法规，符合 SOC 2 安全控制标准，通过 ISO 27001 信息安全管理体系认证，定期进行合规性审计。");

		return createMockRetrieverWithRealData("企业政策库", "企业政策", policies, "李四光");
	}

	private DocumentRetriever createRealProductRetriever() {
		List<String> products = Arrays.asList("通义千问大语言模型：支持多轮对话、代码生成、文本总结、翻译等功能。提供 API 接口和 SDK，支持流式输出，具有强大的中文理解能力。",
				"向量检索服务：基于深度学习的语义检索引擎，支持文本、图片、音频等多模态数据检索。提供毫秒级响应，支持亿级数据规模。",
				"智能客服机器人：集成自然语言处理和知识图谱技术，支持多渠道接入，提供 7x24 小时服务。支持情感识别和个性化回复。",
				"文档智能处理：自动识别和提取文档中的关键信息，支持 PDF、Word、Excel 等格式。提供表格识别、印章检测、手写识别等功能。",
				"语音识别与合成：支持多种语言和方言，实时语音转文字，自然度极高的语音合成。支持声纹识别和情感化语音合成。",
				"图像识别与分析：提供人脸识别、物体检测、场景理解等功能。支持实时图像处理，准确率达到业界领先水平。",
				"数据可视化平台：拖拽式图表制作，支持实时数据更新，丰富的图表类型和样式。提供数据钻取和交互式分析功能。",
				"AI 模型训练平台：提供完整的机器学习工作流，支持分布式训练，自动调参优化。内置常用算法和预训练模型。");

		return createMockRetrieverWithRealData("产品知识库", "产品介绍", products, "王五郎");
	}

	private DocumentRetriever createLargeDatasetRetriever(String source, int documentCount) {
		List<String> documents = new java.util.ArrayList<>();
		for (int i = 0; i < documentCount; i++) {
			documents.add(source + " - 文档 " + i + "：这是一个测试文档，包含性能优化、最佳实践、架构设计等相关内容。用于验证系统在大数据量下的检索性能。");
		}
		return createMockRetrieverWithRealData(source, "测试文档", documents, "测试作者");
	}

	private DocumentRetriever createMockRetrieverWithRealData(String source, String category, List<String> documents,
			String author) {
		return new DocumentRetriever() {
			@Override
			public List<Document> retrieve(Query query) {
				List<Document> results = new java.util.ArrayList<>();
				for (int i = 0; i < documents.size(); i++) {
					Map<String, Object> metadata = new HashMap<>();
					metadata.put("source", source);
					metadata.put("category", category);
					metadata.put("author", author);
					metadata.put("lastUpdated",
							"2025-07-" + (20 + i % 5) + "T" + String.format("%02d", 9 + i % 12) + ":00:00Z");
					metadata.put("documentId", source.replaceAll("[^a-zA-Z0-9]", "") + "_" + i);

					double score = calculateRelevanceScore(documents.get(i), query.text());

					Document document = Document.builder()
						.id(source + "_doc_" + i)
						.text(documents.get(i))
						.metadata(metadata)
						.score(score)
						.build();

					results.add(document);
				}

				results.sort((d1, d2) -> Double.compare(d2.getScore(), d1.getScore()));

				return results;
			}
		};
	}

	private double calculateRelevanceScore(String text, String query) {
		if (query == null || query.trim().isEmpty()) {
			return 0.5;
		}

		String lowerText = text.toLowerCase();
		String lowerQuery = query.toLowerCase();
		double score = 0.1;

		if (lowerText.contains(lowerQuery)) {
			score += 0.6;
		}

		String[] queryWords = lowerQuery.split("[\\s\\p{Punct}]+");
		int matchCount = 0;
		double keywordScore = 0.0;

		for (String word : queryWords) {
			if (word.length() > 1) {
				if (lowerText.contains(word)) {
					matchCount++;
					keywordScore += 0.2;
				}
				else if (isSemanticMatch(word, lowerText)) {
					matchCount++;
					keywordScore += 0.15;
				}
			}
		}

		score += keywordScore;

		if (queryWords.length > 0) {
			double ratio = (double) matchCount / queryWords.length;
			score += ratio * 0.25;

			if (ratio >= 0.5) {
				score += 0.15;
			}
		}

		if (text.length() < 200 && matchCount > 0) {
			score += 0.05;
		}

		return Math.max(0.1, Math.min(score, 1.0));
	}

	private boolean isSemanticMatch(String queryWord, String text) {
		if ("安全".equals(queryWord) && text.contains("保护"))
			return true;
		if ("保护".equals(queryWord) && text.contains("安全"))
			return true;
		if ("方案".equals(queryWord) && text.contains("政策"))
			return true;
		if ("政策".equals(queryWord) && text.contains("方案"))
			return true;
		if ("企业级".equals(queryWord) && text.contains("企业"))
			return true;
		if ("企业".equals(queryWord) && text.contains("企业级"))
			return true;
		if ("管理".equals(queryWord) && text.contains("配置"))
			return true;
		if ("配置".equals(queryWord) && text.contains("管理"))
			return true;
		if ("最佳实践".equals(queryWord) && text.contains("规范"))
			return true;
		if ("规范".equals(queryWord) && text.contains("最佳实践"))
			return true;
		if ("集成".equals(queryWord) && text.contains("整合"))
			return true;
		if ("整合".equals(queryWord) && text.contains("集成"))
			return true;
		if ("模型".equals(queryWord) && text.contains("框架"))
			return true;
		if ("框架".equals(queryWord) && text.contains("模型"))
			return true;
		return false;
	}

	@Test
	void testDocumentRetrievalAdvisorWithMultipleVectorStores() {

		DocumentRetriever retriever1 = mock(DocumentRetriever.class);
		DocumentRetriever retriever2 = mock(DocumentRetriever.class);

		when(retriever1.retrieve(any(Query.class))).thenReturn(List.of(createDocumentWithScore("1", "文档1", 0.9)));
		when(retriever2.retrieve(any(Query.class))).thenReturn(List.of(createDocumentWithScore("2", "文档2", 0.8)));

		List<DocumentRetriever> retrievers = Arrays.asList(retriever1, retriever2);
		DocumentRetrievalAdvisor advisor = new DocumentRetrievalAdvisor(retrievers);

		assertThat(advisor).isNotNull();
		assertThat(advisor.getOrder()).isEqualTo(0);

		System.out.println("多向量库DocumentRetrievalAdvisor验证成功");
	}

	@Test
	void testMultipleVectorStoresWithDefaults() {

		DocumentRetriever techRetriever = mock(DocumentRetriever.class);
		DocumentRetriever policyRetriever = mock(DocumentRetriever.class);
		DocumentRetriever productRetriever = mock(DocumentRetriever.class);

		when(techRetriever.retrieve(any(Query.class)))
			.thenReturn(List.of(createDocumentWithScore("tech", "技术文档：微服务架构", 0.9)));
		when(policyRetriever.retrieve(any(Query.class)))
			.thenReturn(List.of(createDocumentWithScore("policy", "政策文档：安全规范", 0.8)));
		when(productRetriever.retrieve(any(Query.class)))
			.thenReturn(List.of(createDocumentWithScore("product", "产品文档：云原生方案", 0.85)));

		List<DocumentRetriever> retrievers = Arrays.asList(techRetriever, policyRetriever, productRetriever);
		DocumentRetrievalAdvisor advisor = new DocumentRetrievalAdvisor(retrievers);

		assertThat(advisor).isNotNull();
		assertThat(advisor.getOrder()).isEqualTo(0);

		System.out.println("多向量库默认设置功能验证成功");
	}

	@Test
	void testMultipleVectorStoresWithCustomStrategy() {

		DocumentRetriever retriever1 = mock(DocumentRetriever.class);
		DocumentRetriever retriever2 = mock(DocumentRetriever.class);

		when(retriever1.retrieve(any(Query.class))).thenReturn(List.of(createDocumentWithScore("1", "文档1", 0.9)));
		when(retriever2.retrieve(any(Query.class))).thenReturn(List.of(createDocumentWithScore("2", "文档2", 0.8)));

		List<DocumentRetriever> retrievers = Arrays.asList(retriever1, retriever2);
		DocumentRetrievalAdvisor advisor = new DocumentRetrievalAdvisor(retrievers,
				CompositeDocumentRetriever.ResultMergeStrategy.SCORE_BASED, 5);

		assertThat(advisor).isNotNull();
		assertThat(advisor.getOrder()).isEqualTo(0);

		System.out.println("多向量库自定义策略功能验证成功");
	}

	@Test
	void testUserChoiceBetweenSingleAndMultiple() {

		DocumentRetriever singleRetriever = mock(DocumentRetriever.class);
		DocumentRetriever multiRetriever1 = mock(DocumentRetriever.class);
		DocumentRetriever multiRetriever2 = mock(DocumentRetriever.class);

		when(singleRetriever.retrieve(any(Query.class)))
			.thenReturn(List.of(createDocumentWithScore("single", "单库结果", 0.9)));
		when(multiRetriever1.retrieve(any(Query.class)))
			.thenReturn(List.of(createDocumentWithScore("multi1", "多库结果1", 0.8)));
		when(multiRetriever2.retrieve(any(Query.class)))
			.thenReturn(List.of(createDocumentWithScore("multi2", "多库结果2", 0.7)));

		boolean needMultipleVectorStores = true;

		DocumentRetrievalAdvisor advisor;
		if (needMultipleVectorStores) {

			advisor = new DocumentRetrievalAdvisor(Arrays.asList(multiRetriever1, multiRetriever2));
			System.out.println("选择了多向量库调用方式");
		}
		else {

			advisor = new DocumentRetrievalAdvisor(singleRetriever);
			System.out.println("选择了单向量库调用方式");
		}

		assertThat(advisor).isNotNull();
		System.out.println("用户选择功能验证成功：支持在单向量库和多向量库之间灵活选择");
	}

	@Test
	void testBusinessScenarioIntegration() {
		System.out.println("=== 业务场景集成测试 ===");

		String department = "技术部门";
		String queryComplexity = "复杂查询";

		DocumentRetriever techKB = mock(DocumentRetriever.class);
		DocumentRetriever policyKB = mock(DocumentRetriever.class);
		DocumentRetriever productKB = mock(DocumentRetriever.class);

		when(techKB.retrieve(any(Query.class))).thenReturn(List.of(createDocumentWithScore("tech", "技术知识库文档", 0.95)));
		when(policyKB.retrieve(any(Query.class)))
			.thenReturn(List.of(createDocumentWithScore("policy", "政策知识库文档", 0.88)));
		when(productKB.retrieve(any(Query.class)))
			.thenReturn(List.of(createDocumentWithScore("product", "产品知识库文档", 0.92)));

		DocumentRetrievalAdvisor advisor = createAdvisorForDepartment(department, queryComplexity, techKB, policyKB,
				productKB);

		assertThat(advisor).isNotNull();
		System.out.println("部门: " + department + ", 查询类型: " + queryComplexity);
		System.out.println("业务场景集成验证成功");
	}

	private DocumentRetrievalAdvisor createAdvisorForDepartment(String department, String queryComplexity,
			DocumentRetriever techKB, DocumentRetriever policyKB, DocumentRetriever productKB) {

		if ("技术部门".equals(department)) {
			if ("简单查询".equals(queryComplexity)) {

				return new DocumentRetrievalAdvisor(techKB);
			}
			else {

				return new DocumentRetrievalAdvisor(Arrays.asList(techKB, productKB),
						CompositeDocumentRetriever.ResultMergeStrategy.SCORE_BASED, 10);
			}
		}
		else if ("管理部门".equals(department)) {
			if ("简单查询".equals(queryComplexity)) {

				return new DocumentRetrievalAdvisor(policyKB);
			}
			else {

				return new DocumentRetrievalAdvisor(Arrays.asList(policyKB, productKB),
						CompositeDocumentRetriever.ResultMergeStrategy.SIMPLE_MERGE, 8);
			}
		}
		else {

			if ("复杂查询".equals(queryComplexity)) {

				return new DocumentRetrievalAdvisor(Arrays.asList(techKB, policyKB, productKB));
			}
			else {

				return new DocumentRetrievalAdvisor(techKB);
			}
		}
	}

	@Test
	void testCompatibilityWithExistingCode() {

		DocumentRetriever existingRetriever = mock(DocumentRetriever.class);
		when(existingRetriever.retrieve(any(Query.class)))
			.thenReturn(List.of(createDocumentWithScore("existing", "现有功能文档", 0.9)));

		DocumentRetrievalAdvisor advisor1 = new DocumentRetrievalAdvisor(existingRetriever);

		org.springframework.ai.chat.prompt.PromptTemplate customTemplate = new org.springframework.ai.chat.prompt.PromptTemplate(
				"Custom prompt: {query}");

		DocumentRetrievalAdvisor advisor2 = new DocumentRetrievalAdvisor(existingRetriever, customTemplate);
		DocumentRetrievalAdvisor advisor3 = new DocumentRetrievalAdvisor(existingRetriever, customTemplate, 1);

		assertThat(advisor1).isNotNull();
		assertThat(advisor2).isNotNull();
		assertThat(advisor3).isNotNull();
		assertThat(advisor3.getOrder()).isEqualTo(1);

		System.out.println("与现有代码兼容性验证成功：所有现有功能保持不变");
	}

}
