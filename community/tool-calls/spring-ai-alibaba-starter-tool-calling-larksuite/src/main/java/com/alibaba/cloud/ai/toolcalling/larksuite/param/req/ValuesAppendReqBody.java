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

/**
 * @author NewGK
 */
public class ValuesAppendReqBody {

	@SerializedName("valueRange")
	private ValueRange valueRange;

	public ValueRange getValueRange() {
		return valueRange;
	}

	public void setValueRange(ValueRange valueRange) {
		this.valueRange = valueRange;
	}

	public ValuesAppendReqBody(Builder builder) {
		this.valueRange = builder.valueRange;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		@SerializedName("valueRange")
		private ValueRange valueRange;

		public Builder valueRange(ValueRange valueRange) {
			this.valueRange = valueRange;
			return this;
		}

		public ValuesAppendReqBody build() {
			return new ValuesAppendReqBody(this);
		}

	}

}
