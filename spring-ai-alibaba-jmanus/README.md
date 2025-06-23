# Spring AI Alibaba Java Manus

[English](./README.md) | [中文](./README-zh.md)

The Spring AI implementation of <a href="https://github.com/mannaandpoem/OpenManus/" target="_blank">OpenManus</a>

# features 

Spring AI Alibaba Java Manus provides the following key features:

1. **Perfect Implementation of OpenManus Multi-Agent Framework**: A comprehensive implementation that faithfully recreates the OpenManus architecture in Java with Spring AI.
![Image](https://github.com/user-attachments/assets/893c7fc1-5e6e-4ec9-8389-182f14d86b18)
2. **Agent Configuration via Web Interface**: Easily configure agents through an intuitive web-based administration interface without modifying code.
![Image](https://github.com/user-attachments/assets/5afdfe2e-0e98-4100-bff1-b7aaf413850b)
3. **MCP (Model Context Protocol) Integration**: Seamless integration with Model Context Protocol allows agents to interact with various models and services.
![Image](https://github.com/user-attachments/assets/31d915a9-04dc-45b2-9635-488cc06ba468)
4. **PLAN-ACT Pattern Support**: Implements the powerful PLAN-ACT pattern for sophisticated reasoning and execution workflows.
![Image](https://github.com/user-attachments/assets/d9cbf980-9d56-4b58-b165-6840b6c9411b)



## Stable Versions

If you want a previous stable version, you can find it here:  
[Stable Versions](https://github.com/rainerWJY/Java-Open-Manus/releases)


## How to Run

### Prerequisites

1. Make sure you have JDK 17 or later installed.
2. Install npx globally using npm:

   ```shell
   npm install -g npx
   ```

3. Set <a href="https://help.aliyun.com/zh/model-studio/getting-started/first-api-call-to-qwen" target="_blank">dashscope api key</a>.

 ```shell
 export AI_DASHSCOPE_API_KEY=xxxx
 ```

4. [Optional] Visit the [mcp sse free service](https://mcp.higress.ai/). Obtain an MCP service for quick testing.

### Run with IDE

Import the this module as an independent project into your favorite IDE.

Open `OpenManusSpringBootApplication` in the editor and click `run`.

### Run with Maven

```shell
mvn spring-boot:run
```

## Architecture

![aaa](https://github.com/user-attachments/assets/4ad14a72-667b-456e-85c1-b05eef8fd414)
