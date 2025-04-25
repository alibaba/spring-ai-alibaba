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
package com.alibaba.cloud.ai.toolcalling.larksuite.param.req;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author NewGK
 */
public class ValueRange {

	@SerializedName("range")
	private String range;

	@SerializedName("values")
	private List<List<String>> values;

	public String getRange() {
		return range;
	}

	public void setRange(String range) {
		this.range = range;
	}

	public List<List<String>> getValues() {
		return values;
	}

	public void setValues(List<List<String>> values) {
		this.values = values;
	}

	public ValueRange(Builder builder) {
		this.range = builder.range;
		this.values = builder.values;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		@SerializedName("range")
		private String range;

		@SerializedName("values")
		private List<List<String>> values;

		public Builder range(String range) {
			this.range = range;
			return this;
		}

		public Builder values(List<List<String>> values) {
			this.values = values;
			return this;
		}

		public ValueRange build() {
			return new ValueRange(this);
		}

	}

}
