# 百智云 MCP 限定工具研究示例（可选）

本示例是 `framework/tutorials/mcp` 下独立、可选的补充用例，演示：

```text
检索公开信息（websearch_search） -> 读取相关页面（web_scrape / web_extract） -> 根据工具返回内容作答
```

它复用现有的 Streamable HTTP MCP 客户端和 Spring AI `ToolCallback` 接入 `ReactAgent`，不新增 MCP 传输实现、不修改核心 API、不替换 `RemoteMcpToolsExample` 中的出行示例，也不在 `application.yml` 中添加默认服务配置。

百智云 MCP 是外部托管服务，需要用户自备 API Key（服务控制台：https://agent-toolkit.app.baizhi.cloud/ ，接入说明：https://github.com/chaitin/baizhi-agent-toolkit）；工具参数会发送到该服务，部分调用可能产生费用。本仓库开源的是示例与测试，不代表托管服务本身开源。

## 文件

| 文件 | 作用 |
| --- | --- |
| `BaizhiMcpResearchConfig.java` | 读取并校验服务地址、服务凭据和超时；缺 Key 时在任何网络请求前失败 |
| `BaizhiMcpResearchClient.java` | 建立 Streamable HTTP 会话、发现工具、按名称选择工具、持有回调并清理资源 |
| `TextOnlyMcpToolCallback.java` | 保留远端输入 schema 的回调；只接受文本结果，错误和非文本结果显式失败 |
| `BaizhiMcpResearchExample.java` | 用选中的工具构建 `ReactAgent` 并执行研究任务 |
| `BaizhiMcpResearchExampleController.java` | 按需触发示例的 HTTP 入口 |
| `src/test/.../LocalBaizhiMcpServer.java` | 测试专用的本地 Streamable HTTP MCP 服务（合成凭据） |
| `src/test/.../BaizhiMcpResearchClientTest.java` | 本地集成测试 |

## 配置

服务凭据与模型凭据是两套独立配置：

```bash
# MCP 服务：完整的 Streamable HTTP 端点（显式配置，无内置默认值）
export BAIZHI_MCP_URL=https://agent-toolkit.app.baizhi.cloud/mcp

# MCP 服务凭据：用户自己的百智云 API Key，仅作为 Bearer 头发往上述服务
export BAIZHI_API_KEY=<your-own-key>

# 模型凭据：与 MCP 服务凭据无关
export AI_DASHSCOPE_API_KEY=<your-dashscope-key>
```

可选超时（秒）：

```bash
export BAIZHI_MCP_CONNECT_TIMEOUT_SECONDS=30
export BAIZHI_MCP_REQUEST_TIMEOUT_SECONDS=120
```

触发示例：

```bash
curl -X POST http://localhost:8080/baizhiMcpResearch/limitedTools
```

说明：

- `BAIZHI_API_KEY` 不写入示例、测试、命令行参数或日志；仅由 HTTP 客户端作为 `Authorization: Bearer ...` 头发往 `BAIZHI_MCP_URL` 指定的源。
- 示例关闭 HTTP 重定向，避免凭据跟随重定向到其他源；非 loopback 地址必须使用 HTTPS（仅测试中的 `127.0.0.1` 允许 HTTP）。
- 缺少 `BAIZHI_API_KEY` 时在创建网络客户端之前失败，不会尝试连接服务。
- `AI_DASHSCOPE_API_KEY` 仅用于模型调用；两个 Key 不可互相替代。

## 工具选择与执行语义

1. 初始化 MCP 会话后调用 `listTools()` 真实发现工具。
2. 只要求并按顺序选择以下三个工具：`websearch_search`、`web_scrape`、`web_extract`。
3. 任一必需工具缺失时停止并报错，**不回退为全部工具**；最多注册三个工具。
4. 回调的 `ToolDefinition` 由 `McpToolUtils.createToolDefinition(...)` 基于发现的 `McpSchema.Tool` 构建，保留远端 `inputSchema`，不采用通用示例中的 `FunctionToolCallback.inputType(Map.class)`。
5. 结果语义：
   - 文本内容按实际返回内容拼接后返回；
   - MCP `isError=true` 或传输异常抛出 `ToolExecutionException`，不包装成成功字符串；
   - 非文本内容或仅 `structuredContent` 的结果显式报错，不静默丢弃。
6. 客户端通过 try-with-resources 关闭；重复关闭不会抛出业务异常。

工具返回的网页内容属于不可信输入，示例不会据此执行额外指令或披露凭据。

## 验证

```bash
JAVA_HOME=/path/to/jdk17 ./mvnw -f examples/documentation/pom.xml \
  -Dtest=BaizhiMcpResearchClientTest test
```

测试使用合成凭据和本地 Streamable HTTP MCP 服务，覆盖：初始化与 Bearer 头、工具发现与三工具选择、输入 schema 保留、真实工具回调调度、缺少 Key 时联网前失败、错误 Key、必需工具缺失不回落、调用超时、MCP 错误结果、非文本结果和客户端清理。

依赖版本（`examples/documentation/pom.xml` 当前声明）：

- JDK 17
- Spring Boot 3.5.7
- Spring AI 1.1.2
- Spring AI Alibaba 1.1.2.2
- MCP Java SDK 0.17.0（由 `spring-ai-mcp` 1.1.2 引入）

### 验证边界

本地测试只证明示例自身的配置校验、工具选择、schema 贯通、错误语义和清理行为，**不**构成对百智云托管服务或生产环境的兼容性证明。真实百智云服务和真实模型调用尚未执行；需要用户自己的 Key，并按服务控制台的实际工具目录、权限和计费规则另行验收。若服务返回的工具名、输入 schema 或非文本内容与示例假设不同，应按实际发现结果调整，而不是放开为全部工具。
