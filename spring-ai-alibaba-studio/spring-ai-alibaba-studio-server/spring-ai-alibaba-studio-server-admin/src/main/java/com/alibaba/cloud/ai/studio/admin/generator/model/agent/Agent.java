package com.alibaba.cloud.ai.studio.admin.generator.model.agent;

import java.util.List;
import java.util.Map;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/25 17:28
 */
public class Agent {

    // 基础属性
    private String agentClass;  // ReactAgent, SequentialAgent, ParallelAgent.etc
    private String name;
    private String description;
    private String outputKey;
    private String inputKey;

    // LLM相关配置
    private String model;
    private String instruction;
    private Integer maxIterations;
    private Map<String, Object> chatOptions;

    // 工具配置
    private List<String> tools;
    private Map<String, Object> toolConfig;

    // 子agent配置
    private List<Agent> subAgents;

    // 流程控制配置
    private Map<String, Object> flowConfig;

    // 状态管理配置
    private Map<String, String> stateConfig;

    // 钩子配置
    private Map<String, Object> hooks;

    public Agent() {}

    public String getAgentClass() { return agentClass; }
    public void setAgentClass(String agentClass) { this.agentClass = agentClass; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOutputKey() { return outputKey; }
    public void setOutputKey(String outputKey) { this.outputKey = outputKey; }

    public String getInputKey() { return inputKey; }
    public void setInputKey(String inputKey) { this.inputKey = inputKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public Integer getMaxIterations() { return maxIterations; }
    public void setMaxIterations(Integer maxIterations) { this.maxIterations = maxIterations; }

    public Map<String, Object> getChatOptions() { return chatOptions; }
    public void setChatOptions(Map<String, Object> chatOptions) { this.chatOptions = chatOptions; }

    public List<String> getTools() { return tools; }
    public void setTools(List<String> tools) { this.tools = tools; }

    public Map<String, Object> getToolConfig() { return toolConfig; }
    public void setToolConfig(Map<String, Object> toolConfig) { this.toolConfig = toolConfig; }

    public List<Agent> getSubAgents() { return subAgents; }
    public void setSubAgents(List<Agent> subAgents) { this.subAgents = subAgents; }

    public Map<String, Object> getFlowConfig() { return flowConfig; }
    public void setFlowConfig(Map<String, Object> flowConfig) { this.flowConfig = flowConfig; }

    public Map<String, String> getStateConfig() { return stateConfig; }
    public void setStateConfig(Map<String, String> stateConfig) { this.stateConfig = stateConfig; }

    public Map<String, Object> getHooks() { return hooks; }
    public void setHooks(Map<String, Object> hooks) { this.hooks = hooks; }
}
