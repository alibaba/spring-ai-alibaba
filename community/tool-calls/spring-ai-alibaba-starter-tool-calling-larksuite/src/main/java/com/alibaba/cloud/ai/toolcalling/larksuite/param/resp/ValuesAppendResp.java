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
package com.alibaba.cloud.ai.toolcalling.larksuite.param.resp;

import com.lark.oapi.core.response.BaseResponse;

/**
 * @author huaiziqing
 */

public class ValuesAppendResp extends BaseResponse<ValuesAppendRespBody> {

	private String requestId;

	public ValuesAppendResp() {
		super();
	}

	public String getSpreadsheetToken() {
		ValuesAppendRespBody data = getData();
		if (data == null) {
			throw new IllegalStateException("Data is null");
		}
		return data.getSpreadsheetToken();
	}

	public ValuesAppendRespBodyUpdates getUpdates() {
		ValuesAppendRespBody data = getData();
		if (data == null || data.getUpdates() == null) {
			throw new IllegalStateException("Updates data is null");
		}
		return data.getUpdates();
	}

	@Override
	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	@Override
	public String toString() {
		return "ValuesAppendResp{" + "code=" + getCode() + ", msg='" + getMsg() + '\'' + ", spreadsheetToken='"
				+ getSpreadsheetToken() + '\'' + ", updates=" + getUpdates() + '}';
	}

	public static class Builder {

		private final ValuesAppendResp instance = new ValuesAppendResp();

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

		public Builder data(ValuesAppendRespBody data) {
			instance.setData(data);
			return this;
		}

		public ValuesAppendResp build() {
			return instance;
		}

	}

}
