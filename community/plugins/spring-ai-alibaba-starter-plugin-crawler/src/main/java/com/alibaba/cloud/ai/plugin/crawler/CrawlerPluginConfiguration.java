package com.alibaba.cloud.ai.plugin.crawler;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

@AutoConfiguration
public class CrawlerPluginConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Description("A crawler plugin, can crawl the text content of the specified web page")
    public CrawlerService crawlerService() {
        return new CrawlerService();
    }
}
