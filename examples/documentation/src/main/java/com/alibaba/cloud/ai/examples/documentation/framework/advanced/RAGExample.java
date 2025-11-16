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
package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 检索增强生成（RAG）示例
 *
 * 演示如何使用RAG技术为LLM提供外部知识，包括：
 * 1. 构建知识库
 * 2. 两步RAG
 * 3. Agentic RAG
 * 4. 混合RAG
 *
 * 参考文档: advanced_doc/rag.md
 */
public class RAGExample {

	private final ChatModel chatModel;
	private final VectorStore vectorStore;

	public RAGExample(ChatModel chatModel, VectorStore vectorStore) {
		this.chatModel = chatModel;
		this.vectorStore = vectorStore;
	}

	/**
	 * Main方法：运行所有示例
	 *
	 * 注意：需要配置ChatModel和VectorStore实例才能运行
	 */
	public static void main(String[] args) {
		// 创建 DashScope API 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// 创建 ChatModel
		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// TODO: 请配置您的VectorStore实例
		// 例如：VectorStore vectorStore = new YourVectorStoreImplementation();
		VectorStore vectorStore = null; // 请替换为实际的VectorStore实例

		if (chatModel == null || vectorStore == null) {
			System.err.println("错误：请先配置ChatModel和VectorStore实例");
			System.err.println("请设置 AI_DASHSCOPE_API_KEY 环境变量，并配置VectorStore实例");
			return;
		}

		// 创建示例实例
		RAGExample example = new RAGExample(chatModel, vectorStore);

		// 运行所有示例
		example.runAllExamples();
	}

	/**
	 * 示例1：构建知识库
	 *
	 * 从文档加载、分割、嵌入并存储到向量数据库
	 */
	public void example1_buildKnowledgeBase() {
		// 1. 加载文档
		Resource resource = new FileSystemResource("path/to/document.txt");
		TextReader textReader = new TextReader(resource);
		List<Document> documents = textReader.get();

		// 2. 分割文档为块
		TokenTextSplitter splitter = new TokenTextSplitter();
		List<Document> chunks = splitter.apply(documents);

		// 3. 将块添加到向量存储
		vectorStore.add(chunks);

		// 现在可以使用向量存储进行检索
		List<Document> results = vectorStore.similaritySearch("查询文本");

		System.out.println("知识库构建完成，检索到 " + results.size() + " 个相关文档");
	}

	/**
	 * 示例2：两步RAG
	 *
	 * 检索步骤总是在生成步骤之前执行
	 */
	public void example2_twoStepRAG() {
		// 两步RAG：检索 -> 生成
		String userQuestion = "Spring AI Alibaba支持哪些模型？";

		// Step 1: Retrieve relevant documents
		List<Document> relevantDocs = vectorStore.similaritySearch(userQuestion);

		// Step 2: Build context from documents
		String context = relevantDocs.stream()
				.map(Document::getText)
				.collect(Collectors.joining("\n\n"));

		// Step 3: Generate answer with context
		ChatClient chatClient = ChatClient.builder(chatModel).build();
		String answer = chatClient.prompt()
				.user(u -> u.text("基于以下上下文回答问题：\n\n上下文：\n" + context + "\n\n问题：" + userQuestion))
				.call()
				.content();

		System.out.println("答案: " + answer);

		// 检索到的文档作为上下文添加到提示中
		// ChatModel 使用增强的上下文生成答案

		System.out.println("两步RAG示例执行完成");
	}

