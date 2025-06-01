package com.alibaba.cloud.ai.example.manus2.nodes;

import com.alibaba.cloud.ai.example.manus.contants.NodeConstants;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutor;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus2.plan.ExecutionPlan;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.fastjson.JSON;
import io.micrometer.common.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.ToolCallback;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutor.*;

public class ExecutorNode implements NodeAction {

    protected final PlanExecutionRecorder recorder;

    // 匹配字符串开头的方括号，支持中文和其他字符
    Pattern pattern = Pattern.compile("^\\s*\\[([^\\]]+)\\]");

    private final List<DynamicAgentEntity> agents;

    private final Map<String, PlanningFactory.ToolCallBackContext> toolCallBackContextMap;

    private final AgentService agentService;

    private ChatClient.Builder chatClient;

    private Map<String, Object> envData = new HashMap<>();

    protected String collectEnvData(String toolCallName) {
        PlanningFactory.ToolCallBackContext context = toolCallBackContextMap.get(toolCallName);
        if (context != null) {
            return context.getFunctionInstance().getCurrentToolStateString();
        }
        return "";
    }

    protected Map<String, Object> collectAndSetEnvDataForTools(List<String> availableToolKeys) {
        Map<String, Object> toolEnvDataMap = new HashMap<>();
        Map<String, Object> oldMap = getEnvData();
        toolEnvDataMap.putAll(oldMap);

        // 用新数据覆盖旧数据
        for (String toolKey : availableToolKeys) {
            String envData = collectEnvData(toolKey);
            toolEnvDataMap.put(toolKey, envData);
        }
        return toolEnvDataMap;
    }

    protected Message getNextStepWithEnvMessage(DynamicAgentEntity agent, Map<String, Object> initSettings) {
        if (StringUtils.isBlank(agent.getNextStepPrompt())) {
            return new UserMessage("");
        }

        StringBuilder envDataStringBuilder = new StringBuilder();

        for (String toolKey : agent.getAvailableToolKeys()) {
            Object value = getEnvData().get(toolKey);
            if (value == null || value.toString().isEmpty()) {
                continue;
            }
            envDataStringBuilder.append(toolKey).append(" 的上下文信息：\n");
            envDataStringBuilder.append("    ").append(value.toString()).append("\n");
        }

        Map<String, Object> data = new HashMap<>();
        data.putAll(initSettings);
        data.put(PlanExecutor.EXECUTION_ENV_STRING_KEY, envDataStringBuilder.toString());

        PromptTemplate promptTemplate = new PromptTemplate(agent.getNextStepPrompt());
        Message userMessage = promptTemplate.createMessage(data);
        return userMessage;
    }

    public Map<String, Object> getEnvData() {
        return envData;
    }

    public void setEnvData(Map<String, Object> envData) {
        this.envData = Collections.unmodifiableMap(new HashMap<>(envData));
    }

    /**
     * 获取步骤对应的agent
     *
     * @param stepRequirement 步骤要求
     * @return 对应的DynamicAgentEntity
     */
    private DynamicAgentEntity getAgent(String stepRequirement) {
        String stepAgentName = getStepFromStepReq(stepRequirement);
        Map<String, DynamicAgentEntity> map = agents.stream()
                .collect(Collectors.toMap(DynamicAgentEntity::getAgentName, Function.identity()));
        DynamicAgentEntity agentEntity = map.get(stepAgentName);
        if (agentEntity == null) {
            throw new IllegalStateException("Agent not found for step: " + stepAgentName);
        }
        return agentEntity;
    }

    public ExecutorNode(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder,
                        AgentService agentService,
                        ChatClient.Builder chatClient, Map<String, PlanningFactory.ToolCallBackContext> toolCallBackContextMap) {
        this.agents = agents;
        this.recorder = recorder;
        this.agentService = agentService;
        this.chatClient = chatClient;
        this.toolCallBackContextMap = toolCallBackContextMap;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 获取当前计划
        ExecutionPlan plan = (ExecutionPlan) state.value(NodeConstants.CURRENT_PLAN).get();
        if (plan == null) {
            throw new IllegalStateException("No execution plan found in state");
        }

        List<ExecutionStep> steps = plan.getSteps();

        // 为每个步骤执行
        for (int i = 0; i < steps.size(); i++) {
            ExecutionStep step = steps.get(i);
            executeStep(plan, step, i);
        }

        return Map.of();
    }

    private void executeStep(ExecutionPlan plan, ExecutionStep step, int stepIndex) throws Exception {

        // 创建 ReActAgent
        List<ToolCallback> toolCallbacks = initTools(plan, step, agents, toolCallBackContextMap);

        // 创建 LlmNode
        LlmNode llmNode = LlmNode.builder()
                .chatClient(chatClient.build())
                .beforeHook(state -> {
                    // 在每次think时收集环境信息
                    DynamicAgentEntity agentEntity = getAgent(step.getStepRequirement());
                    Map<String, Object> stringObjectMap = collectAndSetEnvDataForTools(agentEntity.getAvailableToolKeys());
                    state.updateState(Map.of("messages", stringObjectMap));
                    return null;
                })
                .toolCallbacks(toolCallbacks)
                .build();

        // 创建 ToolNode
        ToolNode toolNode = ToolNode.builder()
                .toolCallbacks(toolCallbacks)
                .build();

        // 创建 CompileConfig
        CompileConfig compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().build())
                .build();

