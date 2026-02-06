package com.alibaba.cloud.ai.higress.model;

/**
 * @Author NGshiyu
 * @Description Auth higress by access_key and secret_key
 * @CreateTime 2026/2/4 17:20
 */
public interface HigressHmac {

	String getAccessKeyValue();

	String getSecretKeyValue();

}
