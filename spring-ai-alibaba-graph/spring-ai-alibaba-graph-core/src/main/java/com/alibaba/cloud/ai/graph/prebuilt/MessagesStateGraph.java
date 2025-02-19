package com.alibaba.cloud.ai.graph.prebuilt;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;

/**
 * Represents a state graph with messages for generic type T. Extends the
 * {@link StateGraph} for specific state handling of {@link MessagesState<T>}.
 *
 * @param <T> type parameter representing the data associated with each message state
 */
public class MessagesStateGraph<T> extends StateGraph<MessagesState<T>> {

	/**
	 * Constructs a new instance of {@code MessagesStateGraph}.
	 * @param stateSerializer the serializer for messages states, must not be null
	 */
	public MessagesStateGraph(StateSerializer<MessagesState<T>> stateSerializer) {
		super(MessagesState.SCHEMA, stateSerializer);
	}

	/**
	 * Default constructor that initializes a new instance of {@link MessagesStateGraph}.
	 * This constructor uses the default schema and constructor from the base class.
	 */
	public MessagesStateGraph() {
		super(MessagesState.SCHEMA, MessagesState::new);
	}

}