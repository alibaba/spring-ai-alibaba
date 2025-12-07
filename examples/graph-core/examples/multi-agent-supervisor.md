---
title: 多智能体监督者模式
description: 使用 Spring AI Alibaba Graph 实现多智能体监督者模式，通过监督者智能体协调不同的工作智能体
keywords: [Spring AI Alibaba, Graph, 多智能体, Supervisor, 监督者模式, Multi-Agent]
---

# 多智能体监督者模式

多智能体监督者模式是一种常见的多智能体系统架构，其中有一个监督者智能体负责协调和路由任务到不同的工作智能体。

## 架构概述

在多智能体监督者模式中：

- **Supervisor Agent（监督者智能体）**：负责路由到不同的 worker agents
- **Worker Agents（工作智能体）**：执行具体任务的智能体，如 Researcher Agent、Coder Agent 等

## 实现示例

<Code
  language="java"
  title="多智能体监督者模式实现" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/MultiAgentSupervisorExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class MultiAgentSupervisorExample {

    private final ChatModel chatModel;
    private final ChatModel chatModelWithTool;

    public MultiAgentSupervisorExample(ChatModel chatModel, ChatModel chatModelWithTool) {
        this.chatModel = chatModel;
        this.chatModelWithTool = chatModelWithTool;
    }

    /**
     * 创建 Multi-Agent Supervisor Graph
     */
    public CompiledGraph createSupervisorGraph() throws GraphStateException {
        // 定义状态管理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("messages", new AppendStrategy());
            strategies.put("next", new ReplaceStrategy());
            return strategies;
        };

        // 创建 agents
        String[] members = {"researcher", "coder"};
        SupervisorNode supervisor = new SupervisorNode(chatModel, members);
        ResearcherNode researcher = new ResearcherNode(chatModelWithTool);
        CoderNode coder = new CoderNode(chatModelWithTool);

        // 构建 StateGraph
        StateGraph workflow = new StateGraph(keyStrategyFactory)
                .addNode("supervisor", node_async(supervisor))
                .addNode("researcher", node_async(researcher))
                .addNode("coder", node_async(coder))
                .addEdge(START, "supervisor")
                .addConditionalEdges(
                        "supervisor",
                        edge_async(state -> {
                            String next = (String) state.value("next").orElse("FINISH");
                            return next;
                        }),
                        Map.of(
                                "FINISH", END,
                                "researcher", "researcher",
                                "coder", "coder"
                        )
                )
                .addEdge("researcher", "supervisor")
                .addEdge("coder", "supervisor");

        return workflow.compile();
    }

    /**
     * Supervisor Agent Node
     * 负责决定将任务路由到哪个 worker
     */
    public static class SupervisorNode implements NodeAction {
        private final ChatClient chatClient;
        private final String[] members;

        public SupervisorNode(ChatModel model, String[] members) {
            this.chatClient = ChatClient.builder(model).build();
            this.members = members;
        }

        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            // 获取最后一条消息
            List<Object> messages = (List<Object>) state.value("messages").orElse(List.of());
            if (messages.isEmpty()) {
                throw new IllegalStateException("No messages in state");
            }

            // 获取最后一条消息的文本内容
            String lastMessageText = extractTextFromMessage(messages.get(messages.size() - 1));

            // 构建系统提示
            String membersList = String.join(", ", members);
            String systemPrompt = String.format(
                    "你是一个 supervisor，负责管理以下 workers 之间的对话：%s。\n" +
                            "根据以下用户请求，响应应该由哪个 worker 来处理。\n" +
                            "每个 worker 将执行任务并返回结果和状态。\n" +
                            "当任务完成时，响应 FINISH。\n" +
                            "只返回 worker 名称（%s）或 FINISH，不要返回其他内容。",
                    membersList, membersList
            );

            // 调用 LLM 决定路由
            String result = chatClient.prompt()
                    .system(systemPrompt)
                    .user("用户消息: " + lastMessageText)
                    .call()
                    .content();

            // 清理结果，确保只返回 worker 名称或 FINISH
            String next = normalizeRoute(result, members);

            return Map.of("next", next);
        }

        /**
         * 规范化路由结果
         */
        private String normalizeRoute(String result, String[] members) {
            if (result == null || result.trim().isEmpty()) {
                return "FINISH";
            }

            String normalized = result.trim().toLowerCase();

            // 检查是否是 FINISH
            if (normalized.equals("finish") || normalized.contains("finish")) {
                return "FINISH";
            }

            // 检查是否匹配任何成员
            for (String member : members) {
                if (normalized.equals(member.toLowerCase()) ||
                        normalized.contains(member.toLowerCase())) {
                    return member;
                }
            }

            // 如果无法确定，根据消息内容推断
            return members.length > 0 ? members[0] : "FINISH";
        }

        private String extractTextFromMessage(Object message) {
            if (message instanceof Map) {
                Map<?, ?> msgMap = (Map<?, ?>) message;
                Object content = msgMap.get("content");
                if (content != null) {
                    return content.toString();
                }
            }
            return message.toString();
        }
    }

    /**
     * Researcher Agent Node
     * 负责执行研究任务
     */
    public static class ResearcherNode implements NodeAction {
        private final ChatClient chatClient;

        public ResearcherNode(ChatModel model) {
            ToolCallback searchTool = FunctionToolCallback.builder("search", new SearchTool())
                    .description(SearchTool.DESCRIPTION)
                    .inputType(SearchTool.SearchRequest.class)
                    .build();

            this.chatClient = ChatClient.builder(model)
                    .defaultTools(searchTool)
                    .build();
        }

        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            // 获取最后一条消息
            List<Object> messages = (List<Object>) state.value("messages").orElse(List.of());
            if (messages.isEmpty()) {
                throw new IllegalStateException("No messages in state");
            }

            String lastMessageText = extractTextFromMessage(messages.get(messages.size() - 1));

            // 使用 ChatClient 调用 LLM，LLM 可能会调用搜索工具
            String result = chatClient.prompt()
                    .user(lastMessageText)
                    .call()
                    .content();

            // 返回结果消息
            return Map.of("messages", List.of(
                    Map.of("role", "assistant", "content", result)
            ));
        }

        private String extractTextFromMessage(Object message) {
            if (message instanceof Map) {
                Map<?, ?> msgMap = (Map<?, ?>) message;
                Object content = msgMap.get("content");
                if (content != null) {
                    return content.toString();
                }
            }
            return message.toString();
        }
    }

    /**
     * Coder Agent Node
     * 负责执行代码任务
     */
    public static class CoderNode implements NodeAction {
        private final ChatClient chatClient;

        public CoderNode(ChatModel model) {
            ToolCallback coderTool = FunctionToolCallback.builder("executeCode", new CoderTool())
                    .description(CoderTool.DESCRIPTION)
                    .inputType(CoderTool.CodeRequest.class)
                    .build();

            this.chatClient = ChatClient.builder(model)
                    .defaultTools(coderTool)
                    .build();
        }

        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            // 获取最后一条消息
            List<Object> messages = (List<Object>) state.value("messages").orElse(List.of());
            if (messages.isEmpty()) {
                throw new IllegalStateException("No messages in state");
            }

            String lastMessageText = extractTextFromMessage(messages.get(messages.size() - 1));

            // 使用 ChatClient 调用 LLM，LLM 可能会调用代码执行工具
            String result = chatClient.prompt()
                    .user(lastMessageText)
                    .call()
                    .content();

            // 返回结果消息
            return Map.of("messages", List.of(
                    Map.of("role", "assistant", "content", result)
            ));
        }

        private String extractTextFromMessage(Object message) {
            if (message instanceof Map) {
                Map<?, ?> msgMap = (Map<?, ?>) message;
                Object content = msgMap.get("content");
                if (content != null) {
                    return content.toString();
                }
            }
            return message.toString();
        }
    }

    /**
     * 搜索工具（模拟实现）
     */
    public static class SearchTool implements BiFunction<SearchTool.SearchRequest, ToolContext, String> {

        public static final String DESCRIPTION = """
                使用此工具在互联网上执行搜索。
                
                Usage:
                - query 参数是要搜索的查询字符串
                - 工具会执行搜索并返回搜索结果
                - 这是一个模拟实现，返回固定的搜索结果
                """;

        @Override
        public String apply(SearchRequest request, ToolContext toolContext) {
            System.out.println("搜索查询: '" + request.query + "'");
            // 模拟搜索结果
            return "下一届冬奥会将在意大利的科尔蒂纳举行，时间是2026年";
        }

        /**
         * 搜索请求结构
         */
        public static class SearchRequest {
            @JsonProperty(required = true)
            @JsonPropertyDescription("要搜索的查询字符串")
            public String query;

            public SearchRequest() {
            }

            public SearchRequest(String query) {
                this.query = query;
            }
        }
    }

    /**
     * 代码执行工具（模拟实现）
     */
    public static class CoderTool implements BiFunction<CoderTool.CodeRequest, ToolContext, String> {

        public static final String DESCRIPTION = """
                使用此工具执行 Java 代码并进行数学计算。
                
                Usage:
                - request 参数是要执行的代码请求
                - 如果你想查看某个值的输出，应该使用 System.out.println(...); 打印出来
                - 这对用户可见
                - 这是一个模拟实现，返回固定的执行结果
                """;

        @Override
        public String apply(CodeRequest request, ToolContext toolContext) {
            System.out.println("代码执行请求: '" + request.request + "'");
            // 模拟代码执行结果
            return "2";
        }

        /**
         * 代码执行请求结构
         */
        public static class CodeRequest {
            @JsonProperty(required = true)
            @JsonPropertyDescription("要执行的代码请求")
            public String request;

            public CodeRequest() {
            }

            public CodeRequest(String request) {
                this.request = request;
            }
        }
    }
}`}
</Code>

## 执行示例

<Code
  language="java"
  title="执行多智能体监督者图" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/MultiAgentSupervisorExample.java"
>
{`// 执行 Graph（Supervisor -> Coder）
public void executeGraphWithCoder(CompiledGraph graph) {
    Map<String, Object> input = Map.of(
            "messages", List.of(
                    Map.of("role", "user", "content", "1 + 1 的结果是多少？")
            )
    );

    RunnableConfig config = RunnableConfig.builder()
            .threadId("supervisor-coder-thread")
            .build();

    graph.stream(input, config)
            .doOnNext(event -> System.out.println("节点: " + event.node() + ", 状态: " + event.state()))
            .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
            .doOnComplete(() -> System.out.println("流完成"))
            .blockLast();
}

