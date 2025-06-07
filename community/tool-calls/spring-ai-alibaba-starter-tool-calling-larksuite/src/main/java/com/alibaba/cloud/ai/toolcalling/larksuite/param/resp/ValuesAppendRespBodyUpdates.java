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

public class ValuesAppendRespBodyUpdates {

	private final int revision;

	private final String spreadsheetToken;

	private final int updatedCells;

	private final int updatedColumns;

	private final String updatedRange;

	private final int updatedRows;

	@JsonCreator
	public ValuesAppendRespBodyUpdates(@JsonProperty("revision") int revision,
			@JsonProperty("spreadsheetToken") String spreadsheetToken, @JsonProperty("updatedCells") int updatedCells,
			@JsonProperty("updatedColumns") int updatedColumns, @JsonProperty("updatedRange") String updatedRange,
			@JsonProperty("updatedRows") int updatedRows) {
		this.revision = revision;
		this.spreadsheetToken = spreadsheetToken;
		this.updatedCells = updatedCells;
		this.updatedColumns = updatedColumns;
		this.updatedRange = updatedRange;
		this.updatedRows = updatedRows;
	}

	public int getRevision() {
		return revision;
	}

	public String getSpreadsheetToken() {
		return spreadsheetToken;
	}

	public int getUpdatedCells() {
		return updatedCells;
	}

	public int getUpdatedColumns() {
		return updatedColumns;
	}

	public String getUpdatedRange() {
		return updatedRange;
	}

	public int getUpdatedRows() {
		return updatedRows;
	}

	@Override
	public String toString() {
		return "ValuesAppendRespBodyUpdates{" + "revision=" + revision + ", spreadsheetToken='" + spreadsheetToken
				+ '\'' + ", updatedCells=" + updatedCells + ", updatedColumns=" + updatedColumns + ", updatedRange='"
				+ updatedRange + '\'' + ", updatedRows=" + updatedRows + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ValuesAppendRespBodyUpdates that))
			return false;
		return revision == that.revision && updatedCells == that.updatedCells && updatedColumns == that.updatedColumns
				&& updatedRows == that.updatedRows && spreadsheetToken.equals(that.spreadsheetToken)
				&& updatedRange.equals(that.updatedRange);
	}

	@Override
	public int hashCode() {
		return Objects.hash(revision, spreadsheetToken, updatedCells, updatedColumns, updatedRange, updatedRows);
	}

	public static class Builder {

		private int revision;

		private String spreadsheetToken;

		private int updatedCells;

		private int updatedColumns;

		private String updatedRange;

		private int updatedRows;

		public Builder revision(int revision) {
			this.revision = revision;
			return this;
		}

		public Builder spreadsheetToken(String spreadsheetToken) {
			this.spreadsheetToken = spreadsheetToken;
			return this;
		}

		public Builder updatedCells(int updatedCells) {
			this.updatedCells = updatedCells;
			return this;
		}

		public Builder updatedColumns(int updatedColumns) {
			this.updatedColumns = updatedColumns;
			return this;
		}

		public Builder updatedRange(String updatedRange) {
			this.updatedRange = updatedRange;
			return this;
		}

		public Builder updatedRows(int updatedRows) {
			this.updatedRows = updatedRows;
			return this;
		}

		public ValuesAppendRespBodyUpdates build() {
			return new ValuesAppendRespBodyUpdates(this.revision, this.spreadsheetToken, this.updatedCells,
					this.updatedColumns, this.updatedRange, this.updatedRows);
		}

	}

}
