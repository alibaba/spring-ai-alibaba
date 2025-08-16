/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.utils.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * AES encryption and decryption utility class.
 *
 * @since 1.0.0.3
 */
public class AESCryptUtils {

	/** AES cipher algorithm specification */
	private static final String AES_CIPHER = "AES/ECB/PKCS5Padding";

	/** AES encryption key */
	public static final String AES_KEY = "agentscope_5qAI8#nO-d@xK7$kdF+Dh";

	/**
	 * Encrypts a string using AES algorithm.
	 * @param original The string to be encrypted
	 * @return Base64 encoded encrypted string
	 */
	public static String encrypt(String original) {
		try {
			byte[] raw = AES_KEY.getBytes();
			SecretKeySpec secKey = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance(AES_CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, secKey);
			byte[] byte_content = original.getBytes(StandardCharsets.UTF_8);
			byte[] encode_content = cipher.doFinal(byte_content);
			return org.apache.commons.codec.binary.Base64.encodeBase64String(encode_content);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Decrypts an AES encrypted string.
	 * @param encrypted Base64 encoded encrypted string
	 * @return Decrypted string
	 */
	public static String decrypt(String encrypted) {
		try {
			byte[] raw = AES_KEY.getBytes();
			SecretKeySpec secKey = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance(AES_CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, secKey);
			byte[] encode_content = org.apache.commons.codec.binary.Base64.decodeBase64(encrypted);
			byte[] byte_content = cipher.doFinal(encode_content);
			return new String(byte_content, StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Test method for encryption.
	 */
	public static void main(String[] args) {
		String encrypted = encrypt("sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		System.out.println(encrypted);
	}

}
