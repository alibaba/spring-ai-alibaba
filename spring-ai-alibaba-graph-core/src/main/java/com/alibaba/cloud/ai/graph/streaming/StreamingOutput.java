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
package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static java.lang.String.format;

public class StreamingOutput<T> extends NodeOutput {

	@Deprecated
	private final String chunk;

	private final Message message;

	@JsonIgnore
	private final T originData;

	private OutputType outputType;

	public StreamingOutput(T originData, String node, OverAllState state) {
		super(node, state);
		this.chunk = null;
		this.message = null;
		this.originData = originData;
		trySetTokenUsage(originData);
	}

	// agentName is for graph and node working on Agent mode
	public StreamingOutput(T originData, String node, String agentName, OverAllState state) {
		super(node, agentName, state);
		this.chunk = null;
		this.message = null;
		this.originData = originData;
		trySetTokenUsage(originData);
	}

	public StreamingOutput(T originData, String node, String agentName, OverAllState state, OutputType outputType) {
		super(node, agentName, state);
		this.chunk = null;
		this.message = null;
		this.originData = originData;
		this.outputType = outputType;
		trySetTokenUsage(originData);
	}

	// new constructor to support Message
	public StreamingOutput(Message message, T originData, String node, String agentName, OverAllState state) {
		super(node, agentName, state);
		this.message = message;
		this.originData = originData;
		this.chunk = extractChunkFromMessage(message);
		trySetTokenUsage(originData);
	}

	public StreamingOutput(Message message, T originData, String node, String agentName, OverAllState state, OutputType outputType) {
		super(node, agentName, state);
		this.message = message;
		this.originData = originData;
		this.chunk = extractChunkFromMessage(message);
		this.outputType = outputType;
		trySetTokenUsage(originData);
	}

	public StreamingOutput(Message message, String node, String agentName, OverAllState state) {
		super(node, agentName, state);
		this.message = message;
		this.chunk = extractChunkFromMessage(message);
		this.originData = null;
	}

	public StreamingOutput(Message message, String node, String agentName, OverAllState state, OutputType outputType) {
		super(node, agentName, state);
		this.message = message;
		this.chunk = extractChunkFromMessage(message);
		this.originData = null;
		this.outputType = outputType;
	}

	// Constructor for Message with OverAllState and Usage (for buildNodeOutput)
	public StreamingOutput(Message message, String node, String agentName, OverAllState state, Usage usage) {
		super(node, agentName, state);
		this.message = message;
		this.chunk = extractChunkFromMessage(message);
		this.originData = null;
		setTokenUsage(usage);
	}

	public StreamingOutput(Message message, String node, String agentName, OverAllState state, Usage usage, OutputType outputType) {
		super(node, agentName, state);
		this.message = message;
		this.chunk = extractChunkFromMessage(message);
		this.originData = null;
		this.outputType = outputType;
		setTokenUsage(usage);
	}

	// Constructor for node output without Message but with Usage
	public StreamingOutput(String node, String agentName, OverAllState state, Usage usage) {
		super(node, agentName, state);
		this.message = null;
		this.chunk = null;
		this.originData = null;
		setTokenUsage(usage);
	}

	public StreamingOutput(String node, String agentName, OverAllState state, Usage usage, OutputType outputType) {
		super(node, agentName, state);
		this.message = null;
		this.chunk = null;
		this.originData = null;
		this.outputType = outputType;
		setTokenUsage(usage);
	}

	@Deprecated
	public StreamingOutput(String chunk, T originData, String node, String agentName, OverAllState state) {
		super(node, agentName, state);
		this.chunk = chunk;
		this.message = null;
		this.originData = originData;
		trySetTokenUsage(originData);
	}

	@Deprecated
	public StreamingOutput(String chunk, String node, String agentName, OverAllState state) {
		super(node, agentName, state);
		this.chunk = chunk;
		this.message = null;
		this.originData = null;
	}

	private static String extractChunkFromMessage(Message message) {
		if (message instanceof AssistantMessage assistantMessage) {
			if (!assistantMessage.hasToolCalls()) {
				return assistantMessage.getText();
			}
		}
		return null;
	}


	private void trySetTokenUsage(T originData) {
		if (originData instanceof ChatResponse chatResponse) {
			setTokenUsage(chatResponse.getMetadata().getUsage());
		} else if (originData instanceof Usage usage) {
			setTokenUsage(usage);
		}
	}

	@Deprecated
	public String chunk() {
		return chunk;
	}

	@JsonIgnore
	public T getOriginData() {
		return originData;
	}

	public Message message() {
		return message;
	}

	public OutputType getOutputType() {
		return outputType;
	}

	@Override
	public String toString() {
		if (node() == null) {
			return format("StreamingOutput{message=%s, chunk=%s}", message(), chunk());
		}
		return format("StreamingOutput{node=%s, agent=%s, message=%s, chunk=%s, tokenUsage=%s, state=%s, subGraph=%s}",
				node(), agent(), message(), chunk(), tokenUsage(), state(), isSubGraph());
	}

}
