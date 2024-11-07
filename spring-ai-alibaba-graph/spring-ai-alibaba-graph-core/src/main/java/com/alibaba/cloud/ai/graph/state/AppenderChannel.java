package com.alibaba.cloud.ai.graph.state;

import com.alibaba.cloud.ai.graph.utils.CollectionsUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.listOf;

/*
 * AppenderChannel is a {@link Channel} implementation that
 * is used to accumulate a list of values.
 *
 * @param <T> the type of the values being accumulated
 * @see Channel
 */
@Slf4j
public class AppenderChannel<T> implements Channel<List<T>> {

	private final Reducer<List<T>> reducer;

	private final Supplier<List<T>> defaultProvider;

	@Override
	public Optional<Reducer<List<T>>> getReducer() {
		return ofNullable(reducer);
	}

	@Override
	public Optional<Supplier<List<T>>> getDefault() {
		return ofNullable(defaultProvider);
	}

	public static <T> AppenderChannel<T> of(Supplier<List<T>> defaultProvider) {
		return new AppenderChannel<>(defaultProvider);
	}

	private AppenderChannel(Supplier<List<T>> defaultProvider) {
		this.reducer = new Reducer<List<T>>() {
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

	public Object update(String key, Object oldValue, Object newValue) {

		if (newValue == null) {
			return oldValue;
		}
		try {
			List<?> list = null;
			if (newValue instanceof List) {
				list = (List<?>) newValue;
			}
			else if (newValue.getClass().isArray()) {
				list = Arrays.asList((Object[]) newValue);
			}
			if (list != null) {
				if (list.isEmpty()) {
					return oldValue;
				}
				return Channel.super.update(key, oldValue, list);
			}
			// this is to allow single value other than List or Array
			try {
				T typedValue = (T) newValue;
				return Channel.super.update(key, oldValue, CollectionsUtils.listOf(typedValue));
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
