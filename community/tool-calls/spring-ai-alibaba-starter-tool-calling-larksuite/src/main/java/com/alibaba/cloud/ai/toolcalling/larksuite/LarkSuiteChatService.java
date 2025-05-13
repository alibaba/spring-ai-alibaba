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
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * @author 北极星
 */
public class LarkSuiteChatService implements Function<LarkSuiteChatService.IMChatRequest, CreateMessageResp> {

	private static final Logger logger = LoggerFactory.getLogger(LarkSuiteChatService.class);

	LarkSuiteProperties larkSuiteProperties;

	public LarkSuiteChatService(LarkSuiteProperties properties) {
		this.larkSuiteProperties = properties;
	}

	@Override
	public CreateMessageResp apply(IMChatRequest request) {
		Client client = Client.newBuilder(larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret()).build();
		CreateMessageReq req = CreateMessageReq.newBuilder()
			.receiveIdType(request.receiveIdType())
			.createMessageReqBody(CreateMessageReqBody.newBuilder()
				.receiveId(request.receiveId())
				.msgType(request.msgType())
				.content(request.content())
				.uuid(request.uuid())
				.build())
			.build();

		CreateMessageResp resp = null;
		try {
			resp = client.im().v1().message().create(req);
			if (!resp.success()) {
				logger.error("code:{},msg:{},reqId:{}, resp:{}", resp.getCode(), resp.getMsg(), resp.getRequestId(),
						Jsons.createGSON(true, false)
							.toJson(JsonParser
								.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
			}
		}
		catch (Exception e) {
			logger.error("创建飞书文档异常");
		}

		return resp;
	}

	public record IMChatRequest(@JsonProperty(required = true, value = "receive_id") String receiveId,
			@JsonProperty(required = true, value = "receive_id_type", defaultValue = "open_id") String receiveIdType,
			@JsonProperty(required = true, value = "msg_type") String msgType,
			@JsonProperty(required = true, value = "content") String content,
			@JsonProperty(required = false, value = "uuid") String uuid) {
	}

}
