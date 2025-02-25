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
package com.alibaba.cloud.ai.toolcalling.dingtalk;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.util.ObjectUtils;

import java.util.function.Function;

/**
 * @author YunLong
 */
public class DingTalkRobotService implements Function<DingTalkRobotService.Request, DingTalkRobotService.Response> {

	private final DingTalkProperties dingTalkProperties;

	public DingTalkRobotService(DingTalkProperties dingTalkProperties) {
		this.dingTalkProperties = dingTalkProperties;
	}

	/**
	 * The old version of DingTalk SDK. Some interfaces have not been fully replaced yet.
	 * Official Document
	 * Address：https://open.dingtalk.com/document/orgapp/custom-robots-send-group-messages
	 */
	@Override
	public Response apply(Request request) {

		if (ObjectUtils.isEmpty(dingTalkProperties.getCustomRobotAccessToken())
				|| ObjectUtils.isEmpty(dingTalkProperties.getCustomRobotSignature())) {
			throw new IllegalArgumentException("spring.ai.alibaba.plugin.dingtalk.custom-robot must not be null.");
		}

		String accessToken = dingTalkProperties.getCustomRobotAccessToken();
		String signature = dingTalkProperties.getCustomRobotSignature();

		// Request Body, please see the official document for more parameters.
		OapiRobotSendRequest req = new OapiRobotSendRequest();
		req.setMsgtype("text");
		req.setText(String.format("{\"content\":\"%s\"}", request.message()));

		try {
			DingTalkClient client = new DefaultDingTalkClient(
					String.format("https://oapi.dingtalk.com/robot/send?%s", SignTools.getSign(signature)));
			OapiRobotSendResponse response = client.execute(req, accessToken);

			if (response.isSuccess()) {
				return new Response("The custom robot message was sent successfully!");
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Custom robot message sending failed!");
		}

		return null;
	}

	@JsonClassDescription("Send group chat messages using a custom robot")
	public record Request(@JsonProperty(required = true,
			value = "message") @JsonPropertyDescription("Customize what the robot needs to send") String message) {
	}

	public record Response(String message) {
	}

}
