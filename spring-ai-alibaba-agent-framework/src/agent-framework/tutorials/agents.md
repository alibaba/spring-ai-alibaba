---
title: Agents
description: 学习如何使用ReactAgent将语言模型与工具结合，创建能够推理任务、决定工具使用并迭代解决问题的智能系统
keywords: [Agents, ReactAgent, ReAct范式, 工具调用, 推理行动, LLM Agent, 智能代理]
---

# Agents

Agents 将大语言模型与工具结合，创建具备任务推理、工具使用决策、工具调用的自动化系统，系统具备持续推理、工具调用的循环迭代能力，直至问题解决。

Spring AI Alibaba 提供了基于 `ReactAgent` 的生产级 Agent 实现。

**一个 LLM Agent 在循环中通过运行工具来实现目标**。Agent 会一直运行直到满足停止条件 —— 即当模型输出最终答案或达到迭代限制时。

<!-- ![react-agent-architecture](../imgs/reactagent.png) -->

## ReactAgent 理论基础

### 什么是 ReAct

ReAct（Reasoning + Acting）是一种将推理和行动相结合的 Agent 范式。在这个范式中，Agent 会：

1. **思考（Reasoning）**：分析当前情况，决定下一步该做什么
2. **行动（Acting）**：执行工具调用或生成最终答案
3. **观察（Observation）**：接收工具执行的结果
4. **迭代**：基于观察结果继续思考和行动，直到完成任务

这个循环使 Agent 能够：
- 将复杂问题分解为多个步骤
- 动态调整策略基于中间结果
- 处理需要多次工具调用的任务
- 在不确定的环境中做出决策

### ReactAgent 的工作原理

Spring AI Alibaba 中的`ReactAgent` 基于 **Graph 运行时**构建。Graph 由节点（steps）和边（connections）组成，定义了 Agent 如何处理信息。Agent 在这个 Graph 中移动，执行如下节点：

- **Model Node (模型节点)**：调用 LLM 进行推理和决策
- **Tool Node (工具节点)**：执行工具调用
- **Hook Nodes (钩子节点)**：在关键位置插入自定义逻辑

ReactAgent 的核心执行流程：

<img src="/img/agent/agents/reactagent.png" alt="reactagent" width="360" />

## 核心组件

### Model（模型）

Model 是 Agent 的推理引擎。Spring AI Alibaba 支持多种配置方式。

#### 基础模型配置

最直接的方式是使用 `ChatModel` 实例：

```java
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

// 创建 DashScope API 实例
DashScopeApi dashScopeApi = DashScopeApi.builder()
    .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
    .build();

// 创建 ChatModel
ChatModel chatModel = DashScopeChatModel.builder()
    .dashScopeApi(dashScopeApi)
    .build();

// 创建 Agent
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .build();
```

#### 高级模型配置

通过 `ChatOptions` 可以精细控制模型行为：

```java
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

ChatModel chatModel = DashScopeChatModel.builder()
    .dashScopeApi(dashScopeApi)
    .defaultOptions(DashScopeChatOptions.builder()
        .temperature(0.7)      // 控制随机性
        .maxTokens(2000)       // 最大输出长度
        .topP(0.9)            // 核采样参数
        .build())
    .build();
```

**常用参数说明**：
- `temperature`：控制输出的随机性（0.0-1.0），值越高越有创造性
- `maxTokens`：限制单次响应的最大 token 数
- `topP`：核采样，控制输出的多样性
- 更多参数请参考 ChatModel 适配

### Tools（工具）

工具赋予 Agent 执行操作的能力，支持顺序执行、并行调用、动态选择和错误处理。

#### 定义和使用工具

```java
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import java.util.function.BiFunction;

// 定义工具
public class SearchTool implements BiFunction<String, ToolContext, String> {
    @Override
    public String apply(
        @ToolParam(description = "搜索关键词") String query,
        ToolContext toolContext) {
        return "搜索结果：" + query;
    }
}

// 创建工具回调
ToolCallback searchTool = FunctionToolCallback
    .builder("search", new SearchTool())
    .description("搜索信息的工具")
    .inputType(String.class)
    .build();

// 使用多个工具
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .tools(searchTool, weatherTool, calculatorTool)
    .build();
```

#### 工具错误处理

使用 `ToolInterceptor` 统一处理工具错误：

```java
public class ToolErrorInterceptor extends ToolInterceptor {
	@Override
	public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
		try {
			return handler.call(request);
		} catch (Exception e) {
			return ToolCallResponse.of(request.getToolCallId(), request.getToolName(),
					"Tool failed: " + e.getMessage());
		}
	}
}

ReactAgent agent = ReactAgent.builder()
    .interceptors(ToolErrorHandler.builder().build())
    .build();
```

**ReAct 循环示例**：Agent 自动交替进行推理和工具调用，直到获得最终答案。

```
用户: 查询杭州天气并推荐活动
→ [推理] 需要查天气 → [行动] get_weather("杭州") → [观察] 晴，25°C
→ [推理] 需要推荐活动 → [行动] search("户外活动") → [观察] 西湖游玩...
→ [推理] 信息充足 → [行动] 生成答案
```

