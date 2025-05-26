package com.alibaba.cloud.ai.toolcalling.larksuite.param.resp;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 @author huaiziqing
 */

public class ValuesAppendRespBody {
	/**
	 * 飞书表格数据追加接口返回的主体响应内容
	 * 包含版本号、表格 token、写入范围及更新详情
	 */
	@SerializedName("revision")
	private final int revision;

	@SerializedName("spreadsheetToken")
	private final String spreadsheetToken;

	@SerializedName("tableRange")
	private final String tableRange;

	@SerializedName("updates")
	private final ValuesAppendRespBodyUpdates updates;

	private ValuesAppendRespBody(Builder builder) {
		this.revision = builder.revision;
		this.spreadsheetToken = builder.spreadsheetToken;
		this.tableRange = builder.tableRange;
		this.updates = builder.updates;
	}

	public static Builder newBuilder() {
		return new Builder();
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
		return "ValuesAppendRespBody{" +
				"revision=" + revision +
				", spreadsheetToken='" + spreadsheetToken + '\'' +
				", tableRange='" + tableRange + '\'' +
				", updates=" + updates +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ValuesAppendRespBody that))
			return false;

		if (revision != that.revision)
			return false;

		if (!Objects.equals(spreadsheetToken, that.spreadsheetToken))
			return false;

		if (!Objects.equals(tableRange, that.tableRange))
			return false;

		return Objects.equals(updates, that.updates);
	}

	@Override
	public int hashCode() {
		int result = Integer.hashCode(revision);
		result = 31 * result + (spreadsheetToken != null ? spreadsheetToken.hashCode() : 0);
		result = 31 * result + (tableRange != null ? tableRange.hashCode() : 0);
		result = 31 * result + (updates != null ? updates.hashCode() : 0);
		return result;
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
			return new ValuesAppendRespBody(this);
		}
	}
}
