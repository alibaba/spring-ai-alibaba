# 数据库 Schema 管控模块

## 模块简介

本模块旨在提供一个轻量级的 `evidence` 及 `schema` 的管控服务，用于初始化数据库的 `schema` 信息，并支持对业务逻辑解释（evidence）进行增删改查操作。

该模块被设计为可复用的 Service 层组件，**仅提供核心功能实现，不包含 RESTful 接口及独立启动能力**。适用于集成到其他 Spring Boot 项目中使用。

---

## 功能特性

- **数据库 Schema 初始化**  
  提供一键初始化功能，快速生成指定的数据库表结构。

- **业务逻辑管理**  
  支持对业务逻辑的增删改查操作，方便开发者维护复杂的业务规则。

- **向量化处理支持**  
  集成 DashScope Embedding 模型，支持文本内容的向量化表示和搜索匹配。

- **多数据库适配**  
  支持 MySQL / PostgreSQL 等主流关系型数据库的 schema 读取与分析。

---

## 技术栈

- **后端**: Java 17+ (Spring Boot)
- **依赖模块**: `com.alibaba.cloud.ai:${spring-ai-alibaba.version}`
- **向量引擎**: 云原生数据仓库 AnalyticDB PostgreSQL 版、SimpleVector
- **嵌入模型**: DashScope Embedding Model

---

## 使用说明

### 前置依赖

- [Java](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html) >= 17
- [PostgreSQL](https://www.postgresql.org/) >= 13（用于存储 schema 元数据）
- [MySQL](https://www.mysql.com/)（可选）
- [Gradle](https://gradle.org/) 或 [Maven](https://maven.apache.org/) 构建工具（用于主项目的构建）

### 引入方式

将本模块作为依赖引入到你的 Spring Boot 项目中：

#### Maven 示例：

```xml
<dependency>
  <groupId>com.alibaba.cloud.ai</groupId>
  <artifactId>spring-ai-alibaba-starter-nl2sql</artifactId>
  <version>${spring-ai-alibaba.version}</version>
</dependency>
```

#### Gradle 示例：

```groovy
implementation 'com.alibaba.cloud.ai:spring-ai-alibaba-starter-nl2sql:${spring-ai-alibaba.version}'
```

### 配置说明

目前支持两种向量存储方式：
- **AnalyticDB**（推荐生产环境，支持大规模数据和高性能检索）
- **SimpleVector**（适合本地开发、测试或小规模场景，无需依赖外部数据库）

#### AnalyticDB 配置示例

在 `application.yml` 中配置以下参数以启用 AnalyticDB 向量功能：

```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key
    vectorstore:
      analytic:
        collectName: your-collection-name
        regionId: cn-hangzhou
        dbInstanceId: your-db-instance-id
        managerAccount: manager-account
        managerAccountPassword: manager-password
        namespace: your-namespace
        namespacePassword: namespace-password
        defaultTopK: 6
        defaultSimilarityThreshold: 0.75
        accessKeyId: your-access-key-id
        accessKeySecret: your-access-key-secret
```

> ⚠️ 注意：AnalyticDB PostgreSQL 实例需提前开启向量引擎优化，详见[创建实例文档](https://help.aliyun.com/zh/analyticdb/analyticdb-for-postgresql/getting-started/create-an-instance-instances-with-vector-engine-optimization-enabled)

#### SimpleVector 配置示例

无需配置，默认启动 SimpleVector

---

## 核心类说明

- **`VectorStoreManagementService`**  
  核心服务类，封装了以下功能：
  - 将数据库 schema 信息写入向量数据库
  - 对业务逻辑解释（evidence）进行增删改查
  - 提供向量搜索接口，用于召回相关业务规则
  - 支持执行向量查询/删除操作

---

## 贡献指南

欢迎参与本模块的开发与优化！请参考 [Spring AI Alibaba 贡献指南](https://github.com/alibaba/spring-ai-alibaba/blob/main/CONTRIBUTING.md) 了解如何参与开源项目的开发。

---

## 许可证

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 开源协议。

---

## 联系方式

如有任何问题，请联系：

- 邮箱: xuqirui.xqr@alibaba-inc.com
- GitHub: [littleahri](https://github.com/littleahri)

---
