package com.alibaba.cloud.ai.plugin.sinanews;

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
@ConditionalOnClass(SinaNewsService.class)
@ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.sinanews", name = "enabled", havingValue = "true")
public class SinaNewsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Get the news from the Sina news (获取新浪新闻).")
	public SinaNewsService getSinaNewsFunction() {
		return new SinaNewsService();
	}

}
