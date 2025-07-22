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

import com.alibaba.cloud.ai.example.deepresearch.config.PythonCoderProperties;
import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepository;
import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepositoryImpl;
import com.alibaba.cloud.ai.example.deepresearch.tool.PlannerTool;
import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import com.alibaba.cloud.ai.example.deepresearch.util.ResourceUtil;
import com.alibaba.cloud.ai.toolcalling.jinacrawler.JinaCrawlerConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Configuration
public class AgentsConfiguration {

    private final Resource researcherPrompt;
    private final Resource coderPrompt;
    private final Resource interactionPrompt;
    private final Resource reflectionPrompt;
    private final Resource reporterPrompt;
    private final ApplicationContext context;
    private final Map<String, AsyncMcpToolCallbackProvider> agent2AsyncMcpToolCallbackProvider;
    private final Map<String, SyncMcpToolCallbackProvider> agent2SyncMcpToolCallbackProvider;
    private final PythonCoderProperties coderProperties;
    private final PlannerTool plannerTool;
    private final AgentModelsConfiguration agentModelsConfiguration;
    private final ModelParamRepository modelParamRepository;

    public AgentsConfiguration(
            @Value("classpath:prompts/researcher.md") Resource researcherPrompt,
            @Value("classpath:prompts/coder.md") Resource coderPrompt,
            @Value("classpath:prompts/buildInteractiveHtmlPrompt.md") Resource interactionPrompt,
            @Value("classpath:prompts/reflection.md") Resource reflectionPrompt,
            @Value("classpath:prompts/reporter.md") Resource reporterPrompt,
            ApplicationContext context,
            Optional<Map<String, AsyncMcpToolCallbackProvider>> asyncProviders,
            Optional<Map<String, SyncMcpToolCallbackProvider>> syncProviders,
            PythonCoderProperties coderProperties,
            PlannerTool plannerTool,
            AgentModelsConfiguration agentModelsConfiguration,
            ModelParamRepository modelParamRepository
    ) {
        this.researcherPrompt = researcherPrompt;
        this.coderPrompt = coderPrompt;
        this.interactionPrompt = interactionPrompt;
        this.reflectionPrompt = reflectionPrompt;
        this.reporterPrompt = reporterPrompt;
        this.context = context;
        this.agent2AsyncMcpToolCallbackProvider = asyncProviders.orElse(null);
        this.agent2SyncMcpToolCallbackProvider = syncProviders.orElse(null);
        this.coderProperties = coderProperties;
        this.plannerTool = plannerTool;
        this.agentModelsConfiguration = agentModelsConfiguration;
        this.modelParamRepository = modelParamRepository;
    }

    private String getModelNameForAgent(String agentName) {
        return modelParamRepository.loadModels().stream()
            .filter(m -> agentName.equals(m.agentName()))
            .map(ModelParamRepositoryImpl.AgentModel::modelName)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No modelName found for agent: " + agentName));
    }

    @Bean
    public ChatClient researchAgent() {
        String modelName = getModelNameForAgent(AgentEnum.RESEARCH_AGENT.getAgentName());
        return buildResearchAgent(modelName);
    }

    @Bean
    public ChatClient coderAgent() {
        String modelName = getModelNameForAgent(AgentEnum.CODER_AGENT.getAgentName());
        return buildCoderAgent(modelName);
    }

    @Bean
    public ChatClient coordinatorAgent() {
        String modelName = getModelNameForAgent(AgentEnum.COORDINATOR_AGENT.getAgentName());
        return buildCoordinatorAgent(modelName);
    }

    @Bean
    public ChatClient plannerAgent() {
        String modelName = getModelNameForAgent(AgentEnum.PLANNER_AGENT.getAgentName());
        return buildPlannerAgent(modelName);
    }

    @Bean
    public ChatClient reporterAgent() {
        String modelName = getModelNameForAgent(AgentEnum.REPORTER_AGENT.getAgentName());
        return buildReporterAgent(modelName);
    }

    @Bean
    public ChatClient interactionAgent() {
        String modelName = getModelNameForAgent(AgentEnum.INTERACTION_AGENT.getAgentName());
        return buildInteractionAgent(modelName);
    }

    @Bean
    public ChatClient infoCheckAgent() {
        String modelName = getModelNameForAgent(AgentEnum.INFO_CHECK_AGENT.getAgentName());
        return buildInfoCheckAgent(modelName);
    }

    @Bean
    public ChatClient reflectionAgent() {
        String modelName = getModelNameForAgent(AgentEnum.REFLECTION_AGENT.getAgentName());
        return buildReflectionAgent(modelName);
    }

    /**
     * 统一入口，根据agentName和modelName构建ChatClient
     */
    public ChatClient buildAgentByName(String agentName, String modelName) {
        AgentEnum agentEnum = AgentEnum.fromBeanName(agentName);
        return buildAgentByEnum(agentEnum, modelName);
    }

