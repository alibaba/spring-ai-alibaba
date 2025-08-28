package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/28 17:53
 */
public class AgentShell {
    private final String type;
    private final String name;
    private final String description;
    private final String inputKey;
    private final String outputKey;

    public AgentShell(String type, String name, String description, String inputKey, String outputKey) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.inputKey = inputKey;
        this.outputKey = outputKey;
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getInputKey() { return inputKey; }
    public String getOutputKey() { return outputKey; }

    public static AgentShell of(String type, String name, String description, String inputKey, String outputKey) {
        return new AgentShell(type, name, description, inputKey, outputKey);
    }
}
