package com.alibaba.cloud.ai.agent.nacos.vo;

import lombok.Data;

@Data
public class ModelVO {
	private String baseUrl;

	private String apiKey;

	private String model;

	private String temperature;

	private String maxTokens;
}
