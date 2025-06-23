# [Spring AI Alibaba](https://java2ai.com)

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![CI Status](https://github.com/alibaba/spring-ai-alibaba/workflows/%F0%9F%9B%A0%EF%B8%8F%20Build%20and%20Test/badge.svg)](https://github.com/alibaba/spring-ai-alibaba/actions?query=workflow%3A%22%F0%9F%9B%A0%EF%B8%8F+Build+and+Test%22)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/alibaba/spring-ai-alibaba)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.alibaba.cloud.ai/spring-ai-alibaba/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.alibaba.cloud.ai/spring-ai-alibaba)
<img alt="gitleaks badge" src="https://img.shields.io/badge/protected%20by-gitleaks-blue">

[中文版本](./README-zh.md)

[Spring AI Alibaba](https://java2ai.com) is an agentic AI framework for building ChatBot, Workflow, and Multi-agent applications.

## Core Features

<p align="center">
    <img src="./docs/imgs/spring-ai-alibaba-architecture.png" alt="architecture" style="max-width: 740px; height: 508px" /> 
</p>


Spring AI Alibaba provides the following core capabilities to help developers quickly build Chatbot, Workflow, or Multi-agent applications:

1. **Graph based multi-agent framework**, with Spring AI Alibaba Graph, developers can quickly build workflows and multi-agent applications in ease. Graph code can be generated from Dify DSL and debugged in a visual way.
2. **Enterprise-ready AI ecosystem integration, bring agents from demo to production.** Spring AI Alibaba supports integration with the Aliyun Bailian platform, providing LLM model service and RAG knowledge  solutions; Support seamless integration of AI observation products such as ARMS and Langfuse; Support enterprise level MCP integration, including Nacos MCP Registry for MCP discovery and routing, etc.
3. **Plan-Act agent products and platforms.**
* JManus, Spring AI Alibaba based Manus implementation, supports delicacy plan adjustment, plan reuse.
* DeepResearch, Spring AI Alibaba based research and report agent with powerful tools like search engines, web crawlers, Python and MCP services.

## Get Started

To quickly get started with Spring AI Alibaba, add 'spring-ai-alibaba-starter-dashscope' dependency to your java project.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.alibaba.cloud.ai</groupId>
      <artifactId>spring-ai-alibaba-bom</artifactId>
      <version>1.0.0.2</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
  </dependency>
</dependencies>
```

Please check [Quick Start](https://java2ai.com/docs/1.0.0.2/get-started/chatbot) on our official website to learn more details. More starters include `spring-ai-alibaba-graph-core`, `spring-ai-alibaba-starter-nl2sql`,`spring-ai-alibaba-starter-nacos-mcp-client`, etc, please refer to the official website documentation.

> NOTE!
> 1. Requires JDK 17+.
> 2. If there are any `spring-ai` dependency issue, please lean how to configure the `spring-milestones` Maven repository on [FAQ page](https://java2ai.com/docs/1.0.0.2/faq).

### Playground and Example

The community has developed a [Playground](https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-playground) agent that includes a complete front-end UI and back-end implementation. The Playground back-end is developed using Spring AI Alibaba and gives users a quick overview of all core framework capabilities such as chatbot, multi-round conversations, image generation, multi-modality, tool calling, MCP, and RAG.

<p align="center">
    <img src="./docs/imgs/playground.png" alt="PlayGround" style="max-width: 949px; height: 537px; border-radius: 15px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);" /> 
</p>

You can [deploy the Playground example locally](https://github.com/springaialibaba/spring-ai-alibaba-examples) and access the experience through your browser, or copy the source code and tweak it to your own business needs to build your own set of AI apps more quickly.
For more examples, please refer to our official example repository: [https://github.com/springaialibaba/spring-ai-alibaba-examples](https://github.com/springaialibaba/spring-ai-alibaba-examples)

## Spring AI Alibaba Graph Multi-gent Framework

Spring AI Alibaba Graph enables developers to implement workflow and multi-agent application orchestration. Its core design is mainly from LangGraph, and we have added a rich set of prebuilt Nodes and simplified the Graph State definition, allowing developers to better integrate with low-code platforms and write popular multi-agent pattern applications.

Core features:

+ Workflow, built-in workflow nodes, aligned with mainstream low-code platforms;
+ Multi-agent, built-in ReAct Agent, Supervisor and other modes;
+ Native streaming support;
+ Human-in-the-loop, waiting for human confirmation, modifying states and resuming execution;
+ Memory and persistent storage;
+ Graph state snapshot;
+ Nested and paralleled graph;
+ PlantUML and Mermaid format export.

## Enterprise-ready AI Ecosystem Integration

To bring agent from demo to production, developers and organizations face lots of challenges, from evaluation, tracing, MCP integration, prompt management, to token rate-limit, etc. Spring AI Alibaba, as am enterprise solution incubated from serving enterprise agent development, provides profound solutions by integrating with Nacos MCP Registry, Higress AI gateway, Alibaba Cloud ARMS, Alibaba Cloud Vector Stores, Alibaba Cloud Bailian platform, etc.

<p align="center">   
    <img src="https://img.alicdn.com/imgextra/i2/O1CN01sON0wZ21yKROGt2SJ_!!6000000007053-2-tps-5440-2928.png" alt="spring-ai-alibaba-architecture" style="max-width: 700px; height: 400px"/> 
</p>

1. **Distributed MCP discovery and proxy:** Support distributed MCP Server discovery and load balancing based on Nacos MCP Registry. Zero code change to transform HTTP and Dubbo services into MCP servers with  Spring AI Alibaba MCP Gateway and Higress;
2. **Higress LLM model proxy:** Higress as a LLM proxy, `spring-ai-starter-model-openai` adapter can leverage the unified Higress OpenAI model proxy API;
3. **Better and easy data integration:**
	- a. Bailian RAG integration. Leverage Bailian platform's excellent performance on data filtering, chunking, and vectoring, while using Spring AI Alibaba to do RAG retrieval;
	- b. Bailian ChatBI integration. Spring AI Alibaba Nl2SQL, built on Bailian ChatBI, completely open-source, can generate SQL based on natural language query.
4. **Observation and evaluation platforms:** Thanks to the sdk-native instrumentation of Spring AI, observation and evaluation can be achieved by reporting to OpenTelemetry compatible platforms such as Langfuse and Alibaba Cloud ARMS.

## Agent Products and Platforms

### JManus

The emergence of Manus has given people unlimited space with the ability of general intelligent agents to automatically plan-act on various tasks. It is expected to be very good at solving open-ended issues and can have a wide range of applications in daily life, work, and other scenarios.

JManus is not just a Spring AI Alibaba version Manus replica, it's also designed as a platform that can help developers to build their own fine-tuned agents targeting specific business scenarios. The typical characteristic of enterprise level agent is determinism, that means we need customized tools and sub agents, as well as stable and deterministic planning and processes. Therefore, we hope that JManus can become an intelligent agent development platform, allowing users to build their own domain specific intelligent agent implementations in the most intuitive and low-cost way.

<p align="center">
    <img src="./docs/imgs/jmanus.png" alt="jmanus" style="max-width: 749px; height: 467px; border-radius: 15px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);" /> 
</p>

### DeepResearch

Spring AI Alibaba DeepResearch is a deep research agent developed based on the Spring AI Alibaba Graph, which includes a complete front-end web UI (under development) and back-end implementation. DeepResearch can help users complete various deep research reports with the help of large models and a series of carefully designed tools such as Web Search, Crawling, Python script engine, etc.

<p align="center">
    <img src="./docs/imgs/deepresearch.png" alt="Deep Research" style="max-width: 770px; height: 850px">
</p>

## Contribution Guide

Please refer to the [Contribution Guide](./CONTRIBUTING.md) to learn how to participate in the development of Spring AI
Alibaba.

## Contact Us

* Dingtalk Group (钉钉群), search `124010006813` and join.
* WeChat Group (微信公众号), scan the QR code below and follow us.

<p align="center">
    <img src="./docs/imgs/wechat-account.png" alt="Deep Research" style="max-width: 400px; height: 400px">
</p>

## Credits

Some of this project's ideas and codes are inspired by or rewrote from the following projects. Great thanks to those who
have created and open-sourced these projects.

* [Spring AI](https://github.com/spring-projects/spring-ai), a Spring-friendly API and abstractions for developing AI
  applications licensed under the Apache License 2.0.
* [Langgraph](https://github.com/langchain-ai/langgraph), a library for building stateful, multi-actor applications with
  LLMs, used to create agent and multi-agent workflows licensed under the MIT license.
* [Langgraph4J](https://github.com/bsorrentino/langgraph4j), a porting of
  original [LangGraph](https://github.com/langchain-ai/langgraph) from
  the [LangChain AI project](https://github.com/langchain-ai) in Java fashion.
