package com.alibaba.cloud.ai.example.deepresearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yingzi
 * @date 2025/6/8 16:28
 */
@ConfigurationProperties(prefix = DeepResearchProperties.PREFIX)
public class DeepResearchProperties {

	public static final String PREFIX = "spring.ai.ai.baba.deep-research";

}
