package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.adapters;

import com.alibaba.cloud.ai.studio.admin.generator.model.App;
import com.alibaba.cloud.ai.studio.admin.generator.model.AppMetadata;
import com.alibaba.cloud.ai.studio.admin.generator.model.agent.Agent;
import com.alibaba.cloud.ai.studio.admin.generator.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Workflow;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractDSLAdapter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.Serializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/25 18:31
 */
@Component
public class AgentDSLAdapter extends AbstractDSLAdapter {

    private final Serializer serializer;
    private final ObjectMapper objectMapper;

    public AgentDSLAdapter(@Qualifier("yaml") Serializer serializer) {
        this.serializer = serializer;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void validateDSLData(Map<String, Object> dslData) {
        // TODO: 实现Agent DSL的验证逻辑
        if (dslData == null) {
            throw new IllegalArgumentException("invalid agent dsl: data is null");
        }
        // 验证必要的agent字段
        if (!dslData.containsKey("agent_class") || !dslData.containsKey("name")) {
            throw new IllegalArgumentException("invalid agent dsl: missing required fields");
        }
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public AppMetadata mapToMetadata(Map<String, Object> data) {
        AppMetadata metadata = new AppMetadata();
        metadata.setMode(AppMetadata.AGENT_MODE);  // 固定为agent模式
        metadata.setId(UUID.randomUUID().toString());
        metadata.setName((String) data.getOrDefault("name", "agent-" + metadata.getId()));
        metadata.setDescription((String) data.getOrDefault("description", ""));
        return metadata;
    }

    @Override
    public Map<String, Object> metadataToMap(AppMetadata metadata) {
        Map<String, Object> data = new HashMap<>();
        // Agent模式下的元数据转换
        data.put("name", metadata.getName());
        data.put("description", metadata.getDescription());
        data.put("mode", "agent");
        return data;
    }

    @Override
    public Agent mapToAgent(Map<String, Object> data) {
        // TODO: 实现从YAML到Agent的转换逻辑
        Agent agent = new Agent();

        // 设置基础属性
        agent.setAgentClass((String) data.get("agent_class"));
        agent.setName((String) data.get("name"));
        agent.setDescription((String) data.get("description"));
        agent.setOutputKey((String) data.get("output_key"));
        agent.setInputKey((String) data.get("input_key"));

        // 设置LLM相关配置
        agent.setModel((String) data.get("model"));
        agent.setInstruction((String) data.get("instruction"));
        if (data.get("max_iterations") != null) {
            agent.setMaxIterations((Integer) data.get("max_iterations"));
        }

        // 设置工具配置
        if (data.get("tools") != null) {
            agent.setTools((List<String>) data.get("tools"));
        }

        // 设置子agent配置（递归处理）
        if (data.get("sub_agents") != null) {
            List<Map<String, Object>> subAgentMaps = (List<Map<String, Object>>) data.get("sub_agents");
            List<Agent> subAgents = subAgentMaps.stream()
                    .map(this::mapToAgent)
                    .collect(Collectors.toList());
            agent.setSubAgents(subAgents);
        }

        // 设置流程配置
        if (data.get("flow_config") != null) {
            agent.setFlowConfig((Map<String, Object>) data.get("flow_config"));
        }

        // 设置状态配置
        if (data.get("state_config") != null) {
            agent.setStateConfig((Map<String, String>) data.get("state_config"));
        }

        // 设置钩子配置
        if (data.get("hooks") != null) {
            agent.setHooks((Map<String, Object>) data.get("hooks"));
        }

        return agent;
    }

    @Override
    public Map<String, Object> agentToMap(Agent agent) {
        // TODO: 实现从Agent到YAML的转换逻辑
        Map<String, Object> data = new HashMap<>();

        // 转换基础属性
        data.put("agent_class", agent.getAgentClass());
        data.put("name", agent.getName());
        data.put("description", agent.getDescription());
        data.put("output_key", agent.getOutputKey());
        data.put("input_key", agent.getInputKey());

        // 转换LLM相关配置
        data.put("model", agent.getModel());
        data.put("instruction", agent.getInstruction());
        data.put("max_iterations", agent.getMaxIterations());

        // 转换工具配置
        data.put("tools", agent.getTools());

        // 转换子agent配置（递归处理）
        if (agent.getSubAgents() != null) {
            List<Map<String, Object>> subAgentMaps = agent.getSubAgents().stream()
                    .map(this::agentToMap)
                    .collect(Collectors.toList());
            data.put("sub_agents", subAgentMaps);
        }

        // 转换流程配置
        data.put("flow_config", agent.getFlowConfig());

        // 转换状态配置
        data.put("state_config", agent.getStateConfig());

        // 转换钩子配置
        data.put("hooks", agent.getHooks());

        return data;
    }

    // 实现其他必要的抽象方法（返回null或空实现，因为Agent模式不需要这些）
    @Override
    public Workflow mapToWorkflow(Map<String, Object> data) {
        // Agent模式不需要workflow转换
        return null;
    }

    @Override
    public Map<String, Object> workflowToMap(Workflow workflow) {
        // Agent模式不需要workflow转换
        return new HashMap<>();
    }

    @Override
    public ChatBot mapToChatBot(Map<String, Object> data) {
        // Agent模式不需要chatbot转换
        return null;
    }

    @Override
    public Map<String, Object> chatbotToMap(ChatBot chatbot) {
        // Agent模式不需要chatbot转换
        return new HashMap<>();
    }

    @Override
    public Boolean supportDialect(DSLDialectType dialectType) {
        // 只支持agent类型的DSL
        return DSLDialectType.AGENT.equals(dialectType);
    }
}
