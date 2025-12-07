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

<Code
  language="java"
  title="ReactAgent 基础配置示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
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
    .build();`}
</Code>

#### 高级模型配置

通过 `ChatOptions` 可以精细控制模型行为：

<Code
  language="java"
  title="DashScopeChatModel 高级配置" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

ChatModel chatModel = DashScopeChatModel.builder()
    .dashScopeApi(dashScopeApi)
    .defaultOptions(DashScopeChatOptions.builder()
		.withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
        .withTemperature(0.7)    // 控制随机性
        .withMaxToken(2000)      // 最大输出长度
        .withTopP(0.9)           // 核采样参数
        .build())
    .build();`}
</Code>

**常用参数说明**：
- `temperature`：控制输出的随机性（0.0-1.0），值越高越有创造性
- `maxTokens`：限制单次响应的最大 token 数
- `topP`：核采样，控制输出的多样性
- 更多参数请参考 ChatModel 适配

### Tools（工具）

工具赋予 Agent 执行操作的能力，支持顺序执行、并行调用、动态选择和错误处理。

#### 定义和使用工具

<Code
  language="java"
  title="SearchTool 自定义工具示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import java.util.function.BiFunction;

// 定义工具（示例：仅一个搜索工具）
public class SearchTool implements BiFunction<String, ToolContext, String> {
    @Override
    public String apply(String query, ToolContext context) {
        // 实现搜索逻辑
        return "搜索结果: " + query;
    }
}

// 创建工具回调
ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchTool())
    .description("搜索工具")
    .build();

// 在Agent中使用
ReactAgent agent = ReactAgent.builder()
    .name("search_agent")
    .model(chatModel)
    .tools(searchTool)
    .build();`}
</Code>

#### 工具错误处理

<Code
  language="java"
  title="ToolErrorInterceptor 工具错误处理" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;

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

    @Override
    public String getName() {
        return "ToolErrorInterceptor";
    }
}

ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .interceptors(new ToolErrorInterceptor())
    .build();`}
</Code>

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

