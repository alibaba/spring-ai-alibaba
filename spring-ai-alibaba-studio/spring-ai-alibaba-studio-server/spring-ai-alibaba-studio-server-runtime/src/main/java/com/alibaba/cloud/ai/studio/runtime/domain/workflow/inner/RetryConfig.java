package com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 错误重试配置
 *
 * @since 1.0.0-beta
 */
@Data
public class RetryConfig implements Serializable {

	@JsonProperty("max_retries")
	private Integer maxRetries;

	@JsonProperty("retry_enabled")
	private Boolean retryEnabled;

	@JsonProperty("retry_interval")
	private Integer retryInterval;

}
