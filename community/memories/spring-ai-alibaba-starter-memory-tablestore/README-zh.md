# Spring AI Alibaba Tablestore Memory 模块

[English](./README.md)

## 简介

Spring AI Alibaba Tablestore Memory 模块是 Spring AI Alibaba 项目的核心组件之一，
专门提供基于 Tablestore 的存储解决方案。该模块利用 Tablestore 的高性能宽表、全文检索和分布式特性，为 AI 应用提供快速、可靠的对话历史和上下文数据存储服务，使 AI 系统能够"记住"之前的交互，从而提供更连贯、更个性化的用户体验。

## 主要特性

- **Tablestore 存储**：利用 Tablestore 的高性能和分布式特性，实现对话历史和上下文数据的快速存取
- **与 Spring 生态无缝集成**：完美兼容 Spring 框架和 Spring Boot 应用
- **成熟业务验证**: 通义 App、夸克等业务均通过 Tablestore 实现 Memory 能力
- **更完善的 Memory 接口设计和使用文档**: 可以查看 Github [alibabacloud-tablestore-for-agent-memory](https://github.com/aliyun/alibabacloud-tablestore-for-agent-memory) 查看更丰富的 Memory 能力和使用文档。

## 快速开始

### Maven 依赖

将以下依赖添加到你的项目中：

```xml
<dependency>
    <groupId>com.alibaba.spring.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-memory-tablestore</artifactId>
    <version>${latest.version}</version>
</dependency>
```


### 基础配置
仅需配置 Tablestore 客户端即可。

```java

void init() {
    // 构建 Tablestore java 客户端
    String endPoint = System.getenv("tablestore_end_point");
    String instanceName = System.getenv("tablestore_instance_name");
    String accessKeyId = System.getenv("tablestore_access_key_id");
    String accessKeySecret = System.getenv("tablestore_access_key_secret");
    SyncClient client = new SyncClient(endPoint, accessKeyId, accessKeySecret, instanceName);
    // 初始化 TablestoreChatMemoryRepository
    TablestoreChatMemoryRepository chatMemoryRepository = new TablestoreChatMemoryRepository(client);
}

```

### 高级配置
可以通过 `store` 参数进行高级配置，使用更丰富的会话管理、消息管理、全文检索等能力。
```java

void init() {
    // 构建 Tablestore java 客户端
    String endPoint = System.getenv("tablestore_end_point");
    String instanceName = System.getenv("tablestore_instance_name");
    String accessKeyId = System.getenv("tablestore_access_key_id");
    String accessKeySecret = System.getenv("tablestore_access_key_secret");
    SyncClient client = new SyncClient(endPoint, accessKeyId, accessKeySecret, instanceName);
    // 通过 store 初始化 TablestoreChatMemoryRepository. 
    MemoryStoreImpl store = xxx; // store 初始化参考 https://github.com/aliyun/alibabacloud-tablestore-for-agent-memory/blob/main/java/examples/src/main/java/com/aliyun/openservices/tablestore/agent/memory/MemoryStoreInitExample.java
    TablestoreChatMemoryRepository chatMemoryRepository = new TablestoreChatMemoryRepository(store);
}

// 底层依赖的 MemoryStore 使用可参考 https://github.com/aliyun/alibabacloud-tablestore-for-agent-memory
```
