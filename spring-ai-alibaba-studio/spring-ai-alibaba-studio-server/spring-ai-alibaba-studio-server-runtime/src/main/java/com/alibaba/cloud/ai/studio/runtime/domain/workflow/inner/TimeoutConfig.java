package com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner;

import lombok.Data;

import java.io.Serializable;

@Data
public class TimeoutConfig implements Serializable {

	private Integer read;

	private Integer write;

	private Integer connect;

	public static TimeoutConfig createDefault() {
		TimeoutConfig config = new TimeoutConfig();
		config.setRead(5);
		config.setWrite(5);
		config.setConnect(5);
		return config;
	}

}
