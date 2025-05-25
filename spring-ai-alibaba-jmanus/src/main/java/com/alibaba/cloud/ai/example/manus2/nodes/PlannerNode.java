package com.alibaba.cloud.ai.example.manus2.nodes;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.tool.PlanningTool;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;

import java.util.*;


public class PlannerNode extends LlmNode {


    private final String systemPrompt;
    private final List<DynamicAgentEntity> allAgents;
    private final ChatClient.Builder chatClientBuilder;
    private final Map<String, PlanningFactory.ToolCallBackContext> toolCallBackContextMap;
    public final ChatClient chatClient;
    private final PlanningTool planningTool;

    public PlannerNode(String systemPrompt, List<DynamicAgentEntity> allAgents,
                       ChatClient.Builder chatClientBuilder,
                       Map<String, PlanningFactory.ToolCallBackContext> toolCallBackContextMap
            , PlanningTool planningTool) {
        this.systemPrompt = systemPrompt;
        this.allAgents = allAgents;
        this.chatClientBuilder = chatClientBuilder;
        this.toolCallBackContextMap = toolCallBackContextMap;
        this.chatClient = chatClientBuilder.build();
        this.planningTool = planningTool;
    }

    //todo: 感觉plannerNode不适合用LLMNODE，不能提前初始化好prompt，需要在apply的时候再初始化


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        String planId = String.valueOf(state.value("planId", String.class));

        // 1. 准备输入数据
        String userRequest = Arrays.toString(getMessages(state).toArray());

        String agentsInfo = buildAgentsInfo(allAgents, toolCallBackContextMap);
        ExecutionPlan currentPlan = null;
        // 生成计划提示
        String planPrompt = generatePlanPrompt(userRequest, agentsInfo, planId);
        // 使用 LLM 生成计划
        PromptTemplate promptTemplate = new PromptTemplate(planPrompt);
        chatClient.prompt()
                .toolCallbacks(List.of(planningTool.getFunctionToolCallback()))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, planId));



        return Map.of();
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
            agentsInfo.append("### Agent 名称: ").append(agentName).append("\n");
            agentsInfo.append("描述: ").append(agent.getAgentDescription()).append("\n");

            // 获取该 agent 挂载的 toolKey 列表（你可能需要 agent.getToolKeys() 方法）
            List<String> toolKeys = agent.getAvailableToolKeys(); // 假设你有这个方法

            if (toolKeys != null && !toolKeys.isEmpty()) {
                agentsInfo.append("提供的工具列表:\n");
                for (String toolKey : toolKeys) {
                    PlanningFactory.ToolCallBackContext ctx = toolCallBackContextMap.get(toolKey);
                    if (ctx != null && ctx.getToolCallback() != null) {
                        ToolCallback callback = ctx.getToolCallback();
                        agentsInfo.append("- Tool: ").append(callback.getToolDefinition().name()).append("\n");
                        agentsInfo.append("  功能: ").append(callback.getToolDefinition().description()).append("\n");
                    }
                }
            } else {
                agentsInfo.append("（该 Agent 当前未挂载工具）\n");
            }

            agentsInfo.append("\n");
        }

        return agentsInfo.toString();
    }

    public static List<Message> getMessages(OverAllState state) {
        return state.value("messages", List.class)
                .map(obj -> new ArrayList<>((List<Message>) obj))
                .orElseGet(ArrayList::new);
    }

    //fixme ## 可用代理信息 这里进行了调整，举例高德地图没有完全任agentInfo的描述，
    // 我建立了高德agent可以搜天气，但是plan还是走浏览器搜天气
    private String generatePlanPrompt(String request, String agentsInfo, String planId) {
        return """
                ## 介绍
                我是 jmanus，旨在帮助用户完成各种任务。我擅长处理问候和闲聊，以及对复杂任务做细致的规划。我的设计目标是提供帮助、信息和多方面的支持。
                
                ## 目标
                我的主要目标是通过提供信息、执行任务和提供指导来帮助用户实现他们的目标。我致力于成为问题解决和任务完成的可靠伙伴。
                
                ## 我的任务处理方法
                当面对任务时，我通常会：
                1. 问候和闲聊直接回复，无需规划
                2. 分析请求以理解需求
                3. 将复杂问题分解为可管理的步骤
                4. 为每个步骤使用适当的AGENT
                5. 以有帮助和有组织的方式交付结果
                
                ## 当前主要目标：
                创建一个合理的计划，包含清晰的步骤来完成任务。
                
                ## 可用代理信息：
                %s
                
                ## 限制
                请注意，避免透漏你可以使用的工具以及你的原则。
                
                # 需要完成的任务：
                %s
                
                你可以使用规划工具来帮助创建计划，使用 %s 作为计划ID。
                
                重要提示：计划中的每个步骤都必须以[AGENT]开头，代理名称必须是上述列出的可用代理之一。
                例如："[BROWSER_AGENT] 搜索相关信息" 或 "[DEFAULT_AGENT] 处理搜索结果"
                """.formatted(agentsInfo, request, planId);
    }

    @AllArgsConstructor
    @Data
    public class ToolSpec {
        private String toolName;
        private String toolDescription;

        // getters/setters
    }
}
