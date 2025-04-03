# Spring AI Alibaba

[Spring AI Alibaba](https://java2ai.com) 是一款 Java 语言实现的 AI 应用开发框架，旨在简化 Java AI 应用程序开发，让 Java 开发者像使用 Spring 开发普通应用一样开发 AI 应用。Spring AI Alibaba 基于 Spring AI 开源项目构建，默认提供阿里云基础模型服务、开源及商业生态组件的集成与最佳实践。

[English](./README.md) | [日本語](./README-ja.md)

## 快速开始

请参考[快速开始](https://java2ai.com/docs/dev/get-started/) 了解如何使用 Spring AI Alibaba 快速开发生成式 AI 应用。

总的来说，使用 Spring AI Alibaba 开发应用与使用普通 Spring Boot 没有什么区别，只需要增加 `spring-ai-alibaba-starter` 依赖，将 `ChatClient` Bean 注入就可以实现与模型聊天了。

> 因为 Spring AI Alibaba 基于 Spring Boot 3.x 开发，因此对 JDK 要求 17 及以上版本。

1. 在项目中加入 `spring-ai-alibaba-starter` 依赖。

 ```xml
 <dependency>
  <groupId>com.alibaba.cloud.ai</groupId>
  <artifactId>spring-ai-alibaba-starter</artifactId>
  <version>1.0.0-M6.1</version>
 </dependency>
 ```

 > 注意：由于 spring-ai 相关依赖包还没有发布到中央仓库，如出现 spring-ai-core 等相关依赖解析问题，请在您项目的 pom.xml 依赖中加入如下仓库配置。
 >
 > ```xml
 > <repositories>
 >  <repository>
 >   <id>spring-milestones</id>
 >   <name>Spring Milestones</name>
 >   <url>https://repo.spring.io/milestone</url>
 >   <snapshots>
 >    <enabled>false</enabled>
 >   </snapshots>
 >  </repository>
 > </repositories>
 > ```
>
 > 补充：如果您的本地 maven settings.xml 中的 mirrorOf 标签配置了通配符 * ，请根据以下示例修改。
>
 > ```xml
 > <mirror>
 >   <id>xxxx</id>
 >   <mirrorOf>*,!spring-milestones</mirrorOf>
 >   <name>xxxx</name>
 >   <url>xxxx</url>
 > </mirror>
 > ```

2. 注入 `ChatClient`。

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

## 示例

请在 [spring-ai-alibaba-examples](https://github.com/springaialibaba/spring-ai-alibaba-examples) 查看更多 Example 示例。

* Hello World
* Chat Model
* Multi Model
* Function Calling
* Structured Output
* Prompt
* RAG
* Flight Booking Playground，一个贴近实际使用场景，综合运用了 prompt template、function calling、chat memory 和 rag 等的智能机票助手应用。

## 特性

以下是 Spring AI Alibaba 支持的核心能力，未来更多高级功能将以这些核心能力为基础。请参考官网文档 [Spring AI Alibaba 核心概念](https://java2ai.com/docs/dev/concepts/)以及 [AI 应用开发最佳实践](https://java2ai.com/docs/dev/practices/playground-flight-booking/)。

* 开发复杂 AI 应用的高阶抽象 Fluent API -- ChatClient
* 提供多种大模型服务对接能力，包括主流开源与阿里云通义大模型服务（百炼）等
* 支持的模型类型包括聊天、文生图、音频转录、文生语音等
* 支持同步和流式 API，在保持应用层 API 不变的情况下支持灵活切换底层模型服务，支持特定模型的定制化能力（参数传递）
* 支持 Structured Output，即将 AI 模型输出映射到 POJOs
* 支持矢量数据库存储与检索
* 支持函数调用 Function Calling
* 支持构建 AI Agent 所需要的工具调用和对话内存记忆能力
* 支持 RAG 开发模式，包括离线文档处理如 DocumentReader、Splitter、Embedding、VectorStore 等，支持 Retrieve 检索

## Roadmap

Spring AI Alibaba 提供 AI 开源框架以及与阿里巴巴整体开源生态的深度适配，以帮助 Java 开发者快速构建原生 AI 应用架构。

* Prompt Template 管理
* 事件驱动的 AI 应用程序
* 更多 Vector Database 支持
* 函数计算等部署模式
* 可观测性建设
* AI 代理节点开发能力，如绿网、限流、多模型切换等
* 开发者工具集

![ai-native-architecture](./docs/imgs/spring-ai-alibaba-arch.png)

## 贡献指南

请参考 [贡献指南](./CONTRIBUTING.md) 了解如何参与 Spring AI Alibaba 的开发。

## 参考资料

* [Spring AI](https://docs.spring.io/spring-ai/reference/index.html)
* [Spring AI Alibaba](https://java2ai.com/docs/dev/overview/)
* [阿里云百炼大模型应用开发平台](https://help.aliyun.com/zh/model-studio/getting-started/what-is-model-studio/)

## 联系我们

* 钉钉群：请通过群号 `64485010179` 搜索入群
* 微信公众号：请扫描一下二维码关注公众号

<img src="./docs/imgs/wechat-account.png" style="max-width:200px;"/>

## Credit

本项目的一些想法和代码受到以下项目的启发或重写于以下项目，非常感谢那些创建和开源这些项目的开发者。

* [Spring AI](https://github.com/spring-projects/spring-ai)，一款面向 Spring 开发者的 AI 智能体应用开发框架，提供 Spring 友好的 API 和抽象。基于 Apache License V2 开源协议。
* [Langgraph](https://github.com/langchain-ai/langgraph)，一个用于使用LLM构建有状态、多参与者应用程序的库，用于创建代理和多代理工作流。基于 MIT 开源协议。
* [Langgraph4J](https://github.com/bsorrentino/langgraph4j)，[LangGraph]项目的 Java 移植版本。基于 MIT 开源协议。
