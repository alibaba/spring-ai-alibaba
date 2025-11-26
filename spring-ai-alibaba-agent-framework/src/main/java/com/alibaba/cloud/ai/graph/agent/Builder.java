/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;

import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.SpringAIStateSerializer;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import org.springframework.util.Assert;

public abstract class Builder {

	protected String name;

	protected String description;

	protected String instruction;

	protected String systemPrompt;

	protected ChatModel model;

	protected ChatOptions chatOptions;

	protected ChatClient chatClient;

	protected List<ToolCallback> tools = new ArrayList<>();

	protected List<ToolCallbackProvider> toolCallbackProviders = new ArrayList<>();

	protected List<String> toolNames = new ArrayList<>();

	protected ToolCallbackResolver resolver;

	protected ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

	protected Map<String, Object> toolContext = new HashMap<>();

	protected boolean releaseThread;

	protected BaseCheckpointSaver saver;

	protected List<Hook> hooks = new ArrayList<>();
	protected List<Interceptor> interceptors = new ArrayList<>();
	protected List<ModelInterceptor> modelInterceptors = new ArrayList<>();
	protected List<ToolInterceptor> toolInterceptors = new ArrayList<>();

	protected boolean includeContents = true;
	protected boolean returnReasoningContents;

	protected String outputKey;

	protected KeyStrategy outputKeyStrategy;

	protected String inputSchema;
	protected Type inputType;

	protected String outputSchema;
	protected Class<?> outputType;

	protected ObservationRegistry observationRegistry;

	protected ChatClientObservationConvention customObservationConvention;

	protected AdvisorObservationConvention advisorObservationConvention;

	protected boolean enableLogging;

	protected StateSerializer stateSerializer;
	
	protected Executor executor;

	public Builder name(String name) {
		this.name = name;
		return this;
	}

	@Deprecated
	public Builder chatClient(ChatClient chatClient) {
		this.chatClient = chatClient;
		return this;
	}

	public Builder model(ChatModel model) {
		this.model = model;
		return this;
	}

	public Builder chatOptions(ChatOptions chatOptions) {
		this.chatOptions = chatOptions;
		return this;
	}

	public Builder tools(List<ToolCallback> tools) {
		Assert.notNull(tools, "tools cannot be null");
		Assert.noNullElements(tools, "tools cannot contain null elements");
		this.tools.addAll(tools);
		return this;
	}

	public Builder tools(ToolCallback... tools) {
		Assert.notNull(tools, "tools cannot be null");
		Assert.noNullElements(tools, "tools cannot contain null elements");
		this.tools.addAll(List.of(tools));
		return this;
	}

	public Builder methodTools(Object... toolObjects) {
		Assert.notNull(toolObjects, "toolObjects cannot be null");
		Assert.noNullElements(toolObjects, "toolObjects cannot contain null elements");
		this.tools.addAll(Arrays.asList(ToolCallbacks.from(toolObjects)));
		return this;
	}

	public Builder toolCallbackProviders(ToolCallbackProvider... toolCallbackProviders) {
		Assert.notNull(toolCallbackProviders, "toolCallbackProviders cannot be null");
		Assert.noNullElements(toolCallbackProviders, "toolCallbackProviders cannot contain null elements");
		this.toolCallbackProviders.addAll(List.of(toolCallbackProviders));
		return this;
	}

	public Builder toolNames(String... toolNames) {
		Assert.notNull(toolNames, "toolNames cannot be null");
		Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
		this.toolNames.addAll(List.of(toolNames));
		return this;
	}

	public Builder resolver(ToolCallbackResolver resolver) {
		this.resolver = resolver;
		return this;
	}

	public Builder toolExecutionExceptionProcessor(ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
		this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
		return this;
	}

	public Builder toolContext(Map<String, Object> toolContext) {
		Assert.notNull(toolContext, "toolContext cannot be null");
		Assert.noNullElements(toolContext.keySet(), "toolContext keys cannot contain null elements");
		Assert.noNullElements(toolContext.values(), "toolContext values cannot contain null elements");
		this.toolContext.putAll(toolContext);
		return this;
	}

	public Builder releaseThread(boolean releaseThread) {
		this.releaseThread = releaseThread;
		return this;
	}

