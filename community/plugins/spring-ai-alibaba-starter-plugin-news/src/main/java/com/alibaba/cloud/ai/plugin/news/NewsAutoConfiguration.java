package com.alibaba.cloud.ai.plugin.news;

import com.alibaba.cloud.ai.plugin.news.service.SinaService;
import com.alibaba.cloud.ai.plugin.news.service.ToutiaoService;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * @Author: XiaoYunTao
 * @Date: 2024/12/18
 */
@Configuration
@ConditionalOnClass({ SinaService.class, ToutiaoService.class })
public class NewsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Get the news from the toutiao news (获取今日头条新闻).")
	public ToutiaoService getToutiaoNews() {
		return new ToutiaoService();
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("Get the news from the Sina news (获取新浪新闻).")
	public SinaService getSinaNews() {
		return new SinaService();
	}

}
