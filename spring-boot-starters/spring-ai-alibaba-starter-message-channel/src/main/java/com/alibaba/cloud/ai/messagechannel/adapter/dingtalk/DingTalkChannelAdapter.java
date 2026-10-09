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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.alibaba.cloud.ai.messagechannel.adapter.MessageChannelAdapter;
import com.alibaba.cloud.ai.messagechannel.model.ChannelMessage;
import com.alibaba.cloud.ai.messagechannel.model.ChannelReply;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * Reference adapter for DingTalk enterprise inner-app robots.
 *
 * <p>Inbound: DingTalk signs each callback with two headers, {@code timestamp} and
 * {@code sign}, where {@code sign = base64(HmacSHA256(timestamp + "\n" + appSecret,
 * appSecret))}. Mismatch is treated as a forged request.</p>
 *
 * <p>Reply: returned synchronously in the HTTP response body using DingTalk's
 * {@code msgtype}/{@code text}/{@code markdown} envelope. This is the "smart reply"
 * path that does not require an access token.</p>
 *
 * <p>Active push: posts to a configured robot webhook URL (the same URL DingTalk
 * shows you in the bot setup page) with a fresh {@code timestamp}+{@code sign}
 * query string. Cross-conversation push to arbitrary users requires OAuth and is
 * not handled here — extend the class if you need it.</p>
 */
public class DingTalkChannelAdapter implements MessageChannelAdapter {

	private static final Logger log = LoggerFactory.getLogger(DingTalkChannelAdapter.class);

	private static final String HMAC_ALGO = "HmacSHA256";

	private static final long FIVE_MINUTES_MS = 5L * 60 * 1000;

	private final String name;

	private final String appSecret;

	private final String webhookUrl;

	private final ObjectMapper objectMapper;

	private final RestClient restClient;

	public DingTalkChannelAdapter(String name, String appSecret, String webhookUrl,
			ObjectMapper objectMapper, RestClient restClient) {
		this.name = name;
		this.appSecret = appSecret;
		this.webhookUrl = webhookUrl;
		this.objectMapper = objectMapper;
		this.restClient = restClient;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public ChannelMessage parseInbound(Map<String, String> headers, String rawBody) {
		String timestamp = headers.get("timestamp");
		String sign = headers.get("sign");
		verifySignature(timestamp, sign);

		JsonNode root;
		try {
			root = objectMapper.readTree(rawBody);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid DingTalk payload", e);
		}

		String text = root.path("text").path("content").asText("").trim();
		String senderId = firstNonBlank(root.path("senderStaffId").asText(null),
				root.path("senderId").asText(null));
		String conversationId = root.path("conversationId").asText(null);

		Map<String, Object> attrs = new HashMap<>();
		attrs.put("senderNick", root.path("senderNick").asText(null));
		attrs.put("conversationType", root.path("conversationType").asText(null));
		attrs.put("sessionWebhook", root.path("sessionWebhook").asText(null));
		attrs.put("sessionWebhookExpiredTime", root.path("sessionWebhookExpiredTime").asLong(0));

		return new ChannelMessage(name, senderId, conversationId, text, null, attrs);
	}

	@Override
	public Object serializeSyncReply(ChannelMessage message, ChannelReply reply) {
		return toDingTalkEnvelope(reply);
	}

	@Override
	public void push(String userId, String conversationId, ChannelReply reply) {
		if (webhookUrl == null || webhookUrl.isBlank()) {
			throw new IllegalStateException("DingTalk channel '" + name
					+ "' is not configured with webhook-url; cannot push proactively");
		}
		long timestamp = System.currentTimeMillis();
		String sign = signWebhookCall(timestamp);
		String url;
		try {
			url = webhookUrl + (webhookUrl.contains("?") ? "&" : "?")
					+ "timestamp=" + timestamp
					+ "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to build DingTalk push URL", e);
		}

		String response = restClient.post()
				.uri(url)
				.body(toDingTalkEnvelope(reply))
				.retrieve()
				.body(String.class);
		log.debug("DingTalk push response: {}", response);
	}

	private Map<String, Object> toDingTalkEnvelope(ChannelReply reply) {
		Map<String, Object> envelope = new HashMap<>();
		switch (reply.contentType()) {
			case MARKDOWN -> {
				envelope.put("msgtype", "markdown");
				Map<String, Object> md = new HashMap<>();
				md.put("title", "Agent");
				md.put("text", reply.content());
				envelope.put("markdown", md);
			}
			default -> {
				envelope.put("msgtype", "text");
				envelope.put("text", Map.of("content", reply.content()));
			}
		}
		return envelope;
	}

	private void verifySignature(String timestamp, String sign) {
		if (timestamp == null || sign == null) {
			throw new SecurityException("Missing DingTalk signature headers");
		}
		long ts;
		try {
			ts = Long.parseLong(timestamp);
		}
		catch (NumberFormatException e) {
			throw new SecurityException("Invalid timestamp header");
		}
		if (Math.abs(System.currentTimeMillis() - ts) > FIVE_MINUTES_MS) {
			throw new SecurityException("Timestamp outside the 5-minute replay window");
		}
		String expected = sign(timestamp + "\n" + appSecret);
		if (!constantTimeEquals(expected, sign)) {
			throw new SecurityException("Signature mismatch");
		}
	}

	private String signWebhookCall(long timestamp) {
		return sign(timestamp + "\n" + appSecret);
	}

	private String sign(String stringToSign) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGO);
			mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
			byte[] raw = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(raw);
		}
		catch (Exception e) {
			throw new IllegalStateException("HMAC-SHA256 unavailable", e);
		}
	}

	private static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null || a.length() != b.length()) {
			return false;
		}
		int diff = 0;
		for (int i = 0; i < a.length(); i++) {
			diff |= a.charAt(i) ^ b.charAt(i);
		}
		return diff == 0;
	}

	private static String firstNonBlank(String a, String b) {
		if (a != null && !a.isBlank()) {
			return a;
		}
		return b;
	}

}
