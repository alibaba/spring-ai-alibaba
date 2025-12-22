package com.alibaba.cloud.ai.studio.admin.config;

import com.alibaba.cloud.ai.studio.admin.service.impl.NacosClientService;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NacosConfig {
    
    @Bean
    public NacosClientService nacosClientService(NacosProperties nacosProperties) throws NacosException {
        return new NacosClientService(nacosProperties.getNacosProperties());
    }
    
}
