# ToolSelectionInterceptor 工具选择拦截器使用指南

## 概述

`ToolSelectionInterceptor` 是 Spring AI Alibaba Agent Framework 中的一个模型拦截器，用于在 Agent 拥有大量工具时，通过 LLM 智能选择最相关的工具。这可以减少 token 使用量，并帮助主模型专注于正确的工具。

### 增强功能：工具描述支持

在最新版本中，`ToolSelectionInterceptor` 增加了对**工具描述**的支持。当进行工具选择时，不仅会传递工具名称，还会传递工具描述给选择模型，从而大幅提高工具选择的准确性。

**改进前（仅工具名称）：**
```
- searchProducts
- getOrderDetails
- updateInventory
```

**改进后（包含工具描述）：**
```
- searchProducts: 按名称、类别或价格范围搜索产品
- getOrderDetails: 根据订单ID获取订单详情
- updateInventory: 更新产品库存数量
```

---

## 快速开始

### 1. 基本用法

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

// 创建工具选择拦截器
ToolSelectionInterceptor toolSelectionInterceptor = ToolSelectionInterceptor.builder()
    .selectionModel(chatModel)  // 用于选择工具的模型
    .maxTools(3)                // 每次最多选择3个工具
    .build();

// 创建 ReactAgent 并配置拦截器
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .tools(weatherTool, ticketTool, hotelTool, mapTool, translatorTool)
    .interceptors(toolSelectionInterceptor)
    .build();

// 调用 Agent
Optional<OverAllState> result = agent.invoke("帮我查询北京的天气");
```

### 2. 使用 alwaysInclude 确保关键工具始终可用

```java
ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
    .selectionModel(chatModel)
    .maxTools(3)
    .alwaysInclude("error_handler", "logging_tool")  // 这些工具始终包含
    .build();
```

### 3. 自定义系统提示词

```java
ToolSelectionInterceptor interceptor = ToolSelectionInterceptor.builder()
    .selectionModel(chatModel)
    .maxTools(3)
    .systemPrompt("你是一个工具选择专家，请根据用户查询选择最相关的工具。优先选择能直接解决用户问题的工具。")
    .build();
```

---

## 工具定义最佳实践

为了让 `ToolSelectionInterceptor` 更准确地选择工具，请遵循以下最佳实践：

### 1. 清晰的工具名称

```java
// 好的命名
@Tool(name = "searchProducts")
@Tool(name = "getUserProfile")
@Tool(name = "sendEmailNotification")

// 不好的命名
@Tool(name = "search")      // 太模糊
@Tool(name = "tool1")       // 无意义
@Tool(name = "doSomething") // 不明确
```

### 2. 详细的工具描述

```java
// 好的描述
@Tool(name = "searchProducts",
      description = "按名称、类别或价格范围搜索产品。" +
                   "返回匹配产品的列表，包含名称、价格和库存信息。" +
                   "适用于用户想要查找或浏览产品的场景。")

// 不好的描述
@Tool(name = "search", description = "搜索")  // 太简短，缺乏上下文
```

### 3. 参数描述

```java
@Tool(name = "searchProducts",
      description = "按条件搜索产品")
public List<Product> searchProducts(
    @ToolParam(description = "产品名称关键词，支持模糊匹配") String keyword,
    @ToolParam(description = "产品类别，如'电子产品'、'服装'") String category,
    @ToolParam(description = "价格范围上限") Double maxPrice) {
    // implementation
}
```

---

## 配置参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `selectionModel` | ChatModel | 是 | 用于选择工具的 LLM 模型 |
| `maxTools` | Integer | 否 | 每次最多选择的工具数量。如果工具总数 <= maxTools，则跳过选择 |
| `systemPrompt` | String | 否 | 自定义系统提示词，默认为 "Your goal is to select the most relevant tools for answering the user's query." |
| `alwaysInclude` | Set<String> | 否 | 始终包含的工具名称列表，不受 maxTools 限制 |

---

## 工作原理

### 数据流程

```
1. Agent 接收用户查询
      ↓
