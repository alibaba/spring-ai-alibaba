/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.messagechannel.dispatcher;

import java.util.List;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.messagechannel.model.ChannelMessage;
import com.alibaba.cloud.ai.messagechannel.model.ChannelReply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

/**
 * Routes a {@link ChannelMessage} to its bound {@link Agent}, runs the Agent
 * synchronously, and turns the resulting graph state into a {@link ChannelReply}.
 *
 * <p>The Agent is invoked with a {@link RunnableConfig#threadId() threadId} derived
 * from {@code channelName + ":" + conversationId} so that any checkpointer wired into
 * the Agent automatically segregates conversations across channels.</p>
 */
public class MessageChannelDispatcher {

	private static final Logger log = LoggerFactory.getLogger(MessageChannelDispatcher.class);

	private final AgentBindingRegistry bindings;

	public MessageChannelDispatcher(AgentBindingRegistry bindings) {
		this.bindings = bindings;
	}

	public ChannelReply dispatch(ChannelMessage message) {
		Agent agent = bindings.resolve(message.channelName());
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadIdFor(message))
				.build();
		try {
			Optional<OverAllState> result = agent.invoke(message.text(), config);
			return result.map(this::toReply).orElseGet(() -> ChannelReply.text(""));
		}
		catch (Exception e) {
			log.error("Agent invocation failed for channel={} user={}",
					message.channelName(), message.userId(), e);
			return ChannelReply.text("[agent error] " + e.getMessage());
		}
	}

	private String threadIdFor(ChannelMessage message) {
		String conv = message.conversationId();
		if (conv == null || conv.isBlank()) {
			conv = message.userId();
		}
		return message.channelName() + ":" + (conv == null ? "default" : conv);
	}

	@SuppressWarnings("unchecked")
	private ChannelReply toReply(OverAllState state) {
		Object value = state.value("messages").orElse(null);
		if (!(value instanceof List<?> messages) || messages.isEmpty()) {
			return ChannelReply.text("");
		}
		for (int i = messages.size() - 1; i >= 0; i--) {
			Object m = messages.get(i);
			if (m instanceof AssistantMessage assistant && assistant.getText() != null) {
				return ChannelReply.text(assistant.getText());
			}
			if (m instanceof Message generic && generic.getMessageType() == MessageType.ASSISTANT
					&& generic.getText() != null) {
				return ChannelReply.text(generic.getText());
			}
		}
		return ChannelReply.text("");
	}

}
