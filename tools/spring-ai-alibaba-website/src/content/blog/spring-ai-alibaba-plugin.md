---
title: 支持40+插件，Spring Ai Alibaba 让智能体私域数据集成更简单
keywords: [Spring AI, Spring AI Alibaba, 插件, Function Calling, Document Reader, RAG]
description: "社区官方 20+ RAG 数据源和 20+ Function Calling 接口，帮助开发者快速接入多种外部数据源（如 GitHub、飞书、云OSS 等）以及调用各种工具（如天气预报、地图导航、翻译服务等）。"
author: "张震霆 & 何裕墙"
date: "2025-02-12"
category: article
---

在 AI 智能体（AI Agent）开发的过程中，**RAG（Retrieval-Augmented Generation）** 和 **Tool Calling** 已经成为两种至关重要的模式。**RAG** 通过结合检索技术和生成模型的强大能力，使智能体能够实时从外部数据源获取信息，并在生成过程中增强其知识深度和推理能力。通过这种方式，智能体不仅能依赖于模型的预训练知识，还能动态访问和处理更加广泛、详细的外部数据，从而显著提升其在复杂任务中的表现。

与此同时，**Tool Calling** 模式为智能体提供了调用外部工具的能力，极大地扩展了其应用范围。智能体可以通过调用外部工具（如天气预报、地图导航、社交媒体平台等），完成更为复杂的任务和操作。这种灵活性使得智能体在各种实际场景中都能表现得更为高效和精确。

目前，我们的开源社区已集成了超过 **20 种不同的 RAG 数据源**，以及 **20 多种工具调用接口**。这意味着开发者可以轻松接入各种类型的外部数据源和工具，而无需重新实现复杂的底层逻辑。

**RAG 数据源支持的示例包括：**

+ **PDF文件**：自动解析和提取PDF中的文本数据。
+ **语雀**：集成了企业级文档管理与检索服务。
+ **飞书**：从飞书企业沟通平台中检索信息。
+ **云OSS（对象存储服务）**：支持从各大云平台的存储中读取和查询数据。
+ **网页爬虫**：通过集成爬虫工具，智能体可以实时抓取和解析网页数据。

**Function Calling 支持的示例包括：**

+ **天气预报**：调用天气API提供精准的气象信息。
+ **地图导航**：集成地图服务，提供位置查询、路线规划等功能。
+ **钉钉**：通过钉钉平台与企业内部系统进行互动，进行消息通知或日程管理等。
+ **金融数据查询**：实时调用金融市场数据接口，获取股票、汇率等信息。
+ **计算工具**：调用数学计算引擎进行复杂的算法运算。

通过这些集成，开发者可以在极短的时间内，将这些常用的数据源和工具引入到智能体中，从而专注于业务逻辑的实现，避免了繁琐的底层开发工作。

接下来的内容将详细介绍如何利用这些开源实现，轻松接入 RAG 数据源和调用外部工具，帮助你快速构建功能强大且灵活的智能体。

## Document Reader
在构建智能体（AI Agent）时，RAG（Retrieval-Augmented Generation，检索增强生成）是一种至关重要的技术，它能够显著提升智能体在处理复杂任务时的表现。RAG方法通过结合大规模数据源的检索和生成模型的能力，使得智能体能够动态地从外部知识库中提取信息，并在生成过程中充分利用这些信息。这种方法尤其对于需要回答大量开放性问题、进行多轮对话或完成复杂推理任务的应用场景非常重要。

### 社区版 DocumentReader 实现（20+）
在 RAG 系统中，数据源的解析（对应 Spring AI 中的 DocumentReader 抽象）是其中一个关键环节。它决定了智能体如何有效地从不同的数据源中提取、整理和处理信息，从而保证后续检索和生成步骤的准确性与效率。

Spring AI Alibaba 的扩展性在于它提供了多种不同来源文档加载器（DocumentLoader）和不同文档格式的解析器（DocumentParser）实现，能够支持多种数据源的集成。这意味着用户可以根据实际需求，灵活地选择不同的数据处理方式，并且能够将 Spring AI 与各种文档存储系统（如数据库、API、文件系统等）和信息源（如网页、PDF文件、文本数据等）无缝连接。Spring AI Alibaba 不仅提供了基础的文本处理功能，还允许自定义的数据源解析方式，进一步提升了系统的灵活性与可扩展性。

