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
