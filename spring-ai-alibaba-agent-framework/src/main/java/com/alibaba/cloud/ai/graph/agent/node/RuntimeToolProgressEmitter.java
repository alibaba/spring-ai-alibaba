/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ToolProgressEmitter;
import com.alibaba.cloud.ai.graph.agent.tools.ToolStreamingChunk;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runtime emitter that converts tool progress updates into graph streaming events.
 *
 * <p>
 * This class is package-private because it is an execution-time adapter used only by
 * {@link AgentToolNode}. It keeps tool progress emission logic separate from the main
 * node implementation while preserving the existing tool streaming contract.
 * </p>
 */
final class RuntimeToolProgressEmitter implements ToolProgressEmitter {

	private final AssistantMessage.ToolCall toolCall;

	private final Consumer<GraphResponse<NodeOutput>> sinkEmitter;

	private final Function<Throwable, String> errorMessageExtractor;

	private final OverAllState state;

	private final RunnableConfig config;

	private final AtomicBoolean terminated = new AtomicBoolean(false);

	RuntimeToolProgressEmitter(AssistantMessage.ToolCall toolCall, Consumer<GraphResponse<NodeOutput>> sinkEmitter,
			Function<Throwable, String> errorMessageExtractor, OverAllState state, RunnableConfig config) {
		this.toolCall = toolCall;
		this.sinkEmitter = sinkEmitter;
		this.errorMessageExtractor = errorMessageExtractor;
		this.state = state;
		this.config = config;
	}

	@Override
	public boolean next(String content, Map<String, Object> metadata) {
		if (terminated.get() || content == null) {
			return false;
		}
		ToolStreamingChunk chunk = new ToolStreamingChunk(toolCall.id(), toolCall.name(), content, metadata);
		sinkEmitter.accept(GraphResponse.of(new StreamingOutput<>(chunk, RunnableConfig.AGENT_TOOL_NAME,
				(String) config.metadata(RunnableConfig.AGENT_NAME_KEY).orElse(""), state,
				OutputType.AGENT_TOOL_STREAMING)));
		return true;
	}

	@Override
	public boolean error(Throwable throwable) {
		if (!terminated.compareAndSet(false, true)) {
			return false;
		}
		String message = errorMessageExtractor.apply(throwable);
		ToolStreamingChunk chunk = new ToolStreamingChunk(toolCall.id(), toolCall.name(), message, Map.of("error", true));
		sinkEmitter.accept(GraphResponse.of(new StreamingOutput<>(chunk, RunnableConfig.AGENT_TOOL_NAME,
				(String) config.metadata(RunnableConfig.AGENT_NAME_KEY).orElse(""), state,
				OutputType.AGENT_TOOL_STREAMING)));
		return true;
	}

	@Override
	public boolean isTerminated() {
		return terminated.get();
	}

	void finish() {
		terminated.set(true);
	}

}
