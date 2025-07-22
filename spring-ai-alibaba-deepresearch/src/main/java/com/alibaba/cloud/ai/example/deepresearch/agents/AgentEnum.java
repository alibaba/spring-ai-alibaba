package com.alibaba.cloud.ai.example.deepresearch.agents;

public enum AgentEnum {
    RESEARCH_AGENT("researchAgent"),
    CODER_AGENT("coderAgent"),
    COORDINATOR_AGENT("coordinatorAgent"),
    PLANNER_AGENT("plannerAgent"),
    REPORTER_AGENT("reporterAgent"),
    INTERACTION_AGENT("interactionAgent"),
    INFO_CHECK_AGENT("infoCheckAgent"),
    REFLECTION_AGENT("reflectionAgent");

    private final String agentName;

    AgentEnum(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentName() {
        return agentName;
    }

    public static AgentEnum fromBeanName(String agentName) {
        for (AgentEnum e : values()) {
            if (e.getAgentName().equals(agentName)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown agent agentName: " + agentName);
    }
} 