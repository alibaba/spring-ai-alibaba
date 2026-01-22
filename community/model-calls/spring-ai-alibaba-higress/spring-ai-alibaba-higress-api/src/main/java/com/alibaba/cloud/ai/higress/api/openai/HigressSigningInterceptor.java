/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.higress.api.openai;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Higress签名拦截器，在HTTP请求发出前添加HMAC签名
 */
public class HigressSigningInterceptor implements ClientHttpRequestInterceptor {

	private static final String DATE_HEADER = "Date";

	private static final String SIGNATURE_HEADER = "X-Hmac-Signature";

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
			.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
			.withZone(ZoneId.of("GMT"));

	private final HmacSigner hmacSigner;

	private final boolean enabled;

	public HigressSigningInterceptor(HmacSigner hmacSigner) {
		this(hmacSigner, true);
	}

	public HigressSigningInterceptor(HmacSigner hmacSigner, boolean enabled) {
		this.hmacSigner = hmacSigner;
		this.enabled = enabled;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		if (!enabled || hmacSigner == null) {
			return execution.execute(request, body);
		}

		try {
			// 生成日期时间字符串
			String date = DATE_FORMATTER.format(Instant.now());
			
			// 添加Date头
			HttpHeaders headers = request.getHeaders();
			headers.set(DATE_HEADER, date);

			// 获取请求方法和路径
			String method = request.getMethod().name();
			String path = request.getURI().getPath();
			String query = request.getURI().getQuery();
			if (query != null && !query.isEmpty()) {
				path = path + "?" + query;
			}

			// 获取请求体内容
			String bodyString = body != null && body.length > 0
					? new String(body, StandardCharsets.UTF_8)
					: "";

			// 生成签名
			String signature = hmacSigner.generateSignature(method, path, date, bodyString);

			// 添加签名头
			headers.set(SIGNATURE_HEADER, signature);

			// 执行请求
			return execution.execute(request, body);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IOException("Failed to generate HMAC signature", e);
		}
	}
}
