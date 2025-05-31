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
