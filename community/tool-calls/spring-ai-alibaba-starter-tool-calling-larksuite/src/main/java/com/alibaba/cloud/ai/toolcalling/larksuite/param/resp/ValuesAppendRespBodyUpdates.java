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

/**
 * @author NewGK
 */
public class ValuesAppendRespBodyUpdates {

	private int revision;

	private String spreadsheetToken;

	private int updatedCells;

	private int updatedColumns;

	private String updatedRange;

	private int updatedRows;

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

}
