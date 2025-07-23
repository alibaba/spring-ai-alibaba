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

package com.alibaba.cloud.ai.constant;

/**
 * Defines the types of content for streaming responses to the frontend.
 */
public enum StreamResponseType {

	/**
	 * Represents a status update or progress message.
	 */
	STATUS("status"),

	/**
	 * Represents the rewritten user query.
	 */
	REWRITE("rewrite"),

	/**
	 * Represents a generated SQL query.
	 */
	SQL("sql"),

	/**
	 * Represents explanatory text or logs.
	 */
	EXPLANATION("explanation"),

	/**
	 * Represents the final data result, typically a table.
	 */
	RESULT("result"),

	/**
	 * Represents the extracted keywords.
	 */
	KEYWORD_EXTRACT("keyword_extract"),

	SCHEMA_RECALL("schema_recall"),

	EXECUTE_SQL("execute_sql"),

	VALIDATION("validation"), OUTPUT_REPORT("output_report"), SCHEMA_DEEP_RECALL("schema_deep_recall"),
	PYTHON_ANALYSIS("python_analysis"),
	/**
	 * Represents the generated execution plan.
	 */
	PLAN_GENERATION("plan_generation");

	private final String value;

	StreamResponseType(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

}
