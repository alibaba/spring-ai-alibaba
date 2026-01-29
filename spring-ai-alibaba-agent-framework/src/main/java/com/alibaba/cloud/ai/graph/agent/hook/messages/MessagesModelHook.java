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
package com.alibaba.cloud.ai.graph.agent.hook.messages;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;

import org.springframework.ai.chat.messages.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class MessagesModelHook implements Hook {
	private String agentName;
	private ReactAgent agent;

	public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
		return new AgentCommand(previousMessages);
	}

	public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
		return new AgentCommand(previousMessages);
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getAgentName() {
		return agentName;
	}

	@Override
	public ReactAgent getAgent() {
		return agent;
	}

	@Override
	public void setAgent(ReactAgent agent) {
		this.agent = agent;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	/**
	 * Creates a BeforeModelAction instance for the given MessagesModelHook.
	 * @param hook the MessagesModelHook instance to proxy
	 * @return a BeforeModelAction instance
	 */
	public static BeforeModelAction beforeModelAction(MessagesModelHook hook) {
		return new BeforeModelAction(hook);
	}

	/**
	 * Creates an AfterModelAction instance for the given MessagesModelHook.
	 * @param hook the MessagesModelHook instance to proxy
	 * @return an AfterModelAction instance
	 */
	public static AfterModelAction afterModelAction(MessagesModelHook hook) {
		return new AfterModelAction(hook);
	}

	/**
	 * Internal static class that proxies MessagesModelHook and implements
	 * AsyncNodeActionWithConfig interface.
	 */
	public static class BeforeModelAction implements AsyncNodeActionWithConfig {
		private final MessagesModelHook messagesModelHook;

		public BeforeModelAction(MessagesModelHook messagesModelHook) {
			this.messagesModelHook = messagesModelHook;
		}

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			@SuppressWarnings("unchecked")
			List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());

			AgentCommand command = messagesModelHook.beforeModel(messages, config);

			Map<String, Object> result = new HashMap<>();
			if (command.getMessages() != null) {
				if (UpdatePolicy.REPLACE == command.getUpdatePolicy()) {
					result.put("messages", ReplaceAllWith.of(command.getMessages()));
				} else {
					result.put("messages", command.getMessages());
				}
			}
			if (command.getJumpTo() != null) {
				result.put("jump_to", command.getJumpTo().name());
			}

			return CompletableFuture.completedFuture(result);
		}
	}

	/**
	 * Internal static class that proxies MessagesModelHook and implements
	 * AsyncNodeActionWithConfig interface for afterModel hook.
	 */
	public static class AfterModelAction implements AsyncNodeActionWithConfig {
		private final MessagesModelHook messagesModelHook;

		public AfterModelAction(MessagesModelHook messagesModelHook) {
			this.messagesModelHook = messagesModelHook;
		}

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			@SuppressWarnings("unchecked")
			List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());

			AgentCommand command = messagesModelHook.afterModel(messages, config);

			Map<String, Object> result = new HashMap<>();
			if (command.getMessages() != null) {
				if (UpdatePolicy.REPLACE == command.getUpdatePolicy()) {
					result.put("messages", ReplaceAllWith.of(command.getMessages()));
				} else {
					result.put("messages", command.getMessages());
				}
			}
			if (command.getJumpTo() != null) {
				result.put("jump_to", command.getJumpTo().name());
			}


			return CompletableFuture.completedFuture(result);
		}
	}

}
