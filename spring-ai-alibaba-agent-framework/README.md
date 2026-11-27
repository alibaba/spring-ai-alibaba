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

## Related Projects
Spring AI Alibaba Agent Framework depends on the following projects:

* spring-ai-alibaba-graph: [https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-graph-core](https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-graph-core)
* spring-ai-extensions: [https://github.com/spring-ai-alibaba/spring-ai-extensions](https://github.com/spring-ai-alibaba/spring-ai-extensions)
* spring-ai: [https://github.com/spring-projects/spring-ai](https://github.com/spring-projects/spring-ai)

## Blocking tool execution

Blocking tools share a dedicated executor across agents. The default thread count is four times the JVM's available processors, with a minimum of 8 and a maximum of 32. The queue holds up to 128 tasks across the executor. Idle threads expire after 60 seconds, and a full executor returns a tool overload error.

Set the JVM properties `spring.ai.alibaba.tools.threads` and `spring.ai.alibaba.tools.queue-capacity` before the first tool run, or supply your own `Executor` with `ReactAgent.builder().toolExecutor(executor)`. The application owns the lifecycle of a supplied executor. A tool-node override in `RunnableConfig` takes precedence. The existing default parallel executor remains a fallback when explicitly configured.

Use `maxParallelTools` for the per-batch concurrency limit and `toolExecutionTimeout` to bound parallel batches and delegated agent calls. Ordinary async callbacks keep their own timeout. Enable `wrapSyncToolsAsAsync` to apply `toolExecutionTimeout` to sequential blocking tools as well. A timed-out or cancelled blocking callback must honor interruption to stop work that has already started.
