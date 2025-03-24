package com.alibaba.cloud.ai.example.manus.config;

import com.alibaba.cloud.ai.example.manus.llm.ToolBuilder;
import com.alibaba.cloud.ai.example.manus.service.ChromeDriverService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfiguration {
    
    @Bean
    public ToolBuilder toolBuilder(ChromeDriverService chromeDriverService) {
        return new ToolBuilder(chromeDriverService);
    }
}
