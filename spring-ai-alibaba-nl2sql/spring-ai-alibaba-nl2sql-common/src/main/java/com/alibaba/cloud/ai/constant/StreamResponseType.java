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
