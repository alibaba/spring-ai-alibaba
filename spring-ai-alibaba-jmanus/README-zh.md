# Spring AI Alibaba Java Manus

[English](./README.md) | 中文

Spring AI 对 [OpenManus](https://github.com/mannaandpoem/OpenManus/) 的 Java 实现。

https://github.com/user-attachments/assets/fc8153bc-8353-4d5c-8789-755d2fd7c4f3

# 功能特性

Spring AI Alibaba Java Manus 提供以下关键功能：

1. **完美实现 OpenManus 多 Agent 框架**：使用 Spring AI 和 Java 全面实现 OpenManus 架构。

2. **通过网页界面配置 Agent**：通过直观的网页管理界面轻松配置 agent，无需修改代码。

3. **MCP (Model Context Protocol) 接入 Agent**：无缝集成 Model Context Protocol，使 agent 能够与各种模型和服务交互。

4. **支持 PLAN-ACT 模式**：实现强大的 PLAN-ACT 模式，支持复杂的推理和执行工作流。

## 运行方法

### 先决条件

1. 确保安装了 JDK 17 或更新版本。
2. 使用 npm 全局安装 npx：
   ```shell
   npm install -g npx
   ```
3. 设置 [dashscope api key](https://help.aliyun.com/zh/model-studio/getting-started/first-api-call-to-qwen)。

 ```shell
 export AI_DASHSCOPE_API_KEY=xxxx
 ```

4. [可选] 设置 [SerpApi key](https://serpapi.com/users/sign_in)，注册后每月可获得免费额度。

 ```shell
 export SERP_API_KEY=xxxx
 ```

5. [可选] 设置 [百度地图 key](https://lbsyun.baidu.com/apiconsole/key)。修改 mcp-servers-config.json 文件中的 "ak"。

 ```shell
 "BAIDU_MAP_API_KEY": "your_baidu_AK"
 ```

### 使用 IDE 运行

将此模块作为独立项目导入到您喜欢的 IDE 中。

在编辑器中打开 `OpenManusSpringBootApplication` 并点击 `运行`。

### 使用 Maven 运行

```shell
mvn spring-boot:run
```

## 架构

![架构图](https://github.com/user-attachments/assets/4ad14a72-667b-456e-85c1-b05eef8fd414)
