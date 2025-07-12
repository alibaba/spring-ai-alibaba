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
package com.alibaba.cloud.ai.toolcalling.larksuite;

import com.alibaba.cloud.ai.toolcalling.larksuite.param.req.ValuesGetReq;
import com.alibaba.cloud.ai.toolcalling.larksuite.param.resp.ValuesGetResp;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lark.oapi.Client;
import com.lark.oapi.core.response.RawResponse;
import com.lark.oapi.core.token.AccessTokenType;
import com.lark.oapi.core.utils.UnmarshalRespUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.util.function.Function;

/**
 * @author huaiziqing
 */
public class LarkSuiteGetSheetContentService
		implements Function<LarkSuiteGetSheetContentService.SheetGetRequest, Object> {

	private static final String LARK_SHEET_GET_URL = "https://open.feishu.cn/open-apis/sheets/v3/spreadsheets/:spreadsheet_token";

	private static final Logger logger = LoggerFactory.getLogger(LarkSuiteGetSheetContentService.class);

	LarkSuiteProperties larkSuiteProperties;

	public LarkSuiteGetSheetContentService(LarkSuiteProperties properties) {
		this.larkSuiteProperties = properties;
	}

	@Override
	public Object apply(SheetGetRequest request) {
		if (ObjectUtils.isEmpty(larkSuiteProperties.getAppId())
				|| ObjectUtils.isEmpty(larkSuiteProperties.getAppSecret())) {
			logger.error("current larksuite appId or appSecret must not be null.");
			throw new IllegalArgumentException("current larksuite appId or appSecret must not be null.");
		}

		Client client = Client.newBuilder(larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret()).build();

		try {
			ValuesGetReq req = ValuesGetReq.newBuilder()
				.spreadsheetToken(request.spreadsheetToken())
				.range(request.range())
				.build();

			RawResponse httpResponse = client.get(
					String.format(LARK_SHEET_GET_URL, req.getSpreadsheetToken(), req.getRange()), null,
					AccessTokenType.Tenant);

			ValuesGetResp resp = UnmarshalRespUtil.unmarshalResp(httpResponse, ValuesGetResp.class);
			if (resp == null) {
				throw new IllegalArgumentException("The result returned by the server is illegal");
			}

			if (!resp.success()) {
				throw new RuntimeException("获取表格数据失败: " + resp.getMsg());
			}

			return resp.getData().getValues();
		}
		catch (Exception e) {
			logger.error("获取表格数据异常", e);
			throw new RuntimeException(e);
		}
	}

	public record SheetGetRequest(@JsonProperty(required = true, value = "spreadsheetToken") String spreadsheetToken,
			@JsonProperty(required = true, value = "range") String range) {
	}

}