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
package com.alibaba.cloud.ai.toolcalling.larksuite.param.req;

/**
 * @author huaiziqing
 */
public class ValuesGetReq {

    private String spreadsheetToken;

    private String range;

    public String getSpreadsheetToken() {
        return spreadsheetToken;
    }

    public void setSpreadsheetToken(String spreadsheetToken) {
        this.spreadsheetToken = spreadsheetToken;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String spreadsheetToken;

        private String range;

        public Builder spreadsheetToken(String spreadsheetToken) {
            this.spreadsheetToken = spreadsheetToken;
            return this;
        }

        public Builder range(String range) {
            this.range = range;
            return this;
        }

        public ValuesGetReq build() {
            ValuesGetReq req = new ValuesGetReq();
            req.setSpreadsheetToken(this.spreadsheetToken);
            req.setRange(this.range);
            return req;
        }
    }
}