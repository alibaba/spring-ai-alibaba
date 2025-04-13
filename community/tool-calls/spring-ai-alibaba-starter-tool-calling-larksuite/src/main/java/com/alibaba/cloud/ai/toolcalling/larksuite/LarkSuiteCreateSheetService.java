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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.lark.oapi.Client;
import com.lark.oapi.service.docx.v1.model.CreateDocumentReq;
import com.lark.oapi.service.docx.v1.model.CreateDocumentReqBody;
import com.lark.oapi.service.docx.v1.model.CreateDocumentResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.util.function.Function;

/**
 * @author kone_net
 */
public class LarkSuiteCreateSheetService implements Function<LarkSuiteCreateSheetService.SheetRequest, Object> {

	private static final Logger logger = LoggerFactory.getLogger(LarkSuiteCreateSheetService.class);

	LarkSuiteProperties larkSuiteProperties;

	public LarkSuiteCreateSheetService(LarkSuiteProperties properties) {
		this.larkSuiteProperties = properties;
	}

	/**
	 * 创建飞书doc
	 * @param request the function argument
	 * @return CreateDocumentResp
	 */
	@Override
	public Object apply(SheetRequest request) {
		if (ObjectUtils.isEmpty(larkSuiteProperties.getAppId())
				|| ObjectUtils.isEmpty(larkSuiteProperties.getAppSecret())) {
			logger.error("current larksuite appId or appSecret must not be null.");
			throw new IllegalArgumentException("current larksuite appId or appSecret must not be null.");
		}

		logger.debug("current larksuite.appId is {},appSecret is {}",
				larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret());

		Client client = Client.newBuilder(larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret()).build();

		CreateDocumentResp resp;

		try {
			resp = client.docx()
				.document()
				.create(CreateDocumentReq.newBuilder()
					.createDocumentReqBody(CreateDocumentReqBody.newBuilder()
						.title(request.title)
//						.folderToken(request.folderToken)
						.build())
					.build());
			if (!resp.success()) {
				logger.error("code:{},msg:{},reqId:{}", resp.getCode(), resp.getMsg(), resp.getRequestId());
				return resp.getError();
			}
			return resp.getData();
		}
		catch (Exception e) {
			logger.error("failed to invoke larksuite sheet create, caused by:{}", e.getMessage());
		}
		return null;
	}

	public record SheetRequest(
			@JsonProperty(required = true,
					value = "title") @JsonPropertyDescription("the larksuite sheet title") String title,
			@JsonProperty(required = true,
					value = "email") @JsonPropertyDescription("email that needs to be authorized for the user")
			String email,
			@JsonProperty(required = true,
					value = "data") @JsonPropertyDescription("the larksuite sheet data") String data) {
	}

}