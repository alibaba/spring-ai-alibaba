# Spring AI Alibaba MCP 服务器

这是一个基于 MCP (Model Context Protocol) 0.11.0 的完整示例应用程序，使用 Spring Boot 和 WebFlux 实现。

## 功能特性

- ✅ 完整的 MCP 0.11.0 协议支持
- ✅ WebFlux 响应式编程
- ✅ 多种工具回调实现
- ✅ 流式响应支持
- ✅ 错误处理和日志
- ✅ JSON-RPC 2.0 协议
- ✅ 数据库操作工具集成

## 可用工具

### 1. 计算器 (calculator)
执行数学计算，支持基本的算术运算。

**参数:**
- `expression` (string): 要计算的数学表达式

**示例:**
```json
{
  "expression": "2 + 3 * 4"
}
```

### 2. 回显 (echo)
回显输入的消息，用于测试和调试。

**参数:**
- `message` (string): 要回显的消息

**示例:**
```json
{
  "message": "Hello World"
}
```

### 3. Ping (ping)
测试服务器连接状态。

**参数:** 无

**示例:**
```json
{}
```

### 4. 天气 (weather)
获取指定城市的天气信息。

**参数:**
- `city` (string): 城市名称

**示例:**
```json
{
  "city": "北京"
}
```

### 5. 翻译 (translate)
翻译文本内容。

**参数:**
- `text` (string): 要翻译的文本
- `target_language` (string): 目标语言 (en, zh, ja, ko, fr, de, es)

**示例:**
```json
{
  "text": "你好",
  "target_language": "en"
}
```

### 6. 日期时间 (datetime)
获取当前日期时间信息。

**参数:**
- `format` (string): 日期时间格式 (full, date, time, timestamp)

**示例:**
```json
{
  "format": "full"
}
```

### 7. 随机数 (random)
生成随机数。

**参数:**
- `min` (integer): 最小值
- `max` (integer): 最大值

**示例:**
```json
{
  "min": 1,
  "max": 100
}
```

### 8. 数据库 (database) 🆕
数据库操作工具，支持 SQL 执行、表结构查询、索引查询等。

**支持的操作:**

#### 8.1 执行 SQL (execute_sql)
**参数:**
- `action` (string): "execute_sql"
- `query` (string): SQL 语句
- `datasourceName` (string, 可选): 数据源名称

**示例:**
```json
{
  "action": "execute_sql",
  "query": "SELECT * FROM users LIMIT 10"
}
```

#### 8.2 获取表名 (get_table_name)
**参数:**
- `action` (string): "get_table_name"
- `text` (string): 中文表名或表描述
- `datasourceName` (string, 可选): 数据源名称

**示例:**
```json
{
  "action": "get_table_name",
  "text": "用户表"
}
```

#### 8.3 获取表索引 (get_table_index)
**参数:**
- `action` (string): "get_table_index"
- `text` (string): 表名
- `datasourceName` (string, 可选): 数据源名称

**示例:**
```json
{
  "action": "get_table_index",
  "text": "user"
}
```

#### 8.4 获取表元数据 (get_table_meta)
**参数:**
- `action` (string): "get_table_meta"
- `text` (string, 可选): 模糊搜索表描述，留空获取所有表
- `datasourceName` (string, 可选): 数据源名称

**示例:**
```json
{
  "action": "get_table_meta",
  "text": "用户"
}
```

#### 8.5 获取数据源信息 (get_datasource_info)
**参数:**
- `action` (string): "get_datasource_info"
- `datasourceName` (string, 可选): 数据源名称，留空获取所有可用数据源

**示例:**
```json
{
  "action": "get_datasource_info"
}
```

## 快速开始

### 1. 启动服务器

```bash
# 使用启动脚本
chmod +x run-mcp-server.sh
./run-mcp-server.sh

# 或者手动启动
mvn clean compile
java -cp target/classes com.alibaba.cloud.ai.example.manus.inhouse.WebFluxStreamableServerApplication
```

### 2. 测试服务器

