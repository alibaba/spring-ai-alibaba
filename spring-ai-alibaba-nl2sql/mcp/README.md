# Spring AI MCP WebFlux 服务器示例

## 项目简介

本项目是一个基于 Spring AI 框架的 Model Context Protocol (MCP) WebFlux 服务器示例。它展示了如何构建一个支持 WebFlux 和 STDIO 两种通信方式的 MCP 服务器，提供天气查询和空气质量信息等工具服务。

## 主要功能

- 支持 WebFlux 和 STDIO 两种通信方式
- 提供nl2sql服务
- 支持响应式编程模型
- 支持工具函数的动态注册和调用

## 技术栈

- Java 17+
- Spring Boot 3.x
- Spring WebFlux
- Spring AI MCP Server
- Maven

## 核心组件

### ChatApplication

应用程序的主入口类，负责：
- 配置 Spring Boot 应用
- 注册nl2sql服务
- 初始化 MCP 服务器

### McpService

提供主要工具服务：
1. `nl2Sql`: 获取指定自然语言问题转化后的sql


## 配置说明

### 服务器配置

在 `application.properties` 中：

```properties
# 服务器配置
spring.ai.mcp.server.name=nl2sql-server
spring.ai.mcp.server.version=0.0.1

# 使用 STDIO 传输时的配置
spring.main.banner-mode=off
# logging.pattern.console=
```

### WebFlux 客户端配置

在客户端的 `application.properties` 中：

```properties
# 基本配置
server.port=8888
spring.application.name=mcp
spring.main.web-application-type=none
# SSE 连接配置
spring.ai.mcp.client.sse.connections.server1.url=http://localhost:8080

# 调试日志
logging.level.io.modelcontextprotocol.client=DEBUG
logging.level.io.modelcontextprotocol.spec=DEBUG

# 编码配置
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true
spring.mandatory-file-encoding=UTF-8

#业务配置
# OpenAI 兼容接口配置（指向 DashScope）
spring.ai.openai.base-url=https://dashscope.aliyuncs.com/compatible-mode 
spring.ai.openai.api-key=sk
spring.ai.openai.model=qwen-max

# DashScope 特有配置（用于 Embedding 等）
spring.ai.dashscope.api-key=sk

# 向量数据库配置：AnalyticDB PostgreSQL 版
spring.ai.vectorstore.analytic.collectName=chatbi
spring.ai.vectorstore.analytic.regionId=cn-hangzhou
spring.ai.vectorstore.analytic.dbInstanceId=gp-bp11vjucxhw757v9p
spring.ai.vectorstore.analytic.managerAccount=
spring.ai.vectorstore.analytic.managerAccountPassword=
spring.ai.vectorstore.analytic.namespace=
spring.ai.vectorstore.analytic.namespacePassword=
spring.ai.vectorstore.analytic.defaultTopK=10
spring.ai.vectorstore.analytic.defaultSimilarityThreshold=0.01
spring.ai.vectorstore.analytic.accessKeyId=
spring.ai.vectorstore.analytic.accessKeySecret=

# 数据库连接配置（chatbi.dbconfig）
chatbi.dbconfig.url=jdbc:mysql://host:port/database
chatbi.dbconfig.username=
chatbi.dbconfig.password=
chatbi.dbconfig.connectiontype=jdbc
chatbi.dbconfig.dialecttype=mysql
chatbi.dbconfig.schema=
```

## 使用方法

### 1. 编译项目

```bash
mvn clean package -DskipTests
```

### 2. 启动服务器

#### 作为 Web 服务器启动

```bash
mvn spring-boot:run
```

服务器将在 http://localhost:8080 启动。

#### 作为 STDIO 服务器启动

```bash
java -Dspring.ai.mcp.server.stdio=true \
     -Dspring.main.web-application-type=none \
     -Dlogging.pattern.console= \
     -jar target/package.jar
```

### 3. 客户端示例

#### WebFlux 客户端

```java
// 配置 WebFlux 客户端
var transport = new WebFluxSseClientTransport(
    WebClient.builder().baseUrl("http://localhost:8080")
);

// 创建聊天客户端
var chatClient = chatClientBuilder
    .defaultTools(tools)
    .build();

// 发送问题并获取回答
System.out.println("\n>>> QUESTION: " + userInput);
System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
```

#### STDIO 客户端

```java
var stdioParams = ServerParameters.builder("java")
    .args("-Dspring.ai.mcp.server.stdio=true",
          "-Dspring.main.web-application-type=none",
          "-Dlogging.pattern.console=",
          "-jar",
          "-target/package.jar")
    .build();

var transport = new StdioClientTransport(stdioParams);
new SampleClient(transport).run();
```

## 注意事项
1. 使用 STDIO 传输时，必须禁用控制台日志和 banner
2. 默认作为 WebFlux 服务器运行，支持 HTTP 通信
3. 客户端需要正确配置 SSE 连接 URL
4. 确保环境变量 `DASH_SCOPE_API_KEY` 已正确设置

## 扩展开发

如需添加新的工具服务：

1. 创建新的服务类
2. 使用 `@Tool` 注解标记方法
3. 在 `Application` 中注册服务

示例：

```java
@Service
public class MyNewService {
    @Tool(description = "新工具描述")
    public String myNewTool(String input) {
        // 实现工具逻辑
        return "处理结果: " + input;
    }
}

// 在 Application 中注册
@Bean
public ToolCallbackProvider myTools(MyNewService myNewService) {
    return MethodToolCallbackProvider.builder()
        .toolObjects(myNewService)
        .build();
}
```

## 许可证

Apache License 2.0 
