# Spring AI Alibaba Elasticsearch Document Reader

This module provides a document reader implementation for Elasticsearch, allowing you to retrieve documents from Elasticsearch indices for use with Spring AI.

本模块提供了 Elasticsearch 的文档读取器实现，允许从 Elasticsearch 索引中检索文档以供 Spring AI 使用。

## Features | 特性

- Read documents from Elasticsearch indices | 从 Elasticsearch 索引中读取文档
- Support for both single node and cluster mode | 支持单节点和集群模式
- Support for HTTPS and basic authentication | 支持 HTTPS 和基本认证
- Customizable query field | 可自定义查询字段
- Configurable maximum results | 可配置最大结果数
- Support for both simple retrieval and query-based search | 支持简单检索和基于查询的搜索

## Usage | 使用方法

### Maven Dependency | Maven 依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>es-document-reader</artifactId>
    <version>${version}</version>
</dependency>
```

### Single Node Configuration | 单节点配置

```java
ElasticsearchConfig config = new ElasticsearchConfig();
config.setHost("localhost");          // Default: localhost | 默认值：localhost
config.setPort(9200);                 // Default: 9200 | 默认值：9200
config.setIndex("your-index");        // Required | 必填
config.setQueryField("content");      // Default: content | 默认值：content
config.setMaxResults(10);             // Default: 10 | 默认值：10
config.setScheme("https");           // Default: http | 默认值：http

// Optional authentication | 可选的认证配置
config.setUsername("your-username");
config.setPassword("your-password");

ElasticsearchDocumentReader reader = new ElasticsearchDocumentReader(config);
```

### Cluster Configuration | 集群配置

```java
ElasticsearchConfig config = new ElasticsearchConfig();
// Configure cluster nodes | 配置集群节点
config.setNodes(Arrays.asList(
    "node1:9200",
    "node2:9201",
    "node3:9202"
));
config.setIndex("your-index");
config.setQueryField("content");
config.setScheme("https");

// Optional authentication (applied to all nodes) | 可选的认证配置（应用于所有节点）
config.setUsername("your-username");
config.setPassword("your-password");

ElasticsearchDocumentReader reader = new ElasticsearchDocumentReader(config);
```

### Reading Documents | 读取文档

```java
// Get all documents | 获取所有文档
List<Document> documents = reader.get();

// Get document by ID | 通过 ID 获取文档
Document document = reader.getById("document-id");

// Search documents by query | 通过查询搜索文档
List<Document> queryResults = reader.readWithQuery("your search query");
```

## Configuration Properties | 配置属性

| Property 属性 | Description 描述 | Default Value 默认值 |
|------------|----------------|------------------|
| host       | Elasticsearch host 主机地址 | localhost |
| port       | Elasticsearch port 端口 | 9200 |
| nodes      | List of cluster nodes (host:port) 集群节点列表 | [] |
| index      | Index name to query 要查询的索引名称 | - |
| queryField | Field to search in 搜索字段 | content |
| username   | Username for authentication 认证用户名 | - |
| password   | Password for authentication 认证密码 | - |
| maxResults | Maximum number of results to return 最大返回结果数 | 10 |
| scheme     | Connection scheme (http/https) 连接方案 | http |

## Cluster Support | 集群支持

The reader supports both single node and cluster configurations:
读取器支持单节点和集群两种配置方式：

- If `nodes` is provided, it will use cluster mode | 如果提供了 `nodes`，将使用集群模式
- If `nodes` is empty, it will use single node mode with `host` and `port` | 如果 `nodes` 为空，将使用 `host` 和 `port` 的单节点模式
- All nodes in the cluster share the same authentication and scheme settings | 集群中的所有节点共享相同的认证和方案设置

## HTTPS Support | HTTPS 支持

For secure connections: | 对于安全连接：

1. Set `scheme` to "https" | 将 `scheme` 设置为 "https"
2. The reader will automatically: | 读取器将自动：
   - Create a secure SSL context | 创建安全的 SSL 上下文
   - Trust all certificates (for development) | 信任所有证书（用于开发环境）
   - Handle hostname verification | 处理主机名验证
   - Apply authentication if provided | 应用提供的认证信息

## Testing | 测试

The module includes comprehensive tests. To run the tests:
模块包含完整的测试。要运行测试：

```bash
mvn test
```

## Requirements | 要求

- Java 17 or later | Java 17 或更高版本
- Elasticsearch 8.x | Elasticsearch 8.x 版本
- Docker (for running tests) | Docker（用于运行测试） 