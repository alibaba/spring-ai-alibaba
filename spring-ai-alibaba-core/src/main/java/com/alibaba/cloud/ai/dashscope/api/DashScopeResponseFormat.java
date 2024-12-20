package com.alibaba.cloud.ai.dashscope.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lets you specify the format of the returned content. Valid values: {"type": "text"} or
 * {"type": "json_object"}. When set to {"type": "json_object"}, a JSON string in standard
 * format is output. Params reference:
 * <a href="https://help.aliyun.com/zh/dashscope/developer-reference/qwen-api">...</a>
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

		return "DashScopeResponseFormat { " + "type='" + type + '}';
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
