---
title: 使用 Ollama 本地模型与 Spring AI Alibaba 的强强结合，打造下一代 RAG 应用
keywords: [Spring AI, Spring AI Alibaba, RAG, Web Search, DeepSeek, Module RAG, Ollama]
description: "使用 Ollama 本地模型与 Spring AI Alibaba 的强强结合，打造下一代 RAG 应用"
author: "牧生"
date: "2025-03-16"
category: article
---

Spring AI Alibaba RAG Example 示例项目源码地址：https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-rag-example

## RAG 应用架构概述

### 1.1 核心组件

- Spring AI：Spring 生态的 Java AI 开发框架，提供统一 API 接入大模型、向量数据库等 AI 基础设施。
- Ollama：本地大模型运行引擎，大模型时代的 Docker，支持快速体验部署大模型。
- Spring AI Alibaba：Spring AI 增强，集成 DashScope 模型平台，快速构建大模型应用。Elasticsearch：向量数据库，存储文本向量化数据，支撑语义检索。

### 1.2 模型选型

- Embedding 模型：nomic-embed-text:latest，用于将文本数据向量化。- Ollama Chat 模型：deepseek-r1:8b，生成最终答案。

## 环境准备

### 2.1 启动 Ollama 服务

Docker Compose 启动 Ollama：（同时启动一个模型前端系统，和 Ollama 模型交互。）

```yml
services:

   ollama:     
     container_name: ollama    
     image: ollama/ollama:latest     
     ports:       
       - 11434:11434

   open-webui:     
     image:  ghcr.io/open-webui/open-webui:main     container_name: open-webui     
     ports:       
       - 3005:8080     
     environment:       
       - 'OLLAMA_BASE_URL=http://host.dockerinternal:11434'   
     # 允许容器访问宿主机网络     
     extra_hosts:      
      - host.docker.internal:host-gateway
```

### 2.2 下载模型执行以下命令：

```shell
docker exec -it ollama ollama pull deepseek-r1:8b
docker exec -it ollama ollama pull nomic-embed-text:latest
```

在 open-webui 中调用 deepseek-r1:8b 模型：

