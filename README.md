# [Spring AI Alibaba](https://java2ai.com)

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![CI Status](https://github.com/alibaba/spring-ai-alibaba/workflows/%F0%9F%9B%A0%EF%B8%8F%20Build%20and%20Test/badge.svg)](https://github.com/alibaba/spring-ai-alibaba/actions?query=workflow%3A%22%F0%9F%9B%A0%EF%B8%8F+Build+and+Test%22)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/alibaba/spring-ai-alibaba)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.alibaba.cloud.ai/spring-ai-alibaba/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.alibaba.cloud.ai/spring-ai-alibaba)
<img alt="gitleaks badge" src="https://img.shields.io/badge/protected%20by-gitleaks-blue">

<html>
    <h3 align="center">
      A code-first framework for building and Agentic, Workflow, and Multi-agent applications.
    </h3>
    <h3 align="center">
      <a href="https://java2ai.com/">Agent Framework Docs</a>,
      <a href="https://java2ai.com/">Graph Docs</a>,
      <a href="https://github.com/spring-ai-alibaba/spring-ai-extensions">Spring AI</a>,
      <a href="https://github.com/spring-ai-alibaba/examples">Examples</a>.
    </h3>
</html>

## What's Agent Framework

<p align="center">
    <img src="./docs/imgs/saa-architecture.png" alt="architecture" style="max-width: 740px; height: 508px" />
</p>

Spring AI Alibaba Agent Framework is an agent development framework centered around the design philosophy of **ReactAgent**, enabling developers to build agents with core capabilities such as automatic **Context Engineering** and **Human In The Loop** interaction. For scenarios requiring more complex process control, Agent Framework offers built-in workflows like `SequentialAgent`, `ParallelAgent`, `RoutingAgent`, and `LoopAgen`t based on its **Graph Runtime**. Developers can also flexibly orchestrate more complex workflows using the Graph API.

## Core Features

* **ReactAgent**: Build intelligent agents with reasoning and acting capabilities, following the ReAct (Reasoning + Acting) paradigm for iterative problem-solving.

* **Multi-Agent Orchestration**: Compose multiple agents with built-in patterns including `SequentialAgent`, `ParallelAgent`, `LlmRoutingAgent`, and `LoopAgent` for complex task execution.

* **Context Engineering**: Built-in best practices for prompt engineering, context management, and conversation flow control to improve agent reliability and performance.

* **Human In The Loop**: Seamlessly integrate human feedback and approval steps into agent workflows, enabling supervised execution for critical tools and operations.

- **Streaming Support**: Real-time streaming of agent responses

- **Error Handling**: Robust error recovery and retry mechanisms

* **Graph-based Workflow**: Graph based workflow runtime and api for conditional routing, nested graphs, parallel execution, and state management. Export workflows to PlantUML and Mermaid formats.

* **A2A Support**: Agent-to-Agent communication support with Nacos integration, enabling distributed agent coordination and collaboration across services.

* **Rich Model, Tool and MCP Support**: Leveraging core concepts of Spring AI, supports multiple LLM providers (DashScope, OpenAI, etc.), tool calling, and Model Context Protocol (MCP).

## Getting Started

### Prerequisites

* Requires JDK 17+.
* Choose your LLM provider and get the API-KEY.

### Add Dependencies

```xml
<dependencies>
  <dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-agent-framework</artifactId>
    <version>1.1.0.0-SNAPSHOT</version>
  </dependency>
  <!-- Assume you are going to use DashScope Model. Refer to docs for how to choose model.-->
  <dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    <version>1.1.0.0-SNAPSHOT</version>
  </dependency>
</dependencies>
```

### Create Your First Agent

**1. A simple ReactAgent**

Initialize `ChatModel` instance first.

```java
// Create DashScopeApi instance using the API key from environment variable
DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
// Create DashScope ChatModel instance
this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
```

Create a basic `ReactAgent` instance named `writer_agent`.

```java
ReactAgent writerAgent = ReactAgent.builder()
	.name("writer_agent")
	.model(chatModel)
	.description("可以写文章。")
	.instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
	.outputKey("article")
	.build();
```

**2. A workflow agent that composes two agents**

Let's create another agent called `reviewer_agent` and compose these two agents with `SequentialAgent` workflow agent.

```java
ReactAgent reviewerAgent = ReactAgent.builder()
	.name("reviewer_agent")
	.model(chatModel)
	.description("可以对文章进行评论和修改。")
	.instruction("你是一个知名的评论家，擅长对文章进行评论和修改。对于散文类文章，请确保文章中必须包含对于西湖风景的描述。最终只返回修改后的文章，不要包含任何评论信息。")
	.outputKey("reviewed_article")
	.build();

SequentialAgent blogAgent = SequentialAgent.builder()
	.name("blog_agent")
	.description("可以根据用户给定的主题写一篇文章，然后将文章交给评论员进行评论。")
	.subAgents(List.of(writerAgent, reviewerAgent)) // writerAgent and reviewerAgent will be executed in sequential order.
	.build();
```

**3. Call the agent**

```java
// Call a single agent
AssistantMessage message = writerAgent.call("帮我写一篇100字左右散文。");

// Call SequentialAgent
Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的散文");
```

Please check [Quick Start](https://java2ai.com/docs/1.0.0.3/get-started/chatbot) on our official website to learn more details.

> NOTE!.
> If you have encountered any `spring-ai` dependency issue, please lean how to configure the `spring-milestones` Maven repository on [FAQ page](https://java2ai.com/docs/1.0.0.3/faq).

## 📚 Documentation (Working In Progress...)
* Overview - High level overview of the framework
* Quick Start - Get started with a simple agent
* Tutorials - Step by step tutorials
* User Guide - In-depth user guide for building agents and workflows

## Project Structure

This project consists of three core components:

* **Agent Framework**: A ReactAgent-based development framework designed for building intelligent agents with built-in context engineering best practices. For scenarios requiring more complex flow control, the Agent Framework leverages the underlying Graph runtime to provide orchestration capabilities, supporting SequentialAgent, ParallelAgent, LoopAgent, RoutingAgent, and more. Developers can also use the Graph API to flexibly orchestrate their own workflows.

* **Graph**: The underlying runtime for Agent Framework. We recommend developers to use Agent Framework but it's totally fine to use the Graph API directly. Graph is a low-level workflow and multi-agent orchestration framework that enables developers to implement complex application orchestration. Inspired by LangGraph, it features a rich set of prebuilt nodes and simplified Graph State definitions, making it easier to integrate with low-code platforms and implement popular multi-agent patterns.

* **Spring Boot Starters**: Starters integrating Agent Framework with Nacos to provide A2A and dynamic config features.

## More Projects
 Repository | Description | ⭐
  --- | --- | ---
| [Spring AI Alibaba Graph](https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-graph-core) | A low-level orchestration framework and runtime for building, managing, and deploying long-running, stateful agents. | ![GitHub Repo stars](https://img.shields.io/github/stars/alibaba/spring-ai-alibaba?style=for-the-badge&label=)
| [Spring AI Alibaba Admin](https://github.com/spring-ai-alibaba/spring-ai-alibaba-admin) |  Local visualization toolkit for the development of agent applications, supporting project management, runtime visualization, tracing, and agent evaluation. | ![GitHub Repo stars](https://img.shields.io/github/stars/spring-ai-alibaba/spring-ai-alibaba-admin?style=for-the-badge&label=)
| [Spring AI Extensions](https://github.com/spring-ai-alibaba/spring-ai-extensions) | Extended implementations for Spring AI core concepts, including DashScopeChatModel, MCP registry, etc. |  ![GitHub Repo stars](https://img.shields.io/github/stars/spring-ai-alibaba/spring-ai-extensions?style=for-the-badge&label=)
| [Spring AI Alibaba Examples](https://github.com/spring-ai-alibaba/examples) | Spring AI Alibaba Examples. |  ![GitHub Repo stars](https://img.shields.io/github/stars/spring-ai-alibaba/examples?style=for-the-badge&label=)
| [JManus](https://github.com/spring-ai-alibaba/jmanus) | A Java implementation of Manus built with Spring AI Alibaba, currently used in many applications within Alibaba Group. | ![GitHub Repo stars](https://img.shields.io/github/stars/spring-ai-alibaba/jmanus?style=for-the-badge&label=)
| [DataAgent](https://github.com/spring-ai-alibaba/dataagent) | A natural language to SQL project based on Spring AI Alibaba, enabling you to query databases directly with natural language without writing complex SQL. | ![GitHub Repo stars](https://img.shields.io/github/stars/spring-ai-alibaba/dataagent?style=for-the-badge&label=)
| [DeepResearch](https://github.com/spring-ai-alibaba/deepresearch) |  Deep Research implemented based on spring-ai-alibaba-graph. | ![GitHub Repo stars](https://img.shields.io/github/stars/spring-ai-alibaba/deepresearch?style=for-the-badge&label=)

## Contact Us

* Dingtalk Group (钉钉群), search `130240015687` and join.
* WeChat Group (微信公众号), scan the QR code below and follow us.


## Star History

[![Star History Chart](https://starchart.cc/alibaba/spring-ai-alibaba.svg?variant=adaptive)](https://starchart.cc/alibaba/spring-ai-alibaba)

---

<p align="center">
    Made with ❤️ by the Spring AI Alibaba Team

