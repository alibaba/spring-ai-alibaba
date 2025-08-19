# Spring AI Alibaba MCP Router

基于 Spring AI 的 MCP (Model Context Protocol) Router 实现，提供 MCP 服务的发现、向量存储和智能路由功能。

## 功能特性

- **MCP Server 发现**: 根据任务描述，对合适的 MCP Server 进行语义搜索
- **MCP Server 管理**: 添加并连接到新的 MCP Server
- **请求代理**: 将 Tools 请求转发到相应的 MCP Server
- **向量存储**: 使用 Spring AI 的向量存储技术，支持语义搜索
- **REST API**: 提供完整的 REST API 接口
- **可扩展**: 支持自定义服务发现和向量存储实现
- **定时监控**: 自动监控和更新 MCP Server 状态

## 核心架构

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   REST API      │    │  McpRouter       │    │  Vector Store   │
│   Controller    │◄──►│  Service         │◄──►│  Implementation │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  McpRouterWatcher│
                       │  (定时监控)       │
                       └──────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  McpService      │
                       │  Discovery       │
                       └──────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  Nacos/Registry  │
                       └──────────────────┘
```

## 核心组件

### 1. MCP Router 服务 (McpRouterService)

提供五个核心工具方法：

#### Tools 列表

- **searchMcpServer**: 根据任务描述和关键词搜索合适的 MCP Server
- **addMcpServer**: 添加新的 MCP Server 到生态系统
- **useTool**: 代理 LLM client 和目标 MCP Server 之间的请求
- **getAllMcpServers**: 获取所有可用的 MCP Server 列表
- **removeMcpServer**: 移除指定的 MCP Server

### 2. MCP Router 监控器 (McpRouterWatcher)

- 继承自 `AbstractRouterWatcher`，提供定时监控功能
- 默认每 30 秒执行一次服务刷新
- 支持优雅关闭和异常处理
- 自动监控配置的服务列表

### 3. 抽象监控器 (AbstractRouterWatcher)

- 提供定时任务框架
- 使用 `ScheduledExecutorService` 实现定时执行
- 支持自定义轮询间隔
- 提供统一的异常处理机制

### 4. 服务发现层 (Service Discovery)

- `McpServiceDiscovery`: 服务发现接口
- `NacosMcpServiceDiscovery`: 基于 Nacos 的服务发现实现
- 支持本地缓存和版本检测
- 提供服务信息获取和搜索功能

### 5. 向量存储层 (Vector Store)

- `McpServerVectorStore`: 向量存储接口
- `SimpleMcpServerVectorStore`: 基于 Spring AI SimpleVectorStore 的实现
- 支持语义搜索和相似度过滤
- 自动向量化服务描述信息

### 6. 管理服务 (McpRouterManagementService)

- 协调服务发现和向量存储
- 提供服务生命周期管理
- 支持批量操作和统计信息

### 7. API 层 (REST API)

- `McpRouterController`: REST API 控制器
- 提供完整的服务管理接口
- 支持搜索、统计和健康检查

### 8. 工具服务 (OpenMeteoService)

- 集成天气服务作为示例工具
- 使用 OpenMeteo 免费 API
- 提供天气预报和空气质量查询功能

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-mcp-router</artifactId>
    <version>${revision}</version>
</dependency>
```

### 2. 配置属性

```yaml
spring:
  ai:
    dashscope:
      api-key: your-dashscope-api-key

  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos
        password: nacos

# MCP Router 配置
spring:
  ai:
    alibaba:
      mcp:
        router:
          enabled: true
          service-names: ["echo-server", "weather-server"]
```

### 3. 使用示例

```java
@Autowired
private McpRouterService mcpRouterService;

// 搜索 MCP Server
String result = mcpRouterService.searchMcpServer(
    "数据库查询工具",
    "sql,database,query",
    5
);

// 添加 MCP Server
String result = mcpRouterService.addMcpServer(
    "my-mcp-server",
    "提供数据库查询功能的 MCP Server",
    "database,sql,query"
);

// 使用工具
String result = mcpRouterService.useTool(
    "my-mcp-server",
    "execute_sql",
    "{\"query\": \"SELECT * FROM users\"}"
);
```

## API 接口

### 服务管理

