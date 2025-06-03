# Spring AI Alibaba Java Manus

[English](./README.md) | [中文](./README-zh.md)

The Spring AI implementation of <a href="https://github.com/mannaandpoem/OpenManus/" target="_blank">OpenManus</a>

# features 

Spring AI Alibaba Java Manus provides the following key features:

1. **Perfect Implementation of OpenManus Multi-Agent Framework**: A comprehensive implementation that faithfully recreates the OpenManus architecture in Java with Spring AI.
![Image](https://github.com/user-attachments/assets/f27b763e-9c68-44e0-a57d-4f31d04c0200)
2. **Agent Configuration via Web Interface**: Easily configure agents through an intuitive web-based administration interface without modifying code.
![Image](https://github.com/user-attachments/assets/96d5902a-f741-4e82-9007-136cf4c56bb0)
3. **MCP (Model Context Protocol) Integration**: Seamless integration with Model Context Protocol allows agents to interact with various models and services.
![Image](https://github.com/user-attachments/assets/df24679a-77f1-4e66-a15f-5e0fadcffacf)
4. **PLAN-ACT Pattern Support**: Implements the powerful PLAN-ACT pattern for sophisticated reasoning and execution workflows.
![Image](https://github.com/user-attachments/assets/d00fc59d-3f10-4163-a548-784eb21f77d6)

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

4. [Optional] Set <a href="https://serpapi.com/users/sign_in" target="_blank">SerpApi key</a>, register to get free tokens for each month.

 ```shell
 export SERP_API_KEY=xxxx
 ```

5. [Optional] Set <a href="https://lbsyun.baidu.com/apiconsole/key" target="_blank">Baidu Map key</a>. Modify the "ak" in the mcp-servers-config.json file.

 ```shell
 "BAIDU_MAP_API_KEY": "your_baidu_AK"
 ```

### Run with IDE

Import the this module as an independent project into your favorite IDE.

Open `OpenManusSpringBootApplication` in the editor and click `run`.

### Run with Maven

```shell
mvn spring-boot:run
```

## Architecture

![aaa](https://github.com/user-attachments/assets/4ad14a72-667b-456e-85c1-b05eef8fd414)

## Previous Versions
If you want a previous stable version, you can find it here:  
[Previous Stable Versions](https://github.com/rainerWJY/Java-Open-Manus/releases)
