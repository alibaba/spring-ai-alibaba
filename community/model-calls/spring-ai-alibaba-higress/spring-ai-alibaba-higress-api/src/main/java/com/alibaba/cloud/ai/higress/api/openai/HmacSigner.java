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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/**
 * HMAC签名工具类，用于对HTTP请求进行签名
 */
public class HmacSigner {

	private final String secretKey;

	private final String algorithm;

	public HmacSigner(String secretKey) {
		this(secretKey, "HmacSHA256");
	}

	public HmacSigner(String secretKey, String algorithm) {
		this.secretKey = secretKey;
		this.algorithm = algorithm;
	}

	/**
	 * 生成待签名字符串
	 * @param method HTTP方法
	 * @param path 请求路径
	 * @param date 日期时间字符串
	 * @param body 请求体内容
	 * @return 待签名字符串
	 */
	public String getStringToSign(String method, String path, String date, String body) {
		StringBuilder sb = new StringBuilder();
		sb.append(method).append("\n");
		sb.append(path).append("\n");
		sb.append(date).append("\n");
		if (body != null && !body.isEmpty()) {
			sb.append(body);
		}
		return sb.toString();
	}

	/**
	 * 计算HMAC签名
	 * @param stringToSign 待签名字符串
	 * @return Base64编码的签名
	 * @throws NoSuchAlgorithmException 算法不存在
	 * @throws InvalidKeyException 密钥无效
	 */
	public String sign(String stringToSign) throws NoSuchAlgorithmException, InvalidKeyException {
		Mac mac = Mac.getInstance(algorithm);
		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithm);
		mac.init(secretKeySpec);
		byte[] signature = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(signature);
	}

	/**
	 * 生成完整的签名（包含待签名字符串生成和签名计算）
	 * @param method HTTP方法
	 * @param path 请求路径
	 * @param date 日期时间字符串
	 * @param body 请求体内容
	 * @return Base64编码的签名
	 * @throws NoSuchAlgorithmException 算法不存在
	 * @throws InvalidKeyException 密钥无效
	 */
	public String generateSignature(String method, String path, String date, String body)
			throws NoSuchAlgorithmException, InvalidKeyException {
		String stringToSign = getStringToSign(method, path, date, body);
		return sign(stringToSign);
	}
}
