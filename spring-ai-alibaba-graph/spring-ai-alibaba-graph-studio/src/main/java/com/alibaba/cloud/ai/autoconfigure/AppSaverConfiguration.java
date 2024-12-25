package com.alibaba.cloud.ai.autoconfigure;

import com.alibaba.cloud.ai.saver.AppMemorySaver;
import com.alibaba.cloud.ai.saver.AppSaver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppSaverConfiguration {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.studio";

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "saver", havingValue = "memory", matchIfMissing = true)
	public AppSaver appMemorySaver() {
		return new AppMemorySaver();
	}

}