	public Builder saver(BaseCheckpointSaver saver) {
		this.saver = saver;
		return this;
	}

	public Builder description(String description) {
		this.description = description;
		return this;
	}

	public Builder instruction(String instruction) {
		this.instruction = instruction;
		return this;
	}

	public Builder systemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
		return this;
	}

	public Builder outputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
	}

	public Builder outputKeyStrategy(KeyStrategy outputKeyStrategy) {
		this.outputKeyStrategy = outputKeyStrategy;
		return this;
	}

	public Builder inputSchema(String inputSchema) {
		this.inputSchema = inputSchema;
		return this;
	}

	public Builder inputType(Type inputType) {
		this.inputType = inputType;
		return this;
	}

	public Builder outputSchema(String outputSchema) {
		this.outputSchema = outputSchema;
		return this;
	}

	public Builder outputType(Class<?> outputType) {
		this.outputType = outputType;
		return this;
	}

	public Builder includeContents(boolean includeContents) {
		this.includeContents = includeContents;
		return this;
	}

	public Builder returnReasoningContents(boolean returnReasoningContents) {
		this.returnReasoningContents = returnReasoningContents;
		return this;
	}

	public Builder hooks(List<? extends Hook> hooks) {
		Assert.notNull(hooks, "hooks cannot be null");
		Assert.noNullElements(hooks, "hooks cannot contain null elements");
		this.hooks.addAll(hooks);
		return this;
	}

	public Builder hooks(Hook... hooks) {
		Assert.notNull(hooks, "hooks cannot be null");
		Assert.noNullElements(hooks, "hooks cannot contain null elements");
		this.hooks.addAll(List.of(hooks));
		return this;
	}

	public Builder interceptors(List<? extends Interceptor> interceptors) {
		Assert.notNull(interceptors, "interceptors cannot be null");
		Assert.noNullElements(interceptors, "interceptors cannot contain null elements");
		this.interceptors.addAll(interceptors);
		return this;
	}

	public Builder interceptors(Interceptor... interceptors) {
		Assert.notNull(interceptors, "interceptors cannot be null");
		Assert.noNullElements(interceptors, "interceptors cannot contain null elements");
		this.interceptors.addAll(List.of(interceptors));
		return this;
	}


	public Builder observationRegistry(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
		return this;
	}

	public Builder customObservationConvention(ChatClientObservationConvention customObservationConvention) {
		this.customObservationConvention = customObservationConvention;
		return this;
	}

	public Builder advisorObservationConvention(AdvisorObservationConvention advisorObservationConvention) {
		this.advisorObservationConvention = advisorObservationConvention;
		return this;
	}

	public Builder enableLogging(boolean enableLogging) {
		this.enableLogging = enableLogging;
		return this;
	}

	/**
	 * Sets the state serializer for the agent.
	 * @param stateSerializer the state serializer to use
	 * @return this builder instance
	 */
	public Builder stateSerializer(StateSerializer stateSerializer) {
		this.stateSerializer = stateSerializer;
		return this;
	}

	/**
	 * Sets the state serializer for the agent.
	 * @param stateSerializer the SpringAI state serializer to use
	 * @return this builder instance
	 * @deprecated Use {@link #stateSerializer(StateSerializer)} instead
	 */
	@Deprecated
	public Builder stateSerializer(SpringAIStateSerializer stateSerializer) {
		this.stateSerializer = stateSerializer;
		return this;
	}

	/**
	 * Sets the executor for parallel nodes.
	 * <p>
	 * This executor will be used for all parallel nodes in the agent's execution graph.
	 * When a parallel node is executed, it will use this executor to run the parallel
	 * branches concurrently.
	 * @param executor the {@link Executor} to use for parallel nodes
	 * @return this builder instance
	 */
	public Builder executor(Executor executor) {
		Assert.notNull(executor, "executor cannot be null");
		this.executor = executor;
		return this;
	}

	protected CompileConfig buildConfig() {
		SaverConfig saverConfig = SaverConfig.builder()
				.register(saver)
				.build();
		return CompileConfig.builder()
				.saverConfig(saverConfig)
				.recursionLimit(Integer.MAX_VALUE)
				.releaseThread(releaseThread)
				.build();
	}

	public abstract ReactAgent build();

}
