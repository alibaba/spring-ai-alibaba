# NL2SQL 管控模块

## 模块简介

本模块为 NL2SQL (自然语言转SQL) 功能提供了一个后端管理系统和可视化操作界面。它支持知识配置、数据库 Schema 管理，并提供了一个交互式的聊天界面，用于将自然语言查询转换为 SQL。

本模块是一个可独立运行的 Spring Boot 应用。

---

## 功能特性

- **数据库 Schema 管理**: 提供一键初始化功能，快速将指定数据库表的 Schema 信息生成并存入向量库。
- **知识配置**: 支持对“证据”（即业务逻辑的文字解释）进行增删操作，并将其存入向量库。
- **可视化界面**: 包含一个 Web UI，方便用户与 NL2SQL 服务进行交互。
- **流式聊天**: 提供流式聊天接口，实时返回自然语言查询到 SQL 的转换结果。
- **多向量库支持**: 同时支持 AnalyticDB 和 SimpleVectorStore 作为向量存储和检索方案。

---

## 技术栈

- **后端**: Java 17+ (Spring Boot)
- **向量库**: AnalyticDB, SimpleVectorStore
- **嵌入模型**: DashScope Embedding Model

---

## 使用说明

### 前置依赖

- [Java](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html) >= 17
- [Maven](https://maven.apache.org/) (用于项目构建)
- 一个正在运行的数据库实例 (例如 MySQL, PostgreSQL)

### 运行应用

1.  **配置数据库连接**:
    在 `src/main/resources` 目录下，修改 `application.properties` 或 `application.yml` 文件，填入你的数据库连接信息 (URL, 用户名, 密码)。

2.  **构建并运行应用**:
    你可以直接在 IDE 中运行 `com.alibaba.cloud.ai.Application` 类中的 `main` 方法来启动应用，或者使用以下 Maven 命令：

    ```bash
    mvn spring-boot:run
    ```

3. **运行前端页面**:
    前端页面使用 Vue 3 + Vite 构建。请确保你已经安装了 Node.js 和 npm/yarn。然后在 `spring-ai-alibaba-nl2sql-web-ui` 目录下执行以下命令：

    ```bash
    # 安装依赖
    npm install

    # 启动开发服务器
    npm run dev
    ```

    或者使用 yarn：

    ```bash
    yarn install
    yarn dev
    ```

3.  **访问应用**:
    应用启动后，在浏览器中访问 `http://localhost:3000` 即可打开 Web 界面。

### API 端点

本应用暴露了以下 RESTful API 端点：

- `GET /nl2sql/init`: 初始化数据库 Schema 到向量库。
- `GET /nl2sql/search`: 执行一次非流式的 NL2SQL 查询。
- `GET /nl2sql/stream/search`: 执行一次流式的 NL2SQL 查询。
- `POST /chat`: 使用 AnalyticDB 时进行 NL2SQL 查询的端点。
- `POST /simpleChat`: 使用 SimpleVectorStore 时进行 NL2SQL 查询的端点。

---

## 核心类说明

- **`Nl2sqlForGraphController`**: 处理 NL2SQL 请求（包括流式请求）的核心控制器。
- **`VectorStoreManagementService`**: 用于管理向量库的接口，包含了对 `AnalyticDB` 和 `SimpleVectorStore` 的具体实现。
- **`Application`**: Spring Boot 应用的主启动类。

---

## 贡献指南

欢迎参与本模块的开发与优化！请参考 [Spring AI Alibaba 贡献指南](https://github.com/alibaba/spring-ai-alibaba/blob/main/CONTRIBUTING-zh.md) 了解如何参与开源项目的开发。

---

## 许可证

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 开源协议。
