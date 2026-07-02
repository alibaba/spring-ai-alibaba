package com.alibaba.cloud.ai.prompt.nacos.autoconfigure;

import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.ai.NacosAiService;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(NacosAiService.class)
@EnableConfigurationProperties(NacosPromptProperties.class)
@ConditionalOnProperty(prefix = NacosPromptProperties.PREFIX, value = "enabled", havingValue = "true", matchIfMissing = true)
public class NacosPromptManagerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AiService aiService(NacosPromptProperties nacosPromptProperties) throws NacosException {
		return AiFactory.createAiService(nacosPromptProperties.getNacosProperties());
	}

}
