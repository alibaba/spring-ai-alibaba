---
title: OpenAI
keywords: [Spring AI,OpenAI,Spring AI Alibaba]
description: "Spring AI 接入 OpenAI 模型"
---

在本章节中，我们将学习如何使用 Spring AI Alibaba 接入OpenAI 系列模型。在开始学习之前，请确保您已经了解相关概念。

1. [Chat Client](../tutorials/basics/chat-client.md)；
2. [Chat Model](../tutorials/basics/chat-model.md)；
3. [Spring AI Alibaba 快速开始](../get-started.md)；
4. 本章节的代码您可以在 [Spring AI Alibaba Example](https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-chat-example) 仓库找到。

> 本示例主要演示如何以 ChatModel 形式接入。关于如何使用 ChatClient，请参考 Github 代码仓库示例。

## OpenAI 

OpenAI，是一个美国人工智能研究实验室，由非营利组织 OpenAI Inc，和其营利组织子公司 OpenAI LP 所组成。OpenAI 进行 AI 研究的目的是促进和发展友好的人工智能，使人类整体受益。

是 GPT 系列模型的创始和发布公司。其发布的众多模型在数学，自然语言，图像等领域能力突出，推动了 AI 发展。

## Spring AI Alibaba 接入

1. 引入 `spring-ai-openai-spring-boot-starter`

    ```xml
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>1.0.0-M6</version>
    </dependency>
    ```

2. 配置 `application.yml`。（国内连接 OpenAI 时，可能会不通，需要借助代理地址访问）

    ```yaml
    spring:
      ai:
        openai:
          api-key: ${OPENAI_API_KEY}
          base-url: https://api.openai-hk.com
    ```

3. 注入 ChatModel

    ```java
    private final ChatModel deepSeekChatModel;

    public DeepSeekChatModelController (ChatModel chatModel) {
        this.deepSeekChatModel = chatModel;
    }
    ```

4. 编写 Controller 控制器

    ```java
    @GetMapping("/simple/chat")
    public String simpleChat () {

        return deepSeekChatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
    }
    ```
