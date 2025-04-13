package com.alibaba.cloud.ai.toolcalling.larksuite.param.req;

import com.google.gson.annotations.SerializedName;
import com.lark.oapi.core.annotation.Body;

/**
 * @author NewGK
 */
public class ValuesAppendReq {

    @Body
    private ValuesAppendReqBody body;

    @SerializedName("spreadsheet_token")
    private String spreadsheetToken;

    public ValuesAppendReqBody getBody() {
        return body;
    }

    public void setBody(ValuesAppendReqBody body) {
        this.body = body;
    }

    public String getSpreadsheetToken() {
        return spreadsheetToken;
    }

    public void setSpreadsheetToken(String spreadsheetToken) {
        this.spreadsheetToken = spreadsheetToken;
    }

    public ValuesAppendReq(Builder builder) {
        this.body = builder.body;
        this.spreadsheetToken = builder.spreadsheetToken;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private ValuesAppendReqBody body;

        private String spreadsheetToken;


        public Builder body(ValuesAppendReqBody body) {
            this.body = body;
            return this;
        }

        public Builder spreadsheetToken(String spreadsheetToken) {
            this.spreadsheetToken = spreadsheetToken;
            return this;
        }

        public ValuesAppendReq build() {
            return new ValuesAppendReq(this);
        }
    }
}
