# Spring AI Alibaba Chat Memory Example

本示例演示如何使用 Chat Memory 聊天记忆功能。

> 包含此依赖的 Spring AI Alibaba 版本尚未发布，如果需要体验此 Demo，需要本地 install。如果不体验可正常跳过，不影响其他 example 启动。

## Spring AI Alibaba Chat Memory 实现

1. spring ai 提供了基于内存的 InMemory 实现； 
2. Spring AI Alibaba 提供了基于 Redis 和 JDBC 的 ChatMemory 实现。
    
    - MySQL
    - PostgreSQL
    - Oracle
    - SQLite
    - SqlServer
   
当然，SQLite 也是 POC 的不错选择。

## Example 演示

下面以 Redis 和 SQLite JDBC 为例。

> 使用 [Docker Compose 启动 Redis 服务](../docker-compose/redis)。
> 如果使用对应的 JDBC Memory 实现，需要在 pom 中引入对应的数据库驱动依赖。

在体验示例之前，确保代码中的 SQLite 和 Redis 数据库连接参数正确且数据库可以正常对外提供服务。

在一轮问答中，您应该得看到这样的回复：
参考[chat-memory.http](chat-memory.http)

```shell

### 内存API
GET http://localhost:8080/advisor/memory/in/call?query=请记住你是影子&conversation_id=yingzi
### 信息
GET http://localhost:8080/advisor/memory/in/messages?conversation_id=yingzi

### SQLite Memory API
GET http://localhost:8080/advisor/memory/sqlite/call?query=你好，我的外号是影子，请记住呀&conversation_id=yingzi
### 信息
GET http://localhost:8080/advisor/memory/sqlite/messages?conversation_id=yingzi

### Mysql Memory API
GET http://localhost:8080/advisor/memory/mysql/call?query=你好，我的外号是影子，请记住呀&conversation_id=yingzi
### 信息
http://localhost:8080/advisor/memory/mysql/messages?conversation_id=yingzi

### Redis Memory API
GET http://localhost:8080/advisor/memory/redis/call?query=你好，我的外号是影子，请记住呀&conversation_id=yingzi
### 信息
GET http://localhost:8080/advisor/memory/redis/messages?conversation_id=yingzi
```


MATCH (n)
DETACH DELETE n