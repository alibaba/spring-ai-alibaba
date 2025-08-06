# MCP 多 Endpoint 功能实现

## 概述

本项目实现了支持多个 endpoint 的 MCP 服务器，参考了 `WebFluxStreamableServerApplication` 的多 endpoint 逻辑。每个 endpoint 对应一组工具，实现了动态工具加载和分组管理。

## 核心特性

✅ **多 Endpoint 支持**: 每个 endpoint 对应独立的工具组
✅ **动态工具加载**: 通过 `CoordinatorService` 动态加载协调器工具
✅ **本地工具集成**: 支持本地注册的工具和远程协调器工具
✅ **路由合并**: 自动合并多个 endpoint 的路由函数
✅ **独立服务器管理**: 每个 endpoint 创建独立的 MCP 服务器实例
✅ **业务逻辑分离**: 工具 POJO 与业务逻辑分离，便于维护

## 架构设计

### 1. McpServerApplication

主要的 MCP 服务器应用类，负责：

- 启动多 endpoint MCP 服务器
- 集成 `CoordinatorService` 和 `McpToolRegistry`
- 合并多个路由函数
- 管理多个 MCP 服务器实例

### 2. CoordinatorService

协调器服务，负责：

- 从 Spring 容器获取 `CoordinatorTool` 实例
- 按 endpoint 分组工具
- 为工具设置不同的 endpoint 地址
- **处理所有业务逻辑**（计划执行、状态轮询、结果处理）
- 创建工具规范

### 3. CoordinatorTool (POJO)

简化的工具类，只包含：

- 工具基本信息（名称、描述、Schema）
- Endpoint 配置
- 不包含任何业务逻辑

### 4. CoordinatorResult

结果包装类，用于：

- 统一的结果格式
- 与 `CoordinatorService` 放在同一目录

### 5. 工具注册机制

支持两种工具注册方式：

1. **本地工具**: 通过 `McpToolRegistry` 注册的本地工具
2. **协调器工具**: 通过 `CoordinatorService` 加载的远程工具

## 重构后的架构优势

### 职责分离

```
CoordinatorTool (POJO)
├── 工具基本信息
├── Endpoint 配置
└── 无业务逻辑

CoordinatorService (业务逻辑)
├── 工具加载和管理
├── 计划执行逻辑
├── 状态轮询逻辑
├── 结果处理逻辑
└── 工具规范创建

CoordinatorResult (数据模型)
├── 统一的结果格式
└── 与 Service 同目录
```

### 代码组织

```
service/
├── CoordinatorService.java    # 业务逻辑
├── CoordinatorResult.java     # 结果模型
└── ...

tool/coordinator/
└── CoordinatorTool.java       # 工具 POJO
```

## 实现细节

### 多 Endpoint 创建流程

```java
// 1. 加载协调器工具
Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint = coordinatorService.loadCoordinatorTools();

// 2. 获取本地注册的工具
List<McpServerFeatures.SyncToolSpecification> localTools = mcpToolRegistry.discoverAndRegisterTools();

// 3. 创建合并的路由函数
RouterFunction<?> combinedRouter = createCombinedRouter(coordinatorToolsByEndpoint, localTools);
```

### 工具规范创建

```java
// 使用 CoordinatorService 创建工具规范
McpServerFeatures.SyncToolSpecification toolSpec = coordinatorService.createToolSpecification(tool);
```

### Endpoint 分组逻辑

- **协调器工具**: 按 `CoordinatorTool.getEndpoint()` 分组
- **本地工具**: 统一使用 `/mcp/tools` endpoint

### 服务器实例管理

每个 endpoint 创建独立的：
- `WebFluxStreamableServerTransportProvider`
- `McpServer.SyncSpecification`
- 路由函数

## 使用示例

### 1. 启动服务器

```java
@Component
public class McpServerApplication implements ApplicationListener<ApplicationReadyEvent> {
    // 自动启动多 endpoint MCP 服务器
}
```

### 2. 注册协调器工具

```java
@Component
@McpTool(name = "coordinator", description = "计划协调工具")
public class CoordinatorTool {
    private String endpoint;
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
}
```

### 3. 业务逻辑处理

```java
@Service
public class CoordinatorService {
    // 处理所有业务逻辑
    public CallToolResult invokeTool(CallToolRequest request) {
        // 计划执行逻辑
        // 状态轮询逻辑
        // 结果处理逻辑
    }
}
```

### 4. 访问不同的 Endpoint

- 协调器工具: `http://localhost:20881/mcp/coordinator-1`
- 本地工具: `http://localhost:20881/mcp/tools`

## 配置说明

### 端口配置

```java
private static final int PORT = 20881;
```

### Endpoint 命名规则

- 协调器工具: `/mcp/coordinator-{index}`
- 本地工具: `/mcp/tools`

## 扩展性

### 添加新的工具类型

1. 创建工具 POJO 类
2. 添加 `@McpTool` 注解
3. 在 `CoordinatorService` 中实现业务逻辑

### 自定义 Endpoint 策略

可以修改 `CoordinatorService.createExampleCoordinatorTools()` 方法来实现自定义的 endpoint 分配策略。

## 监控和日志

服务器启动时会输出：

```
JManus InHouse MCP 服务器
==========================================
启动 JManus InHouse MCP Server...
服务器将在端口 20881 上运行

协调器工具:
  Endpoint: /mcp/coordinator-1
    - coordinator: 计划协调工具 - 执行计划模板并返回结果

本地工具:
  - weather: 天气查询工具
  - file_processor: 文件处理工具
```

## 故障排除

### 常见问题

1. **工具未注册**: 检查 `@McpTool` 注解和 Spring 容器配置
2. **Endpoint 冲突**: 确保 endpoint 名称唯一
3. **依赖注入失败**: 检查 `CoordinatorTool` 的构造函数依赖
4. **业务逻辑错误**: 检查 `CoordinatorService` 中的业务逻辑

### 调试模式

启用详细日志：

```properties
logging.level.com.alibaba.cloud.ai.example.manus.inhouse.mcp=DEBUG
```

## 重构优势

### 1. 职责分离
- **CoordinatorTool**: 纯 POJO，只包含工具信息
- **CoordinatorService**: 处理所有业务逻辑
- **CoordinatorResult**: 统一的结果模型

### 2. 易于维护
- 业务逻辑集中在 Service 层
- 工具定义简洁明了
- 结果格式统一

### 3. 易于测试
- 可以独立测试业务逻辑
- 工具 POJO 易于模拟
- 结果模型清晰

### 4. 易于扩展
- 新增工具只需创建 POJO
- 业务逻辑复用性强
- 架构清晰，便于理解

## 未来改进

1. **动态 Endpoint 配置**: 支持配置文件定义 endpoint
2. **负载均衡**: 为多个 endpoint 添加负载均衡
3. **健康检查**: 为每个 endpoint 添加健康检查机制
4. **监控指标**: 添加详细的监控指标收集
5. **缓存机制**: 为工具规范添加缓存
6. **异步处理**: 支持异步工具调用 