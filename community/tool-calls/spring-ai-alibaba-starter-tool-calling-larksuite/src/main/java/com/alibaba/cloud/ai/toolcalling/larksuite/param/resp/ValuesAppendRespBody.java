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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author huaiziqing
 */

public class ValuesAppendRespBody {

	private final int revision;

	private final String spreadsheetToken;

	private final String tableRange;

	private final ValuesAppendRespBodyUpdates updates;

	@JsonCreator
	public ValuesAppendRespBody(@JsonProperty("revision") int revision,
			@JsonProperty("spreadsheetToken") String spreadsheetToken, @JsonProperty("tableRange") String tableRange,
			@JsonProperty("updates") ValuesAppendRespBodyUpdates updates) {
		this.revision = revision;
		this.spreadsheetToken = spreadsheetToken;
		this.tableRange = tableRange;
		this.updates = updates;
	}

	public int getRevision() {
		return revision;
	}

	public String getSpreadsheetToken() {
		return spreadsheetToken;
	}

	public String getTableRange() {
		return tableRange;
	}

	public ValuesAppendRespBodyUpdates getUpdates() {
		return updates;
	}

	@Override
	public String toString() {
		return "ValuesAppendRespBody{" + "revision=" + revision + ", spreadsheetToken='" + spreadsheetToken + '\''
				+ ", tableRange='" + tableRange + '\'' + ", updates=" + updates + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ValuesAppendRespBody that))
			return false;
		return revision == that.revision && spreadsheetToken.equals(that.spreadsheetToken)
				&& tableRange.equals(that.tableRange) && updates.equals(that.updates);
	}

	@Override
	public int hashCode() {
		return Objects.hash(revision, spreadsheetToken, tableRange, updates);
	}

	public static class Builder {

		private int revision;

		private String spreadsheetToken;

		private String tableRange;

		private ValuesAppendRespBodyUpdates updates;

		public Builder revision(int revision) {
			this.revision = revision;
			return this;
		}

		public Builder spreadsheetToken(String spreadsheetToken) {
			this.spreadsheetToken = spreadsheetToken;
			return this;
		}

		public Builder tableRange(String tableRange) {
			this.tableRange = tableRange;
			return this;
		}

		public Builder updates(ValuesAppendRespBodyUpdates updates) {
			this.updates = updates;
			return this;
		}

		public ValuesAppendRespBody build() {
			return new ValuesAppendRespBody(this.revision, this.spreadsheetToken, this.tableRange, this.updates);
		}

	}

}