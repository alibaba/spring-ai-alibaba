/*
 * Copyright 2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.streaming;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import static com.alibaba.cloud.ai.graph.streaming.RunEvent.Channel;

/** Selects which in-process events are produced and bounds the live delivery queue. */
public record StreamOptions(Set<Channel> channels, boolean includeNested, boolean includeReasoning,
		boolean includeToolArguments, int bufferCapacity, int candidateByteBudget, boolean includeCandidateLifecycle) {

	private static final int DEFAULT_CAPACITY = 256;

	private static final int DEFAULT_BYTE_BUDGET = 256 * 1024;

	public StreamOptions {
		channels = Set.copyOf(Objects.requireNonNull(channels));
		if (bufferCapacity < 1 || candidateByteBudget < 1) {
			throw new IllegalArgumentException("Stream buffer limits must be positive");
		}
	}

	public StreamOptions(Set<Channel> channels, boolean includeNested, boolean includeReasoning,
			boolean includeToolArguments, int bufferCapacity, int candidateByteBudget) {
		this(channels, includeNested, includeReasoning, includeToolArguments, bufferCapacity, candidateByteBudget, false);
	}

	public static StreamOptions defaults() {
		return new StreamOptions(Set.of(Channel.LIFECYCLE, Channel.MESSAGES), false, false, false,
				DEFAULT_CAPACITY, DEFAULT_BYTE_BUDGET);
	}

	public static StreamOptions all() {
		return new StreamOptions(EnumSet.allOf(Channel.class), true, true, true, DEFAULT_CAPACITY, DEFAULT_BYTE_BUDGET);
	}

	public StreamOptions withNested(boolean enabled) {
		return new StreamOptions(channels, enabled, includeReasoning, includeToolArguments, bufferCapacity,
				candidateByteBudget, includeCandidateLifecycle);
	}

	public StreamOptions withCandidateLifecycle(boolean enabled) {
		return new StreamOptions(channels, includeNested, includeReasoning, includeToolArguments, bufferCapacity,
				candidateByteBudget, enabled);
	}
}