### System Prompt（系统提示）

System Prompt 塑造 Agent 处理任务的方式。

#### 基础用法

通过 `systemPrompt` 参数提供字符串：

```java
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .systemPrompt("你是一个专业的技术助手。请准确、简洁地回答问题。")
    .build();
```

#### 使用 instruction

对于更详细的指令，使用 `instruction` 参数：

```java
String instruction = """
    你是一个经验丰富的软件架构师。

    在回答问题时，请：
    1. 首先理解用户的核心需求
    2. 分析可能的技术方案
    3. 提供清晰的建议和理由
    4. 如果需要更多信息，主动询问

    保持专业、友好的语气。
    """;

ReactAgent agent = ReactAgent.builder()
    .name("architect_agent")
    .model(chatModel)
    .instruction(instruction)
    .build();
```

#### 动态 System Prompt

使用 `ModelInterceptor` 实现基于上下文的动态提示：

```java
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

public class DynamicPromptInterceptor implements ModelInterceptor {
    @Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		// 基于用户角色动态调整提示
		List<Message> messages = request.getMessages();
		Map<String, Object> context = request.getContext();// this is the context set in RunnableConfig

		// do anything with messages to adjust prompt, history messages, user request dynamically

		// create modified request
		ModelRequest modifiedRequest = ModelRequest.builder()
				.messages(messages)
				.options(request.getOptions())
				.tools(request.getTools())
				.build();
		return handler.call(modifiedRequest);
	}
}

ReactAgent agent = ReactAgent.builder()
    .name("adaptive_agent")
    .model(chatModel)
    .interceptors(new DynamicPromptInterceptor())
    .build();
```

## 调用 Agent

### 基础调用

使用 `call` 方法获取最终响应：

```java
import org.springframework.ai.chat.messages.AssistantMessage;

// 字符串输入
AssistantMessage response = agent.call("杭州的天气怎么样？");
System.out.println(response.getText());

// UserMessage 输入
UserMessage userMessage = new UserMessage("帮我分析这个问题");
AssistantMessage response = agent.call(userMessage);

// 多个消息
List<Message> messages = List.of(
    new UserMessage("我想了解 Java 多线程"),
    new UserMessage("特别是线程池的使用")
);
AssistantMessage response = agent.call(messages);
```

### 获取完整状态

使用 `invoke` 方法获取完整的执行状态：

```java
import com.alibaba.cloud.ai.graph.OverAllState;
import java.util.Optional;

Optional<OverAllState> result = agent.invoke("帮我写一首诗");

if (result.isPresent()) {
    OverAllState state = result.get();

    // 访问消息历史
    Optional<Object> messages = state.value("messages");
    List<Message> messageList = (List<Message>) messages.get();

    // 访问自定义状态
    Optional<Object> customData = state.value("custom_key");

    System.out.println("完整状态：" + state);
}
```

### 使用配置

通过 `RunnableConfig` 传递运行时配置：

```java
import com.alibaba.cloud.ai.graph.RunnableConfig;

RunnableConfig runnableConfig = RunnableConfig.builder()
	.threadId(threadId)
	.addMetadata("key", "value")
	.build();

AssistantMessage response = agent.call("你的问题", config);
```

## 高级特性

### 结构化输出

在某些情况下，你可能希望 Agent 以特定格式返回输出。ReactAgent 提供了两种策略。

#### 使用 outputType

通过 Java 类定义输出结构，Agent 会自动生成对应的 JSON Schema：

```java
public class PoemOutput {
    private String title;
    private String content;
    private String style;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
}

ReactAgent agent = ReactAgent.builder()
    .name("poem_agent")
    .model(chatModel)
    .outputType(PoemOutput.class)
    .saver(new MemorySaver())
    .build();

AssistantMessage response = agent.call("写一首关于春天的诗");
// 输出会遵循 PoemOutput 的结构
System.out.println(response.getText());
```

#### 使用 outputSchema

直接提供 JSON Schema 字符串进行更灵活的控制：

```java
String customSchema = """
    请严格按照以下JSON格式返回结果：
    {
        "summary": "内容摘要",
        "keywords": ["关键词1", "关键词2", "关键词3"],
        "sentiment": "情感倾向（正面/负面/中性）",
        "confidence": 0.95
    }
    """;

ReactAgent agent = ReactAgent.builder()
    .name("analysis_agent")
    .model(chatModel)
    .outputSchema(customSchema)
    .saver(new MemorySaver())
    .build();

AssistantMessage response = agent.call("分析这段文本：春天来了，万物复苏。");
```

**选择建议**：
- `outputType`：类型安全，适合结构固定的场景
- `outputSchema`：灵活性高，适合动态或复杂的输出格式

### Memory（记忆）

Agent 通过状态自动维护对话历史。使用 `CompileConfig` 配置持久化存储。

```java
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

// 配置内存存储
ReactAgent agent = ReactAgent.builder()
    .name("chat_agent")
    .model(chatModel)
    .saver(new MemorySaver())
    .build();

// 使用 thread_id 维护对话上下文
RunnableConfig config = RunnableConfig.builder()
    .threadId("user_123")
    .build();

agent.call("我叫张三", config);
agent.call("我叫什么名字？", config);  // 输出: "你叫张三"
```

