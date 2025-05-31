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

import com.lark.oapi.service.bitable.v1.model.App;

import java.util.Objects;

/**
 @author huaiziqing
 */

public class ValuesAppendRespBodyUpdates {
	/**
	 * 飞书表格数据追加操作的更新详情信息。
	 * 记录实际写入的数据量，包括单元格数、行数、列数和范围
	 */
	private int revision;

	private String spreadsheetToken;

	private int updatedCells;

	private int updatedColumns;

	private String updatedRange;

	private int updatedRows;

	public ValuesAppendRespBodyUpdates(Builder builder) {
		this.revision = builder.revision;
		this.spreadsheetToken = builder.spreadsheetToken;
		this.updatedCells = builder.updatedCells;
		this.updatedColumns = builder.updatedColumns;
		this.updatedRange = builder.updatedRange;
		this.updatedRows = builder.updatedRows;
	}

	public static Builder newBuilder() {
		return new Builder();
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
			return new ValuesAppendRespBodyUpdates(this);
		}
	}



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

	public int getUpdatedCells() {
		return updatedCells;
	}

	public void setUpdatedCells(int updatedCells) {
		this.updatedCells = updatedCells;
	}

	public int getUpdatedColumns() {
		return updatedColumns;
	}

	public void setUpdatedColumns(int updatedColumns) {
		this.updatedColumns = updatedColumns;
	}

	public String getUpdatedRange() {
		return updatedRange;
	}

	public void setUpdatedRange(String updatedRange) {
		this.updatedRange = updatedRange;
	}

	public int getUpdatedRows() {
		return updatedRows;
	}

	public void setUpdatedRows(int updatedRows) {
		this.updatedRows = updatedRows;
	}

	@Override
	public String toString() {
		return "ValuesAppendRespBodyUpdates{" +
				"revision=" + revision +
				", spreadsheetToken='" + spreadsheetToken + '\'' +
				", updatedCells=" + updatedCells +
				", updatedColumns=" + updatedColumns +
				", updatedRange='" + updatedRange + '\'' +
				", updatedRows=" + updatedRows +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ValuesAppendRespBodyUpdates that))
			return false;

		if (revision != that.revision)
			return false;

		if (updatedCells != that.updatedCells)
			return false;

		if (updatedColumns != that.updatedColumns)
			return false;

		if (updatedRows != that.updatedRows)
			return false;

		if (!Objects.equals(spreadsheetToken, that.spreadsheetToken))
			return false;

		return Objects.equals(updatedRange, that.updatedRange);
	}

	@Override
	public int hashCode() {
		int result = Integer.hashCode(revision);
		result = 31 * result + Objects.hashCode(spreadsheetToken);
		result = 31 * result + Integer.hashCode(updatedCells);
		result = 31 * result + Integer.hashCode(updatedColumns);
		result = 31 * result + Objects.hashCode(updatedRange);
		result = 31 * result + Integer.hashCode(updatedRows);
		return result;
	}
}
