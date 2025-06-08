package com.alibaba.cloud.ai.example.deepresearch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // Set async request timeout to 10 minutes (600000 milliseconds)
        configurer.setDefaultTimeout(600000);
    }
} 