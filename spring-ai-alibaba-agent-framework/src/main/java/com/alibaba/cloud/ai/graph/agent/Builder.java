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
import java.util.Arrays;
import java.util.List;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

public abstract class Builder {

	protected String name;

	protected String description;

	protected String instruction;

	protected String systemPrompt;

	protected ChatModel model;

	protected ChatOptions chatOptions;

	protected ChatClient chatClient;

	protected List<ToolCallback> tools;

	protected ToolCallbackResolver resolver;

	protected boolean releaseThread;

	protected BaseCheckpointSaver saver;

	protected List<? extends Hook> hooks;
	protected List<? extends Interceptor> interceptors;
	protected List<ModelInterceptor> modelInterceptors;
	protected List<ToolInterceptor> toolInterceptors;

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

	protected boolean enableLogging;

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
		this.tools = tools;
		return this;
	}

	public Builder tools(ToolCallback... tools) {
		this.tools = Arrays.asList(tools);
		return this;
	}

	public Builder resolver(ToolCallbackResolver resolver) {
		this.resolver = resolver;
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
		this.hooks = hooks;
		return this;
	}

	public Builder hooks(Hook... hooks) {
		this.hooks = Arrays.asList(hooks);
		return this;
	}

	public Builder interceptors(List<? extends Interceptor> interceptors) {
		this.interceptors = interceptors;
		return this;
	}

	public Builder interceptors(Interceptor... interceptors) {
		this.interceptors = Arrays.asList(interceptors);
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

	public Builder enableLogging(boolean enableLogging) {
		this.enableLogging = enableLogging;
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
