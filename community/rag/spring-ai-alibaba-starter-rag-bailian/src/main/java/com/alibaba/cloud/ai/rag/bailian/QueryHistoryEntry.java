/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.rag.bailian;

/**
 * Query history entry for multi-turn conversation rewrite.
 *
 * <p>This class represents a single message in the conversation history, used for
 * multi-turn dialogue rewriting to improve retrieval effectiveness.
 */
public class QueryHistoryEntry {

	private final String role;
	private final String content;

	/**
	 * Creates a new QueryHistoryEntry.
	 *
	 * @param role the role (user or assistant)
	 * @param content the message content
	 */
	public QueryHistoryEntry(String role, String content) {
		if (role == null || (!role.equals("user") && !role.equals("assistant"))) {
			throw new IllegalArgumentException("Role must be 'user' or 'assistant'");
		}
		this.role = role;
		this.content = content;
	}

	/**
	 * Creates a user message entry.
	 *
	 * @param content the user's message
	 * @return a new QueryHistoryEntry with role "user"
	 */
	public static QueryHistoryEntry user(String content) {
		return new QueryHistoryEntry("user", content);
	}

	/**
	 * Creates an assistant message entry.
	 *
	 * @param content the assistant's response
	 * @return a new QueryHistoryEntry with role "assistant"
	 */
	public static QueryHistoryEntry assistant(String content) {
		return new QueryHistoryEntry("assistant", content);
	}

	/**
	 * Gets the role of this message.
	 *
	 * @return the role (user or assistant)
	 */
	public String getRole() {
		return role;
	}

	/**
	 * Gets the content of this message.
	 *
	 * @return the message content
	 */
	public String getContent() {
		return content;
	}
}
