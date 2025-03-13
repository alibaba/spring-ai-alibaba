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
 * @author 北极星
 */
public class LarkSuiteCreateDocService implements Function<LarkSuiteCreateDocService.DocRequest, Object> {

	private static final Logger logger = LoggerFactory.getLogger(LarkSuiteCreateDocService.class);

	LarkSuiteProperties larkSuiteProperties;

	public LarkSuiteCreateDocService(LarkSuiteProperties properties) {
		this.larkSuiteProperties = properties;
	}

	/**
	 * 创建飞书doc
	 * @param request the function argument
	 * @return CreateDocumentResp
	 */
	@Override
	public Object apply(DocRequest request) {
		if (ObjectUtils.isEmpty(larkSuiteProperties.getAppId())
				|| ObjectUtils.isEmpty(larkSuiteProperties.getAppSecret())) {
			logger.error("current spring.ai.alibaba.plugin.tool.larksuite must not be null.");
			throw new IllegalArgumentException("current spring.ai.plugin.tool.larksuite must not be null.");
		}

		logger.debug("current spring.ai.alibaba.plugin.tool.larksuite.appId is {},appSecret is {}",
				larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret());

		Client client = Client.newBuilder(larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret()).build();

		CreateDocumentResp resp = null;

		try {
			resp = client.docx()
				.document()
				.create(CreateDocumentReq.newBuilder()
					.createDocumentReqBody(CreateDocumentReqBody.newBuilder()
						.title(request.title)
						.folderToken(request.folderToken)
						.build())
					.build());
			if (!resp.success()) {
				logger.error("code:{},msg:{},reqId:{}", resp.getCode(), resp.getMsg(), resp.getRequestId());
				return resp.getError();
			}
			return resp.getData();
		}
		catch (Exception e) {
			logger.error("failed to invoke baidu search caused by:{}", e.getMessage());
		}
		return null;
	}

	public record DocRequest(
			@JsonProperty(required = true,
					value = "title") @JsonPropertyDescription("the larksuite title") String title,
			@JsonProperty(required = true,
					value = "folderToken") @JsonPropertyDescription("the larksuite folderToken") String folderToken) {
	}

}