---
title: 检索增强生成（RAG）
description: 了解如何使用检索增强生成(RAG)技术为LLM提供外部知识，构建知识库，实现两步RAG和Agentic RAG
keywords:
  [
    RAG,
    检索增强生成,
    知识库,
    向量存储,
    文档检索,
    Agentic RAG,
    两步RAG,
    语义搜索,
  ]
---

# 检索增强生成（RAG）

大型语言模型（LLM）虽然强大，但有两个关键限制：

- **有限的上下文**——它们无法一次性摄取整个语料库
- **静态知识**——它们的训练数据在某个时间点被冻结

检索通过在查询时获取相关的外部知识来解决这些问题。这是**检索增强生成（RAG）**的基础：使用特定上下文的信息来增强 LLM 的回答。

## 构建知识库

**知识库**是用于检索的文档或结构化数据的存储库。

如果你需要自定义知识库，可以使用 Spring AI Alibaba 的文档加载器和向量存储从你自己的数据构建。

> 如果你已经有一个知识库（例如 SQL 数据库、CRM 或内部文档系统），你**不需要**重建它。你可以：
>
> - 将其连接为 Agent 的**工具**用于 Agentic RAG
> - 查询它并将检索到的内容作为上下文提供给 LLM（[两步 RAG](#2-step-rag)）

### 从检索到 RAG

检索允许 LLM 在运行时访问相关上下文。但大多数实际应用更进一步：它们**将检索与生成集成**以产生基于事实的、上下文感知的答案。

这是**检索增强生成（RAG）**的核心思想。检索管道成为结合搜索和生成的更广泛系统的基础。

### 检索流程

典型的检索工作流如下：

![Spring AI Alibaba RAG](/img/agent/rag/rag1.png)

每个组件都是模块化的：你可以交换加载器、分割器、嵌入或向量存储，而无需重写应用程序的逻辑。

### 构建模块

在 Spring AI Alibaba 中，你可以使用以下组件构建 RAG 系统：

### 文档加载器

从外部源（文件、数据库等）摄取数据，返回标准化的文档对象。Spring AI Alibaba 支持 PDF、Word、Markdown 等多种格式。

### 文本分割器

将大型文档分解为更小的块，这些块可以单独检索并适合模型的上下文窗口。

### 嵌入模型

嵌入模型将文本转换为数字向量，使得具有相似含义的文本在向量空间中靠近在一起。

### 向量存储

用于存储和搜索嵌入的专用数据库。Spring AI Alibaba 支持多种向量数据库如 Milvus、Pinecone 等。

### 检索器

检索器是一个接口，给定非结构化查询返回文档。

## RAG 架构

RAG 可以以多种方式实现，具体取决于你的系统需求。我们在下面的部分概述每种类型。

| 架构            | 描述                                                  | 控制性 | 灵活性 | 延迟    | 使用场景示例             |
| --------------- | ----------------------------------------------------- | ------ | ------ | ------- | ------------------------ |
| **两步 RAG**    | 检索总是在生成之前发生。简单且可预测                  | ✅ 高  | ❌ 低  | ⚡ 快   | FAQ、文档机器人          |
| **Agentic RAG** | LLM 驱动的 Agent 决定*何时*以及*如何*在推理过程中检索 | ❌ 低  | ✅ 高  | ⏳ 可变 | 具有多工具访问的研究助手 |
| **混合 RAG**    | 结合两种方法的特点，包含验证步骤                      | ⚖️ 中  | ⚖️ 中  | ⏳ 可变 | 带质量验证的领域特定问答 |

:::info
**延迟**：延迟在**两步 RAG**中通常更**可预测**，因为 LLM 调用的最大次数是已知且有上限的。这种可预测性假设 LLM 推理时间是主要因素。但是，实际延迟也可能受检索步骤性能的影响——例如 API 响应时间、网络延迟或数据库查询——这些可能因使用的工具和基础设施而异。
:::

### 两步 RAG

在**两步 RAG**中，检索步骤总是在生成步骤之前执行。这种架构简单且可预测，适合许多应用，其中检索相关文档是生成答案的明确前提。

![Spring AI Alibaba RAG](/img/agent/rag/rag2.png)

#### Java 实现示例

```java
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;

// 假设你已经有一个配置好的向量存储
VectorStore vectorStore = ...; // 配置你的向量存储（如Milvus、Pinecone等）

// 创建带有RAG功能的ChatClient
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(
        new QuestionAnswerAdvisor(vectorStore) // [!code highlight]
    )
    .build();

// 两步RAG：检索 -> 生成
String userQuestion = "Spring AI Alibaba支持哪些模型？";

String answer = chatClient.prompt()
    .user(userQuestion)
    .call()
    .content(); // [!code highlight]

System.out.println("答案: " + answer);
```

在这个例子中：

1. `QuestionAnswerAdvisor` 自动从向量存储检索相关文档
2. 检索到的文档作为上下文添加到提示中
3. ChatModel 使用增强的上下文生成答案

#### 构建知识库

```java
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.io.Resource;

// 1. 加载文档
Resource resource = new FileSystemResource("path/to/document.txt");
TextReader textReader = new TextReader(resource);
List<Document> documents = textReader.get();

// 2. 分割文档为块
TokenTextSplitter splitter = new TokenTextSplitter();
List<Document> chunks = splitter.apply(documents);

// 3. 将块添加到向量存储
vectorStore.add(chunks); // [!code highlight]

// 现在你可以使用向量存储进行检索
List<Document> results = vectorStore.similaritySearch("查询文本");
```

### Agentic RAG

**Agentic 检索增强生成（RAG）**将检索增强生成的优势与基于 Agent 的推理相结合。Agent（由 LLM 驱动）不是在回答之前检索文档，而是逐步推理并决定在交互过程中**何时**以及**如何**检索信息。

:::tip
Agent 启用 RAG 行为所需的唯一条件是访问一个或多个可以获取外部知识的**工具**——例如文档加载器、Web API 或数据库查询。
:::

![Spring AI Alibaba RAG](/img/agent/rag/rag3.png)

#### Java 实现示例

```java
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.tool.ToolCallback;
import java.util.function.Function;
import java.util.List;

// 创建文档检索工具
public class DocumentSearchTool implements Function<DocumentSearchTool.Request, DocumentSearchTool.Response> {

    private final VectorStore vectorStore;

    public DocumentSearchTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public record Request(String query) {}
    public record Response(String content) {}

    @Override
    public Response apply(Request request) {
        // 从向量存储检索相关文档
        List<Document> docs = vectorStore.similaritySearch(request.query()); // [!code highlight]

        // 合并文档内容
        String combinedContent = docs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));

        return new Response(combinedContent);
    }
}

// 创建工具回调
DocumentSearchTool searchTool = new DocumentSearchTool(vectorStore);
ToolCallback searchCallback = ToolCallback.builder()
    .name("search_documents")
    .description("搜索文档以查找相关信息")
    .function(searchTool)
    .inputType(DocumentSearchTool.Request.class)
    .build();

// 创建带有检索工具的Agent
ReactAgent ragAgent = ReactAgent.builder()
    .name("rag_agent")
    .model(chatModel)
    .systemPrompt("你是一个智能助手。当需要查找信息时，使用search_documents工具。" +
                  "基于检索到的信息回答用户的问题，并引用相关片段。")
    .tools(searchCallback) // [!code highlight]
    .build();

// Agent会自动决定何时调用检索工具
AssistantMessage response = ragAgent.call("Spring AI Alibaba支持哪些向量数据库？");
System.out.println(response.getText());
```

在这个例子中：

1. Agent 接收用户问题
2. Agent 推理并决定是否需要检索文档
3. 如果需要，Agent 调用 `search_documents` 工具
4. Agent 使用检索到的信息生成答案
5. 如果信息不足，Agent 可以再次调用工具

#### 多工具 Agentic RAG

```java
// 创建多个检索工具
ToolCallback webSearchTool = ToolCallback.builder()
    .name("web_search")
    .description("搜索互联网以获取最新信息")
    .function(webSearchFunction)
    .inputType(WebSearchRequest.class)
    .build();

ToolCallback databaseQueryTool = ToolCallback.builder()
    .name("database_query")
    .description("查询内部数据库")
    .function(dbQueryFunction)
    .inputType(DatabaseQueryRequest.class)
    .build();

ToolCallback documentSearchTool = ToolCallback.builder()
    .name("document_search")
    .description("搜索文档库")
    .function(docSearchFunction)
    .inputType(DocumentSearchRequest.class)
    .build();

// Agent可以访问多个检索源
ReactAgent multiSourceAgent = ReactAgent.builder()
    .name("multi_source_rag_agent")
    .model(chatModel)
    .systemPrompt("你可以访问多个信息源：" +
                  "1. web_search - 用于最新的互联网信息\n" +
                  "2. database_query - 用于内部数据\n" +
                  "3. document_search - 用于文档库\n" +
                  "根据问题选择最合适的工具。")
    .tools(webSearchTool, databaseQueryTool, documentSearchTool) // [!code highlight]
    .build();

AssistantMessage response = multiSourceAgent.call(
    "比较我们的产品文档中的功能和最新的市场趋势"
);
```

### 混合 RAG

混合 RAG 结合了两步 RAG 和 Agentic RAG 的特点。它引入了中间步骤，如查询预处理、检索验证和生成后检查。这些系统比固定管道提供更多灵活性，同时保持对执行的一定控制。

典型组件包括：

- **查询增强**：修改输入问题以提高检索质量。这可能涉及重写不清晰的查询、生成多个变体或用额外上下文扩展查询。
- **检索验证**：评估检索到的文档是否相关且充分。如果不够，系统可能会优化查询并再次检索。
- **答案验证**：检查生成的答案的准确性、完整性以及与源内容的一致性。如果需要，系统可以重新生成或修订答案。

架构通常支持这些步骤之间的多次迭代：

![Spring AI Alibaba RAG](/img/agent/rag/rag4.png)

#### Java 实现示例（概念性）

```java
public class HybridRAGSystem {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final QueryEnhancer queryEnhancer;
    private final AnswerValidator answerValidator;

    public String answer(String userQuestion) {
        // 1. 查询增强
        String enhancedQuery = queryEnhancer.enhance(userQuestion);

        int maxAttempts = 3;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // 2. 检索文档
            List<Document> docs = vectorStore.similaritySearch(enhancedQuery);

            // 3. 检索验证
            if (!isRetrievalSufficient(docs)) {
                enhancedQuery = refineQuery(enhancedQuery, docs);
                continue;
            }

            // 4. 生成答案
            String answer = generateAnswer(userQuestion, docs);

            // 5. 答案验证
            ValidationResult validation = answerValidator.validate(answer, docs);
            if (validation.isValid()) {
                return answer;
            }

            // 6. 根据验证结果决定下一步
            if (validation.shouldRetry()) {
                enhancedQuery = refineBasedOnValidation(enhancedQuery, validation);
            } else {
                return answer; // 返回当前最佳答案
            }
        }

        return "无法生成满意的答案";
    }

    private boolean isRetrievalSufficient(List<Document> docs) {
        // 实现检索质量评估逻辑
        return !docs.isEmpty() && calculateRelevanceScore(docs) > 0.7;
    }

    private String generateAnswer(String question, List<Document> docs) {
        String context = docs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));

        return chatClient.prompt()
            .system("基于以下上下文回答问题：\n" + context)
            .user(question)
            .call()
            .content();
    }
}
```

这种架构适用于：

- 具有模糊或不明确查询的应用
- 需要验证或质量控制步骤的系统
- 领域特定的问答系统，要求高准确性

## 最佳实践

1. **选择合适的架构**：

   - 简单 FAQ → 两步 RAG
   - 复杂研究任务 → Agentic RAG
   - 需要质量保证 → 混合 RAG

2. **优化检索质量**：

   - 使用合适的文本分割策略
   - 选择高质量的嵌入模型
   - 实现查询重写和扩展

3. **控制上下文大小**：

   - 限制检索到的文档数量
   - 使用文档排序和过滤
   - 考虑模型的上下文窗口限制

4. **监控和评估**：

   - 跟踪检索质量指标
   - 评估答案准确性
   - 收集用户反馈

5. **性能优化**：
   - 缓存常见查询的检索结果
   - 使用异步检索
   - 批量处理文档嵌入

## Spring AI Alibaba RAG 组件

Spring AI Alibaba 提供了构建 RAG 系统的核心组件：

```java
// 文档加载和处理
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

// 向量存储
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;

// 嵌入模型
import org.springframework.ai.embedding.EmbeddingModel;

// RAG Advisor
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
```

## 相关文档

- [Tools](../tutorials/tools.md) - 创建检索工具
- [Agents](../tutorials/agents.md) - 构建 Agentic RAG
- [Memory](./memory.md) - 对话记忆管理
- [Multi-Agent](./multi-agent.md) - 多 Agent 协作
