---
title: Ollama
keywords: [Spring AI,OLlama API,Spring AI Alibaba]
description: "Spring AI 接入 Ollama 系列模型"
---

在本章节中，我们将学习如何使用 Spring AI Alibaba 接入 Ollama 系列模型。在开始学习之前，请确保您已经了解相关概念。

1. [Chat Client](../tutorials/basics/chat-client.md)；
2. [Chat Model](../tutorials/basics/chat-model.md)；
3. [Spring AI Alibaba 快速开始](../get-started.md)；
4. 本章节的代码您可以在 [Spring AI Alibaba Example](https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-chat-example) 仓库找到。

> 本示例主要演示如何以 ChatModel 形式接入。关于如何使用 ChatClient，请参考 Github 代码仓库示例。

## Ollama

Ollama 是一个开源的大型语言模型服务工具，旨在帮助用户快速在本地运行大模型。通过简单的安装指令，用户可以通过一条命令轻松启动和运行开源的大型语言模型。

是 LLM 领域的 Docker。

## Spring AI Alibaba 接入

在项目中引入在 Ollama 上部署的模型时，需要引入 `spring-ai-ollama-spring-boot-starter`。同时指定 Ollama 服务的 baseURL 以及运行的模型名称。

1. 引入 `spring-ai-ollama-spring-boot-starter`

    ```xml
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
        <version>1.0.0-M6</version>
    </dependency>
    ```

2. 编写 `application.yml`

    ```yaml
    spring:
      ai:
        ollama:
          base-url: http://localhost:11434
          chat:
            model: llama3
    ```

3. 遵循构造注入的方式注入 ChatModel

    ```java
    private final ChatModel ollamaChatModel;

    public OllamaChatModelController(ChatModel chatModel) {
        this.ollamaChatModel = chatModel;
    }
    ```

4. 编写控制器接口

    ```java
    @GetMapping("/simple/chat")
    public String simpleChat() {

        return ollamaChatModel.call(new Prompt(DEFAULT_PROMPT)).getResult().getOutput().getContent();
    }
    ```

至此，我们便完成 Ollama 系列模型的接入。
