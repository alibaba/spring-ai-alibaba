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
package com.alibaba.cloud.ai.graph.node.code.entity;

/**
 * @author HeYQ
 * @since 2025-01-06 22:58
 */
public enum CodeLanguage {

	PYTHON3("python3"), PYTHON("python"), JINJA2("jinja2"), JAVASCRIPT("javascript"), JAVA("java");

	private final String value;

	CodeLanguage(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	// Optionally, you can add a method to find an enum constant by its string value
	public static CodeLanguage fromValue(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid code language provided");
		}
		for (CodeLanguage lang : values()) {
			if (lang.getValue().equalsIgnoreCase(value)) {
				return lang;
			}
		}
		throw new IllegalArgumentException("No enum constant " + CodeLanguage.class.getName() + "." + value);
	}

	@Override
	public String toString() {
		return this.value;
	}

}
