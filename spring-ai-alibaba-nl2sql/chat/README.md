# 自然语言转SQL模块 (NL2SQL)

## 模块简介

本模块旨在提供一个轻量级的 **自然语言查询转 SQL 语句** 的服务，基于用户输入的自然语言问题，结合数据库 Schema 和业务逻辑解释（evidence），通过大模型推理生成对应的 SQL 查询语句，并支持执行该 SQL 返回结果。

该模块被设计为可复用的 Service 层组件，**仅提供核心功能实现，不包含 RESTful 接口及独立启动能力**。适用于集成到其他 Spring Boot 项目中使用。

---

## 功能特性

- **自然语言理解与关键词提取**
  - 支持从用户提问中提取关键词和时间表达式
  - 基于 Prompt 工程引导大模型准确识别意图

- **Schema 精准匹配**
  - 结合向量库召回相关表结构信息
  - 根据关键词和上下文筛选最相关的数据库表结构

- **SQL 生成**
  - 利用大模型生成符合语义的 SQL 查询语句
  - 支持嵌入业务逻辑解释（evidence）以提高准确性

- **SQL 执行与结果展示**
  - 支持直接执行生成的 SQL 并返回格式化结果（Markdown 表格）

---

## 技术栈

- **后端**: Java 17+ (Spring Boot)
- **依赖模块**: `com.alibaba.cloud.ai:common:1.0-SNAPSHOT`
- **大模型服务**: LLM（如 Qwen、DashScope）
- **数据库连接器**: MySQL / PostgreSQL
- **辅助工具**: Gson、Jackson、Markdown 解析器

---

## 使用说明

### 前置依赖

- [Java](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html) >= 17
- [PostgreSQL](https://www.postgresql.org/) 或 [MySQL](https://www.mysql.com/)
- [Gradle](https://gradle.org/) 或 [Maven](https://maven.apache.org/) 构建工具（用于主项目的构建）

### 引入方式

将本模块作为依赖引入到你的 Spring Boot 项目中：

#### Maven 示例：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>nl2sql-service</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

#### Gradle 示例：

```groovy
implementation 'com.alibaba.cloud.ai:nl2sql-service:1.0-SNAPSHOT'
```

---

## 配置说明

确保主项目中已正确配置以下内容：

### 数据库连接配置 (`application.yml`)

```yaml
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode #类似 OpenAI 接口风格的兼容地址，这里指向的是阿里云 DashScope 的兼容接口。
      api-key: sk
      model: qwen-max #使用的模型名称，推荐使用：qwen-max: 适合复杂任务（如 NL2SQL）qwen-plus: 平衡性能与成本
    dashscope:
      api-key: sk  #DashScope 平台的 API Key，用于调用 Qwen 等模型。获取方式：登录 DashScope 控制台 → 查看或创建 API Key。
    vectorstore:
      analytic:
        collectName: chatbi #向量集合名称，即你要写入数据的“collection”名，例如 chatbi
        regionId: cn-hangzhou #实例所在的区域 ID，比如 cn-hangzhou（杭州）、cn-beijing（北京）等。
        dbInstanceId: gp-bp11vjucxhw757v9p #AnalyticDB PostgreSQL 实例 ID，例如 gp-bp11vjucxhw757v9p
        managerAccount: #实例的管理员账号。
        managerAccountPassword: #实例的管理员密码。
        namespace: #命名空间信息，用于隔离不同用户的向量数据
        namespacePassword: 
        defaultTopK: 10 #默认返回的相似向量数量。
        defaultSimilarityThreshold: 0.01 #通常设为 0.01 到 0.75 之间，根据实际效果调整。
        accessKeyId: #阿里云主账号或 RAM 用户的 AK 信息
        accessKeySecret: 
chatbi:
  dbconfig:
    url: jdbc:mysql://host:port/database #数据库 JDBC 连接地址，示例：MySQL: jdbc:mysql://host:port/databasePostgreSQL: jdbc:postgresql://host:port/database
    username: #数据库用户名
    password: #数据库用户密码
    connectiontype: jdbc
    dialecttype: mysql #数据库类型，可选：postgresql、mysql
    schema: #postgresql类型所需要的schema名称

```

### 大模型服务配置（LLM）

请确保 `LlmService` 实现类已注入容器，并支持调用大模型 API。

### 向量服务配置

确保 `VectorStoreService` 及其依赖项（如 `AnalyticDbVectorStoreProperties`）已正确配置并可用。

---

## 核心类说明

- **`Nl2SqlService`**
  - 主要对外接口服务类，提供以下方法：
    - `nl2sql(String query)`：入口方法，接收自然语言问题，返回格式化结果

---

## 典型使用流程

1. 用户输入自然语言问题，例如：“最近一周销售额最高的产品是哪些？”
2. 模块自动提取关键词“销售额”、“产品”、“最近一周”
3. 结合数据库 Schema 和 evidence 进行表结构筛选
4. 生成对应的 SQL 查询语句
5. 执行 SQL 并返回 Markdown 格式的表格结果

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
