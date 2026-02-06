package com.alibaba.cloud.ai.higress.model;

/**
 * @Author NGshiyu
 * @Description
 * @CreateTime 2026/2/4 17:29
 */
public record SimpleHigressHmac(String accessKey, String secretKey) implements HigressHmac {

	public SimpleHigressHmac(String accessKey, String secretKey) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}

	@Override
	public String getAccessKeyValue() {
		return this.accessKey;
	}

	@Override
	public String getSecretKeyValue() {
		return this.secretKey;
	}

	@Override
	public String toString() {
		return "SimpleHigressHmac{value='***'}";
	}

}
