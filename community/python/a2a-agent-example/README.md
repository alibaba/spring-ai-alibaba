# Python A2A Agent Example

本示例演示如何创建一个 Python A2A Agent，并通过 Nacos 3.x 注册，使其可被 Spring AI Alibaba (SAA) 服务发现和调用。

## 架构说明

```
┌─────────────────────┐     A2A Protocol      ┌─────────────────────┐
│  SAA Service        │◄────(JSON-RPC)────────►│  Python Agent       │
│  (Java/Spring Boot) │                        │  (FastAPI)          │
│                     │                        │                     │
│  - AgentCardProvider│                        │  - /a2a endpoint    │
│  - A2aRemoteAgent   │                        │  - /.well-known/    │
└─────────┬───────────┘                        └─────────┬───────────┘
          │                                              │
          │  Subscribe                          Release  │
          │  getAgentCard()                   AgentCard  │
          ▼                                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          Nacos 3.x                                  │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  A2A Registry (com.alibaba.nacos.api.ai.A2aService)            │ │
│  │  - AgentCard metadata (name, url, capabilities, skills)        │ │
│  │  - AgentEndpoint runtime info (address, port, transport)       │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

## 目录结构

```
a2a-agent-example/
├── README.md                 # 本文档
├── requirements.txt          # Python 依赖
├── .env.example             # 环境变量示例
├── main.py                  # Python Agent 入口
├── a2a_server.py            # A2A 协议服务端实现
├── nacos_a2a.py             # Nacos A2A 注册模块
└── saa-caller-example/      # Java SAA 调用示例
    ├── pom.xml
    └── src/main/java/...
```

## 快速开始

### 前置条件

1. **Nacos 3.x** - A2A 功能需要 Nacos 3.x 版本
2. **Python 3.10+** - Python 运行环境
3. **Java 17+** - 运行 SAA 示例（可选）
4. **DashScope API Key** - 或其他 OpenAI 兼容的 LLM API

### 1. 启动 Nacos

推荐使用 Docker Compose 启动 Nacos 3.x，参考 [nacos-docker 官方文档](https://github.com/nacos-group/nacos-docker/blob/master/README_ZH.md)。

#### 方式一：Docker Compose（推荐）

```bash
# 克隆 nacos-docker 仓库
git clone https://github.com/nacos-group/nacos-docker.git
cd nacos-docker

# 使用单机模式启动（Derby 内置数据库）
docker-compose -f example/standalone-derby.yaml up -d

# 或使用 MySQL 作为存储
docker-compose -f example/standalone-mysql-8.yaml up -d
```

#### 方式二：Docker 命令行

```bash
# 使用 Docker 启动 Nacos 3.x（启用认证）
docker run -d \
  --name nacos \
  -p 8848:8848 \
  -p 9848:9848 \
  -p 8080:8080 \
  -e MODE=standalone \
  -e NACOS_AUTH_ENABLE=true \
  -e NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789 \
  -e NACOS_AUTH_IDENTITY_KEY=serverIdentity \
  -e NACOS_AUTH_IDENTITY_VALUE=security \
  nacos/nacos-server:v3.0.0
```

#### Nacos 端口说明

| 端口 | 说明 |
|------|------|
| `8848` | 主端口（客户端连接、OpenAPI） |
| `9848` | gRPC 端口（客户端与服务端通信） |
| `8080` | 控制台端口（Web UI） |

#### Nacos 认证环境变量说明

| 变量 | 说明 | 示例值 |
|------|------|--------|
| `MODE` | 运行模式 | `standalone` 或 `cluster` |
| `NACOS_AUTH_ENABLE` | 是否启用认证 | `true` |
| `NACOS_AUTH_TOKEN` | 认证密钥（Base64，≥32字符） | `SecretKey0123456789...` |
| `NACOS_AUTH_IDENTITY_KEY` | 身份标识键 | `serverIdentity` |
| `NACOS_AUTH_IDENTITY_VALUE` | 身份标识值 | `security` |
| `NACOS_AUTH_TOKEN_EXPIRE_SECONDS` | Token 过期时间（秒） | `18000` |

> **注意**:
> - 生产环境请替换 `NACOS_AUTH_TOKEN` 为自己的密钥
> - A2A 功能需要 Nacos 3.x 版本，请确保使用 `nacos/nacos-server:v3.0.0` 或更高版本
> - 更多配置请参考 [nacos-docker 官方文档](https://github.com/nacos-group/nacos-docker/blob/master/README_ZH.md)

### 2. 启动 Python Agent

```bash
# 进入示例目录
cd community/python/a2a-agent-example

# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Linux/Mac
# 或 venv\Scripts\activate  # Windows

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env，填入你的 API Key 和 Nacos 地址

# 启动 Agent
python main.py
```

Agent 启动后会：
1. 在 `http://127.0.0.1:8000` 暴露 A2A 端点
2. 自动注册到 Nacos（如果 `NACOS_ENABLED=true`）

### 3. 验证 Python Agent

**验证 Python Agent 正常运行**

```bash
# AgentCard 端点
curl http://127.0.0.1:8000/.well-known/agent.json

# 健康检查
curl http://127.0.0.1:8000/health

# 直接调用 Python Agent（A2A 协议）
curl -X POST http://127.0.0.1:8000/a2a \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "message/send",
    "params": {
      "message": {
        "kind": "message",
        "messageId": "msg-1",
        "role": "user",
        "parts": [{"kind": "text", "text": "Hello, how are you?"}]
      }
    }
  }'
```

### 4. 从 SAA 调用 Python Agent

```bash
# 进入 SAA 示例目录
cd saa-caller-example

# 启动 SAA 应用（指定端口和 Nacos 认证信息）
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --spring.ai.alibaba.a2a.nacos.username=nacos --spring.ai.alibaba.a2a.nacos.password=nacos"
```

或者通过环境变量配置：

```bash
export NACOS_SERVER_ADDR=127.0.0.1:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos

mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### 5. 验证 SAA 调用

**验证 SAA 能发现并调用 Python Agent**

```bash
# 调用翻译接口（GET）
curl "http://localhost:8081/api/translate?text=Hello%20World"

# 或 POST 方式
curl -X POST http://localhost:8081/api/translate \
  -H "Content-Type: application/json" \
  -d '{"text": "人工智能正在改变世界"}'

# 流式调用
curl "http://localhost:8081/api/translate/streaming?text=Good%20morning"
```

**验证 Nacos 注册**

访问 Nacos 控制台：http://127.0.0.1:8080/nacos

1. 使用默认账号登录：`nacos` / `nacos`
2. 进入 **Agent管理** → **Agent列表**
3. 应能看到 `python-translator-agent` 已注册

## 配置说明

### Python Agent 配置 (.env)

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `AGENT_NAME` | Agent 名称，用于 Nacos 注册和发现 | `python-translator-agent` |
| `AGENT_HOST` | Agent 服务地址 | `127.0.0.1` |
| `AGENT_PORT` | Agent 服务端口 | `8000` |
| `NACOS_SERVER_ADDR` | Nacos 服务器地址 | `127.0.0.1:8848` |
| `NACOS_NAMESPACE` | Nacos 命名空间 | `public` |
| `NACOS_ENABLED` | 是否启用 Nacos 注册 | `true` |
| `DASHSCOPE_API_KEY` | DashScope API Key | - |
| `OPENAI_MODEL` | 使用的模型 | `qwen-plus` |

### SAA 配置 (application.yml)

```yaml
spring:
  ai:
    alibaba:
      a2a:
        nacos:
          server-addr: 127.0.0.1:8848
          discovery:
            enabled: true  # 启用服务发现
          registry:
            enabled: false # 此应用仅作为消费者

