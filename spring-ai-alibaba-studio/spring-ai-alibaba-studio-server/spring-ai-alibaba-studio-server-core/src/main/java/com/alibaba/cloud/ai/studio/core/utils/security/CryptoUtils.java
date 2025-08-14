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

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for cryptographic operations. Provides methods for string masking and
 * hashing.
 *
 * @since 1.0.0.3
 */
public class CryptoUtils {

	/**
	 * Masks a string by showing only the first and last 4 characters. If input is blank
	 * or shorter than 12 characters, returns 16 asterisks.
	 * @param original The string to mask
	 * @return Masked string
	 */
	public static String mask(String original) {
		if (StringUtils.isBlank(original) || original.length() <= 12) {
			return "*".repeat(16);
		}

		int prefix_length = 4;
		String start = original.substring(0, prefix_length);
		String end = original.substring(original.length() - prefix_length);
		int middleLength = original.length() - prefix_length * 2;

		return start + "*".repeat(middleLength) + end;
	}

	/**
	 * Generates SHA-512 hash of the input string.
	 * @param original The string to hash
	 * @return Hexadecimal representation of the hash
	 * @throws RuntimeException if SHA-512 algorithm is not available
	 */
	public static String hashWithSha512(String original) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-512");
			byte[] encodedHash = digest.digest(original.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(encodedHash);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("failed to hash with sha-512", e);
		}
	}

	/**
	 * Converts byte array to hexadecimal string.
	 * @param hash Byte array to convert
	 * @return Hexadecimal string representation
	 */
	private static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder(2 * hash.length);
		for (byte b : hash) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

}
