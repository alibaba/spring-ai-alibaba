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
