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
package com.alibaba.cloud.ai.graph.agent.hook.messages;

import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public class AgentCommand {
	private JumpTo jumpTo;
	private List<Message> messages;
	private UpdatePolicy updatePolicy;

	public AgentCommand(JumpTo jumpTo, List<Message> messages, UpdatePolicy updatePolicy) {
		this.jumpTo = jumpTo;
		this.messages = messages;
		this.updatePolicy = updatePolicy;
	}

	public AgentCommand(JumpTo jumpTo, List<Message> messages) {
		this(jumpTo, messages, UpdatePolicy.REPLACE);
	}

	public AgentCommand(List<Message> messages, UpdatePolicy updatePolicy) {
		this(null, messages, updatePolicy);
	}

	public AgentCommand(List<Message> messages) {
		this(null, messages, UpdatePolicy.REPLACE);
	}

	JumpTo getJumpTo() {
		return jumpTo;
	}

	void setJumpTo(JumpTo jumpTo) {
		this.jumpTo = jumpTo;
	}

	List<Message> getMessages() {
		return messages;
	}

	void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	UpdatePolicy getUpdatePolicy() {
		return updatePolicy;
	}

	void setUpdatePolicy(UpdatePolicy updatePolicy) {
		this.updatePolicy = updatePolicy;
	}
}
