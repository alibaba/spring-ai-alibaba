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
package com.alibaba.cloud.ai.graph.serializer;

import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.MessageType;

import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

public class AgentInstructionMessage extends AbstractMessage {
	private transient boolean rendered;

	public AgentInstructionMessage(String textContent, Map<String, Object> metadata, boolean rendered) {
		super(MessageType.USER, textContent, metadata);
		this.rendered = rendered;
	}

	public AgentInstructionMessage(String textContent, Map<String, Object> metadata) {
		super(MessageType.USER, textContent, metadata);
	}

	public AgentInstructionMessage(String textContent) {
		this(textContent, Map.of());
	}

	public boolean isRendered() {
		return rendered;
	}

	public AgentInstructionMessage copy() {
		return new Builder().text(getText()).metadata(Map.copyOf(getMetadata())).rendered(isRendered()).build();
	}

	public AgentInstructionMessage.Builder mutate() {
		return new Builder().text(getText()).metadata(Map.copyOf(getMetadata())).rendered(isRendered());
	}

	@Override
	public String toString() {
		return "AgentInstructionMessage{" + "content='" + getText() + '\'' + ", properties=" + this.metadata + ", messageType="
				+ this.messageType + '}';
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		@Nullable
		private String textContent;

		private boolean rendered;

		private Map<String, Object> metadata = new HashMap<>();

		public Builder text(String textContent) {
			this.textContent = textContent;
			return this;
		}

		public Builder rendered(boolean rendered) {
			this.rendered = rendered;
			return this;
		}

		public Builder metadata(Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}

		public AgentInstructionMessage build() {
			return new AgentInstructionMessage(this.textContent, this.metadata, this.rendered);
		}

	}


}