![open-webui 调用 deepseek-r1](https://mmbiz.qpic.cn/mmbiz_png/yvBJb5IiafvnUJ5aNUE53rKqL4sUK66mBF6cloRoSnEbxEicXG9UZa3psYtdKuQJNTRJCdANg5RraU3XApyvsLibQ/640?wx_fmt=png&from=appmsg&tp=wxpic&wxfrom=5&wx_lazy=1&wx_co=1)

### 2.3 部署 Elasticsearch

```yml
services:

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.16.1
    container_name: elasticsearch
    privileged: true
    environment:
      - "cluster.name=elasticsearch"
      - "discovery.type=single-node"
      - "ES_JAVA_OPTS=-Xms512m -Xmx1096m"
      - bootstrap.memory_lock=true
    volumes:
      - ./config/es.yaml:/usr/share/elasticsearch/config/elasticsearch.yml
    ports:
      - "9200:9200"
      - "9300:9300"
    deploy:
      resources:
        limits:
          cpus: "2"
          memory: 1000M
        reservations:
          memory: 200M
```

准备 es 启动的配置文件：

```yml
cluster.name: docker-es
node.name: es-node-1
network.host: 0.0.0.0
network.publish_host: 0.0.0.0
http.port: 9200
http.cors.enabled: true
http.cors.allow-origin: "*"
bootstrap.memory_lock: true

# 关闭认证授权 es 8.x 默认开启
xpack.security.enabled: false
```

至此，便完成搭建一个简单 RAG 应用的所有环境准备步骤。下面开始搭建项目。

## 3. 项目配置

### 3.1 依赖引入

```xml
<!-- Spring Boot Web Starter -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
	<version>3.3.4</version>
</dependency>

<!-- Spring AI Ollama Starter -->
<dependency>
	<groupId>org.springframework.ai</groupId>
	<artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
	<version>1.0.0-M5</version>
</dependency>

<!-- 向量存储 -->
<dependency>
	<groupId>org.springframework.ai</groupId>
	<artifactId>spring-ai-elasticsearch-store</artifactId>
	<version>1.0.0-M5</version>
</dependency>

<!-- PDF 解析 -->
<dependency>
	<groupId>org.springframework.ai</groupId>
	<artifactId>spring-ai-pdf-document-reader</artifactId>
	<version>1.0.0-M5</version>
</dependency>
```

### 3.2 核心配置

```yml
spring:
  
  ai:    
  # ollama 配置    
  ollama:      
    base-url: http://127.0.0.1:11434      
    chat:        
      model: deepseek-r1:8b      
    embedding:        
      model: nomic-embed-text:latest 
  
  # 向量数据库配置    
  vectorstore:      
    elasticsearch:        
      index-name: ollama-rag-embedding-index        
      similarity: cosine        
      dimensions: 768
      
  elasticsearch:
    uris: http://127.0.0.1:9200
```

其中：

- index-name 为 es 向量索引名；
- dimensions 为向量模型生成的向量维度（需要和向量模型生成的向量维度一致，默认值为 1576）；
- similarity 定义了用于衡量向量之间相似度的算法或度量方式，这里使用余弦相似度，使用高维稀疏向量。

如果您想自定义 es 的实例化配置，需要引入 spring-ai-elasticsearch-store：

```xml
<dependency>  
    <groupId>org.springframework.ai</groupId>  <artifactId>spring-ai-elasticsearch-store</artifactId> 
    <version>1.0.0-M5</version>
</dependency>
```

在项目中通过自定义配置 bean 实现。

### 3.3 Prompt Template

```text
你是一个MacOS专家，请基于以下上下文回答：

---------------------
{question_answer_context}
---------------------

请结合给定上下文和提供的历史信息，用中文 Markdown 格式回答，若答案不在上下文中请明确告知。
```

## 4. 核心实现

### 4.1 文本向量化

在 Spring AI 和 Spring AI Alibaba 中，几乎可以将任意数据源作为知识库来源。此例中使用 PDF 作为知识库文档。

> Spring AI Alibaba 提供了 40+ 的 document-reader 和 parser 插件。用来将数据加载到 RAG 应用中。

```java
public class KnowledgeInitializer implements ApplicationRunner {

	// 注入 VectorStore 实例，负责向量化数据的增查操作
	private final VectorStore vectorStore;

	// 向量数据库客户端，此处使用 es
	private final ElasticsearchClient elasticsearchClient;

	// .....

	@Override
	public void run(ApplicationArguments args) {

		// 1. load pdf resources.
		List<Resource> pdfResources = loadPdfResources();

		// 2. parse pdf resources to Documents.
		List<Document> documents = parsePdfResource(pdfResources);

		// 3. import to ES.
		importToES(documents);
	}

	private List<Document> parsePdfResource (List <Resource> pdfResources) {

		// 按照指定策略切分文本并转为 Document 资源对象
		for (Resource springAiResource : pdfResources) {

			// 1. parse document
			DocumentReader reader = new PagePdfDocumentReader(springAiResource);
			List<Document> documents = reader.get();
			logger.info("{} documents loaded", documents.size());
			
            // 2. split trunks            
			List<Document> splitDocuments = new TokenTextSplitter().apply(documents);            
			logger.info("{} documents split", splitDocuments.size());
			
            // 3. add res list            
			resList.addAll(splitDocuments);       
		}    
	}
	
	// ......
}
```

至此，便完成了将文本数据转为向量数据的过程。

### 4.2 RAG 服务层

接下来，将使用 Spring AI 中的 Ollama Starter 来完成和模型交互。构建 RAG 应用。

AIRagService.java

```java
public class AIRagService {
    // 引入 system prompt tmpl
    @Value("classpath:/prompts/system-qa.st")
    private Resource systemResource;

    // 注入相关 bean 实例
    private final ChatModel ragChatModel;
    private final VectorStore vectorStore;

    // 文本过滤，增强向量检索精度
    private static final String textField = "content";

    // ......
    
    public Flux<String> retrieve(String prompt) {
        // 加载 prompt tmpl
        String promptTemplate = getPromptTemplate(systemResource);
        
        // 启用混合搜索，包括嵌入和全文搜索
        SearchRequest searchRequest = SearchRequest.builder()
            .topK(4)
            .similarityThresholdAll()
            .build();
        
        // build chatClient，发起大模型服务调用。
        return ChatClient.builder(ragChatModel)
            .build()
            .prompt()
            .advisors(new QuestionAnswerAdvisor(vectorStore, searchRequest, promptTemplate))
            .user(prompt)
            .stream()
            .content();
    }
}
```

### 4.3 RAG 服务接口层

编写用户请求接口，处理用户请求，调用 service 获得大模型响应：

```java
@RestController
@RequestMapping("/rag/ai")
public class AIRagController {
    
    @Resource
    public AIRagService aiRagService;

    @GetMapping("/chat/{prompt}")
    public Flux<String> chat(
            @PathVariable("prompt") String prompt,
            HttpServletResponse response
    ) {
        // 解决 stream 模式下响应乱码问题。
        response.setCharacterEncoding("UTF-8");
        
        if (!StringUtils.hasText(prompt)) {
            return Flux.just("prompt is null.");
        }
        
        return aiRagService.retrieve(prompt);
    }
}
```

## 5. 请求演示

这里以 `我现在是一个mac新手，我想配置下 mac 的触控板，让他变得更好用，你有什么建议吗？` 问题为例，可以看到直接调用模型的回答是比较官方，实用性不高。

### 5.1 从 open-webui 直接调用

![open-webui 直接调用](https://mmbiz.qpic.cn/mmbiz_png/yvBJb5IiafvnUJ5aNUE53rKqL4sUK66mBDHiaLSGibMNjlSqxxecdo5X7FMOvberwibw8yRaCL4DkwlrqOE71AGelQ/640?wx_fmt=png&from=appmsg&tp=wxpic&wxfrom=5&wx_lazy=1&wx_co=1)

### 5.2 调用 RAG 应用接口

![RAG 应用接口](https://mmbiz.qpic.cn/mmbiz_png/yvBJb5IiafvnJwibFBKFTRTztDyV2G9fed9tyWxCkc8DqtgIU9ic8ZcRPhR98dHwicIUoiabJ2pKne95zISBfsQQszg/640?wx_fmt=png&from=appmsg&tp=wxpic&wxfrom=5&wx_lazy=1&wx_co=1)

## 6. RAG 优化

### 6.1 使用 DashScope 平台模型

使用本地 Ollama 部署模型服务时，模型运行速度收到本地资源限制，思考过程会花费大量时间。因此我们可以通过一些云平台上的模型来增强使用体验。

修改 application.yaml 改为：

```yml
spring:
  application:
    name: ollama-rag
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
    chat:
      options:
        model: deepseek-r1
    ollama:
      base-url: http://127.0.0.1:11434
      chat:
        model: deepseek-r1:8b
        enabled: false
      embedding:
        model: nomic-embed-text:latest
    vectorstore:
      elasticsearch:
        index-name: ollama-rag-embedding-index
        similarity: cosine
        dimensions: 768
  elasticsearch:
    uris: http://127.0.0.1:9200
```

此处关闭 Ollama 的 Chat 功能，通过 Spring AI Alibaba Starter 依赖使用 DashScope 平台上的 DeepSeekR1 模型。

添加依赖：

```xml
<!-- Spring AI Alibaba DashScope -->
<dependency>  
    <groupId>com.alibaba.cloud.ai</groupId>  <artifactId>spring-ai-alibaba-starter</artifactId>  
    <version>1.0.0-M6.1</version>
</dependency>
```

修改 AIRAGService.java

```java
public Flux<String> retrieve(String prompt) {
    // Get the vector store prompt tmpl.
    String promptTemplate = getPromptTemplate(systemResource);
    
    // Enable hybrid search, both embedding and full text search
    SearchRequest searchRequest = SearchRequest.builder()
        .topK(4)
        .similarityThresholdAll()
        .build();
    
    // Build ChatClient with retrieval rerank advisor:
    ChatClient runtimeChatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(new RetrievalRerankAdvisor(
            vectorStore,
            rerankModel,
            searchRequest,
            promptTemplate,
            0.1
        )).build();
    
    // Spring AI RetrievalAugmentationAdvisor
    Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
        .queryTransformers(RewriteQueryTransformer.builder()
            .chatClientBuilder(ChatClient.builder(ragChatModel).build().mutate())
            .build())
        .documentRetriever(VectorStoreDocumentRetriever.builder()
            .similarityThreshold(0.50)
            .vectorStore(vectorStore)
            .build())
        .build();
    
    // Retrieve and llm generate
    return ragClient.prompt()
        .advisors(retrievalAugmentationAdvisor)
        .user(prompt)
        .stream()
        .content();
}
```

### 6.2 检索优化

Spring AI Alibaba RAG 文档：https://java2ai.com/docs/1.0.0-M5.1/tutorials/rag/

在使用 Spring AI 搭建 RAG 应用时，我们可以在构建 QuestionAnswerAdvisor 时，通过设置一些个性化参数，来让我们的 RAG 应用在检索向量数据时达到最佳状态。

### 6.3 数据预处理优化

在数据预处理过程中，可以通过：

1. 删除不相关的文档。噪音数据，特殊字符等来清理数据文本；
2. 添加一些元数据信息，提高索引数据的质量；
3. 优化索引结构等。

## 7. 问题排查

Q：向量入库失败
A：检查 ES 索引维度是否匹配模型输出
Q：检索结果不相关	
A：检查 Embedding 模型是否与文本类型匹配
Q：响应速度慢	
A：调整 Ollama 的计算资源配置
Q：spring-ai-alibaba-starter依赖拉取失败
A：需要配置 mvn 仓库

```xml
<repositories>
  <repository>
    <id>spring-milestones</id>
    <name>Spring Milestones</name>
    <url>https://repo.spring.io/milestone</url>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
  </repository>
  <repository>
    <id>spring-snapshots</id>
    <name>Spring Snapshots</name>
    <url>https://repo.spring.io/snapshot</url>
    <releases>
      <enabled>false</enabled>
    </releases>
  </repository>
</repositories>
```

## 8. 总结

构建 RAG 应用的全过程分为以下三步：

1. 数据加载与清洗：从外部知识库加载数据，向量化后存储到 Elasticsearch。
2. 模型调用优化：通过检索增强技术（RAG），为大模型提供上下文信息。
3. 交互服务搭建：构建 REST API，实现应用与用户的高效交互。

通过 RAG 的检索增强，模型回答可以更具上下文关联性，最终提升用户体验。
