package com.alibaba.cloud.ai.example.manus2.nodes;

import com.alibaba.cloud.ai.example.manus.contants.NodeConstants.ExecutorConstants;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus2.plan.ExecutionPlan;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ExecutorNode implements NodeAction {

    protected final PlanExecutionRecorder recorder;

    // 匹配字符串开头的方括号，支持中文和其他字符
    Pattern pattern = Pattern.compile("^\\s*\\[([^\\]]+)\\]");

    private final List<DynamicAgentEntity> agents;

    private final AgentService agentService;

    private ChatClient.Builder chatClient;



    public ExecutorNode(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder,
                        AgentService agentService,
                        ChatClient.Builder chatClient) {
        this.agents = agents;
        this.recorder = recorder;
        this.agentService = agentService;
        this.chatClient = chatClient;

    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 获取当前计划
        ExecutionPlan plan = (ExecutionPlan)state.value("current_plan").get();
        if (plan == null) {
            throw new IllegalStateException("No execution plan found in state");
        }

        // 获取计划中的所有步骤
        List<ExecutionStep> steps = plan.getSteps();

        // 为每个步骤创建一个 ReActAgent 并执行
        for (ExecutionStep step : steps) {
            // 创建 ReActAgent
            ReactAgent agent = ReactAgent.builder()
//                    .name()
//                    .chatClient(chatClient.build())
//                    .tools(step.)
//                    .maxIterations(maxIterations)
                    .build();

            // 编译 agent
            agent.getAndCompileGraph();

            // 执行当前步骤
            Map<String, Object> result = agent.asNodeAction(
                    "step_input", "step_output")
                    .apply(state);

            // 更新步骤状态和结果
            step.setReactAgent(agent);
            step.setResult((String) result.get("step_output"));
        }

        return Map.of(

        );
    }
}
