# Spring AI Alibaba Agent Framework

## What's Agent Framework

Spring AI Alibaba Agent Framework is created for Java developers to quickly and easily building, orchestrating, and deploying AI agents. This framework is built upon the ReactAgent concept which features a Reasoning-Acting loop while at the same time supports multi-agent orchestration.

## Core Features
* **ReactAgent**
* **Multi-Agent Orchestration**
* **Context Engineering**
* **Human In The Loop**
* **A2A**
* **Rich Model, Tool and MCP Support**

## Model Call Error Handling

By default, exceptions caught during a synchronous model call or synchronous
streaming setup are converted into an `AssistantMessage` prefixed with
`Exception:`. To propagate those exceptions instead, opt in when building an agent:

```java
ReactAgent agent = ReactAgent.builder()
    .name("assistant")
    .model(chatModel)
    .throwOnModelError(true)
    .build();
```

`throwOnModelError` defaults to `false` and is also available on
`AgentLlmNode.builder()`. When enabled, model interceptors receive the original
exception with its type and cause intact; graph execution may wrap it while
preserving the cause chain. This allows applications to distinguish model failures
from normal responses without parsing error text.

This setting does not add retries or change tool exception handling. Errors emitted
by a streaming publisher after setup continue to propagate in either mode, including
errors after partial output. Existing retry interceptors can still retry or wrap
exceptions according to their own configuration.

## Related Projects
Spring AI Alibaba Agent Framework depends on the following projects:

* spring-ai-alibaba-graph: [https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-graph-core](https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-graph-core)
* spring-ai-extensions: [https://github.com/spring-ai-alibaba/spring-ai-extensions](https://github.com/spring-ai-alibaba/spring-ai-extensions)
* spring-ai: [https://github.com/spring-projects/spring-ai](https://github.com/spring-projects/spring-ai)