python:
  agent:
    name: python-translator-agent  # 要发现的 Python Agent 名称
```

## A2A 协议说明

### AgentCard 格式

```json
{
  "protocolVersion": "0.2.5",
  "name": "python-translator-agent",
  "description": "Python 翻译 Agent",
  "version": "1.0.0",
  "url": "http://127.0.0.1:8000/a2a",
  "preferredTransport": "JSONRPC",
  "capabilities": {
    "streaming": true,
    "pushNotifications": false,
    "stateTransitionHistory": false
  },
  "defaultInputModes": ["text/plain"],
  "defaultOutputModes": ["text/plain"],
  "skills": [...]
}
```

### JSON-RPC 消息格式

**请求 (message/send)**:
```json
{
  "jsonrpc": "2.0",
  "id": "request-id",
  "method": "message/send",
  "params": {
    "message": {
      "kind": "message",
      "messageId": "msg-id",
      "role": "user",
      "parts": [{"kind": "text", "text": "用户输入"}]
    },
    "metadata": {
      "threadId": "thread-id",
      "userId": "user-id"
    }
  }
}
```

**流式响应 (message/stream)**:
```
data: {"jsonrpc":"2.0","id":"...","result":{"kind":"status-update","status":{"state":"working"}}}

data: {"jsonrpc":"2.0","id":"...","result":{"kind":"artifact-update","artifact":{"parts":[{"kind":"text","text":"响应内容"}]}}}

data: {"jsonrpc":"2.0","id":"...","result":{"kind":"status-update","status":{"state":"completed"}}}
```

## Nacos 注册说明

Python Agent 使用 Nacos 3.x Admin API 进行注册：

```
POST /nacos/v3/admin/ai/a2a
Content-Type: application/x-www-form-urlencoded

namespaceId=public
agentName=python-translator-agent
registrationType=URL
agentCard={"name":"...", "url":"...", ...}
```

SAA 通过 `NacosAgentCardProvider` 发现 Agent：

```java
// 从 Nacos 获取 AgentCard
AgentCardWrapper card = agentCardProvider.getAgentCard("python-translator-agent");

// 使用 AgentCard 构建远程调用
A2aRemoteAgent remote = A2aRemoteAgent.builder()
    .name("caller")
    .agentCard(card.getAgentCard())
    .build();
```

## 注意事项

1. **Nacos 版本**: 必须使用 **Nacos 3.x**，A2A API 是 3.x 新增功能

2. **网络可达性**: Python Agent 的 `url` 必须从 SAA 服务网络可达
   - K8s 环境建议使用 Service DNS
   - Docker 环境注意网络配置

3. **协议版本**: 当前使用 A2A 协议版本 `0.2.5`

4. **Transport**: `preferredTransport` 应设为 `JSONRPC`（SAA 默认）

## 扩展开发

### 自定义 Agent 逻辑

修改 `main.py` 中的 `translator_agent_handler` 函数：

```python
async def your_agent_handler(text: str, metadata: dict) -> str:
    """
    自定义 Agent 逻辑

    Args:
        text: 用户输入
        metadata: 请求元数据 (thread_id, user_id)

    Returns:
        Agent 响应
    """
    # 你的业务逻辑
    return "响应内容"
```

### 添加新技能

在 `agent_card` 定义中添加技能：

```python
agent_card = AgentCard(
    name="my-agent",
    skills=[
        AgentSkill(
            id="skill-1",
            name="技能名称",
            description="技能描述",
            examples=["示例1", "示例2"],
        ),
    ],
    # ...
)
```

## 相关链接

- [Spring AI Alibaba 文档](https://java2ai.com)
- [A2A 协议规范](https://github.com/a2a-protocol/a2a-spec)
- [Nacos 官方文档](https://nacos.io)
- [Nacos Docker 部署指南](https://github.com/nacos-group/nacos-docker/blob/master/README_ZH.md)

## License

Apache License 2.0
