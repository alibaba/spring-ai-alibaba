package com.alibaba.cloud.ai.example.manus2.nodes;

import com.alibaba.cloud.ai.example.manus.contants.NodeConstants;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus2.plan.ExecutionPlan;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FinalizerNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(FinalizerNode.class);

    private final ChatClient.Builder chatClientBuilder;

    @Autowired
    public FinalizerNode(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 获取当前计划
        ExecutionPlan plan = ExecutionPlan.fromJson(JSON.toJSONString(state.value(NodeConstants.CURRENT_PLAN)), null);
        if (plan == null) {
            throw new IllegalStateException("No execution plan found in state");
        }

        String summary = generateSummaryWithLLM(state,plan);

        return Map.of(NodeConstants.SUMMARY, summary);
    }

    private String generateSummaryWithLLM(OverAllState state, ExecutionPlan plan) {
        String executionDetail = plan.getPlanExecutionStateStringFormat(false);
        try {

            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate("""
                    您是 jmanus，一个能够回应用户请求的AI助手，你需要根据这个分步骤的执行计划的执行结果，来回应用户的请求。

                    分步骤计划的执行详情：
                    {executionDetail}

                    请根据执行详情里面的信息，来回应用户的请求。

                    """);

            Message systemMessage = systemPromptTemplate.createMessage(Map.of("executionDetail", executionDetail));

            String userRequestTemplate = """
                    当前的用户请求是:
                    {userRequest}
                    """;

            PromptTemplate userMessageTemplate = new PromptTemplate(userRequestTemplate);
            Message userMessage = userMessageTemplate.createMessage(Map.of("userRequest",
                   ExecutionPlan.fromJson(JSON.toJSONString(state.value(NodeConstants.CURRENT_PLAN)), null).getTitle()));

            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            ChatResponse response = chatClientBuilder.build()
                    .prompt(prompt)
                    .call()
                    .chatResponse();

            String summary = response.getResult().getOutput().getText();
            log.info("Generated summary: {}", summary);
            return summary;
        }
        catch (Exception e) {
            log.error("Error generating summary with LLM", e);
            throw new RuntimeException("Failed to generate summary", e);
        }
    }


}