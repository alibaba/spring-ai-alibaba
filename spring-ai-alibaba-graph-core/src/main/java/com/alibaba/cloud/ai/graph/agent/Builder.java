package com.alibaba.cloud.ai.graph.agent;

import java.util.List;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

public abstract class Builder {

	protected String name;

	protected String description;

	protected String instruction;

	protected String outputKey;

	protected ChatModel model;

	protected ChatOptions chatOptions;

	protected ChatClient chatClient;

	protected List<ToolCallback> tools;

	protected ToolCallbackResolver resolver;

	protected int maxIterations = 10;

	protected CompileConfig compileConfig;

	protected KeyStrategyFactory keyStrategyFactory;

	protected Function<OverAllState, Boolean> shouldContinueFunc;

	protected NodeAction preLlmHook;

	protected NodeAction postLlmHook;

	protected NodeAction preToolHook;

	protected NodeAction postToolHook;

	protected String inputKey = "messages";

	public Builder name(String name) {
		this.name = name;
		return this;
	}

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

	public Builder resolver(ToolCallbackResolver resolver) {
		this.resolver = resolver;
		return this;
	}

	public Builder maxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
		return this;
	}

	public Builder state(KeyStrategyFactory keyStrategyFactory) {
		this.keyStrategyFactory = keyStrategyFactory;
		return this;
	}

	public Builder compileConfig(CompileConfig compileConfig) {
		this.compileConfig = compileConfig;
		return this;
	}

	public Builder shouldContinueFunction(Function<OverAllState, Boolean> shouldContinueFunc) {
		this.shouldContinueFunc = shouldContinueFunc;
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

	public Builder outputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
	}

	public Builder preLlmHook(NodeAction preLlmHook) {
		this.preLlmHook = preLlmHook;
		return this;
	}

	public Builder postLlmHook(NodeAction postLlmHook) {
		this.postLlmHook = postLlmHook;
		return this;
	}

	public Builder preToolHook(NodeAction preToolHook) {
		this.preToolHook = preToolHook;
		return this;
	}

	public Builder postToolHook(NodeAction postToolHook) {
		this.postToolHook = postToolHook;
		return this;
	}

	public Builder inputKey(String inputKey) {
		this.inputKey = inputKey;
		return this;
	}

	public abstract ReactAgent build() throws GraphStateException;

}