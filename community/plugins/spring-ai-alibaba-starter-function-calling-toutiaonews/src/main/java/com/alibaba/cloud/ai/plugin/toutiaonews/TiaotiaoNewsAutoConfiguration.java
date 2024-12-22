package com.alibaba.cloud.ai.plugin.toutiaonews;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @Author: XiaoYunTao
 * @Date: 2024/12/18
 */
@Configuration
@ConditionalOnClass(ToutiaoNewsService.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.toutiaonews", name = "enabled", havingValue = "true")
public class TiaotiaoNewsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Get the news from the toutiao news (获取今日头条新闻).")
	public ToutiaoNewsService getToutiaoNewsFunction() {
		return new ToutiaoNewsService();
	}

}
