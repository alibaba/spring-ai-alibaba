---
title: Java开发基于Spring AI Alibaba玩转MCP：从发布、调用到 Claude 集成
keywords: [Model Context Protocol, MCP, Spring Ai, Claude, OpenManus]
description: 本文讨论了 2024 年 11 月 Anthropic 公司推出的 MCP 协议，介绍了其在 Claude 自动化到 Spring AI Alibaba 生态整合中的应用，包括架构、搭建方法及测试过程等。
author: 夏冬
date: "2025-04-02"
category: article
---

<font style="color:rgb(53, 56, 65);">本文作者：夏冬，Spring AI Alibaba Contributor。</font>

<font style="color:rgb(53, 56, 65);"></font>

# 0、文章摘要

1. MCP 基础与快速体验（熟悉的读者可以跳过此部分）
2. 如何将自己开发的 Spring 应用发布为 MCP Server，验证使用 Claude 或 Spring 应用作为客户端接入自己发布的 Java MCP Server。
	* 发布 stdio 模式的 MCP Server
	* 发布 SSE 模式的 MCP Server
	* 开发另一个 Spring 应用作为 MCP Client 调用 MCP Server 服务
	* 使用 Claude 桌面应用接入我们的 Java MCP Server
3. 如何使用自己开发的 Spring 应用调用 MCP Server，包括调用自己发布的 Java MCP Server，也包括市面上其他通用 MCP Server。
	* 配置并调用 stdio 模式的 MCP Server
	* 配置并调用 SSE 模式的 MCP Server
4. 如何在 Spring AI Alibaba OpemManus 实现中使用 MCP 服务。


# 1、模型上下文协议（Model Context Protocol）入门

2024 年 11 月，Anthropic 公司搞了个挺有意思的新玩意 - Model Context Protocol（模型上下文协议）简称为 MCP 协议。简单来说，它就是给 AI 和各类工具数据之间搭了个标准化的"桥梁"，让开发者不用再为对接问题头疼了。大模型应用可以使用别人分享的 MCP 服务来完成各种各样的工作内容，你可以从这些地方获取 MCP 服务：

- awesome-mcp-servers
- mcp.so

MCP 协议在实际的应用场景上非常广泛，列举一些比较常见的应用场景：

- 使用百度/高德地图分析旅线计算时间
- 接 Puppeteer 自动操作网页
- 使用 Github/Gitlab 让大模型接管代码仓库
- 使用数据库组件完成对 Mysql、ES、Redis 等数据库的操作
- 使用搜索组件扩展大模型的数据搜索能力

## 1.1 在 Claude Desktop 中体验 MCP

接下来我们使用 Claude 快速接入 Github 服务（提前申请 token），编辑一下 Claude Desktop 的配置文件：

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

