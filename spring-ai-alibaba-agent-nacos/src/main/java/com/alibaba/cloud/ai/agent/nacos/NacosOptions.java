package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerService;
import com.alibaba.nacos.maintainer.client.ai.NacosAiMaintainerServiceImpl;
import lombok.Data;

import java.util.Properties;

@Data
public class NacosOptions {
    
    protected boolean modelSpecified;
    
    protected boolean modelConfigEncrypted;
    
    protected boolean promptSpecified;
    
    String promptKey;
    
    private NacosConfigService nacosConfigService;
    
    private AiMaintainerService nacosAiMaintainerService;
    
    private String agentId;
    
    private String mcpNamespace;
    
    public NacosOptions(Properties properties) throws NacosException {
        nacosConfigService = new NacosConfigService(properties);
        nacosAiMaintainerService = new NacosAiMaintainerServiceImpl(properties);
    }
}
