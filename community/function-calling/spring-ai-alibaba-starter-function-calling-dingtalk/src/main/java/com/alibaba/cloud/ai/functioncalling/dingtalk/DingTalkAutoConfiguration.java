package com.alibaba.cloud.ai.functioncalling.dingtalk;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @author YunLong
 */
@Configuration
@EnableConfigurationProperties(DingTalkProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.dingtalk", name = "enabled", havingValue = "true")
public class DingTalkAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Send DingTalk group chat messages using a custom robot")
	public DingTalkService dingTalkGroupSendMessageByCustomRobotFunction(DingTalkProperties dingTalkProperties) {
		return new DingTalkService(dingTalkProperties);
	}

}