以下是当前社区版本提供的数据源集成实现【1】，涵盖了从学术文献（arXiv）、代码仓库（GitHub、GitLab）、到云存储（腾讯云COS）、企业文档管理工具（Feishu、Notion、Yuque、Gitbook）等多个数据源，极大地方便了开发者快速集成和使用不同类型的文档数据源。

+ **arxiv-document-reader**
+ **github-document-reader**
+ **mbox-document-reader**
+ **tencent-cos-document-reader**
+ **chatgpt-data-document-reader**
+ **github-reader**
+ **mysql-document-reader**
+ **tencent-cos-reader**
+ **email-document-reader**
+ **gitlab-document-reader**
+ **notion-document-reader**
+ **yuque-document-reader**
+ **feishu-document-reader**
+ **gpt-repo-document-reader**
+ **obsidian-document-reader**
+ **gitbook-document-reader**
+ **huggingface-fs-document-reader**
+ **poi-document-reader**



以下是支持的不同 **文档格式解析工具** 列表，以及它们各自的功能说明：

+ **document-parser-apache-pdfbox**：用于解析 PDF 格式文档。
+ **document-parser-bshtml**：用于解析基于 BSHTML 格式的文档。
+ **document-parser-pdf-tables**：专门用于从 PDF 文档中提取表格数据。
+ **document-parser-bibtex**：用于解析 BibTeX 格式的参考文献数据。
+ **document-parser-markdown**：用于解析 Markdown 格式的文档。
+ **document-parser-tika**：一个多功能文档解析器，支持多种文档格式。

以 document-parser-tika 为例，Tika 是一个强大的文档解析工具，能够支持多种格式的解析，包括但不限于：

+ **PDF**：提取文本、图片、元数据等。
+ **图片（如 JPG、PNG、GIF 等）**：提取图像的元数据或通过 OCR 解析图片中的文本内容。
+ **文本（TXT）**：解析纯文本文件，获取文件内容。
+ **Markdown**：解析 Markdown 格式的文档，保留原有的格式和结构。
+ **HTML**：解析 HTML 文件，提取其中的文本和结构化内容。
+ **以及其他格式**：如 Word、Excel、PPT 等办公文档，甚至包括邮件格式（如 EML、MSG）。

您可以根据要处理的文档格式、解析效果等选择不同的解析工具使用。

### 使用示例 - RAG集成语雀文档
首先，在 Spring AI 应用基础上，增加语雀文档解析工具相关依赖，如下所示：

```xml
 <dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>yuque-document-reader</artifactId>
    <version>1.0.0-M5.1</version>
</dependency>
```



按如下代码片段所示，配置访问语雀平台需要的Token、文档路径后，使用 YuQueDocumentReader 加载语雀来源的文档，请注意这里我们使用的是 Tika 器来解析语雀文档中的内容。

```java
YuQueResource source = YuQueResource.builder().yuQueToken(YU_QUE_TOKEN).resourcePath(RESOURCE_PATH).build();
YuQueDocumentReader reader = new YuQueDocumentReader(source, new TikaDocumentParser());
List<Document> documents = reader.get();
```



接下来，就是标准的 RAG 开发流程了，将文档写入向量数据库并用于后续的检索召回。

```java
// 将从语雀读取的文档写入向量数据库
vectorStore.write(new TokenTextSplitter().transform(documents));

// ......

// 文档召回
vectorStore.similaritySearch("请给我讲一下 Spring AI 开发智能体的优势。").forEach(doc -> {
    logger.info("Similar Document: {}", doc.getContent());
});
```

## Function Calling
通过与外部系统集成，能让智能体通过集成外部工具扩展其功能和知识，提升处理复杂任务的能力。例如，它可以调用翻译服务、地图查询、天气预报等工具，实时获取和处理信息，从而应对更广泛的应用场景，实现更智能、更高效的任务执行。

### 社区版 FunctionCalling 实现（20+）
以下是 **Spring AI Alibaba** 社区支持的 Function Calling 接口列表【1】。这些功能调用接口涵盖了广泛的应用场景，包括但不限于翻译服务、地理位置服务、新闻获取、搜索引擎查询、地图导航、爬虫抓取、企业通讯、天气预报等。开发者可以利用这些现成的工具和API，快速实现智能体的多种功能，而无需从零开始开发。

