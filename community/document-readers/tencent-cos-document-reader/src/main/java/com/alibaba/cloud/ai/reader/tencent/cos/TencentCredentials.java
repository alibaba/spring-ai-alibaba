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
package com.alibaba.cloud.ai.reader.tencent.cos;

import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.auth.COSStaticCredentialsProvider;
import org.springframework.util.Assert;

/**
 * @author HeYQ
 * @since 2024-11-27 21:41
 */
public class TencentCredentials {

	private final String secretId;

	private final String secretKey;

	private final String sessionToken;

	public TencentCredentials(String secretId, String secretKey) {
		this(secretId, secretKey, null);
	}

	public TencentCredentials(String secretId, String secretKey, String sessionToken) {
		Assert.notNull(secretId, "accessKeyId must not be null");
		Assert.notNull(secretKey, "secretAccessKey must not be null");
		this.secretId = secretId;
		this.secretKey = secretKey;
		this.sessionToken = sessionToken;
	}

	public COSCredentialsProvider toCredentialsProvider() {
		return new COSStaticCredentialsProvider(toCredentials());
	}

	public COSCredentials toCredentials() {
		if (sessionToken == null) {
			return new BasicCOSCredentials(secretId, secretKey);
		}

		return new BasicSessionCredentials(secretId, secretKey, sessionToken);
	}

}