    public ChatClient buildAgentByEnum(AgentEnum agentEnum, String modelName) {
        return switch (agentEnum) {
            case RESEARCH_AGENT -> buildResearchAgent(modelName);
            case CODER_AGENT -> buildCoderAgent(modelName);
            case COORDINATOR_AGENT -> buildCoordinatorAgent(modelName);
            case PLANNER_AGENT -> buildPlannerAgent(modelName);
            case REPORTER_AGENT -> buildReporterAgent(modelName);
            case INTERACTION_AGENT -> buildInteractionAgent(modelName);
            case INFO_CHECK_AGENT -> buildInfoCheckAgent(modelName);
            case REFLECTION_AGENT -> buildReflectionAgent(modelName);
            default -> throw new IllegalArgumentException("Unknown agent: " + agentEnum);
        };
    }

    private String[] getAvailableTools(String... toolNames) {
        return toolNames == null ? new String[0] :
                Arrays.stream(toolNames)
                        .filter(context::containsBean)
                        .toArray(String[]::new);
    }

    private ToolCallback[] getMcpToolCallbacks(String agentName) {
        if (CollectionUtils.isEmpty(agent2SyncMcpToolCallbackProvider)
                && CollectionUtils.isEmpty(agent2AsyncMcpToolCallbackProvider)) {
            return new ToolCallback[0];
        }
        if (!CollectionUtils.isEmpty(agent2SyncMcpToolCallbackProvider)) {
            var provider = agent2SyncMcpToolCallbackProvider.get(agentName);
            if (provider != null) return provider.getToolCallbacks();
        }
        if (!CollectionUtils.isEmpty(agent2AsyncMcpToolCallbackProvider)) {
            var provider = agent2AsyncMcpToolCallbackProvider.get(agentName);
            if (provider != null) return provider.getToolCallbacks();
        }
        return new ToolCallback[0];
    }

    private ChatClient buildAgent(
            ChatClient.Builder builder,
            Resource prompt,
            ToolCallback[] callbacks,
            Object... tools
    ) {
        var b = builder;
        if (prompt != null) {
            b = b.defaultSystem(ResourceUtil.loadResourceAsString(prompt));
        }
        if (tools != null && tools.length > 0) {
            b = b.defaultTools(tools);
        }
        if (callbacks != null && callbacks.length > 0) {
            b = b.defaultToolCallbacks(callbacks);
        }
        return b.build();
    }

    private ChatClient coordinatorAgentInternal(ChatClient.Builder builder, PlannerTool plannerTool) {
        return builder
                .defaultOptions(ToolCallingChatOptions.builder()
                        .internalToolExecutionEnabled(false)
                        .build())
                .defaultTools(plannerTool)
                .build();
    }

    public ChatClient buildResearchAgent(String modelName) {
        ChatClient.Builder builder = agentModelsConfiguration.builderChatClient(modelName).mutate();
        return buildAgent(
            builder,
            researcherPrompt,
            getMcpToolCallbacks("researchAgent"),
            (Object[]) getAvailableTools(JinaCrawlerConstants.TOOL_NAME)
        );
    }
    public ChatClient buildCoderAgent(String modelName) {
        ChatClient.Builder builder = agentModelsConfiguration.builderChatClient(modelName).mutate();
        return buildAgent(
            builder,
            coderPrompt,
            getMcpToolCallbacks("coderAgent"),
            new PythonReplTool(coderProperties)
        );
    }

    public ChatClient buildCoordinatorAgent(String modelName) {
        ChatClient.Builder builder = agentModelsConfiguration.builderChatClient(modelName).mutate();
        return coordinatorAgentInternal(builder, plannerTool);
    }

    public ChatClient buildPlannerAgent(String modelName) {
        ChatClient.Builder builder = agentModelsConfiguration.builderChatClient(modelName).mutate();
        return builder.build();
    }

    public ChatClient buildReporterAgent(String modelName) {
        ChatClient.Builder builder = agentModelsConfiguration.builderChatClient(modelName).mutate();
        return buildAgent(builder, reporterPrompt, null);
    }

    public ChatClient buildInteractionAgent(String modelName) {
        ChatClient.Builder builder = agentModelsConfiguration.builderChatClient(modelName).mutate();
        return buildAgent(builder, interactionPrompt, null);
    }

    public ChatClient buildInfoCheckAgent(String modelName) {
        ChatClient.Builder builder = agentModelsConfiguration.builderChatClient(modelName).mutate();
        return builder.build();
    }

    public ChatClient buildReflectionAgent(String modelName) {
        ChatClient.Builder builder = agentModelsConfiguration.builderChatClient(modelName).mutate();
        return buildAgent(builder, reflectionPrompt, null);
    }
}
