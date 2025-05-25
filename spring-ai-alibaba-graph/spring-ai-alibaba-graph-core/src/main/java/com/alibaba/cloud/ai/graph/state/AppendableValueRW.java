package com.alibaba.cloud.ai.graph.state;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

/**
 * A class that implements the {@link AppendableValue} interface and provides
 * functionality to append values to a list and retrieve various properties of the list.
 *
 * @param <T> the type of the value
 */
@Deprecated(forRemoval = true)
public class AppendableValueRW<T> implements AppendableValue<T>, Externalizable {

	private List<T> values;

	/**
	 * Constructs an AppendableValueRW with the given initial collection of values.
	 * @param values the initial collection of values
	 */
	public AppendableValueRW(Collection<T> values) {
		this.values = new ArrayList<>(values);
	}

	/**
	 * Constructs an AppendableValueRW with an empty list of values.
	 */
	public AppendableValueRW() {
		this(Collections.emptyList());
	}

	/**
	 * Appends a value or a collection of values to the list.
	 * @param value the value or collection of values to append
	 */
	public void append(Object value) {
		if (value instanceof Collection) {
			this.values.addAll((Collection<? extends T>) value);
		}
		else {
			this.values.add((T) value);
		}
	}

	/**
	 * Returns an unmodifiable list of values.
	 * @return an unmodifiable list of values
	 */
	public List<T> values() {
		return unmodifiableList(values);
	}

	/**
	 * Checks if the list of values is empty.
	 * @return true if the list of values is empty, false otherwise
	 */
	public boolean isEmpty() {
		return values().isEmpty();
	}

	/**
	 * Returns the size of the list of values.
	 * @return the size of the list of values
	 */
	public int size() {
		return values().size();
	}

	/**
	 * Returns the last value in the list, if present.
	 * @return an Optional containing the last value if present, otherwise an empty
	 * Optional
	 */
	public Optional<T> last() {
		List<T> values = values();
		return (values == null || values.isEmpty()) ? Optional.empty() : Optional.of(values.get(values.size() - 1));
	}

	/**
	 * Returns the value at the specified position from the end of the list, if present.
	 * @param n the position from the end of the list
	 * @return an Optional containing the value at the specified position if present,
	 * otherwise an empty Optional
	 */
	public Optional<T> lastMinus(int n) {
		if (values == null || values.isEmpty())
			return Optional.empty();
		if (n < 0)
			return Optional.empty();
		if (values.size() - n - 1 < 0)
			return Optional.empty();
		return Optional.of(values.get(values.size() - n - 1));
	}

	/**
	 * Returns a string representation of the list of values.
	 * @return a string representation of the list of values
	 */
	public String toString() {
		return String.valueOf(values);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(values);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		values = (List<T>) in.readObject();
	}

}
