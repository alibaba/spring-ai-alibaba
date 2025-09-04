package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent;

import java.util.List;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/28 17:53
 */
public class AgentShell {
    private final String type;
    private final String name;
    private final String description;
    private final String instruction;
    private final String inputKey;
    private final List<String> inputKeys;
    private final String outputKey;

    public AgentShell(String type, String name, String description, String instruction, String inputKey, List<String> inputKeys, String outputKey) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.instruction = instruction;
        this.inputKey = inputKey;
        this.inputKeys = inputKeys;
        this.outputKey = outputKey;
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getInstruction() { return instruction; }
    public String getInputKey() { return inputKey; }
    public List<String> getInputKeys() { return inputKeys; }
    public String getOutputKey() { return outputKey; }

    public static AgentShell of(String type, String name, String description, String instruction, String inputKey, List<String> inputKeys, String outputKey) {
        return new AgentShell(type, name, description, instruction, inputKey, inputKeys, outputKey);
    }
}
