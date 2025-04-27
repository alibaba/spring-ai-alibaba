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

package com.alibaba.cloud.ai.example.manus.recorder.entity;

/**
 * Interface providing JSON serialization capabilities. Classes implementing this
 * interface should provide a method to convert their data to JSON format.
 */
public interface JsonSerializable {

	/**
	 * Converts the object to its JSON representation.
	 * @return A string containing the JSON representation of the object
	 */
	String toJson();

	/**
	 * Escape special characters for JSON string. This is a default method that can be
	 * used by all implementing classes.
	 * @param input String to escape
	 * @return Escaped string
	 */
	default String escapeJson(String input) {
		if (input == null)
			return "";
		return input.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\b", "\\b")
			.replace("\f", "\\f")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}

	/**
	 * Helper method to append a field to the JSON string.
	 * @param json StringBuilder to append to
	 * @param fieldName Name of the field
	 * @param value Value of the field
	 * @param isString Whether the value is a string (needing quotes)
	 */
	default void appendField(StringBuilder json, String fieldName, Object value, boolean isString) {
		if (value != null) {
			json.append("\"").append(fieldName).append("\":");
			if (isString) {
				json.append("\"").append(escapeJson(value.toString())).append("\"");
			}
			else {
				json.append(value);
			}
			json.append(",");
		}
	}

}
