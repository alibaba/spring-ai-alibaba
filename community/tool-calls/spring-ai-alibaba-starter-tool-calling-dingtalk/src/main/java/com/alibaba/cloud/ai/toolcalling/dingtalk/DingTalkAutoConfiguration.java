/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.toolcalling.dingtalk;

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
@ConditionalOnProperty(prefix = DingTalkConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class DingTalkAutoConfiguration {

	@Bean(name = DingTalkConstants.TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Send DingTalk group chat messages using a custom robot")
	public DingTalkRobotService dingTalkGroupSendMessageByCustomRobot(DingTalkProperties dingTalkProperties) {
		return new DingTalkRobotService(dingTalkProperties);
	}

}
