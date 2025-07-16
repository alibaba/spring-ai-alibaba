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
package com.alibaba.cloud.ai.dashscope.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lets you specify the format of the returned content. Valid values: {"type": "text"} or
 * {"type": "json_object"}. When set to {"type": "json_object"}, a JSON string in standard
 * format is output. Params reference:
 * <a href= "https://help.aliyun.com/zh/dashscope/developer-reference/qwen-api">...</a>
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeResponseFormat {

	/**
	 * Parameters must be one of 'text' or 'json_object'.
	 */
	@JsonProperty("type")
	private Type type;

	public Type getType() {

		return type;
	}

	public void setType(Type type) {

		this.type = type;
	}

	public DashScopeResponseFormat() {
	}

	public DashScopeResponseFormat(Type type) {

		this.type = type;
	}

	public static Builder builder() {

		return new Builder();
	}

	/**
	 * Builder for {@link DashScopeResponseFormat}.
	 */
	public static class Builder {

		private Type type;

		public Builder type(Type type) {

			this.type = type;
			return this;
		}

		public DashScopeResponseFormat build() {

			return new DashScopeResponseFormat(this.type);
		}

	}

	@Override
	public String toString() {
		String typeValue = type == Type.TEXT ? "text" : "json_object";
		return "{\"type\":\"" + typeValue + "\"}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DashScopeResponseFormat that = (DashScopeResponseFormat) o;
		return Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type);
	}

	/**
	 * ResponseFormat type. Valid values: {"type": "text"} or {"type": "json_object"}.
	 */
	public enum Type {

		/**
		 * Generates a text response. (default)
		 */
		@JsonProperty("text")
		TEXT,

		/**
		 * Enables JSON mode, which guarantees the message the model generates is valid
		 * JSON string.
		 */
		@JsonProperty("json_object")
		JSON_OBJECT,

	}

}