// 执行 Graph（Supervisor -> Researcher）
public void executeGraphWithResearcher(CompiledGraph graph) {
    Map<String, Object> input = Map.of(
            "messages", List.of(
                    Map.of("role", "user", "content", "下一届冬奥会在哪里举行？")
            )
    );

    RunnableConfig config = RunnableConfig.builder()
            .threadId("supervisor-researcher-thread")
            .build();

    graph.stream(input, config)
            .doOnNext(event -> System.out.println("节点: " + event.node() + ", 状态: " + event.state()))
            .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
            .doOnComplete(() -> System.out.println("流完成"))
            .blockLast();
}`}
</Code>

## 关键特性

1. **监督者路由**：监督者智能体根据用户请求决定将任务路由到哪个工作智能体
2. **条件边**：使用条件边实现动态路由
3. **工具集成**：工作智能体可以使用工具（如搜索工具、代码执行工具）来完成任务
4. **状态管理**：使用 `messages` 和 `next` 键来管理对话状态和路由决策

## 最佳实践

1. **清晰的系统提示**：为监督者智能体提供清晰的系统提示，确保它能正确路由任务
2. **工具设计**：为每个工作智能体设计合适的工具，使其能够高效完成任务
3. **错误处理**：在节点中添加适当的错误处理逻辑
4. **状态规范化**：实现路由结果的规范化逻辑，确保路由决策的一致性

通过多智能体监督者模式，您可以构建一个灵活且可扩展的多智能体系统，能够根据不同的任务类型自动路由到合适的智能体。