<Code
  language="java"
  title="系统提示基础配置" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .systemPrompt("你是一个专业的技术助手。请准确、简洁地回答问题。")
    .build();`}
</Code>

#### 使用 instruction

对于更详细的指令，使用 `instruction` 参数：

<Code
  language="java"
  title="使用 instruction 提供详细指令" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`String instruction = """
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
    .build();`}
</Code>

#### 动态 System Prompt

使用 `ModelInterceptor` 实现基于上下文的动态提示：

<Code
  language="java"
  title="DynamicPromptInterceptor 动态提示拦截器" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import org.springframework.ai.chat.messages.SystemMessage;

public class DynamicPromptInterceptor extends ModelInterceptor {
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 基于上下文构建动态 system prompt
        String userRole = (String) request.getContext().getOrDefault("user_role", "default");
        String dynamicPrompt = switch (userRole) {
            case "expert" -> "你正在与技术专家对话。\n- 使用专业术语\n- 深入技术细节";
            case "beginner" -> "你正在与初学者对话。\n- 使用简单语言\n- 解释基础概念";
            default -> "你是一个专业的助手，保持友好和专业。";
        };

        SystemMessage enhancedSystemMessage;
        if (request.getSystemMessage() == null) {
            enhancedSystemMessage = new SystemMessage(dynamicPrompt);
        } else {
            enhancedSystemMessage = new SystemMessage(request.getSystemMessage().getText() + "\n\n" + dynamicPrompt);
        }

        ModelRequest modified = ModelRequest.builder(request)
            .systemMessage(enhancedSystemMessage)
            .build();
        return handler.call(modified);
    }

    @Override
    public String getName() {
        return "DynamicPromptInterceptor";
    }
}

ReactAgent agent = ReactAgent.builder()
    .name("adaptive_agent")
    .model(chatModel)
    .interceptors(new DynamicPromptInterceptor())
    .build();`}
</Code>

## 调用 Agent

### 基础调用

使用 `call` 方法获取最终响应：

<Code
  language="java"
  title="Agent 基础调用示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import org.springframework.ai.chat.messages.AssistantMessage;

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
AssistantMessage response = agent.call(messages);`}
</Code>

### 获取完整状态

使用 `invoke` 方法获取完整的执行状态：

<Code
  language="java"
  title="使用 invoke 获取完整状态" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.graph.OverAllState;
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
}`}
</Code>

### 使用配置

通过 `RunnableConfig` 传递运行时配置：

<Code
  language="java"
  title="使用 RunnableConfig 传递配置" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.graph.RunnableConfig;

String threadId = "thread_123";
RunnableConfig runnableConfig = RunnableConfig.builder()
    .threadId(threadId)
    .addMetadata("key", "value")
    .build();

AssistantMessage response = agent.call("你的问题", runnableConfig);`}
</Code>

## 高级特性

### 结构化输出

在某些情况下，你可能希望 Agent 以特定格式返回输出。ReactAgent 提供了两种策略。

#### 使用 outputType

通过 Java 类定义输出结构，Agent 会自动生成对应的 JSON Schema：

<Code
  language="java"
  title="PoemOutput 结构化输出示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`public class PoemOutput {
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
System.out.println(response.getText());`}
</Code>

#### 使用 outputSchema

使用 `BeanOutputConverter` 生成输出 Schema，提供类型安全和自动 schema 生成：

<Code
  language="java"
  title="使用 outputSchema 自定义输出格式" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import org.springframework.ai.converter.BeanOutputConverter;

// 定义输出类型
public static class TextAnalysisResult {
    private String summary;
    private List<String> keywords;
    private String sentiment;
    private Double confidence;

    // Getters and Setters
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}

// 使用 BeanOutputConverter 生成 outputSchema
BeanOutputConverter<TextAnalysisResult> outputConverter = new BeanOutputConverter<>(TextAnalysisResult.class);
String format = outputConverter.getFormat();

ReactAgent agent = ReactAgent.builder()
    .name("analysis_agent")
    .model(chatModel)
    .outputSchema(format)
    .saver(new MemorySaver())
    .build();

AssistantMessage response = agent.call("分析这段文本：春天来了，万物复苏。");`}
</Code>

**选择建议**：
- `outputType`：类型安全，适合结构固定的场景（**推荐**）
- `outputSchema`：使用 `BeanOutputConverter` 生成时提供类型安全，手动提供字符串时灵活性高，适合动态或复杂的输出格式

### Memory（记忆）

Agent 通过状态自动维护对话历史。使用 `MemorySaver` 配置持久化存储。

<Code
  language="java"
  title="Memory 配置示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
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
agent.call("我叫什么名字？", config);  // 输出: "你叫张三"`}
</Code>

**生产环境**：使用 `RedisSaver`、`MongoSaver` 等持久化存储替代 `MemorySaver`。

### Hooks（钩子）

Hooks 允许在 Agent 执行的关键点插入自定义逻辑。

#### Hook 类型与使用

<Code
  language="java"
  title="Hook 使用示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.*;

// 1. AgentHook - 在 Agent 开始/结束时执行，每次Agent调用只会运行一次
@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class LoggingHook extends AgentHook {
    @Override
    public String getName() { return "logging"; }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        System.out.println("Agent 开始执行");
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        System.out.println("Agent 执行完成");
        return CompletableFuture.completedFuture(Map.of());
    }
}

// 2. ModelHook - 在模型调用前后执行（例如：消息修剪），区别于AgentHook，ModelHook在一次agent调用中可能会调用多次，也就是每次 reasoning-acting 迭代都会执行
public class MessageTrimmingHook extends ModelHook {
    private static final int MAX_MESSAGES = 10;

    @Override
    public String getName() {
        return "message_trimming";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        Optional<Object> messagesOpt = state.value("messages");
        if (messagesOpt.isPresent()) {
            List<Message> messages = (List<Message>) messagesOpt.get();
            if (messages.size() > MAX_MESSAGES) {
                return CompletableFuture.completedFuture(Map.of("messages",
                    messages.subList(messages.size() - MAX_MESSAGES, messages.size())));
            }
        }
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of());
    }
}`}
</Code>

**Hook 执行位置**：
- `BEFORE_AGENT` / `AFTER_AGENT`：Agent 整体执行前后
- `BEFORE_MODEL` / `AFTER_MODEL`：Agent Loop 循环过程中，每次模型调用前后

### Interceptors（拦截器）

Interceptors 提供更细粒度的控制，可以拦截和修改模型调用和工具执行。

#### 使用示例

<Code
  language="java"
  title="Interceptor 使用示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.interceptor.*;

// ModelInterceptor - 内容安全检查
public class GuardrailInterceptor extends ModelInterceptor {
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
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
public class ToolMonitoringInterceptor extends ToolInterceptor {
    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
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
    .build();`}
</Code>

**常见用途**：
- **ModelInterceptor**：内容安全、动态提示、日志记录、性能监控
- **ToolInterceptor**：错误重试、权限检查、结果缓存、审计日志

### 控制与流式输出

#### 迭代控制

通过 Hooks 控制 Agent 的执行迭代，防止无限循环或过度成本。

<Code
  language="java"
  title="使用 ModelCallLimitHook 限制模型调用次数" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

// 使用内置的 ModelCallLimitHook 限制模型调用次数
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .hooks(ModelCallLimitHook.builder().runLimit(5).build())  // 限制最多调用 5 次
    .saver(new MemorySaver())
    .build();`}
</Code>

<Code
  language="java"
  title="自定义停止条件 Hook" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import org.springframework.ai.chat.messages.AssistantMessage;

// 自定义停止条件：基于状态判断是否继续
@HookPositions({HookPosition.BEFORE_MODEL})
public class CustomStopConditionHook extends ModelHook {

    @Override
    public String getName() {
        return "custom_stop_condition";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 检查是否找到答案，展示使用 OverAllState
        boolean answerFound = (Boolean) state.value("answer_found").orElse(false);
        // 检查错误次数，展示使用 RunnableConfig
        int errorCount = (Integer) config.context().get("error_count").orElse(0);

        // 找到答案或错误过多时停止
        if (answerFound || errorCount > 3) {
            List<Message> messages = new ArrayList<>(
                (List<Message>) state.value("messages").orElse(new ArrayList<>())
            );
            messages.add(new AssistantMessage(
                answerFound ? "已找到答案，Agent 执行完成。"
                            : "错误次数过多 (" + errorCount + ")，Agent 执行终止。"
            ));
            return CompletableFuture.completedFuture(Map.of("messages", messages));
        }

        return CompletableFuture.completedFuture(Map.of());
    }

}

// 使用自定义停止条件
ReactAgent agent = ReactAgent.builder()
    .name("my_agent")
    .model(chatModel)
    .hooks(new CustomStopConditionHook())
    .saver(new MemorySaver())
    .build();`}
</Code>

#### 流式输出

<Code
  language="java"
  title="流式输出示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/framework/tutorials/AgentsExample.java"
>
{`import reactor.core.publisher.Flux;

Flux<NodeOutput> stream = agent.stream("复杂任务");
stream.subscribe(
    response -> System.out.println("进度: " + response),
    error -> System.err.println("错误: " + error),
    () -> System.out.println("完成")
);`}
</Code>

## 下一步

- 学习 [多 Agent 编排](../advanced/multi-agent.md) 构建复杂系统
- 探索 [Graph API](../advanced/workflow.md) 实现自定义工作流
- 查看 [工具开发](./tools.md) 扩展 Agent 能力
- 参考 [示例项目](https://github.com/spring-ai-alibaba/examples) 获取实践指导
