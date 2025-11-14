---
title: 智能体作为工具（Agent Tool）
description: 了解如何在Spring AI Alibaba中实现Multi-agent协作，包括工具调用和交接模式
keywords: [Multi-agent, Multi-Agent, 工具调用, Tool Calling, Handoffs, Agent协作, 子Agent]
---

**Multi-agent** 将复杂的应用程序分解为多个协同工作的专业化Agent。与依赖单个Agent处理所有步骤不同，**Multi-agent架构**允许你将更小、更专注的Agent组合成协调的工作流。

Multi-agent系统在以下情况下很有用：

* 单个Agent拥有太多工具，难以做出正确的工具选择决策
* 上下文或记忆增长过大，单个Agent难以有效跟踪
* 任务需要**专业化**（例如：规划器、研究员、数学专家）

## Multi-agent模式

Spring AI Alibaba支持以下Multi-agent模式：

| 模式 | 工作原理 | 控制流 | 使用场景 |
| ---- | -------- | ------ | -------- |
| [**Tool Calling**](#tool-calling) | Supervisor Agent将其他Agent作为*工具*调用。"工具"Agent不直接与用户对话——它们只执行任务并返回结果。 | 集中式：所有路由都通过调用Agent。 | 任务编排、结构化工作流。 |
| [**Handoffs**](#Handoffs) | 当前的Agent决定将控制权转移给另一个Agent。活动Agent随之变更，用户可以继续与新的Agent直接交互。 | 去中心化：Agent可以改变当前由谁来担当活跃Agent。 | 跨领域对话、专家接管。 |


## 选择模式

| 问题 | 工具调用 | 交接（Handoffs） |
| --- | --- | --- |
| 需要集中控制工作流程？ | ✅ 是 | ❌ 否 |
| 希望Agent直接与用户交互？ | ❌ 否 | ✅ 是 |
| 专家之间复杂的、类人对话？ | ❌ 有限 | ✅ 强 |

> 你可以混合使用两种模式——使用**交接**进行Agent切换，并让每个Agent**将子Agent作为工具调用**来执行专门任务。

## 自定义Agent上下文

Multi-agent设计的核心是**上下文工程**——决定每个Agent看到什么信息。Spring AI Alibaba 为你提供细粒度的控制：

* 将对话或状态的哪些部分传递给每个Agent
* 为子Agent定制专门的提示
* 包含/排除中间推理
* 为每个Agent自定义输入/输出格式

系统的质量**在很大程度上取决于**上下文工程。目标是确保每个Agent都能访问执行任务所需的正确数据，无论它是作为工具还是作为活动Agent。

## 工具调用（Tool Calling）

在**工具调用**中，一个Agent（"**控制器**"）将其他Agent视为*工具*（AgentTool），在需要时调用。控制器管理编排，而工具Agent执行特定任务并返回结果。

流程：

1. **控制器**接收输入并决定调用哪个工具（子Agent）
2. **工具Agent**根据控制器的指令运行其任务
3. **工具Agent**将结果返回给控制器
4. **控制器**决定下一步或完成任务

![agent tool](/img/agent/multi-agent/agent-tool.png)

> 作为工具使用的Agent通常**不期望**与用户继续对话。它们的角色是执行任务并将结果返回给控制器Agent。如果你需要子Agent能够与用户对话，请改用**交接**模式。

### 实现

下面是一个最小示例，其中主Agent通过工具定义访问单个子Agent：

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.tool.AgentTool;
import org.springframework.ai.chat.model.ChatModel;

// 创建子Agent
ReactAgent writerAgent = ReactAgent.builder()
    .name("writer_agent")
    .model(chatModel)
    .description("可以写文章")
    .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
    .build();

// 创建主Agent，将子Agent作为工具
ReactAgent blogAgent = ReactAgent.builder()
    .name("blog_agent")
    .model(chatModel)
    .instruction("根据用户给定的主题写一篇文章。使用写作工具来完成任务。")
    .tools(AgentTool.getFunctionToolCallback(writerAgent)) // [!code highlight]
    .build();

// 使用
Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的散文");
```

在这种模式中：

1. 主Agent在决定任务匹配子Agent的描述时调用工具
2. 子Agent独立运行并返回结果
3. 主Agent接收结果并继续编排

### 自定义点

你可以在几个点控制主Agent和子Agent之间的上下文传递：

1. **子Agent名称**（`"writer_agent"`）：这是主Agent引用子Agent的方式。由于它影响提示，请谨慎选择。
2. **子Agent描述**（`"可以写文章"`）：这是主Agent"知道"的关于子Agent的内容。它直接影响主Agent决定何时调用它。
3. **子Agent的输入**：你可以自定义此输入以更好地塑造子Agent如何解释任务。
4. **子Agent的输出**：这是传递回主Agent的**响应**。你可以调整返回的内容以控制主Agent如何解释结果。

### 控制子Agent的输入

有两个主要杠杆来控制主Agent传递给子Agent的输入：

* **修改提示词**——调整主Agent的提示或工具元数据（即子Agent的名称和描述），以更好地指导何时以及如何调用子Agent。
* **上下文注入**——通过使用 `inputSchema` 或 `inputType` 来定义结构化输入，使子Agent能够接收更丰富的上下文信息。

#### 使用 inputSchema

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.tool.AgentTool;

// 定义子Agent的输入Schema
String writerInputSchema = """
    {
        "topic": "文章主题",
        "wordCount": "字数要求（整数）",
        "style": "文章风格（如：散文、诗歌等）"
    }
    """;

ReactAgent writerAgent = ReactAgent.builder()
    .name("structured_writer_agent")
    .model(chatModel)
    .description("根据结构化输入写文章")
    .instruction("你是一个专业作家。请严格按照输入的主题、字数和风格要求创作文章。")
    .inputSchema(writerInputSchema) // [!code highlight]
    .build();

ReactAgent coordinatorAgent = ReactAgent.builder()
    .name("coordinator_agent")
    .model(chatModel)
    .instruction("你需要调用写作工具来完成用户的写作请求。请根据用户需求，使用结构化的参数调用写作工具。")
    .tools(AgentTool.getFunctionToolCallback(writerAgent))
    .build();

Optional<OverAllState> result = coordinatorAgent.invoke("请写一篇关于春天的散文，大约150字");
```

#### 使用 inputType

使用 Java 类型定义输入，框架会自动生成 JSON Schema：

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.tool.AgentTool;

// 定义输入类型
public record ArticleRequest(
    String topic,      // 文章主题
    int wordCount,     // 字数要求
    String style       // 文章风格
) {}

ReactAgent writerAgent = ReactAgent.builder()
    .name("typed_writer_agent")
    .model(chatModel)
    .description("根据类型化输入写文章")
    .instruction("你是一个专业作家。请严格按照输入的 topic（主题）、wordCount（字数）和 style（风格）要求创作文章。")
    .inputType(ArticleRequest.class) // [!code highlight]
    .build();

ReactAgent coordinatorAgent = ReactAgent.builder()
    .name("coordinator_with_type_agent")
    .model(chatModel)
    .instruction("你需要调用写作工具来完成用户的写作请求。工具接收 JSON 格式的参数。")
    .tools(AgentTool.getFunctionToolCallback(writerAgent))
    .build();

Optional<OverAllState> result = coordinatorAgent.invoke("请写一篇关于秋天的现代诗，大约100字");
```

### 控制子Agent的输出

塑造主Agent从子Agent接收的内容的常见策略：

* **修改提示词**——优化子Agent的提示以指定应返回的确切内容。
  * 当输出不完整、过于冗长或缺少关键细节时很有用。
  * 常见的失败模式是子Agent执行工具调用或推理但**不在最终消息中包含结果**。提醒它控制器（和用户）只看到最终输出，因此必须在那里包含所有相关信息。
* **自定义输出格式**——使用 `outputSchema` 或 `outputType` 定义结构化输出格式。

#### 使用 outputSchema

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.tool.AgentTool;

// 定义输出Schema
String writerOutputSchema = """
    请按照以下JSON格式返回：
    {
        "title": "文章标题",
        "content": "文章正文内容",
        "characterCount": "文章字符数（整数）"
    }
    """;

ReactAgent writerAgent = ReactAgent.builder()
    .name("writer_with_output_schema")
    .model(chatModel)
    .description("写文章并返回结构化输出")
    .instruction("你是一个专业作家。请创作文章并严格按照指定的JSON格式返回结果。")
    .outputSchema(writerOutputSchema) // [!code highlight]
    .build();

ReactAgent coordinatorAgent = ReactAgent.builder()
    .name("coordinator_output_schema")
    .model(chatModel)
    .instruction("调用写作工具完成用户请求，工具会返回结构化的文章数据。")
    .tools(AgentTool.getFunctionToolCallback(writerAgent))
    .build();

Optional<OverAllState> result = coordinatorAgent.invoke("写一篇关于冬天的短文");
```

#### 使用 outputType

使用 Java 类型定义输出，框架会自动生成输出 schema：

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.tool.AgentTool;

// 定义输出类型
public class ArticleOutput {
    private String title;
    private String content;
    private int characterCount;

    // getters and setters
}

ReactAgent writerAgent = ReactAgent.builder()
    .name("writer_with_output_type")
    .model(chatModel)
    .description("写文章并返回类型化输出")
    .instruction("你是一个专业作家。请创作文章并返回包含 title、content 和 characterCount 的结构化结果。")
    .outputType(ArticleOutput.class) // [!code highlight]
    .build();

ReactAgent coordinatorAgent = ReactAgent.builder()
    .name("coordinator_output_type")
    .model(chatModel)
    .instruction("调用写作工具完成用户请求。")
    .tools(AgentTool.getFunctionToolCallback(writerAgent))
    .build();

Optional<OverAllState> result = coordinatorAgent.invoke("写一篇关于夏天的小诗");
```

### 完整类型化示例

同时使用 `inputType` 和 `outputType` 进行完整的类型化Agent工具调用：

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.tool.AgentTool;

// 定义输入和输出类型
public record ArticleRequest(String topic, int wordCount, String style) {}

public class ArticleOutput {
    private String title;
    private String content;
    private int characterCount;
    // getters and setters
}

public class ReviewOutput {
    private String comment;
    private boolean approved;
    private List<String> suggestions;
    // getters and setters
}

// 创建完整类型化的Agent
ReactAgent writerAgent = ReactAgent.builder()
    .name("full_typed_writer")
    .model(chatModel)
    .description("完整类型化的写作工具")
    .instruction("根据结构化输入（topic、wordCount、style）创作文章，并返回结构化输出（title、content、characterCount）。")
    .inputType(ArticleRequest.class) // [!code highlight]
    .outputType(ArticleOutput.class) // [!code highlight]
    .build();

ReactAgent reviewerAgent = ReactAgent.builder()
    .name("typed_reviewer")
    .model(chatModel)
    .description("完整类型化的评审工具")
    .instruction("对文章进行评审，返回评审意见（comment、approved、suggestions）。")
    .outputType(ReviewOutput.class) // [!code highlight]
    .build();

ReactAgent orchestratorAgent = ReactAgent.builder()
    .name("orchestrator")
    .model(chatModel)
    .instruction("协调写作和评审流程。先调用写作工具创作文章，然后调用评审工具进行评审。")
    .tools(
        AgentTool.getFunctionToolCallback(writerAgent),
        AgentTool.getFunctionToolCallback(reviewerAgent)
    )
    .build();

Optional<OverAllState> result = orchestratorAgent.invoke("请写一篇关于友谊的散文，约200字，需要评审");
```