添加如下内容，注意把`<YOUR_TOKEN>`替换成你自己申请的 token：

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-github"
      ],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "`"
      }
    }
  }
}
```

重启Claude之后，可以看到已经加载了MCP对应的工具：

![spring-ai-alibaba-mcp](/img/blog/mcp/1.png)


点开之后可以看到具体的工具内容：

![spring-ai-alibaba-mcp](/img/blog/mcp/2.png)

此时我们就可以享受 Github 服务提供的操作仓库的能力：

![spring-ai-alibaba-mcp](/img/blog/mcp/3.png)

从图上可以看到，通过创建仓库test-mcp这样的提示词，Claude 的大模型自行判断需要使用 mcp 中提供的create_repository能力，从而完成了仓库的创建，接下来我们打开 Github 也确实发现了这个已经创建的仓库。

![spring-ai-alibaba-mcp](/img/blog/mcp/4.png)

通过这种方式，大模型就可以利用MCP接入各式各样的能力，完成各种更为复杂的工作。

## 1.2 MCP 的架构

MCP 主要分为MCP服务和MCP客户端：

- 客户端：一般指的是大模型应用，比如 Claude、通过 Spring AI Alibaba、Langchain 等框架开发的 AI 应用
- 服务端：连接各种数据源的服务和工具

整体架构如下：

![spring-ai-alibaba-mcp](/img/blog/mcp/5.png)

整体的工作流程是这样的：AI 应用中集成MCP客户端，通过MCP协议向MCP服务端发起请求，MCP 服务端可以连接本地/远程的数据源，或者通过 API 访问其他服务，从而完成数据的获取，返回给 AI 应用去使用。

# 2、在 Spring AI 中使用 Mcp Server

## 2.1 Spring AI MCP 的介绍

Spring AI MCP 为模型上下文协议提供 Java 和 Spring 框架集成。它使 Spring AI 应用程序能够通过标准化的接口与不同的数据源和工具进行交互，支持同步和异步通信模式。整体架构如下：

![spring-ai-alibaba-mcp](/img/blog/mcp/6.png)

Spring AI MCP 采用模块化架构，包括以下组件：

- Spring AI 应用程序：使用 Spring AI 框架构建想要通过 MCP 访问数据的生成式 AI 应用程序
- Spring MCP 客户端：MCP 协议的 Spring AI 实现，与服务器保持 1:1 连接

通过 Spring AI MCP，可以快速搭建 MCP 客户端和服务端程序。

## 2.2 使用 Spring AI MCP 快速搭建 MCP Server

Spring AI 提供了两种机制快速搭建 MCP Server，通过这两种方式开发者可以快速向 AI 应用开放自身的能力，这两种机制如下：

- 基于 stdio 的进程间通信传输，以独立的进程运行在 AI 应用本地，适用于比较轻量级的工具。
- 基于 SSE（Server-Sent Events） 进行远程服务访问，需要将服务单独部署，客户端通过服务端的 URL 进行远程访问，适用于比较重量级的工具。

接下来逐一介绍一下这两种方式的实现，示例代码可以通过如下链接获取：https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-mcp-example/starter-example/server

### 2.2.1 基于 stdio 的 MCP 服务端实现

基于 stdio 的 MCP 服务端通过标准输入输出流与客户端通信，适用于作为子进程被客户端启动和管理的场景。

#### 添加依赖

首先，在项目中添加 Spring AI MCP Server Starter 依赖：

```xml
<dependency>
   <groupId>org.springframework.ai</groupId>
   <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
</dependency>
```

#### 配置 MCP 服务端

在`application.yml`中配置 MCP 服务端，这次要实现的是一个天气服务：

```yaml
spring:
  main:
    web-application-type: none  # 必须禁用web应用类型
    banner-mode: off           # 禁用banner
  ai:
    mcp:
      server:
        stdio: true            # 启用stdio模式
        name: my-weather-server # 服务器名称
        version: 0.0.1         # 服务器版本
```

#### 实现 MCP 工具

使用`@Tool`注解标记方法，使其可以被 MCP 客户端发现和调用，通过`@ToolParameter`注解工具的具体参数：

```java
@Service
public class OpenMeteoService {

    private final WebClient webClient;

    public OpenMeteoService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.open-meteo.com/v1")
                .build();
    }

    @Tool(description = "根据经纬度获取天气预报")
    public String getWeatherForecastByLocation(
            @ToolParameter(description = "纬度，例如：39.9042") String latitude,
            @ToolParameter(description = "经度，例如：116.4074") String longitude) {

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current", "temperature_2m,wind_speed_10m")
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析响应并返回格式化的天气信息
            // 这里简化处理，实际应用中应该解析JSON
            return "当前位置（纬度：" + latitude + "，经度：" + longitude + "）的天气信息：\n" + response;
        } catch (Exception e) {
            return "获取天气信息失败：" + e.getMessage();
        }
    }

    @Tool(description = "根据经纬度获取空气质量信息")
    public String getAirQuality(
            @ToolParameter(description = "纬度，例如：39.9042") String latitude,
            @ToolParameter(description = "经度，例如：116.4074") String longitude) {

        // 模拟数据，实际应用中应调用真实API
        return "当前位置（纬度：" + latitude + "，经度：" + longitude + "）的空气质量：\n" +
                "- PM2.5: 15 μg/m³ (优)\n" +
                "- PM10: 28 μg/m³ (良)\n" +
                "- 空气质量指数(AQI): 42 (优)\n" +
                "- 主要污染物: 无";
    }
}
```

这里使用了OpenMeteo， OpenMeteo是一个开源的天气 API，为非商业用途提供免费访问，无需 API 密钥。

#### 注册 MCP 工具

在应用程序入口类中注册工具：

```java
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTools(OpenMeteoService openMeteoService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(openMeteoService)
                .build();
    }
}
```

#### 运行服务端

在控制台中执行如下命令，编译并打包应用：

```bash
mvn clean package -DskipTests
```

### 2.2.2 基于 SSE 的 MCP 服务端实现

基于 SSE 的 MCP 服务端通过 HTTP 协议与客户端通信，适用于作为独立服务部署的场景，可以被多个客户端远程调用，具体做法与 stdio 非常类似。

#### 添加依赖

首先，在您的项目中添加依赖：

```xml
<dependency>
   <groupId>org.springframework.ai</groupId>
   <artifactId>spring-ai-mcp-server-webflux-spring-boot-starter</artifactId>
