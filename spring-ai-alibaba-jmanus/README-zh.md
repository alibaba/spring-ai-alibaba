# Spring AI Alibaba Java Manus

[English](./README.md) | 中文

Spring AI 对 [OpenManus](https://github.com/mannaandpoem/OpenManus/) 的 Java 实现。

# 功能特性

Spring AI Alibaba Java Manus 提供以下关键功能：

1. **完美实现 OpenManus 多 Agent 框架**：使用 Spring AI 和 Java 全面实现 OpenManus 架构。
![Image](https://github.com/user-attachments/assets/893c7fc1-5e6e-4ec9-8389-182f14d86b18)
2. **通过网页界面配置 Agent**：通过直观的网页管理界面轻松配置 agent，无需修改代码。
![Image](https://github.com/user-attachments/assets/5afdfe2e-0e98-4100-bff1-b7aaf413850b)
3. **MCP (Model Context Protocol) 接入 Agent**：无缝集成 Model Context Protocol，使 agent 能够与各种模型和服务交互。
![Image](https://github.com/user-attachments/assets/31d915a9-04dc-45b2-9635-488cc06ba468)
4. **支持 PLAN-ACT 模式**：实现强大的 PLAN-ACT 模式，支持复杂的推理和执行工作流。
![Image](https://github.com/user-attachments/assets/d9cbf980-9d56-4b58-b165-6840b6c9411b)


## 稳定版本的Release

如果你想要之前的稳定版本，可以在这里找到：
[稳定release版](https://github.com/rainerWJY/Java-Open-Manus/releases)


## 运行方法

### 先决条件

1. 确保安装了 JDK 17 或更新版本。
2. 使用 npm 全局安装 npx：

   ```shell
   npm install -g npx
   ```

3. 设置 [dashscope api key](https://help.aliyun.com/zh/model-studio/getting-started/first-api-call-to-qwen)。  dashscope是阿里云百炼的API， 可以登录阿里云免费获取100万的免费token 。

 ```shell
 export AI_DASHSCOPE_API_KEY=xxxx
 ```


4. [可选] 访问 [mcp sse 免费服务](https://mcp.higress.ai/)。获取可以快速测试的mcp服务。

直接将连接复制到mcp配置页面添加即可。

### 使用 IDE 运行

将此模块作为独立项目导入到您喜欢的 IDE 中。

在编辑器中打开 `OpenManusSpringBootApplication` 并点击 `运行`。

### 使用 Maven 运行

```shell
mvn spring-boot:run
```

## 架构

![架构图](https://github.com/user-attachments/assets/4ad14a72-667b-456e-85c1-b05eef8fd414)
