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
package com.alibaba.cloud.ai.graph.agent.hook.pii;

/**
 * Represents a detected instance of PII in text.
 */
public class PIIMatch {

	public final String type;
	public final String value;
	public final int start;
	public final int end;

	public PIIMatch(String type, String value, int start, int end) {
		this.type = type;
		this.value = value;
		this.start = start;
		this.end = end;
	}

	@Override
	public String toString() {
		return String.format("PIIMatch{type='%s', value='%s', start=%d, end=%d}",
				type, value, start, end);
	}
}

