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
package com.alibaba.cloud.ai.toolcalling.larksuite;

import com.lark.oapi.core.response.BaseResponse;

import java.util.List;

/**
 * 飞书 Value 获取数据响应结构
 */
public class ValuesGetResp extends BaseResponse<ValuesGetResp.ValuesGetRespBody> {

	private String requestId;

	public ValuesGetResp() {
		super();
	}

	public String getSpreadsheetToken() {
		return getData().getSpreadsheetToken();
	}

	public String getTableRange() {
		return getData().getTableRange();
	}

	public List<List<String>> getValues() {
		return getData().getValues();
	}

	@Override
	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public static class Builder {

		private final ValuesGetResp instance = new ValuesGetResp();

		public Builder code(int code) {
			instance.setCode(code);
			return this;
		}

		public Builder msg(String msg) {
			instance.setMsg(msg);
			return this;
		}

		public Builder requestId(String requestId) {
			instance.setRequestId(requestId);
			return this;
		}

		public Builder data(ValuesGetRespBody data) {
			instance.setData(data);
			return this;
		}

		public ValuesGetResp build() {
			return instance;
		}

	}

	public static class ValuesGetRespBody {

		private final String spreadsheetToken;

		private final String tableRange;

		private final List<List<String>> values;

		public ValuesGetRespBody(Builder builder) {
			this.spreadsheetToken = builder.spreadsheetToken;
			this.tableRange = builder.tableRange;
			this.values = builder.values;
		}

		public String getSpreadsheetToken() {
			return spreadsheetToken;
		}

		public String getTableRange() {
			return tableRange;
		}

		public List<List<String>> getValues() {
			return values;
		}

		public static class Builder {

			private String spreadsheetToken;

			private String tableRange;

			private List<List<String>> values;

			public Builder spreadsheetToken(String spreadsheetToken) {
				this.spreadsheetToken = spreadsheetToken;
				return this;
			}

			public Builder tableRange(String tableRange) {
				this.tableRange = tableRange;
				return this;
			}

			public Builder values(List<List<String>> values) {
				this.values = values;
				return this;
			}

			public ValuesGetRespBody build() {
				return new ValuesGetRespBody(this);
			}

		}

	}

}
