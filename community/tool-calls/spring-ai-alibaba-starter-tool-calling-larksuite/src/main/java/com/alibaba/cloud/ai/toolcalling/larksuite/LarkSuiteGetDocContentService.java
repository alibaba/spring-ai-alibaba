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
import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.docx.v1.model.RawContentDocumentReq;
import com.lark.oapi.service.docx.v1.model.RawContentDocumentResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * @author 北极星
 */
public class LarkSuiteGetDocContentService
		implements Function<LarkSuiteGetDocContentService.GetDocContentRequest, Object> {

	private static final Logger logger = LoggerFactory.getLogger(LarkSuiteChatService.class);

	LarkSuiteProperties larkSuiteProperties;

	public LarkSuiteGetDocContentService(LarkSuiteProperties properties) {
		this.larkSuiteProperties = properties;
	}

	/**
	 * 使用用户身份获取飞书文档内容 documentId 文档的唯一ID userAccessToken 用户访问凭证
	 * @return String 文档纯文本内容
	 */
	@Override
	public Object apply(GetDocContentRequest request) {

		String documentId = request.documentId;
		String userAccessToken = request.userAccessToken;
		int lang = request.lang;

		Client client = Client.newBuilder(larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret()).build();

		RawContentDocumentReq req = RawContentDocumentReq.newBuilder().documentId(documentId).lang(lang).build();

		RawContentDocumentResp resp = null;
		try {
			resp = client.docx()
				.document()
				.rawContent(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
			if (!resp.success()) {
				logger.error("code ${},msg: ${},reqId: ${}, resp: ${}", resp.getCode(), resp.getMsg(),
						resp.getRequestId(),
						Jsons.createGSON(true, false)
							.toJson(JsonParser
								.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
			}
			return Jsons.DEFAULT.toJson(resp.getData());
		}
		catch (Exception e) {
			logger.error("获取文档异常");
		}

		return resp;
	}

	record GetDocContentRequest(@JsonProperty("documentId") String documentId,
			@JsonProperty("userAccessToken") String userAccessToken, @JsonProperty("lang") int lang) {
	}

}
