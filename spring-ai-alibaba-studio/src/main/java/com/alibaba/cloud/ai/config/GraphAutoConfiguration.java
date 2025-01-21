/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.graph.AiInsurance;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.HumanNode;
import com.alibaba.cloud.ai.graph.InitData;
import com.alibaba.cloud.ai.graph.InitDataSerializer;
import com.alibaba.cloud.ai.graph.IsAgentService;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.NodeOutputSerializer;
import com.alibaba.cloud.ai.graph.PromptNode;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.ToolService;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class GraphAutoConfiguration {

    @Bean
    public InitData initData(StateGraph stateGraph) {
        String title = "AGENT EXECUTOR";
        Map<String, InitData.ArgumentMetadata> inputArgs = new HashMap<>();
        inputArgs.put("input", new InitData.ArgumentMetadata("string", true));
        var graph = stateGraph.getGraph(GraphRepresentation.Type.MERMAID, title, false);
        return new InitData(title, graph.getContent(), inputArgs);
    }

    @Bean
    public StateGraph stateGraph(StateSerializer stateSerializer, AiInsurance aiInsurance) throws GraphStateException {
        MemorySaver saver = new MemorySaver();
        SaverConfig saverConfig = SaverConfig.builder()
                .type(SaverConstant.MEMORY)
                .register(SaverConstant.MEMORY, saver)
                .build();
        CompileConfig compileConfig =
                CompileConfig.builder().saverConfig(saverConfig).build();
        var subGraph = new StateGraph(stateSerializer)
                .addEdge(START, "select_prompt")
                .addNode("select_prompt", node_async(new PromptNode("请选择：1-人寿保险：100元/年，2-健康险：200元/年")))
                .addEdge("select_prompt", "user_select")
                .addNode("user_select", node_async(new HumanNode()))
                .addConditionalEdges( // 条件边，在agent节点之后
                        "user_select",
                        edge_async(aiInsurance::generateBills),
                        Map.of("continue", "do_purchase", "error", "error_prompt"))
                .addNode("do_purchase", node_async(new PromptNode("请使用付款链接#{url}付款，成功后确认", input -> {
                    var url = "www.bill.com?money=" + Integer.parseInt(input) * 100;
                    return Map.of("url", url);
                })))
                .addNode("error_prompt", node_async(new PromptNode("输入错误，请重新选择")))
                .addEdge("error_prompt", "select_prompt")
                .addEdge("do_purchase", "user_confirm")
                .addNode("user_confirm", node_async(new HumanNode()))
                .addConditionalEdges( // 条件边，在agent节点之后
                        "user_confirm",
                        edge_async(aiInsurance::payFinish),
                        Map.of("continue", "pay_success", "error", END, "prompt", "error_prompt1"))
                .addNode("error_prompt1", node_async(new PromptNode("参数错误，请选择确认或者取消")))
                .addEdge("error_prompt1", "user_confirm")
                .addNode("pay_success", node_async(new PromptNode("付款成功")))
                .addEdge("pay_success", END)
                .compile(compileConfig);

        return new StateGraph(stateSerializer)
                .addEdge(START, "welcome")
                .addNode(
                        "welcome",
                        node_async(new PromptNode(
                                "您好！我是您的保险助手popo。无论您是在寻找保障、规划未来，还是需要专业的保险建议，我都在这里为您提供帮助。请告诉我您的保险需求，让我们开始吧！"))) // 调用llm
                .addEdge("welcome", "input_customer_wish") // 下一个节点
                .addNode("input_customer_wish", node_async(new HumanNode()))
                .addConditionalEdges( // 条件边，在agent节点之后
                        "input_customer_wish",
                        edge_async(aiInsurance::questionEnough),
                        Map.of("input_enough", "llm_answer", "input_not_enough", "prompt"))
                .addNode("prompt", node_async(new PromptNode("请填写年龄、性别、学历等完整信息")))
                .addEdge("prompt", "llm_answer") // 下一个节点
                // .addEdge("human", "agent")// 下一个节点
                .addNode("llm_answer", node_async(aiInsurance::callAgent)) // 调用llm
                .addNode("input_customer_purchase_intention", node_async(new HumanNode()))
                .addEdge("llm_answer", "input_customer_purchase_intention") // 下一个节点
                .addConditionalEdges( // 条件边，在agent节点之后
                        "input_customer_purchase_intention",
                        edge_async(aiInsurance::purchaseIntention),
                        Map.of("want_purchase", "purchaseGraph", "not_want_purchase", "want_feedback"))
                .addNode("want_feedback", node_async(new PromptNode("感谢您的咨询，欢迎向我们反馈任何建议")))
                .addEdge("want_feedback", END)
                .addSubgraph("purchaseGraph", subGraph)
                .addEdge("purchaseGraph", END);
    }

    @Bean
    public StateSerializer stateSerializer() {
        return new JSONStateSerializer();
    }

    @Bean
    public AiInsurance aiInsurance(IsAgentService agentService) {
        return new AiInsurance(agentService);
    }

    @Bean
    public IsAgentService agentService(ChatClient.Builder chatClientBuilder, ToolService toolService) {
        return new IsAgentService(chatClientBuilder, toolService);
    }

    @Bean
    public ToolService toolService(ApplicationContext applicationContext) {
        return new ToolService(applicationContext);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customJackson() {
        return jackson2ObjectMapperBuilder -> jackson2ObjectMapperBuilder.modules(new SimpleModule()
                .addSerializer(InitData.class, new InitDataSerializer(InitData.class))
                .addSerializer(NodeOutput.class, new NodeOutputSerializer()));
    }
}
