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

### 1. 配置数据库

修改 `spring-ai-alibaba-nl2sql-management/src/main/resources/application.yml`：

.sql文件在spring-ai-alibaba-example项目仓库中(https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-nl2sql-example)

请直接导入对应的insert.sql文件和schema.sql文件，并进行配置后启动spring-ai-alibaba-nl2sql-management

```yaml
chatBi:
  dbConfig:
    url: jdbc:mysql://localhost:3306/你的数据库?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: 你的用户名
    password: 你的密码
    dialect-type: mysql
```

### 2. 配置API Key

在同一个配置文件中设置（两个部分都需要配置）：

```yaml
spring:
  ai:
    dashscope:
      api-key: 你的DashScope-API-Key
    openai:
      api-key: 你的DashScope-API-Key
```

## 如何使用
### API调用
```bash
# 执行自然语言查询
GET http://localhost:8062/nl2sql/search?query=查询用户数量

# 流式聊天（实时返回结果，想要参与进入开发,一般使用该接口）
GET http://localhost:8062/nl2sql/stream/search?query=你的问题

# 简单聊天接口
POST http://localhost:8062/simpleChat
Content-Type: application/json
```


## 开发集成

如果要在自己的项目中集成NL2SQL功能：

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-nl2sql</artifactId>
    <version>${spring-ai-alibaba.version}</version>
</dependency>
```

### 2. 代码示例

```java
@RestController
public class MyNl2SqlController {
    
    @Autowired
    private SimpleNl2SqlService simpleNl2SqlService;
    
    @PostMapping("/query")
    public String query(@RequestBody String question) throws Exception {
        return simpleNl2SqlService.nl2sql(question);
    }
}
```

## 贡献指南

欢迎提交Issue和PR！详见 [Spring AI Alibaba 贡献指南](https://github.com/alibaba/spring-ai-alibaba/blob/main/CONTRIBUTING-zh.md)

## 开源协议

Apache License 2.0 
