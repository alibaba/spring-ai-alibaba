package com.alibaba.cloud.ai.toolcalling.larksuite.param.resp;

import com.lark.oapi.core.response.BaseResponse;

/**
 @author huaiziqing
 */


public class ValuesAppendResp extends BaseResponse<ValuesAppendRespBody> {
    /**
     * 飞书表格数据追加接口响应类。
     * 封装飞书 API 返回的完整结构，继承自 SDK 的 BaseResponse，
     * 包含通用字段如 code, msg, requestId 以及业务数据 body
     */
    private String requestId;

    public ValuesAppendResp() {
        super();
    }

    public String getSpreadsheetToken() {
        if (this.getData() != null) {
            return this.getData().getSpreadsheetToken();
        }
        return null;
    }

    public ValuesAppendRespBodyUpdates getUpdates() {
        if (this.getData() != null && this.getData().getUpdates() != null) {
            return this.getData().getUpdates();
        }
        return null;
    }

    @Override
    public String toString() {
        return "ValuesAppendResp{" +
                "code=" + getCode() +
                ", msg='" + getMsg() + '\'' +
                ", spreadsheetToken=" + getSpreadsheetToken() +
                ", updates=" + getUpdates() +
                '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String getRequestId() {
        return requestId;
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

    private void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
