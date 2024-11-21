package com.alibaba.cloud.ai;

import com.alibaba.cloud.ai.properties.DingTalkProperties;
import com.alibaba.cloud.ai.service.CustomRobotSendMessageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author YunLong
 */
@EnableConfigurationProperties(DingTalkProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.dingtalk", name = "enabled", havingValue = "true")
public class DingTalkConfig {

    @Bean
    @ConditionalOnMissingBean
    @Description("Send group chat messages using a custom robot")
    public CustomRobotSendMessageService CustomRobotSendMessageFunction(DingTalkProperties dingTalkProperties) {
        return new CustomRobotSendMessageService(dingTalkProperties);
    }
}