+ **spring-ai-alibaba-starter-function-calling-alitranslate**：调用阿里巴巴翻译服务，支持多语言互译。
+ **spring-ai-alibaba-starter-function-calling-larksuite**：集成 Lark（飞书）办公套件，进行消息发送、文件管理等操作。
+ **spring-ai-alibaba-starter-function-calling-amap**：调用高德地图API，提供地理位置查询、路线规划等功能。
+ **spring-ai-alibaba-starter-function-calling-microsofttranslate**：调用微软翻译API，支持多语言翻译服务。
+ **spring-ai-alibaba-starter-function-calling-baidumap**：调用百度地图API，提供地图查询、位置服务等功能。
+ **spring-ai-alibaba-starter-function-calling-regex**：集成正则表达式处理工具，用于文本模式匹配和数据提取。
+ **spring-ai-alibaba-starter-function-calling-baidusearch**：调用百度搜索API，进行网页信息搜索。
+ **spring-ai-alibaba-starter-function-calling-serpapi**：集成 SERP API，提供搜索引擎结果页面（SERP）的抓取与分析。
+ **spring-ai-alibaba-starter-function-calling-baidutranslate**：调用百度翻译API，提供中文及其他语言之间的翻译功能。
+ **spring-ai-alibaba-starter-function-calling-sinanews**：集成新浪新闻API，提供新闻搜索、获取最新新闻等功能。
+ **spring-ai-alibaba-starter-function-calling-bingsearch**：调用微软 Bing 搜索API，进行网页搜索及获取搜索结果。
+ **spring-ai-alibaba-starter-function-calling-time**：提供当前时间和日期的获取功能，支持多种格式。
+ **spring-ai-alibaba-starter-function-calling-crawler**：集成爬虫工具，支持网页内容抓取和数据采集。
+ **spring-ai-alibaba-starter-function-calling-toutiaonews**：调用今日头条API，获取新闻数据和内容。
+ **spring-ai-alibaba-starter-function-calling-dingtalk**：集成钉钉API，支持消息发送、企业内部通讯等功能。
+ **spring-ai-alibaba-starter-function-calling-weather**：提供天气查询功能，获取实时天气预报信息。
+ **spring-ai-alibaba-starter-function-calling-githubtoolkit**：集成 GitHub API，支持代码仓库查询、问题跟踪等功能。
+ **spring-ai-alibaba-starter-function-calling-youdaotranslate**：调用有道翻译API，支持多语言之间的翻译服务。
+ **spring-ai-alibaba-starter-function-calling-googletranslate**：集成 Google Translate API，提供高质量的翻译服务。
+ **spring-ai-alibaba-starter-function-calling-yuque**：集成语雀（Yuque）文档平台API，支持文档创建、管理等操作。
+ **spring-ai-alibaba-starter-function-calling-jsonprocessor**：提供JSON数据处理功能，用于JSON数据的解析、修改等操作。



### 使用示例 - 智能体接入实时天气预报服务
首先，在 Spring AI 应用基础上，增加以下天气预报插件实现依赖，如下所示：

```xml
 <dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-function-calling-weather</artifactId>
    <version>1.0.0-M5.1</version>
</dependency>
```



按如下代码片段所示，在 `ChatClient` 调用中加入 `天气预报插件` 声明，这部分插件信息将随用户 message 一同发送给模型，由模型决策何时调用插件。

```java
String ans = chatClient.prompt().functions("getWeatherServiceFunction").user(text).call().content();
```



请注意，`getWeatherServiceFunction`是官方插件的注册名，必须严格配置为 `getWeatherServiceFunction` 而不能是其他任何名称。如果您需要使用其他插件，请到官网或github仓库查看相应插件的注册名【1】【2】。

## 总结
通过使用社区官方提供的超过 **20 种 RAG 数据源** 和 **20 种 Tool Calling 接口**，开发者可以轻松接入多种外部数据源（如 GitHub、飞书、云 OSS 等）以及调用各种工具（如天气预报、地图导航、翻译服务等）。这些默认实现大大简化了智能体的开发过程，使得开发者无需从零开始，便可以快速构建功能强大的智能体系统。通过这种方式，智能体不仅能够高效处理复杂任务，还能适应各种应用场景，提供更加智能、精准的服务。



1. 社区插件仓库 [https://github.com/alibaba/spring-ai-alibaba/tree/main/community](https://github.com/alibaba/spring-ai-alibaba/tree/main/community)
2. Spring AI Alibaba 官方网站 [https://java2ai.com/](https://java2ai.com/)
