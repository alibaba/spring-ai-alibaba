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
package com.alibaba.cloud.ai.graph.internal.edge;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @param id The unique identifier for the edge value.
 * @param value The condition associated with the edge value.
 */
public record EdgeValue(String id, EdgeCondition value) {

	public EdgeValue(String id) {
		this(id, null);
	}

	public EdgeValue(EdgeCondition value) {
		this(null, value);
	}

	EdgeValue withTargetIdsUpdated(Function<String, EdgeValue> target) {
		if (id != null) {
			return target.apply(id);
		}

		var newMappings = value.mappings().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
			var v = target.apply(e.getValue());
			return (v.id() != null) ? v.id() : e.getValue();
		}));

		return new EdgeValue(null, new EdgeCondition(value.action(), newMappings));

	}

}
