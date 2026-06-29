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
package com.alibaba.cloud.ai.graph.agent.tools;

import java.util.Map;

/**
 * Optional runtime emitter for tool progress chunks.
 *
 * <p>This contract is intentionally lightweight and framework-internal for the MVP.
 * Tools can emit String-based progress updates without depending on Reactor types.</p>
 *
 * @author Zhengcy05
 * @since 1.1.2.2
 */
public interface ToolProgressEmitter {

	/**
	 * Noop emitter used by non-streaming execution paths.
	 */
	ToolProgressEmitter NOOP = new ToolProgressEmitter() {
		@Override
		public boolean next(String content) {
			return false;
		}

		@Override
		public boolean next(String content, Map<String, Object> metadata) {
			return false;
		}

		@Override
		public boolean error(Throwable throwable) {
			return false;
		}

		@Override
		public boolean isTerminated() {
			return true;
		}
	};

	/**
	 * Emit a progress chunk with empty metadata.
	 * @param content the progress content to emit
	 * @return {@code true} if the chunk was accepted for emission, {@code false}
	 * otherwise
	 */
	default boolean next(String content) {
		return next(content, Map.of());
	}

	/**
	 * Emit a progress chunk for the current tool call.
	 *
	 * <p>
	 * The content is exposed as a runtime {@code AGENT_TOOL_STREAMING} event. The
	 * metadata map is optional and may be used by higher-level consumers for additional
	 * rendering or correlation.
	 * </p>
	 * @param content the progress content to emit
	 * @param metadata optional metadata associated with this chunk
	 * @return {@code true} if the chunk was accepted for emission, {@code false}
	 * otherwise
	 */
	boolean next(String content, Map<String, Object> metadata);

	/**
	 * Emit a terminal error chunk for the current tool call.
	 *
	 * <p>
	 * This method is intended for runtime streaming visibility only. It does not replace
	 * the normal tool error handling path, which still determines the final tool result
	 * returned to the agent.
	 * </p>
	 * @param throwable the tool execution error
	 * @return {@code true} if the error chunk was emitted, {@code false} otherwise
	 */
	boolean error(Throwable throwable);

	/**
	 * Whether this emitter has already entered a terminal state.
	 * @return {@code true} if no more chunks should be emitted, {@code false}
	 * otherwise
	 */
	boolean isTerminated();
}
