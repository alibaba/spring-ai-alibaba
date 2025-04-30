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
import com.lark.oapi.core.annotation.Body;

/**
 * @author NewGK
 */
public class ValuesAppendReq {

	@Body
	private ValuesAppendReqBody body;

	@SerializedName("spreadsheet_token")
	private String spreadsheetToken;

	public ValuesAppendReqBody getBody() {
		return body;
	}

	public void setBody(ValuesAppendReqBody body) {
		this.body = body;
	}

	public String getSpreadsheetToken() {
		return spreadsheetToken;
	}

	public void setSpreadsheetToken(String spreadsheetToken) {
		this.spreadsheetToken = spreadsheetToken;
	}

	public ValuesAppendReq(Builder builder) {
		this.body = builder.body;
		this.spreadsheetToken = builder.spreadsheetToken;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private ValuesAppendReqBody body;

		private String spreadsheetToken;

		public Builder body(ValuesAppendReqBody body) {
			this.body = body;
			return this;
		}

		public Builder spreadsheetToken(String spreadsheetToken) {
			this.spreadsheetToken = spreadsheetToken;
			return this;
		}

		public ValuesAppendReq build() {
			return new ValuesAppendReq(this);
		}

	}

}
