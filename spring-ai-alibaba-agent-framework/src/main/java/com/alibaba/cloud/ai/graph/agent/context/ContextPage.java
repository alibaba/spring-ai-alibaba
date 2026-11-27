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
package com.alibaba.cloud.ai.graph.agent.context;

/** A bounded portion of tool-supplied text, addressed by a character offset. */
public record ContextPage(String content, Integer nextOffset) {

	/**
	 * Uses UTF-16 offsets while keeping supplementary characters intact.
	 * The returned offset can be passed to the next read.
	 */
	public static ContextPage read(String text, Integer offset, int limit) {
		int start = offset == null ? 0 : offset;
		if (limit < 2 || start < 0 || (start != 0 && start >= text.length()) || splitsCharacter(text, start)) {
			throw new IllegalArgumentException("Invalid context page offset or size");
		}
		int end = (int) Math.min(text.length(), (long) start + limit);
		if (splitsCharacter(text, end)) {
			end--;
		}
		return new ContextPage(text.substring(start, end), end < text.length() ? end : null);
	}

	private static boolean splitsCharacter(String text, int offset) {
		return offset > 0 && offset < text.length() && Character.isHighSurrogate(text.charAt(offset - 1))
				&& Character.isLowSurrogate(text.charAt(offset));
	}

}
