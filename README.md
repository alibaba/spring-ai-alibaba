# Spring AI Alibaba

[Spring AI Alibaba](https://sca.aliyun.com/ai/) 是一款 Java 语言实现的 AI 应用开发框架，旨在简化 Java AI 应用程序开发，让 Java 开发者像使用 Spring 开发普通应用一样开发 AI 应用。Spring AI Alibaba 基于 Spring AI 开源项目构建，默认提供阿里云基础模型服务、开源及商业生态组件的集成与最佳实践。

## 快速开始
请参考 [官网文档快速开始](https://sca.aliyun.com/ai/get-started/) 了解如何使用 Spring AI Alibaba 快速开发生成式 AI 应用。

总的来说，使用 Spring AI Alibaba 开发应用与使用普通 Spring Boot 没有什么区别，只需要增加 `spring-ai-alibaba-starter` 依赖，将 `ChatClient` Bean 注入就可以实现与模型聊天了。

```xml
<dependency>
	<groupId>com.alibaba.ai</groupId>
	<artifactId>spring-ai-alibaba-starter</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```

```java
@RestController
public class ChatController {

	private final ChatClient chatClient;

	public ChatController(ChatClient.Builder builder) {
		this.chatClient = builder.build();
	}

	@GetMapping("/chat")
	public String chat(String input) {
		return this.chatClient.prompt()
				.user(input)
				.call()
				.content();
	}
}
```

## 特性
以下是 Spring AI Alibaba 支持的核心能力，未来更多高级功能将以这些核心能力为基础。请参考官网文档学习每个[功能详细说明](https://sca.aliyun.com/docs/2023/user-guide/ai/quick-start/)以及 [AI 应用开发最佳实践](https://sca.aliyun.com/docs/2023/user-guide/ai/quick-start/)。

* 提供多种大模型服务对接能力，包括主流开源与阿里云通义大模型服务（百炼）等
* 支持的模型交互类型包括：聊天、文生图、音频转录、文生语音等
* 支持同步和流式 API，在保持应用层 API 不变的情况下支持灵活切换底层模型服务，支持特定模型的定制化能力（参数传递）
* 支持 Structured Output，即将 AI 模型输出映射到 POJOs
* 支持矢量数据库存储与检索
* 支持函数调用 Function Calling
* 支持构建 AI Agent 所需要的工具调用和对话内存记忆能力
* 支持 RAG 开发模式，包括离线文档处理如 DocumentReader、Splitter、Embedding、VectorStore 等，支持 Retrieve 检索

## Roadmap

![Spring AI Alibaba](./imgs/spring-ai-alibaba.png)

基于 Spring AI Alibaba 以及阿里巴巴整体开源生态，可以构建原生 AI 架构的应用。

![ai-native-architecture](./imgs/spring-ai-alibaba-arch.png)

## 参考资料
* [spring-ai 官方文档](https://docs.spring.io/spring-ai/reference/index.html)
* [阿里云百炼大模型应用开发平台](https://help.aliyun.com/zh/model-studio/getting-started/what-is-model-studio/)
* [阿里云灵积模型服务与接入说明](https://help.aliyun.com/zh/dashscope/)
* [LangChain 官方文档](https://langchain.com)


