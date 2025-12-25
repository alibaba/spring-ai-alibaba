package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;

import java.util.Properties;

/**
 * @author zhuoguang
 */
public class NacosClientService {
    
    private final ConfigService configService;
    
    private final NamingService namingService;
    
    public NacosClientService(Properties nacosProperties) throws NacosException {
        this.configService = NacosFactory.createConfigService(nacosProperties);
        this.namingService = NacosFactory.createNamingService(nacosProperties);
    }
    
    public ConfigService getConfigService() {
        return configService;
    }
    
    public NamingService getNamingService() {
        return namingService;
    }

}
