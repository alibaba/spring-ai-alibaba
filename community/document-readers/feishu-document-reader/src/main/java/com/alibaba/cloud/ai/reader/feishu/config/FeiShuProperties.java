package com.alibaba.cloud.ai.reader.feishu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wblu214
 * @author <a href="mailto:2897718178@qq.com">wblu214</a>
 */
@ConfigurationProperties(prefix = FeiShuProperties.FEISHU_PROPERTIES_PREFIX)
public class FeiShuProperties {

	public static final String FEISHU_PROPERTIES_PREFIX = "spring.ai.alibaba.plugin.feishu";

	private Boolean enabled;

	private String appId;

	private String appSecret;

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public void setAppSecret(String appSecret) {
		this.appSecret = appSecret;
	}

	@Override
	public String toString() {
		return "FeiShuProperties{" + "enabled=" + enabled + ", appId='" + appId + '\'' + ", AppSecret='" + appSecret
				+ '\'' + '}';
	}

}
