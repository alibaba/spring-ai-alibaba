/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.flow.strategy;

import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Appends messages while handling a nested flow that returns the parent's full message
 * history.
 *
 * <p>
 * A nested flow receives the parent's state and writes its full state back. Appending that
 * full history would duplicate the parent's messages. When the incoming list already starts
 * with the previous list, this strategy appends only the new suffix. Repeated user messages
 * are still preserved because they are new entries, not a full-history snapshot.
 * </p>
 */
public class MessageAppendStrategy extends AppendStrategy {

	public MessageAppendStrategy() {
		super(true);
	}

	@Override
	public Object apply(Object oldValue, Object newValue) {
		final Object previous = oldValue instanceof Optional<?> optional ? optional.orElse(null) : oldValue;
		if (previous instanceof List<?> oldList && newValue instanceof List<?> newList
				&& newList.size() >= oldList.size() && startsWithSameInstances(newList, oldList)) {
			if (newList.size() == oldList.size()) {
				return oldValue;
			}
			newValue = new ArrayList<>(newList.subList(oldList.size(), newList.size()));
		}
		return super.apply(previous, newValue);
	}

	/**
	 * Checks the prefix by object identity rather than {@code equals}. Messages with
	 * identical content can be legitimate new turns, so content-based prefix detection
	 * would drop one of them.
	 */
	private static boolean startsWithSameInstances(List<?> newList, List<?> oldList) {
		for (int i = 0; i < oldList.size(); i++) {
			if (newList.get(i) != oldList.get(i)) {
				return false;
			}
		}
		return true;
	}

}