| 方法   | 路径                                             | 描述            |
| ------ | ------------------------------------------------ | --------------- |
| POST   | `/api/mcp-router/initialize`                     | 初始化 MCP 服务 |
| POST   | `/api/mcp-router/services`                       | 添加 MCP 服务   |
| DELETE | `/api/mcp-router/services/{serviceName}`         | 移除 MCP 服务   |
| GET    | `/api/mcp-router/services`                       | 获取所有服务    |
| GET    | `/api/mcp-router/services/{serviceName}`         | 获取指定服务    |
| PUT    | `/api/mcp-router/services/{serviceName}/refresh` | 刷新服务        |
| DELETE | `/api/mcp-router/services`                       | 清空所有服务    |

### 搜索功能

| 方法 | 路径                                                 | 描述     |
| ---- | ---------------------------------------------------- | -------- |
| GET  | `/api/mcp-router/search?query={query}&limit={limit}` | 搜索服务 |

### 系统信息

| 方法 | 路径                         | 描述         |
| ---- | ---------------------------- | ------------ |
| GET  | `/api/mcp-router/statistics` | 获取统计信息 |
| GET  | `/api/mcp-router/health`     | 健康检查     |

## 使用示例

### 1. 初始化服务

```bash
curl -X POST http://localhost:18080/api/mcp-router/initialize \
  -H "Content-Type: application/json" \
  -d '["mcp-database-server", "mcp-weather-server", "mcp-file-server"]'
```

### 2. 搜索服务

```bash
curl "http://localhost:18080/api/mcp-router/search?query=数据库查询&limit=5"
```

### 3. 获取所有服务

```bash
curl http://localhost:18080/api/mcp-router/services
```

### 4. 获取统计信息

```bash
curl http://localhost:18080/api/mcp-router/statistics
```

## 配置说明

### Embedding Model 配置

支持配置不同的 Embedding Model：

```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key
    openai:
      api-key: your-openai-api-key
      base-url: https://api.openai.com
```

### 向量存储配置

当前支持内存向量存储，未来可扩展支持：

- AnalyticDB 向量存储
- Redis 向量存储
- Elasticsearch 向量存储

### MCP Router 配置

```yaml
spring:
  ai:
    alibaba:
      mcp:
        router:
          enabled: true
          service-names: ["service1", "service2"]
          update-interval: 30000 # 更新间隔（毫秒）
          vector-store:
            similarity-threshold: 0.2 # 相似度阈值
```

## 扩展开发

### 自定义服务发现

实现 `McpServiceDiscovery` 接口：

```java
@Component
public class CustomServiceDiscovery implements McpServiceDiscovery {
    // 实现接口方法
}
```

### 自定义向量存储

实现 `McpServerVectorStore` 接口：

```java
@Component
public class CustomVectorStore implements McpServerVectorStore {
    // 实现接口方法
}
```

### 自定义监控器

继承 `AbstractRouterWatcher`：

```java
@Component
public class CustomWatcher extends AbstractRouterWatcher {
    @Override
    protected void handleChange() {
        // 实现自定义监控逻辑
    }
}
```

## 核心流程

### 1. MCP Server 发现流程

1. **定时监控**: McpRouterWatcher 定时从 Nacos 获取服务列表
2. **服务识别**: 通过 NacosMcpServiceDiscovery 识别 MCP Server
3. **信息提取**: 提取服务描述、协议、版本等信息
4. **向量化**: 将服务信息存储到向量存储中
5. **语义搜索**: 根据任务描述进行语义搜索

### 2. 请求代理流程

1. **服务查找**: 根据服务名称查找 MCP Server
2. **连接检查**: 检查与目标服务器的连接状态
3. **请求构建**: 构建 MCP 工具调用请求
4. **请求转发**: 将请求转发到目标服务器
5. **响应处理**: 处理并返回响应结果

### 3. 服务管理流程

1. **服务添加**: 从 Nacos 获取服务信息并添加到向量存储
2. **连接建立**: 尝试建立与目标服务器的连接
3. **状态监控**: 监控服务状态和连接健康度
4. **服务移除**: 清理连接并从向量存储中移除

## 监控和日志

### 日志配置

```yaml
logging:
  level:
    com.alibaba.cloud.ai.mcp.router: DEBUG
    com.alibaba.cloud.ai.mcp.nacos: DEBUG
    org.springframework.ai: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 健康检查

- 自动监控服务连接状态
- 提供 REST API 健康检查端点
- 支持服务统计信息查询

## 注意事项

1. 确保配置了有效的 Embedding Model API Key
2. 服务发现需要正确配置 Nacos 连接信息
3. 向量搜索结果的分数阈值默认为 0.2
4. 建议在生产环境中使用持久化的向量存储
5. 定期监控 MCP Server 的连接状态和健康度
6. 默认服务端口为 18080

## 许可证

Apache License 2.0
