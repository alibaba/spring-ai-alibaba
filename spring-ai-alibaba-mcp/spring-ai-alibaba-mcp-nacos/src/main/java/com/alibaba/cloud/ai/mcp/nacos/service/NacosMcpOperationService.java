package com.alibaba.cloud.ai.mcp.nacos.service;

import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpAsyncClient;
import com.alibaba.cloud.ai.mcp.nacos.service.model.NacosMcpServerEndpoint;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerFactory;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Sunriseas
 */
public class NacosMcpOperationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadbalancedMcpAsyncClient.class);
    
    private AiMaintainerService aiMaintainerService;
    
    private String namespace;
    
    private Map<String, List<NacosMcpSubscriber>> subscribers;
    
    private ScheduledExecutorService executorService;
    
    public NacosMcpOperationService(Properties nacosProperties) throws NacosException {
        this.aiMaintainerService = AiMaintainerFactory.createAiMaintainerService(nacosProperties);
        this.namespace = nacosProperties.getProperty(PropertyKeyConst.NAMESPACE, "public");
        this.subscribers = new ConcurrentHashMap<>();
        this.executorService = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r);
            t.setName("nacos-mcp-operation-service");
            t.setDaemon(true);
            return t;
        });
        
        executorService.scheduleWithFixedDelay(this::getServerChange, 30, 30, TimeUnit.SECONDS);
    }
    
    private void getServerChange() {
        for (Map.Entry<String, List<NacosMcpSubscriber>> entry : subscribers.entrySet()) {
            String mcpNameAndVersion = entry.getKey();
            List<NacosMcpSubscriber> nacosMcpSubscribers = entry.getValue();
            try {
                McpServerDetailInfo mcpServerDetailInfo = this.getServerDetail(mcpNameAndVersion);
                if (mcpServerDetailInfo == null) {
                    continue;
                }
                for (NacosMcpSubscriber nacosMcpSubscriber : nacosMcpSubscribers) {
                    nacosMcpSubscriber.receive(mcpServerDetailInfo);
                }
            } catch (Exception e) {
                logger.error("getServerChange error", e);
            }
        }
    }
    
    public NacosMcpServerEndpoint getServerEndpoint(String mcpNameAndVersion) throws NacosException {
        if (mcpNameAndVersion == null) {
            throw new IllegalArgumentException("mcpNameAndVersion must not be null");
        }
        McpServerDetailInfo mcpServerDetailInfo = this.getServerDetail(mcpNameAndVersion);
        if (mcpServerDetailInfo == null) {
            return null;
        }
        List<McpEndpointInfo> mcpEndpointInfoList = mcpServerDetailInfo.getBackendEndpoints();
        String exportPath = mcpServerDetailInfo.getRemoteServerConfig().getExportPath();
        String protocol = mcpServerDetailInfo.getProtocol();
        String realVersion = mcpServerDetailInfo.getVersionDetail().getVersion();
        return new NacosMcpServerEndpoint(mcpEndpointInfoList, exportPath, protocol, realVersion);
    }
    
    public McpServerDetailInfo getServerDetail(String mcpNameAndVersion) throws NacosException {
        if (mcpNameAndVersion == null) {
            throw new IllegalArgumentException("mcpNameAndVersion must not be null");
        }
        String[] nameAndVersion = mcpNameAndVersion.strip().split("::");
        String version = null;
        String mcpName = mcpNameAndVersion;
        if (nameAndVersion.length > 2) {
            throw new NacosException(NacosException.INVALID_PARAM, "mcpName is invalid");
        }
        if (nameAndVersion.length == 2) {
            version = nameAndVersion[1];
            mcpName = nameAndVersion[0];
        }
        return aiMaintainerService.getMcpServerDetail(this.namespace, mcpName, version);
    }
    
    
    public void subscribeNacosMcpServer(String mcpNameAndVersion, NacosMcpSubscriber nacosMcpSubscriber) {
        if (mcpNameAndVersion == null || nacosMcpSubscriber == null) {
            throw new IllegalArgumentException("mcpNameAndVersion and nacosMcpSubscriber must not be null");
        }
        this.subscribers.computeIfAbsent(mcpNameAndVersion, k -> new ArrayList<>()).add(nacosMcpSubscriber);
    }
    
}
