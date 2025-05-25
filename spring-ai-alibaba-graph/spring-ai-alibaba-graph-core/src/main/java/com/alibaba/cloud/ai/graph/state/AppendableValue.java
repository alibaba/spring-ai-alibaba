package com.alibaba.cloud.ai.graph.state;

import java.util.List;
import java.util.Optional;

/**
 * Represents a value that can be appended to and provides various utility methods.
 *
 * @param <T> the type of the value
 */
@Deprecated(forRemoval = true)
public interface AppendableValue<T> {

	/**
	 * Returns the list of values.
	 * @return a list of values
	 */
	List<T> values();

	/**
	 * Checks if the value list is empty.
	 * @return true if the value list is empty, false otherwise
	 */
	boolean isEmpty();

	/**
	 * Returns the size of the value list.
	 * @return the size of the value list
	 */
	int size();

	/**
	 * Returns the last value in the list, if present.
	 * @return an Optional containing the last value if present, otherwise an empty
	 * Optional
	 */
	Optional<T> last();

	/**
	 * Returns the value at the specified position from the end of the list, if present.
	 * @param n the position from the end of the list
	 * @return an Optional containing the value at the specified position if present,
	 * otherwise an empty Optional
	 */
	Optional<T> lastMinus(int n);

}
