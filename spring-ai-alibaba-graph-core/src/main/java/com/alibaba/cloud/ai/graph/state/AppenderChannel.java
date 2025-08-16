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
package com.alibaba.cloud.ai.graph.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Collections.unmodifiableList;
import static java.util.Optional.ofNullable;

public class AppenderChannel<T> implements Channel<List<T>> {

	private static final Logger log = LoggerFactory.getLogger(AppenderChannel.class);

	/**
	 * A functional interface that is used to remove elements from a list.
	 *
	 * @param <T> the type of elements in the list
	 */
	@FunctionalInterface
	public interface RemoveIdentifier<T> {

		/**
		 * Compares the specified element with the element at the given index.
		 * @param element the element to be compared
		 * @param atIndex the index of the element to compare with
		 * @return a negative integer, zero, or a positive integer as this object is less
		 * than, equal to, or greater than the specified object.
		 */
		int compareTo(T element, int atIndex);

	}

	private final Reducer<List<T>> reducer;

	private final Supplier<List<T>> defaultProvider;

	/**
	 * Returns an {@link Optional} containing the current reducer if it is non-null.
	 * @return an {@code Optional} describing the current reducer wrapped in a non-empty
	 * optional, or an empty optional if no such reducer exists
	 */
	@Override
	public Optional<Reducer<List<T>>> getReducer() {
		return ofNullable(reducer);
	}

	/**
	 * Returns the default provider or {@code Optional.empty()} if no default provider is
	 * set.
	 * @return an {@code Optional} containing the default provider, or
	 * {@code Optional.empty()}
	 */
	@Override
	public Optional<Supplier<List<T>>> getDefault() {
		return ofNullable(defaultProvider);
	}

	/**
	 * Creates an instance of `AppenderChannel` using the provided supplier to get the
	 * default list.
	 * @param <T> the type of elements in the list
	 * @param defaultProvider a supplier that provides the default list of elements
	 * @return a new instance of `AppenderChannel`
	 */
	public static <T> AppenderChannel<T> of(Supplier<List<T>> defaultProvider) {
		return new AppenderChannel<>(defaultProvider);
	}

	/**
	 * Constructs a new instance of {@code AppenderChannel} with the specified default
	 * provider.
	 * @param defaultProvider a supplier for the default list that will be used when no
	 * other list is available
	 */
	private AppenderChannel(Supplier<List<T>> defaultProvider) {
		this.reducer = new Reducer<>() {
			/**
			 * Combines two lists into one. If the first list is null, the second list is
			 * returned. Otherwise, the second list is added to the end of the first list
			 * and the resulting list is returned.
			 * @param left the first list; may be null
			 * @param right the second list
			 * @return a new list containing all elements from both input lists
			 */
			@Override
			public List<T> apply(List<T> left, List<T> right) {
				if (left == null) {
					return right;
				}
				left.addAll(right);
				return left;
			}
		};
		this.defaultProvider = defaultProvider;
	}

	/**
	 * This method removes elements from a given list based on the specified
	 * {@link RemoveIdentifier}. It creates a copy of the original list, performs the
	 * removal operation, and returns an immutable view of the result.
	 * @param list The list from which elements will be removed.
	 * @param removeIdentifier An instance of {@link RemoveIdentifier} that defines how to
	 * identify elements for removal.
	 * @return An unmodifiable view of the modified list with specified elements removed.
	 */
	private List<T> remove(List<T> list, RemoveIdentifier<T> removeIdentifier) {
		var result = new ArrayList<>(list);
		removeFromList(result, removeIdentifier);
		return unmodifiableList(result);
	}

	/**
	 * Removes an element from the list that matches the specified identifier.
	 *
	 * <p>
	 * This method iterates over the provided list and removes the first element for which
	 * the {@link RemoveIdentifier#compareTo} method returns zero.
	 * </p>
	 * @param result the list to be modified
	 * @param removeIdentifier the identifier used to find the element to remove
	 */
	private void removeFromList(List<T> result, RemoveIdentifier<T> removeIdentifier) {
		for (int i = 0; i < result.size(); i++) {
			if (removeIdentifier.compareTo(result.get(i), i) == 0) {
				result.remove(i);
				break;
			}
		}
	}

	/**
	 * Represents a record for data removal operations with generic types.
	 *
	 * @param <T> the type of elements in the old values list
	 */
	public record RemoveData<T>(List<T> oldValues, List<? extends Object> newValues) {

		// copy constructor. make sure to copy the list to make them modifiable
		public RemoveData {
			oldValues = new ArrayList<>(oldValues);
			newValues = new ArrayList<>(newValues);
		}
	};

	/**
	 * Evaluates the removal of identifiers from the new values list and updates the
	 * RemoveData object accordingly.
	 * @param oldValues a {@code List} of old values
	 * @param newValues a {@code List} of new values containing {@code RemoveIdentifier}s
	 * to be evaluated for removal
	 * @return a {@literal RemoveData<T>} object with updated old and new values after
	 * removing identifiers
	 */
	@SuppressWarnings("unchecked")
	private RemoveData<T> evaluateRemoval(List<T> oldValues, List<?> newValues) {

		final var result = new RemoveData<>(oldValues, newValues);

		newValues.stream().filter(value -> value instanceof RemoveIdentifier<?>).forEach(value -> {
			result.newValues().remove(value);
			var removeIdentifier = (RemoveIdentifier<T>) value;
			removeFromList(result.oldValues(), removeIdentifier);

		});
		return result;

	}

	/**
	 * Updates the value for a given key in the channel.
	 * @param key The key for which the value needs to be updated.
	 * @param oldValue The old value that is being replaced.
	 * @param newValue The new value to be set. If null, the old value will be returned.
	 * @return The updated old value or the new value if the update was successful.
	 * @throws UnsupportedOperationException If the channel does not support updates,
	 * typically due to an immutable list being used.
	 */
	@SuppressWarnings("unchecked")
	public Object update(String key, Object oldValue, Object newValue) {

		if (newValue == null) {
			return oldValue;
		}

		boolean oldValueIsList = oldValue instanceof List<?>;

		try {
			if (oldValueIsList && newValue instanceof RemoveIdentifier<?>) {
				return remove((List<T>) oldValue, (RemoveIdentifier<T>) newValue);
			}
			List<?> list = null;
			if (newValue instanceof List) {
				list = (List<Object>) newValue;
			}
			else if (newValue.getClass().isArray()) {
				list = Arrays.asList((T[]) newValue);
			}
			if (list != null) {
				if (list.isEmpty()) {
					return oldValue;
				}
				if (oldValueIsList) {
					var result = evaluateRemoval((List<T>) oldValue, list);
					return Channel.super.update(key, result.oldValues(), result.newValues());
				}
				return Channel.super.update(key, oldValue, list);
			}
			// this is to allow single value other than List or Array
			try {
				T typedValue = (T) newValue;
				return Channel.super.update(key, oldValue, List.of(typedValue));
			}
			catch (ClassCastException e) {
				log.error("Unsupported content type: {}", newValue.getClass());
				throw e;
			}
		}
		catch (UnsupportedOperationException ex) {
			log.error(
					"Unsupported operation: probably because the appendable channel has been initialized with a immutable List. Check please !");
			throw ex;
		}
	}

}
