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

import com.google.gson.annotations.SerializedName;

/**
 * @author NewGK
 */
public class ValuesAppendRespBody {

	@SerializedName("revision")
	private int revision;

	@SerializedName("spreadsheetToken")
	private String spreadsheetToken;

	@SerializedName("tableRange")
	private String tableRange;

	@SerializedName("updates")
	private ValuesAppendRespBodyUpdates updates;

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	public String getSpreadsheetToken() {
		return spreadsheetToken;
	}

	public void setSpreadsheetToken(String spreadsheetToken) {
		this.spreadsheetToken = spreadsheetToken;
	}

	public String getTableRange() {
		return tableRange;
	}

	public void setTableRange(String tableRange) {
		this.tableRange = tableRange;
	}

	public ValuesAppendRespBodyUpdates getUpdates() {
		return updates;
	}

	public void setUpdates(ValuesAppendRespBodyUpdates updates) {
		this.updates = updates;
	}

}
