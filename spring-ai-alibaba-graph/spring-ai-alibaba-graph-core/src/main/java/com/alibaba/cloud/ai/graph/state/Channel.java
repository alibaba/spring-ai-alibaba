package com.alibaba.cloud.ai.graph.state;

import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * @param <T>
 */
class BaseChannel<T> implements Channel<T> {

	final Supplier<T> defaultProvider;

	final Reducer<T> reducer;

	BaseChannel(Reducer<T> reducer, Supplier<T> defaultProvider) {
		this.defaultProvider = defaultProvider;
		this.reducer = reducer;
	}

	public Optional<Supplier<T>> getDefault() {
		return ofNullable(defaultProvider);
	}

	public Optional<Reducer<T>> getReducer() {
		return ofNullable(reducer);
	}

}

/**
 * A Channel is a mechanism used to maintain a state property.
 * <p>
 * A Channel is associated with a key and a value. The Channel is updated by calling the
 * {@link #update(String, Object, Object)} method. The update operation is applied to the
 * channel's value.
 * <p>
 * The Channel may be initialized with a default value. This default value is provided by
 * a {@link Supplier}. The {@link #getDefault()} method returns an optional containing the
 * default supplier.
 * <p>
 * The Channel may also be associated with a Reducer. The Reducer is a function that
 * combines the current value of the channel with a new value and returns the updated
 * value.
 * <p>
 * The {@link #update(String, Object, Object)} method updates the channel's value with the
 * provided key, old value and new value. The update operation is applied to the channel's
 * value. If the channel is not initialized, the default value is used. If the channel is
 * initialized, the reducer is used to compute the new value.
 *
 * @param <T> the type of the state property
 */
public interface Channel<T> {

	static <T> Channel<T> of(Supplier<T> defaultProvider) {
		return new BaseChannel<T>(null, defaultProvider);
	}

	static <T> Channel<T> of(Reducer<T> reducer) {
		return new BaseChannel<T>(reducer, null);
	}

	static <T> Channel<T> of(Reducer<T> reducer, Supplier<T> defaultProvider) {
		return new BaseChannel<T>(reducer, defaultProvider);
	}

	/**
	 * The Reducer, if provided, is invoked for each state property to compute value.
	 * @return An optional containing the reducer, if it exists.
	 */
	Optional<Reducer<T>> getReducer();

	/**
	 * a Supplier that provide a default value. The result must be mutable.
	 * @return an Optional containing the default Supplier
	 */
	Optional<Supplier<T>> getDefault();

	/**
	 * Update the state property with the given key and returns the new value.
	 * @param key the key of the state property to be updated
	 * @param oldValue the current value of the state property
	 * @param newValue the new value to be set
	 * @return the new value of the state property
	 */
	default Object update(String key, Object oldValue, Object newValue) {
		T _new = (T) newValue;

		final T _old = (oldValue == null) ? getDefault().map(Supplier::get).orElse(null) : (T) oldValue;

		return getReducer().map(reducer -> reducer.apply(_old, _new)).orElse(_new);
	}

}