</dependency>
```

#### 配置 MCP 服务端

在`application.yml`中配置 MCP 服务端：

```yaml
server:
  port: 8080  # 服务器端口配置

spring:
  ai:
    mcp:
      server:
        name: my-weather-server    # MCP服务器名称
        version: 0.0.1            # 服务器版本号
```

#### 实现 MCP 工具

与基于 stdio 的实现完全相同：

```java
@Service
public class OpenMeteoService {

    private final WebClient webClient;

    public OpenMeteoService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.open-meteo.com/v1")
                .build();
    }

    @Tool(description = "根据经纬度获取天气预报")
    public String getWeatherForecastByLocation(
            @ToolParameter(description = "纬度，例如：39.9042") String latitude,
            @ToolParameter(description = "经度，例如：116.4074") String longitude) {

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("current", "temperature_2m,wind_speed_10m")
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析响应并返回格式化的天气信息
            return "当前位置（纬度：" + latitude + "，经度：" + longitude + "）的天气信息：\n" + response;
        } catch (Exception e) {
            return "获取天气信息失败：" + e.getMessage();
        }
    }

    @Tool(description = "根据经纬度获取空气质量信息")
    public String getAirQuality(
            @ToolParameter(description = "纬度，例如：39.9042") String latitude,
            @ToolParameter(description = "经度，例如：116.4074") String longitude) {

        // 模拟数据，实际应用中应调用真实API
        return "当前位置（纬度：" + latitude + "，经度：" + longitude + "）的空气质量：\n" +
                "- PM2.5: 15 μg/m³ (优)\n" +
                "- PM10: 28 μg/m³ (良)\n" +
                "- 空气质量指数(AQI): 42 (优)\n" +
                "- 主要污染物: 无";
    }
}
```

#### 注册 MCP 工具

在应用程序入口类中注册工具：

```java
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTools(OpenMeteoService openMeteoService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(openMeteoService)
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```

#### 运行服务端

在控制台中输入命令，运行服务端：

```bash
mvn spring-boot:run
```

服务端将在 http://localhost:8080 启动。

## 2.3 在 Claude 中测试 mcp 服务

在上一小节中我们编写完了 MCP 服务，这些服务到底是否能正常运行呢？在 Claude Desktop 中可以测试一下。

修改配置文件，添加weather的配置，一定要注意 jar 包的路径必须是全路径：

```json
{
    "mcpServers": {
        "github": {
            "command": "npx",
            "args": [
                "-y",
                "@modelcontextprotocol/server-github"
            ],
            "env": {
                "GITHUB_PERSONAL_ACCESS_TOKEN": your token
            }
        },
        "weather": {
            "command": "java",
            "args": [
                "-Dspring.ai.mcp.server.stdio=true",
                "-Dspring.main.web-application-type=none",
                "-Dlogging.pattern.console=",
                "-jar",
                "<修改为stdio编译之后的jar包全路径>"
            ],
            "env": {}
        }
    }
}
```

重启 Claude 之后看到，我们编写的两个 Tool 已经被加载进来了：

![spring-ai-alibaba-mcp](/img/blog/mcp/7.png)

输入提示词，查询今天北京的空气质量：

![spring-ai-alibaba-mcp](/img/blog/mcp/8.png)

Claude 触发了我们自己编写的天气服务，展示了完整的数据：

![spring-ai-alibaba-mcp](/img/blog/mcp/9.png)

上面使用了 stdio 的方式在 Claude Desktop 中使用我们自己编写的 MCP 服务，但是很可惜 Claude Desktop 不支持直接通过 SSE 模式访问，必须使用 mcp-proxy 作为中介，所以这里我们不再演示 Claude Desktop 接入 SSE 模式的 MCP 服务。

# 3、在 Spring AI Alibaba 中集成 Mcp Client

对于客户端，Spring AI 同样提供了 stdio 和 SSE 两种机制快速集成 MCP Server，分别对应到 MCP Server 的 stdio 和 SSE 两种模式，参考代码如下：https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-mcp-example/starter-example/client

## 3.1 基于 stdio 的 MCP 客户端实现

基于 stdio 的实现是最常见的 MCP 客户端实现方式，它通过标准输入输出流与 MCP 服务器进行通信。这种方式适用于使用了 stdio 方式本地部署的 MCP 服务器，可以直接在同一台机器上启动 MCP 服务器进程。

### 添加依赖

首先，在项目中添加 Spring AI MCP starter 依赖：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-client-spring-boot-starter</artifactId>
</dependency>
<!-- 添加Spring AI MCP starter依赖 -->
<dependency>
   <groupId>org.springframework.ai</groupId>
   <artifactId>spring-ai-mcp-client-spring-boot-starter</artifactId>
</dependency>
```

### 配置 MCP 服务器

在`application.yml`中配置 MCP 服务器：

```yaml
spring:
  ai:
    dashscope:
      # 配置通义千问API密钥
      api-key: ${DASH_SCOPE_API_KEY}
    mcp:
      client:
        stdio:
          # 指定MCP服务器配置文件路径（推荐）
          servers-configuration: classpath:/mcp-servers-config.json
          # 直接配置示例，和上边的配制二选一
          # connections:
          #   server1:
          #     command: java
          #     args:
          #       - -jar
          #       - /path/to/your/mcp-server.jar
```

这个配置文件设置了 MCP 客户端的基本配置，包括 API 密钥和服务器配置文件的位置。你也可以选择直接在配置文件中定义服务器配置，但是还是建议使用json文件管理 mcp 配置。在`resources`目录下创建`mcp-servers-config.json`配置文件：

```json
{
    "mcpServers": {
        // 定义名为"weather"的MCP服务器
        "weather": {
            // 指定启动命令为java
            "command": "java",
            // 定义启动参数
            "args": [
                "-Dspring.ai.mcp.server.stdio=true",
                "-Dspring.main.web-application-type=none",
                "-jar",
                "<修改为stdio编译之后的jar包全路径>"
            ],
            // 环境变量配置（可选）
            "env": {}
        }
    }
}
```

这个 JSON 配置文件定义了 MCP 服务器的详细配置，包括如何启动服务器进程、需要传递的参数以及环境变量设置，还是要注意引用的 jar 包必须是全路径的。

### 编写一个启动类进行测试：

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // 启动Spring Boot应用
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner predefinedQuestions(
            ChatClient.Builder chatClientBuilder,
            ToolCallbackProvider tools,
            ConfigurableApplicationContext context) {
        return args -> {
            // 构建ChatClient并注入MCP工具
            var chatClient = chatClientBuilder
                    .defaultTools(tools)
                    .build();

            // 定义用户输入
            String userInput = "北京的天气如何？";
            // 打印问题
            System.out.println("\n>>> QUESTION: " + userInput);
            // 调用LLM并打印响应
            System.out.println("\n>>> ASSISTANT: " +
                chatClient.prompt(userInput).call().content());

            // 关闭应用上下文
            context.close();
        };
    }
}
```

这段代码展示了如何在 Spring Boot 应用中使用 MCP 客户端。它创建了一个命令行运行器，构建了 ChatClient 并注入了 MCP 工具，然后使用这个客户端发送查询并获取响应。在 Spring AI Alibaba 中使用 Mcp 工具非常简单，只需要把`ToolCallbackProvider`放到`chatClientBuilder`的`defaultTools`方法中，就可以自动的适配。

通过命令启动程序进行测试：

```bash
mvn spring-boot:run
```

启动之后显示结果为，从日志可以看到我们自己编写的 mcp server 被调用了，返回了数据：

```
>>> QUESTION: 北京的天气如何？
2025-03-31T17:56:17.931+08:00 DEBUG 23455 --- [mcp] [pool-1-thread-1] io.modelcontextprotocol.spec.McpSchema   : Received JSON message: {"jsonrpc":"2.0","id":"60209de5-3","result":{"content":[{"type":"text","text":"\"当前天气:\\n温度: 18.6°C (体感温度: 15.1°C)\\n天气: 多云\\n风向: 南风 (4.7 km/h)\\n湿度: 18%\\n降水量: 0.0 毫米\\n\\n未来天气预报:\\n2025-03-31 (周一):\\n温度: 2.4°C ~ 19.5°C\\n天气: 多云\\n风向: 南风 (8.4 km/h)\\n降水量: 0.0 毫米\\n\\n2025-04-01 (周二):\\n温度: 7.6°C ~ 20.6°C\\n天气: 多云\\n风向: 西北风 (19.1 km/h)\\n降水量: 0.0 毫米\\n\\n2025-04-02 (周三):\\n温度: 6.9°C ~ 18.4°C\\n天气: 晴朗\\n风向: 西北风 (12.8 km/h)\\n降水量: 0.0 毫米\\n\\n2025-04-03 (周四):\\n温度: 7.0°C ~ 19.8°C\\n天气: 多云\\n风向: 南风 (16.3 km/h)\\n降水量: 0.0 毫米\\n\\n2025-04-04 (周五):\\n温度: 7.5°C ~ 21.6°C\\n天气: 多云\\n风向: 西北风 (19.6 km/h)\\n降水量: 0.0 毫米\\n\\n2025-04-05 (周六):\\n温度: 5.6°C ~ 20.7°C\\n天气: 多云\\n风向: 西风 (16.5 km/h)\\n降水量: 0.0 毫米\\n\\n2025-04-06 (周日):\\n温度: 8.4°C ~ 22.3°C\\n天气: 晴朗\\n风向: 南风 (9.4 km/h)\\n降水量: 0.0 毫米\\n\\n\""}],"isError":false}}
2025-03-31T17:56:17.932+08:00 DEBUG 23455 --- [mcp] [pool-1-thread-1] i.m.spec.McpClientSession                : Received Response: JSONRPCResponse[jsonrpc=2.0, id=60209de5-3, result={content=[{type=text, text="当前天气:\n温度: 18.6°C (体感温度: 15.1°C)\n天气: 多云\n风向: 南风 (4.7 km/h)\n湿度: 18%\n降水量: 0.0 毫米\n\n未来天气预报:\n2025-03-31 (周一):\n温度: 2.4°C ~ 19.5°C\n天气: 多云\n风向: 南风 (8.4 km/h)\n降水量: 0.0 毫米\n\n2025-04-01 (周二):\n温度: 7.6°C ~ 20.6°C\n天气: 多云\n风向: 西北风 (19.1 km/h)\n降水量: 0.0 毫米\n\n2025-04-02 (周三):\n温度: 6.9°C ~ 18.4°C\n天气: 晴朗\n风向: 西北风 (12.8 km/h)\n降水量: 0.0 毫米\n\n2025-04-03 (周四):\n温度: 7.0°C ~ 19.8°C\n天气: 多云\n风向: 南风 (16.3 km/h)\n降水量: 0.0 毫米\n\n2025-04-04 (周五):\n温度: 7.5°C ~ 21.6°C\n天气: 多云\n风向: 西北风 (19.6 km/h)\n降水量: 0.0 毫米\n\n2025-04-05 (周六):\n温度: 5.6°C ~ 20.7°C\n天气: 多云\n风向: 西风 (16.5 km/h)\n降水量: 0.0 毫米\n\n2025-04-06 (周日):\n温度: 8.4°C ~ 22.3°C\n天气: 晴朗\n风向: 南风 (9.4 km/h)\n降水量: 0.0 毫米\n\n"}], isError=false}, error=null]
```

## 3.2 基于 SSE 的 MCP 客户端实现

除了基于 stdio 的实现外，Spring AI Alibaba 还提供了基于 Server-Sent Events （SSE）的 MCP 客户端实现。这种方式适用于远程部署的 MCP 服务器，可以通过 HTTP 协议与 MCP 服务器进行通信。

### 添加依赖

首先，在您的项目中添加 Spring AI MCP starter 依赖：

```xml
<dependency>
   <groupId>org.springframework.ai</groupId>
   <artifactId>spring-ai-mcp-client-webflux-spring-boot-starter</artifactId>
