package com.alibaba.cloud.ai.graph.prebuilt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.Channel;

/**
 * Represents the agent's state as a collection of messages
 *
 * @param <T> the type of messages stored in this state
 */
public class MessagesState<T> extends AgentState {

	public static final Map<String, Channel<?>> SCHEMA = Map.of("messages", AppenderChannel.of(ArrayList::new));

	/**
	 * Constructs a new instance of MessagesState with the provided initial data.
	 * @param initData A {@code Map} containing the initial data for this state object.
	 */
	public MessagesState(Map<String, Object> initData) {
		super(initData);
	}

	/**
	 * Retrieves the list of messages.
	 * @return the list of messages
	 * @throws RuntimeException if the "messages" key is not found in the state
	 */
	public List<T> messages() {
		return this.<List<T>>value("messages").orElseThrow(() -> new RuntimeException("messages not found"));
	}

	/**
	 * Returns the last value in the list, if present.
	 * @return an Optional containing the last value if present, otherwise an empty
	 * Optional
	 */
	public Optional<T> lastMessage() {
		var messages = messages();
		return (messages.isEmpty()) ? Optional.empty() : Optional.of(messages.get(messages.size() - 1));
	}

	/**
	 * Returns the value at the specified position from the end of the list, if present.
	 * @param n the position from the end of the list
	 * @return an Optional containing the value at the specified position if present,
	 * otherwise an empty Optional
	 */
	public Optional<T> lastMinus(int n) {
		var messages = messages();
		if (n < 0 || messages.isEmpty()) {
			return Optional.empty();
		}
		var index = messages.size() - n - 1;
		return (index < 0) ? Optional.empty() : Optional.of(messages.get(index));
	}

}