**生产环境**：使用 `RedisSaver`、`MongoSaver` 等持久化存储替代 `MemorySaver`。

### Hooks（钩子）

Hooks 允许在 Agent 执行的关键点插入自定义逻辑。

#### Hook 类型与使用

```java
import com.alibaba.cloud.ai.graph.agent.hook.*;

// 1. AgentHook - 在 Agent 开始/结束时执行，每次Agent调用只会运行一次
public class LoggingHook implements AgentHook {
    @Override
    public String getName() { return "logging"; }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{
            HookPosition.BEFORE_AGENT,
            HookPosition.AFTER_AGENT
        };
    }

    @Override
    public Map<String, Object> beforeAgent(OverAllState state, RunnableConfig config) {
        System.out.println("Agent 开始执行");
        return Map.of();
    }

    @Override
    public Map<String, Object> afterAgent(OverAllState state, RunnableConfig config) {
        System.out.println("Agent 执行完成");
        return Map.of();
    }
}

// 2. ModelHook - 在模型调用前后执行（例如：消息修剪），区别于AgentHook，ModelHook在一次agent调用中可能会调用多次，也就是每次 reasoning-acting 迭代都会执行
public class MessageTrimmingHook implements ModelHook {
    private static final int MAX_MESSAGES = 10;

    @Override
    public Map<String, Object> beforeModel(OverAllState state, RunnableConfig config) {
        Optional<Object> messagesOpt = state.value("messages");
        if (messagesOpt.isPresent()) {
            List<Message> messages = (List<Message>) messagesOpt.get();
            if (messages.size() > MAX_MESSAGES) {
                return Map.of("messages",
                    messages.subList(messages.size() - MAX_MESSAGES, messages.size()));
            }
        }
        return Map.of();
    }
}

**Hook 执行位置**：
- `BEFORE_AGENT` / `AFTER_AGENT`：Agent 整体执行前后
- `BEFORE_MODEL` / `AFTER_MODEL`：Agent Loop 循环过程中，每次模型调用前后

### Interceptors（拦截器）

Interceptors 提供更细粒度的控制，可以拦截和修改模型调用和工具执行。

#### 使用示例

```java
import com.alibaba.cloud.ai.graph.agent.interceptor.*;

// ModelInterceptor - 内容安全检查
public class GuardrailInterceptor implements ModelInterceptor {
    @Override
    public ModelResponse intercept(ModelRequest request, ModelCallHandler handler) {
        // 前置：检查输入
        if (containsSensitiveContent(request.getMessages())) {
            return ModelResponse.blocked("检测到不适当的内容");
        }

        // 执行调用
        ModelResponse response = handler.call(request);

        // 后置：检查输出
        return sanitizeIfNeeded(response);
    }
}

// ToolInterceptor - 监控和错误处理
public class ToolMonitoringInterceptor implements ToolInterceptor {
    @Override
    public ToolCallResponse intercept(ToolCallRequest request, ToolCallHandler handler) {
        long startTime = System.currentTimeMillis();
        try {
            ToolCallResponse response = handler.call(request);
            logSuccess(request, System.currentTimeMillis() - startTime);
            return response;
        } catch (Exception e) {
            logError(request, e, System.currentTimeMillis() - startTime);
            return ToolCallResponse.error(request.getToolCall(),
                "工具执行遇到问题，请稍后重试");
        }
    }
}

// 组合使用
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .interceptors(new GuardrailInterceptor(), new LoggingInterceptor(), new ToolMonitoringInterceptor())
    .saver(new MemorySaver())
    .build();
```

**常见用途**：
- **ModelInterceptor**：内容安全、动态提示、日志记录、性能监控
- **ToolInterceptor**：错误重试、权限检查、结果缓存、审计日志

### 控制与流式输出

#### 迭代控制

```java
// 设置最大迭代次数
ReactAgent agent = ReactAgent.builder()
    .maxIterations(5)  // 默认 10
    .build();

// 自定义停止条件
Function<OverAllState, Boolean> stopCondition = state -> {
    // 找到答案或错误过多时停止
    return !state.value("answer_found").orElse(false)
        && (Integer) state.value("error_count").orElse(0) <= 3;
};

agent = ReactAgent.builder()
    .shouldContinueFunction(stopCondition)
    .build();
```

#### 流式输出

```java
import reactor.core.publisher.Flux;

Flux<GraphResponse> stream = agent.stream("复杂任务");
stream.subscribe(
    response -> System.out.println("进度: " + response),
    error -> System.err.println("错误: " + error),
    () -> System.out.println("完成")
);
```

## 下一步

- 学习 [多 Agent 编排](../advanced/multi-agent.md) 构建复杂系统
- 探索 [Graph API](../advanced/workflow.md) 实现自定义工作流
- 查看 [工具开发](./tools.md) 扩展 Agent 能力
- 参考 [示例项目](https://github.com/spring-ai-alibaba/examples) 获取实践指导
