package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

import lombok.Getter;

@Getter
public enum InvokeSourceEnum {

	console("console", true, 3000, 10 * 60), api("api", false, null, 10 * 60),
	async("async", true, 10 * 1000, 60 * 60 * 12);

	private final String code;

	private final boolean isCached;

	private final Integer cacheRefreshIntervalMs;

	private final Integer timeoutSeconds;

	InvokeSourceEnum(String code, boolean isCached, Integer cacheRefreshIntervalMs, Integer timeoutSeconds) {
		this.code = code;
		this.isCached = isCached;
		this.cacheRefreshIntervalMs = cacheRefreshIntervalMs;
		this.timeoutSeconds = timeoutSeconds;
	}

}
