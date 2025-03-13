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
package com.alibaba.cloud.ai.reader.gitlab;

/**
 * Scope enum. Used to determine the scope of the issue.
 *
 * @author brianxiadong
 */
public enum GitLabScope {

	/**
	 * Issues created by the authenticated user
	 */
	CREATED_BY_ME("created_by_me"),

	/**
	 * Issues assigned to the authenticated user
	 */
	ASSIGNED_TO_ME("assigned_to_me"),

	/**
	 * All issues
	 */
	ALL("all");

	private final String value;

	GitLabScope(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
