package com.alibaba.cloud.ai.autoconfig.dashscope;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_READ_TIMEOUT;

/**
 * Spring AI Alibaba TongYi LLM connection properties.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 2023.0.1.0
 */

@ConfigurationProperties(DashScopeConnectionProperties.CONFIG_PREFIX)
public class DashScopeConnectionProperties extends DashScopeParentProperties {

	public static final String CONFIG_PREFIX = "spring.ai.dashscope";

	private Integer readTimeout;

	public DashScopeConnectionProperties() {
		super.setBaseUrl(DEFAULT_BASE_URL);
		readTimeout = DEFAULT_READ_TIMEOUT;
	}

	public Integer getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Integer readTimeout) {
		this.readTimeout = readTimeout;
	}

}