```bash
# 使用测试脚本
chmod +x run-mcp-test.sh
./run-mcp-test.sh

# 或者手动测试
java -cp target/classes com.alibaba.cloud.ai.example.manus.inhouse.MCPClientTest
```

### 3. 测试数据库工具

```bash
# 使用数据库测试脚本
chmod +x run-mcp-database-test.sh
./run-mcp-database-test.sh

# 或者手动测试
java -cp target/classes com.alibaba.cloud.ai.example.manus.inhouse.MCPDatabaseTest
```

### 4. 手动测试

#### 获取工具列表
```bash
curl -X POST http://localhost:20881/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/list",
    "params": {}
  }'
```

#### 调用数据库工具
```bash
curl -X POST http://localhost:20881/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "2",
    "method": "tools/call",
    "params": {
      "name": "database",
      "arguments": {
        "action": "get_datasource_info"
      }
    }
  }'
```

## API 规范

### 请求格式

所有请求都使用 JSON-RPC 2.0 格式：

```json
{
  "jsonrpc": "2.0",
  "id": "请求ID",
  "method": "方法名",
  "params": {
    // 方法参数
  }
}
```

### 响应格式

成功响应：
```json
{
  "jsonrpc": "2.0",
  "id": "请求ID",
  "result": {
    // 结果数据
  }
}
```

错误响应：
```json
{
  "jsonrpc": "2.0",
  "id": "请求ID",
  "error": {
    "code": 错误代码,
    "message": "错误消息"
  }
}
```

### 支持的方法

1. `tools/list` - 获取可用工具列表
2. `tools/call` - 调用指定工具

## 开发指南

### 添加新工具

1. 创建工具创建方法：

```java
private McpServerFeatures.SyncToolSpecification createMyTool() {
    return McpServerFeatures.SyncToolSpecification.builder()
        .tool(new Tool("my_tool", "工具描述", createMyToolSchema()))
        .callHandler((exchange, request) -> {
            try {
                // 实现工具逻辑
                String result = "工具结果";
                return new CallToolResult(List.of(new McpSchema.TextContent(result)), null);
            } catch (Exception e) {
                return new CallToolResult(List.of(new McpSchema.TextContent("错误: " + e.getMessage())), null);
            }
        })
        .build();
}
```

2. 创建 JSON Schema：

```java
private String createMyToolSchema() {
    return """
        {
        "$schema": "http://json-schema.org/draft-07/schema#",
        "type": "object",
        "properties": {
            "param1": {
                "type": "string",
                "description": "参数描述"
            }
        },
        "required": ["param1"]
        }
        """;
}
```

3. 在工具列表中添加新工具：

```java
List<McpServerFeatures.SyncToolSpecification> tools = List.of(
    // ... 其他工具
    createMyTool()
);
```

### 扩展功能

- 添加流式响应支持
- 实现工具参数验证
- 添加认证和授权
- 支持工具链调用
- 添加监控和日志
- 支持异步工具调用

## 故障排除

### 常见问题

1. **端口被占用**
   - 修改 `WebFluxStreamableServerApplication.java` 中的 `PORT` 常量

2. **工具调用失败**
   - 检查请求格式是否正确
   - 查看服务器日志获取详细错误信息

3. **JSON 解析错误**
   - 确保请求体是有效的 JSON 格式
   - 检查参数类型是否匹配

4. **编译错误**
   - 确保 MCP 0.11.0 依赖已正确配置
   - 检查 Java 版本是否为 17 或更高

5. **数据库工具不可用**
   - 确保 `DatabaseUseTool` 被 Spring 容器管理
   - 检查数据源配置是否正确
   - 查看数据库连接状态

### 日志配置

可以通过修改代码添加更详细的日志：

```java
// 在工具调用中添加日志
System.out.println("调用工具: " + request.name());
System.out.println("参数: " + request.arguments());
```

## 技术栈

- **Java 17+**
- **MCP 0.11.0 SDK**
- **Spring WebFlux**
- **Reactor Netty**
- **Jackson JSON**
- **Spring Boot**

## 许可证

Apache License 2.0 