        // 创建 ReactAgent
        ReactAgent agent = new ReactAgent(
                step.getStepRequirement(),  // name
                llmNode,                    // llmNode
                toolNode,                   // toolNode
                10,                         // maxIterations
                compileConfig,              // compileConfig
                null,                       // state
                null
        );
        CompiledGraph compiledGraph = agent.getAndCompileGraph();

        // 更新步骤状态和结果
        step.setReactAgent(agent);

        // 2. 组装消息
        OverAllState overAllState = compiledGraph.stateGraph.getOverAllState();
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        String currentDateTime = java.time.LocalDate.now().toString();

        Map<String, Object> initSettings = new HashMap<>();
        initSettings.put(PLAN_STATUS_KEY, getPlanExecutionStateStringFormat(plan, stepIndex));
        initSettings.put(CURRENT_STEP_INDEX_KEY, String.valueOf(step.getStepIndex()));
        initSettings.put(STEP_TEXT_KEY, step.getStepRequirement());
        initSettings.put(EXTRA_PARAMS_KEY, plan.getExecutionParams());

        String stepPrompt = """
                - SYSTEM INFORMATION:
                OS: %s %s (%s)
                
                - Current Date:
                %s
                - 全局计划信息:
                {planStatus}
                
                - 当前要做的步骤要求(这个步骤是需要当前智能体完成的!) :
                STEP {currentStepIndex} :{stepText}
                
                - 当前步骤的上下文信息:
                {extraParams}
                
                重要说明：
                1. 使用工具调用时，不需要额外的任何解释说明！
                2. 不要在工具调用前提供推理或描述！
                3. 做且只做当前要做的步骤要求中的内容
                4. 如果当前要做的步骤要求已经做完，则调用terminate工具来完成当前步骤。
                5. 全局目标 是用来有个全局认识的，不要在当前步骤中去完成这个全局目标。
                
                """.formatted(osName, osVersion, osArch, currentDateTime);

        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(stepPrompt);
        Message systemMessage = promptTemplate.createMessage(initSettings);

        // 获取当前步骤对应的agent和工具
        DynamicAgentEntity agentEntity = getAgent(step.getStepRequirement());
        // 获取环境信息消息
        Message envMessage = getNextStepWithEnvMessage(agentEntity, initSettings);

        overAllState.updateState(Map.of(
                "messages", List.of(
                        systemMessage,
                        envMessage
                )
        ));

        // 3. 调用graph.invoke
        Map<String, Object> result = compiledGraph.invoke(overAllState, RunnableConfig.builder().build()).get().data();

        List<Message> messages = (List<Message>) result.get("messages");
        Message lastMessage = messages.get(messages.size() - 1);
        step.setResult(JSON.toJSONString(lastMessage));
    }

    private String getPlanExecutionStateStringFormat(ExecutionPlan plan, int i) {
        StringBuilder state = new StringBuilder();

        // Add execution parameters if they exist
        state.append("\n- 执行参数: ").append("\n");
        if (plan.getExecutionParams() != null && !plan.getExecutionParams().isEmpty()) {
            state.append(JSON.toJSONString(plan.getExecutionParams())).append("\n\n");
        } else {
            state.append("未提供执行参数。\n\n");
        }

        // Add steps and their results up to index i
        state.append("- 已完成的步骤:\n");
        List<ExecutionStep> steps = plan.getSteps();
        for (int j = 0; j < i; j++) {
            ExecutionStep step = steps.get(j);
            state.append("步骤 ")
                    .append(j)
                    .append(": ")
                    .append(step.getStepRequirement())
                    .append("\n");

            String result = step.getResult();
            if (result != null && !result.isEmpty()) {
                state.append("执行结果: ").append("\n").append(result).append("\n\n");
            } else {
                state.append("\n");
            }
        }

        return state.toString();
    }

    private List<ToolCallback> initTools(ExecutionPlan plan, ExecutionStep step, List<DynamicAgentEntity> agents,
                                         Map<String, PlanningFactory.ToolCallBackContext> toolCallBackContextMap) {
        DynamicAgentEntity agentEntity = getAgent(step.getStepRequirement());
        List<String> availableToolKeys = agentEntity.getAvailableToolKeys();

        // 根据可用的工具key从toolCallBackContextMap中获取对应的ToolCallback
        return availableToolKeys.stream()
                .map(toolKey -> {
                    PlanningFactory.ToolCallBackContext context = toolCallBackContextMap.get(toolKey);
                    if (context == null) {
                        throw new IllegalStateException("Tool callback context not found for tool: " + toolKey);
                    }
                    return context.getToolCallback();
                })
                .collect(Collectors.toList());
    }

    public String getStepFromStepReq(String stepRequirement) {
        Matcher matcher = pattern.matcher(stepRequirement);
        if (matcher.find()) {
            // 对匹配到的内容进行trim和转小写处理
            return matcher.group(1).trim();
        }
        return "DEFAULT_AGENT"; // Default agent if no match found
    }

}
