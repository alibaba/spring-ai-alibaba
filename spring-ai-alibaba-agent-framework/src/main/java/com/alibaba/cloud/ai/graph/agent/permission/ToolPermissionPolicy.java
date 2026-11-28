/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.permission;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Application policy for tool calls dispatched by a ReactAgent. Implementations may be called
 * more than once per call and concurrently. Policy evaluation should be side-effect free.
 */
@FunctionalInterface
public interface ToolPermissionPolicy {

	enum Decision {
		ALLOW, DENY, REVIEW
	}

	Decision evaluate(ToolCallRequest request);

	/** Creates an immutable, first-match policy with a decision for unmatched calls. */
	static ToolPermissionPolicy ordered(Decision fallback, Rule... rules) {
		Objects.requireNonNull(fallback, "fallback");
		List<Rule> orderedRules = List.of(rules);
		return request -> {
			for (Rule rule : orderedRules) {
				var decision = rule.evaluate(request);
				if (decision.isPresent()) {
					return decision.get();
				}
			}
			return fallback;
		};
	}

	/** A rule returns an empty decision when it does not apply. */
	@FunctionalInterface
	interface Rule {

		Optional<Decision> evaluate(ToolCallRequest request);

		static Rule tool(String name, Decision decision) {
			Objects.requireNonNull(name, "name");
			return when(request -> name.equals(request.getToolName()), decision);
		}

		static Rule when(Predicate<ToolCallRequest> predicate, Decision decision) {
			Objects.requireNonNull(predicate, "predicate");
			var matched = Optional.of(decision);
			return request -> predicate.test(request) ? matched : Optional.empty();
		}

	}

}
