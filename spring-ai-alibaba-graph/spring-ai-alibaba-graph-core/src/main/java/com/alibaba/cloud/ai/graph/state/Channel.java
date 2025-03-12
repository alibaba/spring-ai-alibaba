/*
 * Copyright 2024-2026 the original author or authors.
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

	/**
	 * Constructs a new BaseChannel with the specified {@code reducer} and
	 * {@code defaultProvider}.
	 * @param reducer the function to apply in the reduction process; must not be null
	 * @param defaultProvider the supplier of default value for operations where no input
	 * is provided; must not be null
	 * @throws NullPointerException if either the {@code reducer} or
	 * {@code defaultProvider} is null
	 */
	BaseChannel(Reducer<T> reducer, Supplier<T> defaultProvider) {
		this.defaultProvider = defaultProvider;
		this.reducer = reducer;
	}

	/**
	 * Returns an {@link Optional} containing the default supplier if it is set;
	 * otherwise, returns an empty {@link Optional}.
	 * @return an {@link Optional} describing the default supplier or an empty
	 * {@link Optional} if none is set
	 */
	public Optional<Supplier<T>> getDefault() {
		return ofNullable(defaultProvider);
	}

	/**
	 * Retrieves the {@link Reducer} instance wrapped in an {@link Optional}.
	 * @return An {@link Optional} containing the {@code Reducer} object if it is
	 * non-null, or an empty {@link Optional} if it is null.
	 */
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

	/**
	 * Creates a channel with the specified default provider.
	 * @param <T> the type of items produced by this channel
	 * @param defaultProvider the supplier of the default item for the channel
	 * @return a new channel with the specified default provider
	 */
	static <T> Channel<T> of(Supplier<T> defaultProvider) {
		return new BaseChannel<T>(null, defaultProvider);
	}

	/**
	 * Creates a channel with the specified {@code Reducer}.
	 * @param <T> the type of elements in the channel
	 * @param reducer the function to reduce values as they are added to the channel
	 * @return a new channel instance
	 */
	static <T> Channel<T> of(Reducer<T> reducer) {
		return new BaseChannel<T>(reducer, null);
	}

	/**
	 * Creates a new channel instance with the specified {@code reducer} and
	 * {@code defaultProvider}.
	 * @param <T> the type of elements in this channel
	 * @param reducer the function to reduce elements from multiple suppliers into one
	 * value
	 * @param defaultProvider the supplier to provide a default value when no other value
	 * is available
	 * @return a new channel instance with the specified parameters
	 */
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
	@SuppressWarnings("unchecked")
	default Object update(String key, Object oldValue, Object newValue) {
		T _new = (T) newValue;

		final T _old = (oldValue == null) ? getDefault().map(Supplier::get).orElse(null) : (T) oldValue;

		return getReducer().map(reducer -> reducer.apply(_old, _new)).orElse(_new);
	}

}