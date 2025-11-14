---
title: Structured Output 结构化输出
description: 学习如何让Agent返回特定格式的结构化数据，使用JSON Schema或Java POJO定义输出格式
keywords: [Structured Output, 结构化输出, JSON Schema, POJO, outputSchema, outputType, 数据格式化]
---

# Structured Output 结构化输出

结构化输出允许 Agent 以特定的、可预测的格式返回数据。相比于解析自然语言响应，您可以直接获得 JSON 对象或 Java POJO 形式的结构化数据，应用程序可以直接使用。

Spring AI Alibaba 的 `ReactAgent.Builder` 通过 `outputSchema` 和 `outputType` 方法处理结构化输出。当您设置所需的结构化输出模式时，Agent 会自动在用户消息中增加模式指令，模型会根据指定的格式生成数据。

```java
ReactAgent agent = ReactAgent.builder()
    .name("agent")
    .model(chatModel)
    .outputSchema(schemaString)  // Custom JSON schema as String
    // OR
    .outputType(MyClass.class)   // Java class - auto-converted to schema
    .build();
```

## 输出格式选项

Spring AI Alibaba 支持两种方式控制结构化输出：

* **`outputSchema(String schema)`**: 提供自定义的 JSON schema 字符串
* **`outputType(Class<?> type)`**: 提供 Java 类 - 使用 `BeanOutputConverter` 自动转换为 JSON schema
* **不指定**: 返回非结构化的自然语言响应

结构化响应在 Agent 的 `AssistantMessage` 中作为 JSON 文本返回，可以解析为您需要的格式。

## 输出 Schema 策略

您可以使用 JSON schema 字符串显式定义输出格式。这使您可以完全控制结构，并可以为每个字段包含详细的描述。

### 方法签名

```java
Builder outputSchema(String outputSchema)
```

**参数:**
- `outputSchema` (String, 必需): 定义结构化输出格式的 JSON schema 字符串。Schema 应包含字段名称、类型、描述和要求，以指导模型。

### 示例

**基本 JSON Schema:**

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.messages.AssistantMessage;

String contactInfoSchema = """
    请按照以下JSON格式输出：
    {
        "name": "人名",
        "email": "电子邮箱地址",
        "phone": "电话号码"
    }
    """;

ReactAgent agent = ReactAgent.builder()
    .name("contact_extractor")
    .model(chatModel)
    .outputSchema(contactInfoSchema)
    .build();

AssistantMessage result = agent.call(
    "从以下信息提取联系方式：张三，zhangsan@example.com，(555) 123-4567"
);

System.out.println(result.getText());
// 输出: {"name": "张三", "email": "zhangsan@example.com", "phone": "(555) 123-4567"}
```

**复杂嵌套 Schema:**

```java
String productReviewSchema = """
    请严格按照以下JSON格式返回产品评价分析：
    {
        "rating": 1-5之间的整数评分,
        "sentiment": "情感倾向（正面/负面/中性）",
        "keyPoints": ["关键点1", "关键点2", "关键点3"],
        "details": {
            "pros": ["优点1", "优点2"],
            "cons": ["缺点1", "缺点2"]
        }
    }
    """;

ReactAgent agent = ReactAgent.builder()
    .name("review_analyzer")
    .model(chatModel)
    .outputSchema(productReviewSchema)
    .build();

AssistantMessage result = agent.call(
    "分析评价：这个产品很棒，5星好评。配送快速，但价格稍贵。"
);

System.out.println(result.getText());
// 输出: {"rating": 5, "sentiment": "正面", "keyPoints": [...], "details": {...}}
```

**结构化分析 Schema:**

```java
String analysisSchema = """
    请按照以下JSON格式返回文本分析结果：
    {
        "summary": "内容摘要（50字以内）",
        "keywords": ["关键词1", "关键词2", "关键词3"],
        "sentiment": "情感倾向（正面/负面/中性）",
        "entities": {
            "persons": ["人名1", "人名2"],
            "locations": ["地点1", "地点2"],
            "organizations": ["组织1", "组织2"]
        }
    }
    """;

ReactAgent agent = ReactAgent.builder()
    .name("text_analyzer")
    .model(chatModel)
    .outputSchema(analysisSchema)
    .build();

AssistantMessage result = agent.call(
    "分析这段文字：昨天，李明在北京参加了阿里巴巴公司的技术大会，感受到了创新的力量。"
);

