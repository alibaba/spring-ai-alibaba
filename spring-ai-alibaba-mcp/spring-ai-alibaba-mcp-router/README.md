# Spring AI Alibaba MCP Router

基于 Spring AI 的 MCP (Model Context Protocol) Router 实现，提供 MCP 服务的发现、向量存储和智能路由功能。

## 功能特性

- **服务发现**: 支持从 Nacos 等注册中心发现 MCP 服务
- **向量存储**: 使用 Spring AI 的向量存储技术，支持语义搜索
- **智能路由**: 基于向量相似度的智能服务路由
- **REST API**: 提供完整的 REST API 接口
- **可扩展**: 支持自定义服务发现和向量存储实现

## 架构设计

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   REST API      │    │  Management      │    │  Vector Store   │
│   Controller    │◄──►│  Service         │◄──►│  Implementation │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  Service         │
                       │  Discovery       │
                       └──────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  Nacos/Registry  │
                       └──────────────────┘
```

## 核心组件

### 1. 服务发现层 (Service Discovery)

- `McpServiceDiscovery`: 服务发现接口
- `NacosMcpServiceDiscovery`: 基于 Nacos 的服务发现实现

### 2. 向量存储层 (Vector Store)

- `McpServerVectorStore`: 向量存储接口
- `SimpleMcpServerVectorStore`: 基于内存的向量存储实现

### 3. 管理层 (Management)

- `McpRouterManagementService`: 核心管理服务，协调发现和存储

### 4. API 层 (REST API)

- `McpRouterController`: REST API 控制器

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
```

### 3. 使用示例

```java
@Autowired
private McpRouterManagementService mcpRouterManagementService;

// 初始化服务
List<String> serviceNames = Arrays.asList("service1", "service2");
mcpRouterManagementService.initializeServices(serviceNames);

// 搜索服务
List<McpServerInfo> results = mcpRouterManagementService.searchServices("查询文本", 10);
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
curl -X POST http://localhost:8080/api/mcp-router/initialize \
  -H "Content-Type: application/json" \
  -d '["service1", "service2", "service3"]'
```

### 2. 搜索服务

```bash
curl "http://localhost:8080/api/mcp-router/search?query=数据库查询&limit=5"
```

### 3. 获取所有服务

```bash
curl http://localhost:8080/api/mcp-router/services
```

### 4. 获取统计信息

```bash
curl http://localhost:8080/api/mcp-router/statistics
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

## 注意事项

1. 确保配置了有效的 Embedding Model API Key
2. 服务发现需要正确配置 Nacos 连接信息
3. 向量搜索结果的分数阈值默认为 0.2
4. 建议在生产环境中使用持久化的向量存储

## 许可证

Apache License 2.0