	/**
	 * 示例3：Agentic RAG
	 *
	 * Agent决定何时以及如何检索信息
	 */
	public void example3_agenticRAG() throws Exception {
		// 创建文档检索工具
		class DocumentSearchTool {
			public Response search(Request request) {
				// 从向量存储检索相关文档
				List<Document> docs = vectorStore.similaritySearch(request.query());

				// 合并文档内容
				String combinedContent = docs.stream()
						.map(Document::getText)
						.collect(Collectors.joining("\n\n"));

				return new Response(combinedContent);
			}

			public record Request(String query) { }

			public record Response(String content) { }
		}

		DocumentSearchTool searchTool = new DocumentSearchTool();

		// 创建工具回调
		ToolCallback searchCallback = FunctionToolCallback.builder("search_documents",
						(Function<DocumentSearchTool.Request, DocumentSearchTool.Response>)
								request -> searchTool.search(request))
				.description("搜索文档以查找相关信息")
				.inputType(DocumentSearchTool.Request.class)
				.build();

		// 创建带有检索工具的Agent
		ReactAgent ragAgent = ReactAgent.builder()
				.name("rag_agent")
				.model(chatModel)
				.instruction("你是一个智能助手。当需要查找信息时，使用search_documents工具。" +
						"基于检索到的信息回答用户的问题，并引用相关片段。")
				.tools(searchCallback)
				.build();

		// Agent会自动决定何时调用检索工具
		ragAgent.invoke("Spring AI Alibaba支持哪些向量数据库？");

		System.out.println("Agentic RAG示例执行完成");
	}

	/**
	 * 示例4：多源RAG
	 *
	 * Agent可以从多个来源检索信息
	 */
	public void example4_multiSourceRAG() throws Exception {
		// 创建多个检索工具
		class WebSearchTool {
			public Response search(Request request) {
				return new Response("从网络搜索到的信息: " + request.query());
			}

			public record Request(String query) { }

			public record Response(String content) { }
		}

		class DatabaseQueryTool {
			public Response query(Request request) {
				return new Response("从数据库查询到的信息: " + request.query());
			}

			public record Request(String query) { }

			public record Response(String content) { }
		}

		class DocumentSearchTool {
			public Response search(Request request) {
				List<Document> docs = vectorStore.similaritySearch(request.query());
				String content = docs.stream()
						.map(Document::getText)
						.collect(Collectors.joining("\n\n"));
				return new Response(content);
			}

			public record Request(String query) { }

			public record Response(String content) { }
		}

		WebSearchTool webSearchTool = new WebSearchTool();
		DatabaseQueryTool dbQueryTool = new DatabaseQueryTool();
		DocumentSearchTool docSearchTool = new DocumentSearchTool();

		ToolCallback webSearchCallback = FunctionToolCallback.builder("web_search",
						(Function<WebSearchTool.Request, WebSearchTool.Response>)
								req -> webSearchTool.search(req))
				.description("搜索互联网以获取最新信息")
				.inputType(WebSearchTool.Request.class)
				.build();

		ToolCallback databaseQueryCallback = FunctionToolCallback.builder("database_query",
						(Function<DatabaseQueryTool.Request, DatabaseQueryTool.Response>)
								req -> dbQueryTool.query(req))
				.description("查询内部数据库")
				.inputType(DatabaseQueryTool.Request.class)
				.build();

		ToolCallback documentSearchCallback = FunctionToolCallback.builder("document_search",
						(Function<DocumentSearchTool.Request, DocumentSearchTool.Response>)
								req -> docSearchTool.search(req))
				.description("搜索文档库")
				.inputType(DocumentSearchTool.Request.class)
				.build();

		// Agent可以访问多个检索源
		ReactAgent multiSourceAgent = ReactAgent.builder()
				.name("multi_source_rag_agent")
				.model(chatModel)
				.instruction("你可以访问多个信息源：" +
						"1. web_search - 用于最新的互联网信息\n" +
						"2. database_query - 用于内部数据\n" +
						"3. document_search - 用于文档库\n" +
						"根据问题选择最合适的工具。")
				.tools(webSearchCallback, databaseQueryCallback, documentSearchCallback)
				.build();

		multiSourceAgent.invoke("比较我们的产品文档中的功能和最新的市场趋势");

		System.out.println("多工具Agentic RAG示例执行完成");
	}

