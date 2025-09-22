/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.utils.common;

import java.util.UUID;

/**
 * Utility class for generating various types of unique identifiers.
 *
 * @since 1.0.0.3
 */
public class IdGenerator {

	/** Worker instance for generating sequential IDs */
	private static final Sequence WORKER = new Sequence();

	/**
	 * Generates an API key with "sk-" prefix
	 * @return API key string
	 */
	public static String genApiKey() {
		return "sk-" + uuid().replace("-", "").toLowerCase();
	}

	/**
	 * Generates a unique numeric ID
	 * @return unique long ID
	 */
	public static Long id() {
		return WORKER.nextId();
	}

	/**
	 * Generates a unique numeric ID as string
	 * @return unique ID string
	 */
	public static String idStr() {
		return String.valueOf(WORKER.nextId());
	}

	/**
	 * Generates a standard UUID with hyphens
	 * @return UUID string
	 */
	public static String uuid() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Generates a UUID without hyphens
	 * @return 32-character UUID string
	 */
	public static String uuid32() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * Generates a unique agent ID with "agent_" prefix
	 * @return agent ID string
	 */
	public static String generateAgentId() {
		return "agent_" + uuid32().substring(0, 16).toLowerCase();
	}

	/**
	 * Generates a unique tool ID with "tool_" prefix
	 * @return tool ID string
	 */
	public static String generateToolId() {
		return "tool_" + uuid32().substring(0, 16).toLowerCase();
	}

}
