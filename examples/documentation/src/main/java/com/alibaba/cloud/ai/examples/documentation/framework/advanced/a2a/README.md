# Spring AI Alibaba A2A 一体化演示（注册 -> 发现 -> 调用）

本示例演示如何在一个应用中完成：
- 本地 ReactAgent 的创建与对外暴露（A2A Server 自动导出）
- **使用 Nacos Registry 进行 Agent 注册**
- 使用 Nacos Discovery 进行 Agent 发现
- 通过 AgentCardProvider 发现并以 A2aRemoteAgent 方式调用

## 架构说明

### A2A 注册与发现流程

1. **本地 Agent 创建**：`A2AAgentConfig` 创建 ReactAgent Bean
2. **A2A Server 自动暴露**：Spring Boot 启动时，A2A Server AutoConfiguration 自动：
   - 根据 ReactAgent Bean 生成 AgentCard
   - 暴露 REST API 端点（`/.well-known/agent.json` 和 `/a2a/message`）
3. **Nacos Registry 注册**：配置 `registry.enabled: true` 后：
   - 自动将 AgentCard 注册到 Nacos A2A 服务注册表
   - 其他服务可通过 Nacos 发现此 Agent
4. **Nacos Discovery 发现**：配置 `discovery.enabled: true` 后：
   - AgentCardProvider 可从 Nacos 查询已注册的 Agent
   - 构造 A2aRemoteAgent 进行远程调用

## 目录
- `A2AAgentConfig.java`：定义本地 ReactAgent（data_analysis_agent）
- `A2AExample.java`：一体化演示入口，完成本地调用、发现、远程调用
- `A2AExampleController.java`：提供 `/api/a2a/demo` HTTP 入口
- `application.yml`：**A2A server、Nacos Registry 和 Nacos Discovery 配置**

## 配置说明

### 关键配置项（application.yml）

```yaml
spring:
  ai:
    alibaba:
      a2a:
        nacos:
          server-addr: 127.0.0.1:8848
          username: nacos
          password: nacos
          discovery:
            enabled: true   # 启用服务发现（查询其他 Agent）
          registry:
            enabled: true   # 启用服务注册（注册本地 Agent）
        server:
          version: 1.0.0
          card:
            name: data_analysis_agent
            description: 专门用于数据分析和统计计算的本地智能体
            provider:
              name: Spring AI Alibaba Documentation
              organization: Spring AI Alibaba
```

**重要**：
- `registry.enabled: true` - 必须启用才能将 Agent 注册到 Nacos
- `discovery.enabled: true` - 启用后才能通过 AgentCardProvider 发现其他 Agent
- `server.card` - 定义注册到 Nacos 的 AgentCard 元数据

## 运行

### 1) 准备环境

```bash
export DASHSCOPE_API_KEY=your-api-key
export NACOS_SERVER_ADDR=127.0.0.1:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
```

确保本地有可用的 Nacos（可用 Docker 启动）：
```bash
docker run --name nacos -d -p 8848:8848 -e MODE=standalone nacos/nacos-server:latest
```

### 2) 启动应用

```bash
mvn -q -pl examples/documentation -am spring-boot:run
```

### 3) 访问演示接口

```bash
curl http://localhost:8080/api/a2a/demo
```

### 4) 验证注册和发现

**本地 AgentCard**：
```bash
curl http://localhost:8080/.well-known/agent.json
```

**Nacos 控制台**：
- 打开 http://localhost:8848/nacos
- 登录（nacos/nacos）
- 查看 A2A 服务注册维度，应该能看到 `data_analysis_agent`

**通过 API 查询 Nacos 中的 Agent**：
```bash
# 需要 Nacos A2A API（如果启用）
curl "http://localhost:8848/nacos/v1/ai/a2a/agent?agentName=data_analysis_agent"
```

## 关键代码

### 注册逻辑（自动）

1. **定义 ReactAgent Bean**（`A2AAgentConfig.java`）：
```java
@Bean(name = "dataAnalysisAgent")
public ReactAgent dataAnalysisAgent(@Qualifier("dashscopeChatModel") ChatModel chatModel) {
    return ReactAgent.builder()
            .name("data_analysis_agent")
            .model(chatModel)
            .description("专门用于数据分析和统计计算的本地智能体")
            .instruction("你是一个专业的数据分析专家...")
            .outputKey("messages")
            .build();
}
```

2. **配置 A2A Server 和 Nacos Registry**（`application.yml`）：
   - `spring.ai.alibaba.a2a.server.card` - 定义 AgentCard
   - `spring.ai.alibaba.a2a.nacos.registry.enabled: true` - 启用注册

3. **自动注册流程**：
   - Spring Boot 启动 → A2A Server AutoConfiguration 检测到 ReactAgent Bean
   - 生成 AgentCard → 注册到 Nacos A2A Registry
   - 暴露 REST API 端点

### 发现与调用逻辑（`A2AExample.java`）

```java
// 1. 通过 AgentCardProvider 发现 Agent
A2aRemoteAgent remote = A2aRemoteAgent.builder()
        .name("data_analysis_agent")
        .agentCardProvider(agentCardProvider)  // 自动从 Nacos 获取 AgentCard
        .description("数据分析远程代理")
        .build();

// 2. 远程调用
Optional<OverAllState> result = remote.invoke("请根据季度数据给出同比与环比分析概要。");
```

## 注册与发现的区别

| 功能 | 配置项 | 作用 | 本示例中的角色 |
|------|--------|------|----------------|
| **Registry（注册）** | `registry.enabled: true` | 将本地 Agent 注册到 Nacos | 本应用作为 **服务提供者** |
| **Discovery（发现）** | `discovery.enabled: true` | 从 Nacos 查询其他 Agent | 本应用作为 **服务消费者** |

本示例同时启用了两者，因此：
- 作为**提供者**：注册 `data_analysis_agent` 到 Nacos
- 作为**消费者**：可发现并调用其他已注册的 Agent（包括自己）

## 注意事项

1. **依赖要求**：
   - 需要添加 `spring-ai-alibaba-starter-a2a-nacos` 依赖
   - 确保 Nacos 服务正常运行

2. **Registry vs Discovery**：
   - `registry.enabled: true` - 注册本地 Agent
   - `discovery.enabled: true` - 发现远程 Agent
   - 两者可独立配置，也可同时启用

3. **多 Agent 注册**：
   - 默认情况下，只有一个 Agent Bean 会被注册
   - 如需注册多个 Agent，需运行多个应用实例，每个实例配置不同的 Agent

4. **AgentCard 元数据**：
   - `server.card.name` 必须与 ReactAgent Bean 的 `name` 一致
   - `server.card.provider` 可选，用于标识 Agent 提供者信息

## 故障排查

### Agent 没有注册到 Nacos
- 检查 `registry.enabled: true` 是否配置
- 查看应用日志，确认 Nacos Registry AutoConfiguration 是否生效
- 验证 Nacos 连接配置（server-addr、username、password）

### AgentCardProvider 无法发现 Agent
- 检查 `discovery.enabled: true` 是否配置
- 确认 Agent 已成功注册到 Nacos
- 验证 agent name 是否匹配

### 远程调用失败
- 确认目标 Agent 的 REST API 端点可访问
- 检查网络连接和防火墙配置
- 查看 A2A 消息传输日志