</dependency>
```

### 配置 MCP 服务器

在`application.yml`中配置 MCP 服务器，这里需要指定 SSE 启动的服务地址，之前我们在 8080 端口上启动了对应的服务：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASH_SCOPE_API_KEY}
    mcp:
      client:
        sse:
          connections:
            server1:
              url: http://localhost:8080  #服务地址
```

### 使用 MCP 客户端

使用方式与基于 stdio 的实现相同，只需注入`ToolCallbackProvider`和`ChatClient.Builder`：

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
                                                ToolCallbackProvider tools,
                                                ConfigurableApplicationContext context) {
        return args -> {
            // 构建ChatClient并注入MCP工具
            var chatClient = chatClientBuilder
                    .defaultTools(tools)
                    .build();

            // 使用ChatClient与LLM交互
            String userInput = "北京的天气如何？";
            System.out.println("\n>>> QUESTION: " + userInput);
            System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());

            context.close();
        };
    }
}
```

通过命令启动程序进行测试：

```bash
mvn spring-boot:run
```

启动之后会有报错：

```
Caused by: java.lang.IllegalStateException: Multiple tools with the same name (spring-ai-mcp-client-getWeatherForecastByLocation, spring-ai-mcp-client-getAirQuality)
        at org.springframework.ai.mcp.SyncMcpToolCallbackProvider.validateToolCallbacks(SyncMcpToolCallbackProvider.java:126) ~[spring-ai-mcp-1.0.0-20250325.064812-147.jar:1.0.0-SNAPSHOT]
        at org.springframework.ai.mcp.SyncMcpToolCallbackProvider.getToolCallbacks(SyncMcpToolCallbackProvider.java:110) ~[spring-ai-mcp-1.0.0-20250325.064812-147.jar:1.0.0-SNAPSHOT]
        at org.springframework.ai.autoconfigure.mcp.client.McpClientAutoConfiguration.toolCallbacksDeprecated(McpClientAutoConfiguration.java:196) ~[spring-ai-mcp-client-spring-boot-autoconfigure-1.0.0-M6.jar:1.0.0-M6]
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[na:na]
        at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[na:na]
        at org.springframework.beans.factory.support.SimpleInstantiationStrategy.lambda$instantiate$0(SimpleInstantiationStrategy.java:171) ~[spring-beans-6.2.0.jar:6.2.0]
        ... 23 common frames omitted
