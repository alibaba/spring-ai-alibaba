/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.manus2.nodes;

import com.alibaba.cloud.ai.example.manus.contants.NodeConstants;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.tool.PlanningTool;
import com.alibaba.cloud.ai.example.manus2.plan.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus2.util.PromptUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.example.manus.contants.NodeConstants.CURRENT_PLAN;
import static com.alibaba.cloud.ai.example.manus.contants.NodeConstants.PLAN_ID;


public class PlannerNode extends LlmNode {


    private final List<DynamicAgentEntity> allAgents;
    private final ChatClient.Builder chatClientBuilder;
    private final Map<String, PlanningFactory.ToolCallBackContext> toolCallBackContextMap;
    public final ChatClient chatClient;
    private final PlanningTool planningTool;


    public PlannerNode(List<DynamicAgentEntity> allAgents,
                       ChatClient.Builder chatClientBuilder,
                       Map<String, PlanningFactory.ToolCallBackContext> toolCallBackContextMap
            , PlanningTool planningTool) {
        this.allAgents = allAgents;
        this.chatClientBuilder = chatClientBuilder;
        this.toolCallBackContextMap = toolCallBackContextMap;
        this.chatClient = chatClientBuilder.build();
        this.planningTool = planningTool;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        String planId = state.value(PLAN_ID, String.class).get();

        List<Message> userMessage = getMessages(state);

        String agentsInfo = buildAgentsInfo(allAgents, toolCallBackContextMap);

        // 1.生成计划提示
        Prompt planPrompt = generatePlanPrompt(state, agentsInfo, userMessage);

        // 2.使用 LLM 生成计划
        // 拿着结果给LLM进行评测
        PlanStructure planStructure = chatClient
                .prompt(planPrompt)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, planId))
                .call()
                .entity(PlanStructure.class);

        ExecutionPlan executionPlan = planStructure.toExecutionPlan(planId);

        return Map.of(
                CURRENT_PLAN, executionPlan
        );
    }


    /**
     * 构建代理信息字符串
     *
     * @param agents                 代理列表
     * @param toolCallBackContextMap
     * @return 格式化的代理信息
     */
    private static String buildAgentsInfo(List<DynamicAgentEntity> agents, Map<String, PlanningFactory.ToolCallBackContext> toolCallBackContextMap) {
        StringBuilder agentsInfo = new StringBuilder("以下是可用的智能代理（Agent）及其功能工具（Tool）：\n\n");

        for (DynamicAgentEntity agent : agents) {
            String agentName = agent.getAgentName();
            agentsInfo.append("### AgentName: ").append(agentName).append("\n");
            agentsInfo.append("AgentDescription: ").append(agent.getAgentDescription()).append("\n");
            // 获取该 agent 挂载的 toolKey 列表（你可能需要 agent.getToolKeys() 方法）
            List<String> toolKeys = agent.getAvailableToolKeys(); // 假设你有这个方法
            StringBuilder stringBuilder = new StringBuilder(agentName + " 提供的工具列表:【");
            if (toolKeys != null && !toolKeys.isEmpty()) {
                for (String toolKey : toolKeys) {
                    PlanningFactory.ToolCallBackContext ctx = toolCallBackContextMap.get(toolKey);
                    if (ctx != null && ctx.getToolCallback() != null) {
                        ToolCallback callback = ctx.getToolCallback();
                        stringBuilder.append("- Tool: ").append(callback.getToolDefinition().name()).append("\n");
                        stringBuilder.append(" Function: ").append(callback.getToolDefinition().description()).append("\n");
                    }
                }
            } else {
                stringBuilder.append("（该 Agent 当前未挂载工具）\n");
            }
            agentsInfo.append("\n").append(stringBuilder.append("】")).append("\n");
        }

        return agentsInfo.toString();
    }

    public static List<Message> getMessages(OverAllState state) {
        Optional<List> messages = state.value(NodeConstants.MESSAGES, List.class);
        ArrayList<Message> messages1 = messages.map(obj -> new ArrayList<>((List<Message>) obj))
                .orElseGet(ArrayList::new);
        return messages1;
    }

    private Prompt generatePlanPrompt(OverAllState state, String agentsInfo, List<Message> userMessage) {
        String plannerContent = PromptUtil.loadPrompt("planner_zh");
        String renderedPrompt = plannerContent
                .replace("{CURRENT_TIME}", ZonedDateTime.now().toString())
                .replace("{messages}", formatMessages(userMessage))
                .replace("{agentsInfo}", agentsInfo);

        // 3. 构建 Prompt 对象（使用 system + user）
        return new Prompt(List.of(
                new SystemMessage(renderedPrompt),
                new UserMessage("请根据以上内容输出 JSON 格式的计划")
        ));
    }

    private String formatMessages(List<Message> messages) {
        return messages.stream()
                .map(message -> {
                    if (message instanceof UserMessage userMsg) {
                        return "用户：" + userMsg.getText();
                    } else if (message instanceof AssistantMessage assistantMsg) {
                        return "助手：" + assistantMsg.getText();
                    } else {
                        return "[未知消息类型]";
                    }
                })
                .collect(Collectors.joining("\n"));
    }



    @Data
    public static class PlanStructure {

        private String title;

        private List<String> steps;

        public ExecutionPlan toExecutionPlan(String planId) {
            ExecutionPlan plan = new ExecutionPlan(planId, title);
            for (int index = 0; index < steps.size(); index++) {
                ExecutionStep executionStep = new ExecutionStep();
                executionStep.setStepIndex(index);
                executionStep.setStepRequirement(steps.get(index));
                plan.addStep(executionStep);
            }
            return plan;
        }
    }
}
