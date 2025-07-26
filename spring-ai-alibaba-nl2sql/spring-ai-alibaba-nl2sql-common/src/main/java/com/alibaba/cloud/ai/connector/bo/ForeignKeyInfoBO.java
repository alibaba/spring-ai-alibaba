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
package com.alibaba.cloud.ai.connector.bo;

public class ForeignKeyInfoBO extends DdlBaseBO {

	private String table;

	private String column;

	private String referencedTable;

	private String referencedColumn;

	public ForeignKeyInfoBO() {
	}

	public ForeignKeyInfoBO(String table, String column, String referencedTable, String referencedColumn) {
		this.table = table;
		this.column = column;
		this.referencedTable = referencedTable;
		this.referencedColumn = referencedColumn;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public String getReferencedTable() {
		return referencedTable;
	}

	public void setReferencedTable(String referencedTable) {
		this.referencedTable = referencedTable;
	}

	public String getReferencedColumn() {
		return referencedColumn;
	}

	public void setReferencedColumn(String referencedColumn) {
		this.referencedColumn = referencedColumn;
	}

	@Override
	public String toString() {
		return "ForeignKeyInfoBO{" + "table='" + table + '\'' + ", column='" + column + '\'' + ", referencedTable='"
				+ referencedTable + '\'' + ", referencedColumn='" + referencedColumn + '\'' + '}';
	}

	public static ForeignKeyInfoBOBuilder builder() {
		return new ForeignKeyInfoBOBuilder();
	}

	public static final class ForeignKeyInfoBOBuilder {

		private String table;

		private String column;

		private String referencedTable;

		private String referencedColumn;

		private ForeignKeyInfoBOBuilder() {
		}

		public static ForeignKeyInfoBOBuilder aForeignKeyInfoBO() {
			return new ForeignKeyInfoBOBuilder();
		}

		public ForeignKeyInfoBOBuilder table(String table) {
			this.table = table;
			return this;
		}

		public ForeignKeyInfoBOBuilder column(String column) {
			this.column = column;
			return this;
		}

		public ForeignKeyInfoBOBuilder referencedTable(String referencedTable) {
			this.referencedTable = referencedTable;
			return this;
		}

		public ForeignKeyInfoBOBuilder referencedColumn(String referencedColumn) {
			this.referencedColumn = referencedColumn;
			return this;
		}

		public ForeignKeyInfoBO build() {
			ForeignKeyInfoBO foreignKeyInfoBO = new ForeignKeyInfoBO();
			foreignKeyInfoBO.setTable(table);
			foreignKeyInfoBO.setColumn(column);
			foreignKeyInfoBO.setReferencedTable(referencedTable);
			foreignKeyInfoBO.setReferencedColumn(referencedColumn);
			return foreignKeyInfoBO;
		}

	}

}
