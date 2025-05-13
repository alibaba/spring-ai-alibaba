---
title: DeepSeek
keywords: [Spring AI,DeepSeek,Spring AI Alibaba]
description: "Spring AI 接入 DeepSeek 模型"
---

在本章节中，我们将学习如何使用 Spring AI Alibaba 接入 DeepSeek 系列模型。在开始学习之前，请确保您已经了解相关概念。

1. [Chat Client](../tutorials/basics/chat-client.md)；
2. [Chat Model](../tutorials/basics/chat-model.md)；
3. [Spring AI Alibaba 快速开始](../get-started.md)；
4. 本章节的代码您可以在 [Spring AI Alibaba Example](https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-chat-example) 仓库找到。

> 本示例主要演示如何以 ChatModel 形式接入。关于如何使用 ChatClient，请参考 Github 代码仓库示例。

## DeepSeek 

DeepSeek是一家创新型科技公司，成立于2023年7月17日，使用数据蒸馏技术，得到更为精炼、有用的数据。 由知名私募巨头幻方量化孕育而生，专注于开发先进的大语言模型（LLM）和相关技术。

其开源的第一代推理模型 DeepSeek-R1 模型，拥有卓越的性能，在数学、代码和推理任务上可与 OpenAI o1 媲美。

## Spring AI Alibaba 接入

在阿里云 DashScope  平台，同样提供了 DeepSeek 模型。同时 Spring AI Alibaba 也做了模型接入适配，您可以通过 `spring-ai-alibaba-starter` 接入 DeepSeek 系列模型。

1. 引入 `spring-ai-alibaba-starter` 

    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.3.4</version>
    </dependency>

    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-starter</artifactId>
        <version>1.0.0-M6</version>
    </dependency>
    ```

2. 在 `application.yml` 文件中指定使用的模型

    ```yaml
    spring:
      ai:
        dashscope:
          api-key: ${AI_DASHSCOPE_API_KEY}
          chat:
            options:
              model: deepseek-r1
    ```

3. 注入 ChatModel

    ```java
    private final DashScopeChatModel chatModel;

    public DeepSeekController(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }
    ```

4. 编写 Controller

    ```java
    @GetMapping("/{prompt}")
	public Generation chat(@PathVariable(value = "prompt") String prompt) {

		ChatResponse chatResponse = chatModel.call(new Prompt(prompt));

		return chatResponse.getResult();
	}
    ```

至此，便完成了 DashScope 平台上 DeepSeek 模型的接入。

另外，Spring AI Alibaba 还支持 DeepSeek 的 Reasoning Content 透出，您可以通过以下方式获得 DeepSeek 的 Reasoning Content：

```java
@GetMapping("/{prompt}")
public String chat(@PathVariable(value = "prompt") String prompt) {

    ChatResponse chatResponse = chatModel.call(new Prompt(prompt));

    if (!chatResponse.getResults().isEmpty()) {
        Map<String, Object> metadata = chatResponse.getResults()
                .get(0)
                .getOutput()
                .getMetadata();

        System.out.println(metadata.get("reasoningContent"));
    }

    return chatResponse.getResult().getOutput().getContent();
}
```

之后，您可以在控制台看到如下输出：

```text
嗯，用户发来“你好”，这是个常见的中文问候。首先，我需要确认用户的需求。可能只是打个招呼，或者想开始一段对话。接下来，我应该用友好的回应回复，同时保持开放，让用户知道可以提供帮助。需要检查有没有拼写错误，确保回复自然。另外，考虑用户可能的后续问题，比如需要信息或帮助解决问题。保持简洁，但足够亲切。使用适当的标点符号和表情符号可能会让回复更温暖，不过现在通常不用表情。还要注意语言是否符合中文习惯，避免机械化的回答。可能还需要快速生成回复，减少等待时间。总之，保持礼貌和专业的平衡，让用户感到被重视和支持。
```

## Spring AI 接入

DeepSeek 模型提供了兼容 OpenAI API 的 API 接口实现，因此我们可以基于 `spring-ai-openai-spring-boot-starter` 接入 DeepSeek 模型。(Spring AI 当前未适配 DeepSeek Reasoning Content)

1. 引入 `spring-ai-openai-spring-boot-starter`

    ```xml
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>1.0.0-M6</version>
    </dependency>
    ```

2. 配置 `application.yml`

    ```yaml
    spring:
      application:
      name: spring-ai-alibaba-deepseek-chat-model-example

      ai:
        openai:
          api-key: ${AI_OPENAI_API_KEY}
          base-url: ${AI_OPENAI_BASE_URL}
          chat:
            options:
              model: deepseek-r1
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
