/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.messagechannel.adapter.dingtalk;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.alibaba.cloud.ai.messagechannel.model.ChannelMessage;
import com.alibaba.cloud.ai.messagechannel.model.ChannelReply;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class DingTalkChannelAdapterTest {

	private static final String SECRET = "SEC1234567890abcdef";

	private DingTalkChannelAdapter adapter;

	private DingTalkChannelAdapter adapterNoWebhook;

	@BeforeEach
	void setUp() {
		ObjectMapper mapper = new ObjectMapper();
		RestClient restClient = RestClient.builder().build();
		adapter = new DingTalkChannelAdapter("dingtalk", SECRET, "https://oapi.dingtalk.com/robot/send?access_token=t",
				mapper, restClient);
		adapterNoWebhook = new DingTalkChannelAdapter("dingtalk", SECRET, null, mapper, restClient);
	}

	@Test
	void parseInbound_validSignature_extractsTextAndIds() {
		long now = System.currentTimeMillis();
		String body = """
				{
				  "msgtype": "text",
				  "text": {"content": "  hello agent  "},
				  "senderStaffId": "u123",
				  "senderNick": "Alice",
				  "conversationId": "cid-9",
				  "conversationType": "1",
				  "sessionWebhook": "https://oapi.dingtalk.com/robot/sendBySession?session=abc"
				}
				""";
		Map<String, String> headers = signedHeaders(now);

		ChannelMessage msg = adapter.parseInbound(headers, body);

		assertThat(msg.channelName()).isEqualTo("dingtalk");
		assertThat(msg.userId()).isEqualTo("u123");
		assertThat(msg.conversationId()).isEqualTo("cid-9");
		assertThat(msg.text()).isEqualTo("hello agent");
		assertThat(msg.attributes()).containsEntry("senderNick", "Alice");
		assertThat(msg.attributes()).containsEntry("conversationType", "1");
	}

	@Test
	void parseInbound_missingHeaders_throwsSecurity() {
		assertThatExceptionOfType(SecurityException.class)
				.isThrownBy(() -> adapter.parseInbound(new HashMap<>(), "{}"))
				.withMessageContaining("Missing");
	}

	@Test
	void parseInbound_tamperedSignature_throwsSecurity() {
		long now = System.currentTimeMillis();
		Map<String, String> headers = signedHeaders(now);
		headers.put("sign", flipFirstChar(headers.get("sign")));

		assertThatExceptionOfType(SecurityException.class)
				.isThrownBy(() -> adapter.parseInbound(headers, "{\"text\":{\"content\":\"x\"}}"))
				.withMessageContaining("Signature mismatch");
	}

	@Test
	void parseInbound_expiredTimestamp_throwsSecurity() {
		long stale = System.currentTimeMillis() - (10L * 60 * 1000);
		Map<String, String> headers = signedHeaders(stale);

		assertThatExceptionOfType(SecurityException.class)
				.isThrownBy(() -> adapter.parseInbound(headers, "{\"text\":{\"content\":\"x\"}}"))
				.withMessageContaining("replay window");
	}

	@Test
	void parseInbound_nonNumericTimestamp_throwsSecurity() {
		Map<String, String> headers = new HashMap<>();
		headers.put("timestamp", "not-a-number");
		headers.put("sign", "anything");

		assertThatExceptionOfType(SecurityException.class)
				.isThrownBy(() -> adapter.parseInbound(headers, "{}"))
				.withMessageContaining("Invalid timestamp");
	}

	@Test
	void serializeSyncReply_text_wrapsInDingTalkEnvelope() {
		Object env = adapter.serializeSyncReply(stubMessage(), ChannelReply.text("hi"));

		assertThat(env).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) env;
		assertThat(map).containsEntry("msgtype", "text");
		assertThat(map.get("text")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.containsEntry("content", "hi");
	}

	@Test
	void serializeSyncReply_markdown_wrapsInDingTalkEnvelope() {
		Object env = adapter.serializeSyncReply(stubMessage(), ChannelReply.markdown("**bold**"));

		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) env;
		assertThat(map).containsEntry("msgtype", "markdown");
		assertThat(map.get("markdown")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.containsEntry("text", "**bold**")
				.containsKey("title");
	}

	@Test
	void push_withoutWebhookUrl_throwsIllegalState() {
		assertThatIllegalStateException()
				.isThrownBy(() -> adapterNoWebhook.push("u1", "c1", ChannelReply.text("x")))
				.withMessageContaining("not configured with webhook-url");
	}

	private Map<String, String> signedHeaders(long timestamp) {
		Map<String, String> headers = new HashMap<>();
		headers.put("timestamp", String.valueOf(timestamp));
		headers.put("sign", computeSign(timestamp));
		return headers;
	}

	private String computeSign(long timestamp) {
		try {
			String payload = timestamp + "\n" + SECRET;
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static String flipFirstChar(String s) {
		char first = s.charAt(0);
		char swapped = (first == 'A') ? 'B' : 'A';
		return swapped + s.substring(1);
	}

	private static ChannelMessage stubMessage() {
		return new ChannelMessage("dingtalk", "u1", "c1", "in", null, Map.of());
	}

}
