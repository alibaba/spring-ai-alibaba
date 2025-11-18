package com.alibaba.cloud.ai.examples.documentation.graph.core;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

/**
 * 核心库概念指南示例
 * 演示 State、Nodes、Edges 的基本用法
 */
public class CoreLibraryExample {

    /**
     * 示例 A: 使用 AppendStrategy
     */
    public static KeyStrategyFactory createKeyStrategyFactory() {
        return () -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            return keyStrategyMap;
        };
    }

    /**
     * 示例 B: 自定义 KeyStrategyFactory
     */
    public static KeyStrategyFactory createCustomKeyStrategyFactory() {
        return () -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("property", (oldValue, newValue) ->
                ((String) newValue).toUpperCase()
            );
            return keyStrategyMap;
        };
    }

    /**
     * 基本节点示例
     */
    public static void basicNodeExample() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

        var myNode = node_async(state -> {
            System.out.println("In myNode: ");
            String input = (String) state.value("input").orElse("");
            return Map.of("results", "Hello " + input);
        });

        var myOtherNode = node_async(state -> Map.of());

        StateGraph builder = new StateGraph(keyStrategyFactory)
            .addNode("myNode", myNode)
            .addNode("myOtherNode", myOtherNode)
            .addEdge(START, "myNode")
            .addEdge("myNode", "myOtherNode")
            .addEdge("myOtherNode", END);

        CompiledGraph graph = builder.compile();
        System.out.println("Graph compiled successfully");
    }

    /**
     * 使用 RemoveByHash 删除消息示例
     */
    public static void removeMessagesExample() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

        var workflow = new StateGraph(keyStrategyFactory)
            .addNode("agent_1", node_async(state ->
                Map.of("messages", "message1")))
            .addNode("agent_2", node_async(state ->
                Map.of("messages", "message2.1")))
            .addNode("agent_3", node_async(state ->
                Map.of("messages", RemoveByHash.of("message2.1"))))
            .addEdge(START, "agent_1")
            .addEdge("agent_1", "agent_2")
            .addEdge("agent_2", "agent_3")
            .addEdge("agent_3", END);

        CompiledGraph graph = workflow.compile();
        System.out.println("Remove messages graph compiled successfully");
    }

    /**
     * 条件边示例
     */
    public static void conditionalEdgesExample() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();

        var workflow = new StateGraph(keyStrategyFactory)
            .addNode("nodeA", node_async(state -> Map.of("data", "A")))
            .addNode("nodeB", node_async(state -> Map.of("data", "B")))
            .addNode("nodeC", node_async(state -> Map.of("data", "C")))
            .addEdge(START, "nodeA")
            .addConditionalEdges("nodeA", edge_async(state -> "nodeB"),
                Map.of("nodeB", "nodeB", "nodeC", "nodeC"))
            .addEdge("nodeB", END)
            .addEdge("nodeC", END);

        CompiledGraph graph = workflow.compile();
        System.out.println("Conditional edges graph compiled successfully");
    }

    public static void main(String[] args) {
        System.out.println("=== 核心库概念示例 ===\n");

        try {
            // 示例 1: 基本节点示例
            System.out.println("示例 1: 基本节点示例");
            basicNodeExample();
            System.out.println();

            // 示例 2: 使用 RemoveByHash 删除消息示例
            System.out.println("示例 2: 使用 RemoveByHash 删除消息示例");
            removeMessagesExample();
            System.out.println();

            // 示例 3: 条件边示例
            System.out.println("示例 3: 条件边示例");
            conditionalEdgesExample();
            System.out.println();

            System.out.println("所有示例执行完成");
        } catch (Exception e) {
            System.err.println("执行示例时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

