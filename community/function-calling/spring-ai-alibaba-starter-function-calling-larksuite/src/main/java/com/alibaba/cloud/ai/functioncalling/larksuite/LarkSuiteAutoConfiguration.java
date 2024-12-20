package com.alibaba.cloud.ai.functioncalling.larksuite;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author 北极星
 */
@EnableConfigurationProperties({ LarkSuiteProperties.class })
@ConditionalOnClass({ LarkSuiteProperties.class })
@ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.larksuite", name = "enabled", havingValue = "true")
public class LarkSuiteAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("It calls the document api to invoke a method to create a larksuite document")
	public LarkSuiteCreateDocService larksuiteCreateDocFunction(LarkSuiteProperties properties) {
		return new LarkSuiteCreateDocService(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("It runs a api to invoke a method to send message including group and single chat")
	public LarkSuiteChatService larksuiteChatFunction(LarkSuiteProperties properties) {
		return new LarkSuiteChatService(properties);
	}

}
