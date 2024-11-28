package com.alibaba.cloud.ai.tencent.cos;

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
