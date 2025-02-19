package com.alibaba.cloud.ai.graph.state;

import java.util.Objects;

/**
 * Represents a record that implements the {@link AppenderChannel.RemoveIdentifier<T>}
 * interface.
 *
 * @param <T> the type of the value to be associated with this RemoveByHash instance
 */
public record RemoveByHash<T>(T value) implements AppenderChannel.RemoveIdentifier<T> {
	/**
	 * Compares the hash code of this object with another element at a specific index.
	 * @param element the element to compare with
	 * @param atIndex the index of the element in the context (ignored in comparison)
	 * @return the difference between the hash codes of this object and the given element
	 */
	@Override
	public int compareTo(T element, int atIndex) {
		return Objects.hashCode(value) - Objects.hashCode(element);
	}

	/**
	 * Creates a new {@code RemoveByHash} instance with the specified value.
	 * @param <T> the type of the value
	 * @param value the value to store in the {@code RemoveByHash}
	 * @return a new {@code RemoveByHash} instance
	 */
	public static <T> RemoveByHash<T> of(T value) {
		return new RemoveByHash<>(value);
	}
}