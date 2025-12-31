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
package com.alibaba.cloud.ai.graph.agent.hook;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;


import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;

/**
 * Hook that supports interruption and feedback mechanism.
 * 
 * This hook checks for interruption requests and handles feedback messages.
 * 
 * Example:
 * InterruptionHook hook = InterruptionHook.builder().build();
 */
@HookPositions({HookPosition.BEFORE_MODEL})
public class InterruptionHook extends ModelHook implements AsyncNodeActionWithConfig, InterruptableAction {
	private static final Logger log = LoggerFactory.getLogger(InterruptionHook.class);
	
	public static final String INTERRUPTION_FEEDBACK_KEY = "INTERRUPTION_FEEDBACK";
	public static final String INTERRUPTION_NODE_NAME = "INTERRUPTION";

	private InterruptionHook(Builder builder) {
		// No additional configuration needed for now
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	@Override
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		String threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
		Map<String, Object> agentThreadState = this.getAgent().getThreadState(threadId);

		if (agentThreadState == null) {
			log.debug("No agent thread state found for threadId {}, continuing normal execution.", threadId);
			return CompletableFuture.completedFuture(Map.of());
		}

		// Atomically get and remove the feedback key using remove()
		// ConcurrentHashMap.remove() is thread-safe and returns the removed value
		// This ensures thread-safety: if updateAgentState() is called concurrently,
		// it will either see the old value (before removal) or set a new value (after removal)
		// The remove() operation is atomic, preventing race conditions
		Object feedback = agentThreadState.remove(INTERRUPTION_FEEDBACK_KEY);
		
		if (feedback == null) {
			log.debug("No interruption feedback found in state, continue reasoning with no updates.");
			return CompletableFuture.completedFuture(Map.of());
		}

		List<Message> feedbackMessages = new ArrayList<>();
		
		// Check if feedback is List<Message>, UserMessage, or String
		if (feedback instanceof List<?> feedbackList) {
			// Check if all elements in the list are Message instances
			for (Object item : feedbackList) {
				if (item instanceof Message) {
					feedbackMessages.add((Message) item);
				} else {
					log.warn("Feedback list contains non-Message item, ignoring. Type: {}", 
							item != null ? item.getClass().getName() : "null");
				}
			}
			if (feedbackMessages.isEmpty()) {
				log.warn("Feedback list is empty or contains no valid Message instances, stop and wait for more input.");
				return CompletableFuture.completedFuture(Map.of());
			}
		} else if (feedback instanceof UserMessage) {
			feedbackMessages.add((UserMessage) feedback);
		} else if (feedback instanceof String) {
			feedbackMessages.add(new UserMessage((String) feedback));
		} else {
			log.warn("Interruption feedback is neither List<Message>, UserMessage nor String, stop and wait for more input. Type: {}",
					feedback.getClass().getName());
			return CompletableFuture.completedFuture(Map.of());
		}
		
		// Get current messages list
		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());

		if (!messages.isEmpty() && messages.get(messages.size() - 1) instanceof AssistantMessage assistantMessage) {
			if (assistantMessage.hasToolCalls()) {
				log.info("Last message is an AssistantMessage with tool calls, not adding interruption feedback to messages list.");
				return CompletableFuture.completedFuture(Map.of());
			}
		}
		
		// Create new messages list with the feedback messages appended
		List<Message> newMessages = new ArrayList<>(feedbackMessages);
		
		Map<String, Object> updates = new HashMap<>();
		updates.put("messages", newMessages);

		
		log.debug("Added {} interruption feedback message(s) to messages list and removed INTERRUPTION_FEEDBACK_KEY from state.", 
				feedbackMessages.size());
		
		return CompletableFuture.completedFuture(updates);
	}
	
	@Override
	public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
		// Check INTERRUPTION_FEEDBACK_KEY from state
		String threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
		Map<String, Object> agentThreadState = this.getAgent().getThreadState(threadId);
		if (agentThreadState == null) {
			log.debug("No agent thread state found for threadId {}, continuing normal execution.", threadId);
			return Optional.empty();
		}

		// Use get() with synchronization awareness - ConcurrentHashMap.get() is thread-safe
		// but we need to be careful about the value being modified between get() and check
		Object feedbackValue = agentThreadState.get(INTERRUPTION_FEEDBACK_KEY);
		
		if (feedbackValue == null) {
			// No INTERRUPTION_FEEDBACK_KEY in state, continue normal execution
			log.debug("No INTERRUPTION_FEEDBACK_KEY in state, continuing normal execution.");
			return Optional.empty();
		}

		// Check if feedback is a list
		if (feedbackValue instanceof List<?> feedbackList) {

			if (feedbackList.isEmpty()) {
				// Empty list - return InterruptionMetadata to interrupt
				InterruptionMetadata interruptionMetadata = InterruptionMetadata.builder(nodeId, state)
						.addMetadata("interruption_requested", true)
						.build();
				
				log.debug("INTERRUPTION_FEEDBACK_KEY is empty list, returning InterruptionMetadata.");
				return Optional.of(interruptionMetadata);
			} else {
				// Non-empty list - continue normal execution
				log.debug("INTERRUPTION_FEEDBACK_KEY has non-empty list, continuing normal execution.");
				return Optional.empty();
			}
		} else {
			// Not a list - continue normal execution
			log.debug("INTERRUPTION_FEEDBACK_KEY is not a list, continuing normal execution.");
			return Optional.empty();
		}
	}
	
	@Override
	public String getName() {
		return INTERRUPTION_NODE_NAME;
	}
	
	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
	}

	@Override
	public Map<String, KeyStrategy> getKeyStrategys() {
		return Map.of(INTERRUPTION_FEEDBACK_KEY, new ReplaceStrategy());
	}

	public static class Builder {
		
		public InterruptionHook build() {
			return new InterruptionHook(this);
		}
	}
}