	/**
	 * 示例5：混合RAG
	 *
	 * 结合查询增强、检索验证和答案验证
	 */
	public void example5_hybridRAG() {
		class HybridRAGSystem {
			private final ChatModel chatModel;
			private final VectorStore vectorStore;

			public HybridRAGSystem(ChatModel chatModel, VectorStore vectorStore) {
				this.chatModel = chatModel;
				this.vectorStore = vectorStore;
			}

			public String answer(String userQuestion) {
				// 1. 查询增强
				String enhancedQuery = enhanceQuery(userQuestion);

				int maxAttempts = 3;
				for (int attempt = 0; attempt < maxAttempts; attempt++) {
					// 2. 检索文档
					List<Document> docs = vectorStore.similaritySearch(enhancedQuery);

					// 3. 检索验证
					if (!isRetrievalSufficient(docs)) {
						enhancedQuery = refineQuery(enhancedQuery, docs);
						continue;
					}

					// 4. 生成答案
					String answer = generateAnswer(userQuestion, docs);

					// 5. 答案验证
					ValidationResult validation = validateAnswer(answer, docs);
					if (validation.isValid()) {
						return answer;
					}

					// 6. 根据验证结果决定下一步
					if (validation.shouldRetry()) {
						enhancedQuery = refineBasedOnValidation(enhancedQuery, validation);
					}
					else {
						return answer; // 返回当前最佳答案
					}
				}

				return "无法生成满意的答案";
			}

			private String enhanceQuery(String query) {
				return query; // 实现查询增强逻辑
			}

			private boolean isRetrievalSufficient(List<Document> docs) {
				return !docs.isEmpty() && calculateRelevanceScore(docs) > 0.7;
			}

			private double calculateRelevanceScore(List<Document> docs) {
				return 0.8; // 实现相关性评分逻辑
			}

			private String refineQuery(String query, List<Document> docs) {
				return query; // 实现查询优化逻辑
			}

			private String generateAnswer(String question, List<Document> docs) {
				String context = docs.stream()
						.map(Document::getText)
						.collect(Collectors.joining("\n\n"));

				ChatClient client = ChatClient.builder(chatModel).build();
				return client.prompt()
						.system("基于以下上下文回答问题：\n" + context)
						.user(question)
						.call()
						.content();
			}

			private ValidationResult validateAnswer(String answer, List<Document> docs) {
				// 实现答案验证逻辑
				return new ValidationResult(true, false);
			}

			private String refineBasedOnValidation(String query, ValidationResult validation) {
				return query; // 基于验证结果优化查询
			}

			class ValidationResult {
				private boolean valid;
				private boolean shouldRetry;

				public ValidationResult(boolean valid, boolean shouldRetry) {
					this.valid = valid;
					this.shouldRetry = shouldRetry;
				}

				public boolean isValid() {
					return valid;
				}

				public boolean shouldRetry() {
					return shouldRetry;
				}
			}
		}

		HybridRAGSystem hybridRAG = new HybridRAGSystem(chatModel, vectorStore);
		String answer = hybridRAG.answer("解释一下Spring AI Alibaba的核心功能");

		System.out.println("混合RAG答案: " + answer);
		System.out.println("混合RAG示例执行完成");
	}

	/**
	 * 运行所有示例
	 */
	public void runAllExamples() {
		System.out.println("=== 检索增强生成（RAG）示例 ===\n");

		try {
			System.out.println("示例1: 构建知识库");
			// example1_buildKnowledgeBase(); // 需要实际文件路径
			System.out.println();

			System.out.println("示例2: 两步RAG");
			example2_twoStepRAG();
			System.out.println();

			System.out.println("示例3: Agentic RAG");
			example3_agenticRAG();
			System.out.println();

			System.out.println("示例4: 多数据源RAG");
			example4_multiSourceRAG();
			System.out.println();

			System.out.println("示例5: 混合RAG");
			example5_hybridRAG();
			System.out.println();

		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

