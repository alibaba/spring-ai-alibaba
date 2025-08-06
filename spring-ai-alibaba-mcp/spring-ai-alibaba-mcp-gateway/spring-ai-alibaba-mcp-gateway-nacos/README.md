# Spring AI Alibaba MCP Gateway Nacos

这个模块提供了基于 Nacos 的 MCP（Model Context Protocol）网关功能，支持多种协议的工具调用。

## 支持的协议

### 1. HTTP/HTTPS 协议

- 支持传统的 HTTP 和 HTTPS 协议
- 使用 JSON 模板进行请求和响应处理
- 支持动态参数替换

### 2. MCP SSE 协议

- 支持 Server-Sent Events (SSE) 协议
- 使用 MCP Java SDK v0.11.0 的 SSE 传输层
- 支持实时流式通信

### 3. MCP Streamable HTTP 协议 (新增)

- 支持 MCP Streamable HTTP 协议
- 基于 MCP Java SDK v0.11.0 的新功能
- 使用协议版本 `2025-06-18`
- 支持双向流式通信

## 新功能：Streamable HTTP 支持

### 特性

- **协议版本支持**: 使用 MCP 协议版本 `2025-06-18`
- **双向通信**: 支持客户端和服务器之间的双向流式通信
- **自动资源管理**: 自动处理连接的建立和清理
- **错误处理**: 完善的异常处理和错误报告

### 配置

Streamable HTTP 协议使用以下配置：

```yaml
# 协议类型
protocol: mcp-streamable

# 导出路径（可选，默认为 /streamable）
exportPath: /streamable

# 服务器配置
remoteServerConfig:
  serviceRef:
    serviceName: your-service-name
```

### 使用示例

```java
// 工具定义
McpGatewayToolDefinition toolDefinition = new NacosMcpGatewayToolDefinition();
toolDefinition.setProtocol("mcp-streamable");

// 创建回调
NacosMcpGatewayToolCallback callback = new NacosMcpGatewayToolCallback(toolDefinition);

// 调用工具
Map<String, Object> args = new HashMap<>();
args.put("param1", "value1");
String result = callback.call("input", toolContext);
```

## 依赖要求

- MCP Java SDK v0.11.0+
- Spring WebFlux
- Nacos Client
- Spring AI

## 更新日志

### v1.0.0.3-SNAPSHOT

- 新增 MCP Streamable HTTP 协议支持
- 基于 MCP Java SDK v0.11.0
- 添加协议版本头支持
- 改进错误处理和日志记录

## 技术细节

### Streamable HTTP 实现

- 使用 `HttpClientStreamableHttpTransport` 作为专用传输层
- 配置 `MCP-Protocol-Version: 2025-06-18` 头
- 支持自定义端点路径
- 自动处理连接生命周期
- 基于 WebClient 的响应式实现

### 协议兼容性

- 向后兼容现有的 SSE 协议
- 支持新的 streamable 协议
- 保持与现有 HTTP/HTTPS 协议的兼容性