2. AgentLlmNode 提取所有工具的名称和描述
      ↓
3. 构建 ModelRequest，包含：
   - tools: List<String> (工具名称列表)
   - toolDescriptions: Map<String, String> (工具名称→描述映射)
      ↓
4. ToolSelectionInterceptor 检查工具数量
   - 如果 tools.size() <= maxTools，跳过选择，直接传递
   - 否则，调用选择模型
      ↓
5. 构建选择提示词，格式为：
   "- toolName1: description1
    - toolName2: description2
    ..."
      ↓
6. 选择模型返回 JSON: {"tools": ["tool1", "tool2"]}
      ↓
7. 过滤工具列表，只保留选中的工具
      ↓
8. 将过滤后的工具传递给主模型
```

### 选择逻辑

```java
// 伪代码
if (availableTools.size() <= maxTools) {
    // 工具数量不超过限制，跳过选择
    return handler.call(request);
}

// 构建工具列表（包含描述）
StringBuilder toolList = new StringBuilder();
for (String toolName : toolNames) {
    toolList.append("- ").append(toolName);
    String description = toolDescriptions.get(toolName);
    if (description != null && !description.isEmpty()) {
        toolList.append(": ").append(description);
    }
    toolList.append("\n");
}

// 调用选择模型
Set<String> selectedTools = selectTools(toolList, userQuery);

// 过滤并传递
ModelRequest filteredRequest = ModelRequest.builder(request)
    .tools(selectedTools)
    .build();
return handler.call(filteredRequest);
```

---

## 常见问题

### Q1: 什么时候应该使用 ToolSelectionInterceptor？

当你的 Agent 有超过 5-10 个工具时，建议使用此拦截器。过多的工具会：
- 增加 token 消耗
- 降低主模型选择正确工具的准确性
- 增加响应延迟

### Q2: selectionModel 应该用什么模型？

建议使用轻量级、快速的模型（如 qwen-turbo），因为工具选择任务相对简单，不需要最强大的模型。

### Q3: maxTools 应该设置多少？

- 简单任务：2-3 个
- 中等复杂任务：3-5 个
- 复杂任务：5-7 个

### Q4: 工具描述为空会怎样？

如果某个工具没有描述，只会显示工具名称，不会导致错误。但建议为所有工具添加描述以提高选择准确性。

### Q5: 如何调试工具选择？

启用 DEBUG 日志级别，可以看到选择了哪些工具：

```properties
logging.level.com.alibaba.cloud.ai.graph.agent.interceptor.toolselection=DEBUG
```

日志示例：
```
INFO  ToolSelectionInterceptor - Selected 2 tools from 5 available: [weather_tool, map_tool]
```

---

## 目录

- `ToolSelectionExample.java`：完整示例代码，包含基础用法、alwaysInclude、自定义提示词、多工具场景

---

## 运行示例

### 1) 准备环境

```bash
export AI_DASHSCOPE_API_KEY=your-api-key
```

### 2) 运行示例

```bash
mvn -q -pl examples/documentation -am exec:java \
  -Dexec.mainClass="com.alibaba.cloud.ai.examples.documentation.framework.advanced.toolselection.ToolSelectionExample"
```

---

## 相关类

| 类名 | 路径 | 说明 |
|------|------|------|
| `ToolSelectionInterceptor` | `agent-framework/.../interceptor/toolselection/ToolSelectionInterceptor.java` | 工具选择拦截器 |
| `ModelRequest` | `agent-framework/.../interceptor/ModelRequest.java` | 模型请求对象，包含工具名称和描述 |
| `ModelInterceptor` | `agent-framework/.../interceptor/ModelInterceptor.java` | 拦截器基类 |
| `AgentLlmNode` | `agent-framework/.../node/AgentLlmNode.java` | LLM 节点，负责提取工具信息 |

---

## 版本历史

| 版本 | 变更 |
|------|------|
| 1.1.0.0-RC2 | 新增 `toolDescriptions` 支持，提高工具选择准确性 |

---

*文档更新日期: 2025-12-17*
