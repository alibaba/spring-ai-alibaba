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
package com.alibaba.cloud.ai.graph.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Ordered sequence of partial state updates emitted within a single node step so
 * {@link com.alibaba.cloud.ai.graph.OverAllState} applies each update in call order
 * against the evolving state value without re-applying {@code KeyStrategy} to an
 * already-reduced result.
 */
public record SequentialUpdates(List<Object> updates) implements Iterable<Object> {

	public SequentialUpdates {
		updates = Collections.unmodifiableList(new ArrayList<>(updates));
	}

	public static SequentialUpdates of(List<?> updates) {
		return new SequentialUpdates(new ArrayList<>(updates));
	}

	public static SequentialUpdates append(Object existing, Object next) {
		List<Object> combined = new ArrayList<>();
		if (existing instanceof SequentialUpdates sequential) {
			combined.addAll(sequential.updates());
		}
		else {
			combined.add(existing);
		}
		combined.add(next);
		return new SequentialUpdates(combined);
	}

	@Override
	public Iterator<Object> iterator() {
		return updates.iterator();
	}

}
