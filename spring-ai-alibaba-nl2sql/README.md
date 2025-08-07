# Spring AI Alibaba DataAgent

## 项目简介

这是一个基于Spring AI Alibaba的自然语言转SQL项目，能让你用自然语言直接查询数据库，不需要写复杂的SQL。

## 项目结构

这个项目分为三个部分：

```
spring-ai-alibaba-nl2sql/
├── spring-ai-alibaba-nl2sql-management    # 管理端（可直接启动的Web应用）
├── spring-ai-alibaba-nl2sql-chat         # 核心功能（不能独立启动，供集成使用）  
└── spring-ai-alibaba-nl2sql-common       # 公共代码
```

## 快速启动

项目进行本地测试是在spring-ai-alibaba-nl2sql-management中进行

### 1. 业务数据库准备

可以在spring-ai-alibaba-example项目仓库获取测试表和数据：

- Schema：https://github.com/springaialibaba/spring-ai-alibaba-examples/blob/main/spring-ai-alibaba-nl2sql-example/chat/sql/schema.sql
- Data：https://github.com/springaialibaba/spring-ai-alibaba-examples/blob/main/spring-ai-alibaba-nl2sql-example/chat/sql/insert.sql

将表和数据导入到你的MySQL数据库中。

### 2. 配置management数据库

在`spring-ai-alibaba-nl2sql-management/src/main/resources/application.yml`中配置你的MySQL数据库连接信息

> 目前程序会自动创建表和数据，所以不需要手动创建。

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/nl2sql?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&allowMultiQueries=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
```

### 3. 配置 API Key

```yaml
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      api-key: ${AI_DASHSCOPE_API_KEY}
      model: qwen-max
      embedding:
        model: text-embedding-v4
```


### 4. 启动管理端

在`spring-ai-alibaba-nl2sql-management`目录下，运行 `spring-ai-alibaba-nl2sql/spring-ai-alibaba-nl2sql-management/src/main/java/com/alibaba/cloud/ai/Application.java` 类。

### 5. 启动WEB页面

进入 `spring-ai-alibaba-nl2sql/spring-ai-alibaba-nl2sql-web-ui` 目录

#### 安装依赖


```bash
# 使用 npm
npm install

# 或使用 yarn
yarn install
```

### 启动服务

```bash
# 使用 npm
npm run dev

# 或使用 yarn
yarn dev
```

启动成功后，访问地址 http://localhost:3000