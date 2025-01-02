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
package com.alibaba.cloud.ai.dashscope.chat.client;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.autoconfig.dashscope.DashScopeAutoConfiguration;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionFinishReason;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.tool.DashScopeFunctionTestConfiguration;
import com.alibaba.cloud.ai.dashscope.chat.tool.MockOrderService;
import com.alibaba.cloud.ai.dashscope.chat.tool.MockWeatherService;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.rag.*;
import com.alibaba.cloud.ai.model.RerankModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * Title React agent test cases.<br/>
 * Description React agent test cases.<br/>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

@TestPropertySource("classpath:application.yml")
@SpringBootTest(classes = { DashScopeAutoConfiguration.class, DashScopeFunctionTestConfiguration.class,
		MockOrderService.class })
public class DashScopeChatClientIT {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeChatClientIT.class);

	@Autowired
	private ChatModel dashscopeChatModel;

	@Autowired
	private DashScopeApi dashscopeChatApi;

	@Autowired
	private EmbeddingModel dashscopeEmbeddingModel;

	@Autowired
	private RerankModel dashscopeRerankModel;

	@Value("classpath:/prompts/rag/system-qa.st")
	private Resource systemResource;

	@Value("classpath:/prompts/rag/system-qa-ref.st")
	private Resource systemResourceRef;

	@Value("classpath:/data/acme/bikes.json")
	private Resource bikesResource;

	@Test
	void callTest() throws IOException {
		DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeChatApi,
				DashScopeDocumentRetrieverOptions.builder().withIndexName("spring-ai知识库").build());

		ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
			.defaultAdvisors(
					new DocumentRetrievalAdvisor(retriever, systemResource.getContentAsString(StandardCharsets.UTF_8)))
			.build();

		ChatResponse response = chatClient.prompt().user("spring ai alibaba 是什么?").call().chatResponse();
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		logger.info("content: {}, request_id: {}", content, response.getMetadata().getId());
	}

	@Test
	void streamTest() throws InterruptedException, IOException {
		DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeChatApi,
				DashScopeDocumentRetrieverOptions.builder().withIndexName("spring-ai知识库").build());
		ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
			.defaultAdvisors(
					new DocumentRetrievalAdvisor(retriever, systemResource.getContentAsString(StandardCharsets.UTF_8)))
			.build();

		Flux<ChatResponse> response = chatClient.prompt()
			.user("如何快速开始百炼?")
			.options(DashScopeChatOptions.builder().withIncrementalOutput(true).build())
			.stream()
			.chatResponse();

		CountDownLatch cdl = new CountDownLatch(1);
		AtomicReference<String> requestId = new AtomicReference<>();
		response.subscribe(data -> {
			System.out.printf("%s", data.getResult().getOutput().getContent());
			if (requestId.get() == null) {
				requestId.set(data.getMetadata().getId());
			}
		}, err -> {
			logger.error("err: {}", err.getMessage(), err);
			cdl.countDown();
		}, () -> {
			logger.info("done");
			cdl.countDown();
		});

		cdl.await();
		logger.info("requestId: {}", requestId.get());
	}

	@Test
	void callWithFunctionTest() {
		ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();

		ChatResponse response = chatClient.prompt()
			.function("getWeather", "根据城市查询天气", new MockWeatherService())
			.user("杭州今天的天气如何?")
			.call()
			.chatResponse();

		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		logger.info("content: {}", content);
	}

	@Test
	void callWithFunctionBeanTest() {
		ChatClient chatClient = ChatClient.builder(dashscopeChatModel).defaultFunctions("getOrderFunction").build();

		ChatResponse response = chatClient.prompt()
			.functions("getCurrentWeather")
			.user("帮我一下订单, 用户编号为1001, 订单编号为2001")
			.call()
			.chatResponse();

		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		logger.info("content: {}", content);
	}

	@Test
	void callWithFunctionAndRagTest() throws IOException {
		DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeChatApi,
				DashScopeDocumentRetrieverOptions.builder().withIndexName("spring-ai知识库").build());

		ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
			.defaultAdvisors(
					new DocumentRetrievalAdvisor(retriever, systemResource.getContentAsString(StandardCharsets.UTF_8)))
			.defaultFunctions("weatherFunction")
			.build();

		ChatResponse response = chatClient.prompt().user("如何快速开始百炼?").call().chatResponse();

		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		logger.info("content: {}", content);
	}

	@Test
	void streamCallWithFunctionAndRagTest() throws InterruptedException, IOException {
		DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeChatApi,
				DashScopeDocumentRetrieverOptions.builder().withIndexName("spring-ai知识库").build());

		ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
			.defaultAdvisors(
					new DocumentRetrievalAdvisor(retriever, systemResource.getContentAsString(StandardCharsets.UTF_8)))
			.defaultFunctions("weatherFunction")
			.build();

		Flux<ChatResponse> response = chatClient.prompt()
			.user("上海今天的天气如何?")
			.options(DashScopeChatOptions.builder().withIncrementalOutput(true).build())
			.stream()
			.chatResponse();

		CountDownLatch cdl = new CountDownLatch(1);
		response.subscribe(data -> {
			System.out.printf("%s", data.getResult().getOutput().getContent());
		}, err -> {
			logger.error("err: {}", err.getMessage(), err);
			cdl.countDown();
		}, () -> {
			System.out.println();
			logger.info("done");
			cdl.countDown();
		});

		cdl.await();
	}

	@Test
	void callWithReferencedRagTest() throws IOException {
		DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeChatApi,
				DashScopeDocumentRetrieverOptions.builder().withIndexName("spring-ai知识库").build());

		ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
			.defaultAdvisors(new DashScopeDocumentRetrievalAdvisor(retriever,
					systemResourceRef.getContentAsString(StandardCharsets.UTF_8), true))
			.build();

		ChatResponse response = chatClient.prompt().user("如何快速开始百炼?").call().chatResponse();

		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		logger.info("content: {}", content);
		List<Document> documents = (List<Document>) response.getMetadata()
			.get(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS);
		Assertions.assertNotNull(documents);

		for (Document document : documents) {
			logger.info("referenced doc name: {}, title: {}, score: {}", document.getMetadata().get("doc_name"),
					document.getMetadata().get("title"), document.getMetadata().get("_score"));
		}
	}

	@Test
	void streamCallWithReferencedRagTest() throws IOException, InterruptedException {
		DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeChatApi,
				DashScopeDocumentRetrieverOptions.builder().withIndexName("spring-ai知识库").build());

		ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
			.defaultAdvisors(new DashScopeDocumentRetrievalAdvisor(retriever,
					systemResourceRef.getContentAsString(StandardCharsets.UTF_8), true))
			.build();

		Flux<ChatResponse> response = chatClient.prompt().user("如何快速开始百炼?").stream().chatResponse();

		CountDownLatch cdl = new CountDownLatch(1);
		response.subscribe(data -> {
			System.out.printf("%s", data.getResult().getOutput().getContent());

			ChatCompletionFinishReason finishReason = ChatCompletionFinishReason
				.valueOf(data.getResult().getMetadata().getFinishReason());
			if (finishReason != ChatCompletionFinishReason.NULL) {
				List<Document> documents = (List<Document>) data.getMetadata()
					.get(DashScopeDocumentRetrievalAdvisor.RETRIEVED_DOCUMENTS);
				Assertions.assertNotNull(documents);

				for (Document document : documents) {
					logger.info("referenced doc name: {}, title: {}, score: {}", document.getMetadata().get("doc_name"),
							document.getMetadata().get("title"), document.getMetadata().get("_score"));
				}
			}
		}, err -> {
			logger.error("err: {}", err.getMessage(), err);
			cdl.countDown();
		}, () -> {
			System.out.println();
			logger.info("done");
			cdl.countDown();
		});

		cdl.await();
	}

	@Test
	void callWithMemory() throws IOException {
		DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeChatApi,
				DashScopeDocumentRetrieverOptions.builder().withIndexName("spring-ai知识库").build());

		ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
			.defaultAdvisors(
					new DocumentRetrievalAdvisor(retriever, systemResource.getContentAsString(StandardCharsets.UTF_8)))
			.defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
			.build();

		String conversantId = UUID.randomUUID().toString();

		ChatResponse response = chatClient.prompt()
			.user("如何快速开始百炼?")
			.advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversantId)
				.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
			.call()
			.chatResponse();
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		logger.info("content: {}", content);

		response = chatClient.prompt()
			.user("可以给一些使用示例吗?")
			.advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversantId)
				.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
			.call()
			.chatResponse();
		content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		logger.info("content: {}", content);
	}

	@Test
	void addDocument() {
		String filePath = "/Users/yuanci/Downloads/安装阿里云百炼SDK_大模型服务平台百炼(BAILIAN)-阿里云帮助中心.pdf";
		DocumentReader reader = new DashScopeDocumentCloudReader(filePath, dashscopeChatApi, null);
		List<Document> documentList = reader.get();

		Assertions.assertNotNull(documentList);

		VectorStore cloudStore = new DashScopeCloudStore(dashscopeChatApi, new DashScopeStoreOptions("test-index"));
		cloudStore.add(documentList);
	}

	@Test
	void reader() {
		String filePath = "/Users/nuocheng.lxm/Desktop/新能源产业有哪些-36氪.pdf";
		DashScopeDocumentCloudReader reader = new DashScopeDocumentCloudReader(filePath, dashscopeChatApi, null);
		List<Document> documentList = reader.get();
		DashScopeDocumentTransformer transformer = new DashScopeDocumentTransformer(dashscopeChatApi);
		List<Document> transformerList = transformer.apply(documentList);
		System.out.println(transformerList.size());
	}

	@Test
	void embed() {
		DashScopeEmbeddingModel embeddingModel = new DashScopeEmbeddingModel(dashscopeChatApi);
		Document document = new Document("你好阿里云");
		float[] vectorList = embeddingModel.embed(document);
		System.out.println(vectorList.length);
	}

	@Test
	void vectorStore() {
		DashScopeCloudStore cloudStore = new DashScopeCloudStore(dashscopeChatApi,
				new DashScopeStoreOptions("诺成SpringAI"));
		List<Document> documentList = Arrays.asList(
				new Document("file_f0b6b18b14994ed8a0b45648ce5d0da5_10001", "abc", new HashMap<>()),
				new Document("file_d3083d64026d4864b4558d18f9ca2a6d_10001", "abc", new HashMap<>()),
				new Document("file_f3cce7cab0b74e3d98a8d684f6fc4b55_10001", "abc", new HashMap<>()));
		// cloudStore.add(documentList);
		cloudStore.delete(Arrays.asList("file_d3083d64026d4864b4558d18f9ca2a6d_10001"));
		List<Document> documents = cloudStore.similaritySearch("南方电网");
		System.out.println(documents.size());
	}

	@Test
	void callRagWithRerank() {
		// Step 1 - Load JSON document as Documents

		logger.info("Loading JSON as Documents");
		JsonReader jsonReader = new JsonReader(bikesResource, "name", "price", "shortDescription", "description");
		List<Document> documents = jsonReader.get();

		// Step 2 - Create embeddings and save to vector store
		logger.info("Creating Embeddings...");
		VectorStore vectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
		vectorStore.add(documents);

		// Step3 - Retrieve and llm generate
		ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
			.defaultAdvisors(new RetrievalRerankAdvisor(vectorStore, dashscopeRerankModel))
			.build();

		ChatResponse response = chatClient.prompt().user("which bike has best performance?").call().chatResponse();
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		logger.info("content: {}", content);
	}

}