```

从日志上分析，是因为注册了相同的服务名`spring-ai-mcp-client-getWeatherForecastByLocation`和`spring-ai-mcp-client-getAirQuality`，但是从代码上分析，这两个服务我们都只注册了一次，那为什么会报错呢？

其实这是 Spring AI 目前的一个 BUG，Spring AI 提供了两个自动配置类去生成客户端工具处理 MCP 服务中 Tool 的获取，分别是`SseHttpClientTransportAutoConfiguration`和`SseWebFluxTransportAutoConfiguration`。这两个自动配置类提供了同步和异步两种方式，本身应该是互斥的，但是 Spring AI 对于互斥的处理上出了问题，导致两个自动配置类都会加载。

`SseWebFluxTransportAutoConfiguration`的加载：

![spring-ai-alibaba-mcp](/img/blog/mcp/10.png)

`SseHttpClientTransportAutoConfiguration`的加载：

![spring-ai-alibaba-mcp](/img/blog/mcp/11.png)

两个自动配置类加载之后，就会向提供 SSE 服务的 MCP 服务申请 Tool，这样就导致同样的 Tool 被申请了两次，自然就会重复了。解决方案也非常简单，在启动类上排除`SseHttpClientTransportAutoConfiguration`实现就可以了。

```java
@SpringBootApplication(exclude = {
        org.springframework.ai.autoconfigure.mcp.client.SseHttpClientTransportAutoConfiguration.class
})
public class Application {
...
```

再次通过命令启动程序进行测试：

```bash
mvn spring-boot:run
```

这一次就输出了正确的结果：

![spring-ai-alibaba-mcp](/img/blog/mcp/12.png)

# 4、在 Spring AI Alibaba 的 Open Manus 中体验 MCP

Spring AI Alibaba 中提供了 Open Manus 的实现，整体架构如下：

![spring-ai-alibaba-mcp](/img/blog/mcp/13.jpeg)

在执行阶段，会调用各种 Tool 来完成任务，如果我们能使用 MCP 增加 Tool 的能力，那势必能 Open Manus 如虎添翼，接下来我们就来看一下 Open Manus 中是如何去使用 MCP 的。

源代码如下：https://github.com/alibaba/spring-ai-alibaba/tree/main/community/openmanus

### 添加依赖

首先，在项目中添加 Spring AI MCP starter 依赖：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-client-spring-boot-starter</artifactId>
    <version>${spring-ai.version}</version>
</dependency>
```

### 配置 MCP 服务器

在`application.yml`中已经配置了 MCP 服务器，设置客户端请求服务端的超时时间为 1 分钟：

![spring-ai-alibaba-mcp](/img/blog/mcp/14.png)

添加`mcp-servers-config.json`，在 json 中配置了百度地图。百度地图核心 API 现已全面兼容 MCP 协议，是国内首家兼容 MCP 协议的地图服务商。百度地图已经完成了 8 个核心 API 接口和 MCP 协议的对接， 涵盖逆地理编码、地点检索、路线规划等。 

使用百度地图的 MCP，需要申请ak：https://lbsyun.baidu.com/apiconsole/key。

```json
{
    "mcpServers": {
        "baidu-map": {
            "command": "npx",
            "args": [
                "-y",
                "@baidumap/mcp-server-baidu-map"
            ],
            "env": {
                "BAIDU_MAP_API_KEY": "your_baidu_AK"
            }
        }
    }
}
```

将其中`BAIDU_MAP_API_KEY`修改为申请的ak。

### 使用 MCP 工具

修改`LlmService`的构造方法源码，在构造时直接通过 Spring 容器获取`ToolCallbackProvider`并加入到`ChatClient.builder`中：

```java
public LlmService(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
    this.chatModel = chatModel;

    this.planningChatClient = ChatClient.builder(chatModel)
       .defaultSystem(PLANNING_SYSTEM_PROMPT)
       .defaultAdvisors(new MessageChatMemoryAdvisor(planningMemory))
       .defaultAdvisors(new SimpleLoggerAdvisor())
       .defaultTools(ToolBuilder.getPlanningAgentToolCallbacks())
       .defaultTools(toolCallbackProvider)
       .build();

    this.chatClient = ChatClient.builder(chatModel)
       .defaultSystem(MANUS_SYSTEM_PROMPT)
       .defaultAdvisors(new MessageChatMemoryAdvisor(memory))
       .defaultAdvisors(new SimpleLoggerAdvisor())
       .defaultTools(ToolBuilder.getManusAgentToolCalls())
       .defaultTools(toolCallbackProvider)
       .defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
       .build();

    this.finalizeChatClient = ChatClient.builder(chatModel)
       .defaultSystem(FINALIZE_SYSTEM_PROMPT)
       .defaultAdvisors(new MessageChatMemoryAdvisor(finalizeMemory))
       .defaultAdvisors(new SimpleLoggerAdvisor())
       .build();
}
```

通过`defaultTools`将 mcp 服务提供的 tool 交给`ChatClient`处理。

### 测试效果

启动 OpenManus，执行提示词： 规划下从上海到北京的路线。但是如果这样写，可能会触发google search，我们可以优化下提示词主动选择百度地图。

使用百度地图规划从北京市到上海市的路线

执行程序之后可以看到规划之后的计划：

```
Steps:
0. [ ] [MANUS] 使用百度地图的地理编码服务获取北京市和上海市的经纬度坐标
1. [ ] [MANUS] 使用百度地图的路线规划服务计算从北京市到上海市的驾车路线
2. [ ] [MANUS] 分析并提供最终的路线信息，包括距离、预计耗时等
```

很显然，这一次 OpenManus 选择了我们集成的百度地图 mcp server，我们来看一下结果。

获取到了北京市和上海市的经纬度坐标：

```
Here is a summary of what we accomplished in this step:
- For Beijing, we received the coordinates: Longitude (lng): 116.4133836971231, Latitude (lat): 39.910924547299565.
- For Shanghai, we received the coordinates: Longitude (lng): 121.48053886017651, Latitude (lat): 31.235929042252014.
```

计算从北京市到上海市的驾车路线：

```
Distance: The total distance of the route is 1,223,200 meters (approximately 1,223 kilometers).
Duration: The estimated travel time is 50,592 seconds (approximately 14 hours and 3 minutes).
```

结果：
总距离：约1223公里
预计耗时：约12小时45分钟
主要途径：京沪高速公路（G2）

# 总结

作为 AI 开发领域的革命性突破，Model Context Protocol（MCP）重新定义了智能体与工具生态的交互范式。通过标准化协议打通地图服务、代码仓库、数据库等核心工具链，MCP 不仅解决了传统 AI 开发中跨平台对接的碎片化难题，更以"开箱即用"的轻量化集成模式，让开发者能够快速构建具备多模态能力的智能应用。

未来，随着更多工具接入 MCP 生态，开发者只需专注于业务逻辑创新，而复杂的工具链整合将真正成为"看不见的底层能力"——这或许正是 AI 普惠化进程中，最具实际意义的技术跃迁。 