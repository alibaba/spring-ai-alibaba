package com.alibaba.cloud.ai;

import com.alibaba.cloud.ai.service.CustomRobotSendMessageService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author YunLong
 */
@AutoConfiguration
public class Config {

    @Bean
    @ConditionalOnMissingBean
    @Description("Send group chat messages using a custom robot")
    public CustomRobotSendMessageService CustomRobotSendMessageFunction() {
        return new CustomRobotSendMessageService();
    }
}
