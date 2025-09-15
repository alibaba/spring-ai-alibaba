# Spring AI Alibaba Tablestore Memory Module

[中文版本](./README-zh.md)

## Introduction

The Spring AI Alibaba Tablestore Memory Module is a core component of the Spring AI Alibaba project,
specifically designed to provide a Tablestore-based storage solution. Leveraging Tablestore's high-performance wide-column storage, full-text search capabilities, and distributed architecture, this module delivers fast and reliable storage services for conversational history and contextual data in AI applications. It enables AI systems to remember previous interactions, thereby facilitating more coherent and personalized user experiences.

## Core Features

- **Tablestore Storage**：Leverages Tablestore's high performance and distributed architecture to enable rapid storage and retrieval of conversational history and contextual data.
- **Seamless Integration with Spring Ecosystem**: Provides full compatibility with the Spring Framework and Spring Boot applications for effortless adoption.
- **Proven in Production**: Tongyi App, Quark, and other major services have successfully implemented Memory capabilities using Tablestore.
- **Comprehensive Memory Interface Design and Documentation**: Explore richer Memory features and detailed usage documentation on GitHub at [alibabacloud-tablestore-for-agent-memory](https://github.com/aliyun/alibabacloud-tablestore-for-agent-memory) .

## Get Started

### Maven Dependency

Add the following dependency to your project:

```xml
<dependency>
    <groupId>com.alibaba.spring.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-memory-tablestore</artifactId>
    <version>${latest.version}</version>
</dependency>
```


### Basic Configuration

Only Tablestore client configuration is required.

```java

void init() {
    // Constructs the Tablestore Java client
    String endPoint = System.getenv("tablestore_end_point");
    String instanceName = System.getenv("tablestore_instance_name");
    String accessKeyId = System.getenv("tablestore_access_key_id");
    String accessKeySecret = System.getenv("tablestore_access_key_secret");
    SyncClient client = new SyncClient(endPoint, accessKeyId, accessKeySecret, instanceName);
    // Initializes the TablestoreChatMemoryRepository
    TablestoreChatMemoryRepository chatMemoryRepository = new TablestoreChatMemoryRepository(client);
}

```

### 高级配置
可以通过 `store` 参数进行高级配置，使用更丰富的会话管理、消息管理、全文检索等能力。
```java

void init() {
    // Constructs the Tablestore Java client
    String endPoint = System.getenv("tablestore_end_point");
    String instanceName = System.getenv("tablestore_instance_name");
    String accessKeyId = System.getenv("tablestore_access_key_id");
    String accessKeySecret = System.getenv("tablestore_access_key_secret");
    SyncClient client = new SyncClient(endPoint, accessKeyId, accessKeySecret, instanceName);
    // Initializes TablestoreChatMemoryRepository via store 
    MemoryStoreImpl store = xxx; // For store initialization, refer to https://github.com/aliyun/alibabacloud-tablestore-for-agent-memory/blob/main/java/examples/src/main/java/com/aliyun/openservices/tablestore/agent/memory/MemoryStoreInitExample.java
    TablestoreChatMemoryRepository chatMemoryRepository = new TablestoreChatMemoryRepository(store);
}

// For implementation details of the underlying MemoryStore, refer to: https://github.com/aliyun/alibabacloud-tablestore-for-agent-memory
```