System.out.println(result.getText());
```

`outputSchema` 方法提供了最大的灵活性，您可以定义任何 JSON 结构，并提供详细的中文或英文指令来指导模型的输出格式。

## 输出类型策略

对于类型安全的结构化输出，您可以提供 Java 类，Spring AI Alibaba 将使用 `BeanOutputConverter` 自动将其转换为 JSON schema。这种方法确保了编译时类型安全和自动 schema 生成。

### 方法签名

```java
Builder outputType(Class<?> outputType)
```

**参数:**
- `outputType` (`Class<?>`, 必需): 定义输出结构的 Java 类。该类应该是带有标准 getter 和 setter 的 POJO。

## 工作原理

当 outputFormat 或 outputType 被指定时，Spring AI Alibaba 会自动选择：

* 当大模型服务支持 “原生结构化输出” 时（目前支持 OpenAiChatModel、DashScopeChatModel），自动使用模型内置的结构化输出能力（这也是目前最稳定、可靠的方式，因为模型服务会自动提供校验支持）。
* 针对其他没有 “原生结构化输出” 的模型，Spring AI Alibaba 会使用内置的 ToolCall策略，通过一个动态的 ToolCall 来格式化模型输出。

结构化响应将在 Agent 的状态对象 OverAllState 中返回，可通过 `structured_output` 读取。


### 模型原生结构化输出

比如，针对 DashScopeChatModel 模型，在配置 outputSchema 或 outputType 后，Spring AI Alibaba 会自动设置如下参数，以启用模型原生结构化输出能力。

```java
ChatOptions options = DashScopeChatOptions.builder()
	.withResponseFormat(
		DashScopeResponseFormat.builder()
			.type(DashScopeResponseFormat.Type.JSON_OBJECT)
			.build())
	.build();
```

同时，Spring AI Alibaba 框架会增强系统 Prompt，引导模型输出格式化内容

```java
```java
// In AgentLlmNode.augmentUserMessage() method
public void augmentUserMessage(List<Message> messages, String outputSchema) {
    if (!StringUtils.hasText(outputSchema)) {
        return;
    }

    for (int i = messages.size() - 1; i >= 0; i--) {
        Message message = messages.get(i);
        if (message instanceof UserMessage userMessage) {
            messages.set(i, userMessage.mutate()
                .text(userMessage.getText() + System.lineSeparator() + outputSchema)
                .build());
            break;
        }
    }
}
```
```

> 注意，相比于 DashScope 模型是通过增强 Prompt 提示词实现最终的 JSON 格式，实现的是一个尽最大努力的效果，OpenAI 模型则是在模型 API 层面支持 Json 格式，提供格式的严格保证支持。

### ToolCall 结构化输出

对于不支持原生结构化输出的模型，Spring AI Alibaba 支持通过调用工具来实现相同效果。此方法适用于所有支持工具调用的模型，即大多数现代模型。

## 错误处理

模型可能不总是返回格式完美的 JSON。以下是处理潜在问题的策略:

### Try-Catch 模式

```java
ReactAgent agent = ReactAgent.builder()
    .name("data_extractor")
    .model(chatModel)
    .outputType(DataOutput.class)
    .build();

try {
    AssistantMessage result = agent.call("提取数据");
    ObjectMapper mapper = new ObjectMapper();
    DataOutput data = mapper.readValue(result.getText(), DataOutput.class);
    // 处理数据
} catch (JsonProcessingException e) {
    System.err.println("JSON解析失败: " + e.getMessage());
    System.err.println("原始输出: " + result.getText());
    // 回退处理
}
```

### 验证模式

```java
public class ValidatedOutput {
    private String title;
    private Integer rating;

    public void validate() throws IllegalArgumentException {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("评分必须在1-5之间");
        }
    }

    // Getter 和 Setter 方法...
}

AssistantMessage result = agent.call("生成评价");
ValidatedOutput output = mapper.readValue(result.getText(), ValidatedOutput.class);
output.validate();  // 如果无效则抛出异常
```

### 重试模式

```java
int maxRetries = 3;
DataOutput data = null;

for (int i = 0; i < maxRetries; i++) {
    try {
        AssistantMessage result = agent.call("提取数据");
        data = mapper.readValue(result.getText(), DataOutput.class);
        break;  // 成功
    } catch (Exception e) {
        if (i == maxRetries - 1) {
            throw new RuntimeException("多次尝试后仍然失败", e);
        }
        System.out.println("第" + (i + 1) + "次尝试失败，重试中...");
    }
}
```

Spring AI Alibaba 专注于简单性和灵活性，允许开发者在显式 schema 字符串（最大控制）和 Java 类（类型安全）之间进行选择。

## 相关文档

- [Agents](./agents.md) - 了解 ReactAgent 功能
- [Models](./models.md) - 支持的聊天模型
- [Messages](./messages.md) - 消息类型和处理
