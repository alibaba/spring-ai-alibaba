/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.mcp.nacos2.registry.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author yingzi
 * @since 2025/5/8:17:52
 */
public class MD5Utils {

	private static final char[] hexDigits = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
			'c', 'd', 'e', 'f' };

	private MessageDigest mdInst;

	public MD5Utils() {
		try {
			this.mdInst = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException var2) {
		}

	}

	public String getMd5(String input) {
		byte[] md5;
		synchronized (this.mdInst) {
			this.mdInst.update(input.getBytes(StandardCharsets.UTF_8));
			md5 = this.mdInst.digest();
		}

		int j = md5.length;
		char[] str = new char[j * 2];
		int k = 0;

		for (int i = 0; i < j; ++i) {
			byte byte0 = md5[i];
			str[k++] = hexDigits[byte0 >>> 4 & 15];
			str[k++] = hexDigits[byte0 & 15];
		}

		return new String(str);
	}

}
