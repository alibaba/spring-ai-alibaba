package com.alibaba.cloud.ai.functioncalling.kuaidi100;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @Author: XiaoYunTao
 * @Date: 2024/12/18
 */
@Configuration
@ConditionalOnClass(Kuaidi100AutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.kuaidi100", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(Kuaidi100Properties.class)
public class Kuaidi100AutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Query courier tracking information")
	public Kuaidi100Service queryTrackFunction(Kuaidi100Properties kuaidi100Properties) {
		return new Kuaidi100Service(kuaidi100Properties);
	}

}
