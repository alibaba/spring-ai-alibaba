/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.mcp.nacos;

import com.alibaba.cloud.ai.mcp.nacos.model.McpServerInfo;
import com.alibaba.cloud.ai.mcp.nacos.model.RemoteServerConfigInfo;
import com.alibaba.cloud.ai.mcp.nacos.model.ServiceRefInfo;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.utils.StringUtils;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.client.naming.NacosNamingService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Sunrisea
 */
public class NacosMcpRegister implements ApplicationListener<WebServerInitializedEvent> {
    
    private static final Logger log = LoggerFactory.getLogger(NacosMcpRegister.class);
    
    private final String toolsGroup = "mcp-tools";
    
    private final String toolsConfigSuffix = "-mcp-tools.json";
    
    private final String configNamespace = "nacos-default-mcp";
    
    private String type;
    
    private NacosMcpRegistryProperties nacosMcpProperties;
    
    private McpSchema.Implementation serverInfo;
    
    private McpAsyncServer mcpAsyncServer;
    
    private CopyOnWriteArrayList<McpServerFeatures.AsyncToolRegistration> tools;
    
    private McpSchema.ServerCapabilities serverCapabilities;
    
    private ConfigService configService;
    
    private ObjectMapper objectMapper;
    
    private ScheduledExecutorService executorService;
    
    public NacosMcpRegister(McpAsyncServer mcpAsyncServer, NacosMcpRegistryProperties nacosMcpProperties, String type) {
        this.mcpAsyncServer = mcpAsyncServer;
        log.info("Mcp server type: " + type);
        this.type = type;
        this.nacosMcpProperties = nacosMcpProperties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.executorService = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r);
            t.setName("nacos-mcp-register");
            t.setDaemon(true);
            return t;
        });
        
        try {
            Class clazz = McpAsyncServer.class;
            
            Field serverInfoField = clazz.getDeclaredField("serverInfo");
            serverInfoField.setAccessible(true);
            this.serverInfo = (McpSchema.Implementation) serverInfoField.get(mcpAsyncServer);
            
            Field serverCapabilitiesField = clazz.getDeclaredField("serverCapabilities");
            serverCapabilitiesField.setAccessible(true);
            this.serverCapabilities = (McpSchema.ServerCapabilities) serverCapabilitiesField.get(mcpAsyncServer);
            
            Field toolsField = clazz.getDeclaredField("tools");
            toolsField.setAccessible(true);
            this.tools = (CopyOnWriteArrayList<McpServerFeatures.AsyncToolRegistration>) toolsField.get(mcpAsyncServer);
            
            Properties configProperties = nacosMcpProperties.getNacosProperties();
            configProperties.put(PropertyKeyConst.NAMESPACE, configNamespace);
            this.configService = new NacosConfigService(configProperties);
            if (this.serverCapabilities.tools() != null) {
                String toolsInNacosContent = this.configService.getConfig(this.serverInfo.name() + toolsConfigSuffix,
                        toolsGroup, 3000);
                if (toolsInNacosContent != null) {
                    updateToolsDescription(toolsInNacosContent);
                }
                List<McpSchema.Tool> toolsNeedtoRegister = this.tools.stream()
                        .map(McpServerFeatures.AsyncToolRegistration::tool).toList();
                String toolsConfigContent = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(new McpSchema.ListToolsResult(toolsNeedtoRegister, null));
                boolean isPublishSuccess = this.configService.publishConfig(this.serverInfo.name() + toolsConfigSuffix,
                        toolsGroup, toolsConfigContent);
                if (!isPublishSuccess) {
                    log.error("Publish tools config to nacos failed.");
                    throw new Exception("Publish tools config to nacos failed.");
                }
                this.configService.addListener(this.serverInfo.name() + toolsConfigSuffix, toolsGroup, new Listener() {
                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        updateToolsDescription(configInfo);
                    }
                    
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }
                });
            }
            
            McpServerInfo mcpServerInfo = new McpServerInfo();
            mcpServerInfo.setName(this.serverInfo.name());
            mcpServerInfo.setVersion(this.serverInfo.version());
            mcpServerInfo.setEnable(true);
            if ("stdio".equals(this.type)) {
                mcpServerInfo.setType("local");
            } else {
                ServiceRefInfo serviceRefInfo = new ServiceRefInfo();
                serviceRefInfo.setNamespace(nacosMcpProperties.getNamespace());
                serviceRefInfo.setService(this.serverInfo.name());
                serviceRefInfo.setGroup(nacosMcpProperties.getGroup());
                RemoteServerConfigInfo remoteServerConfigInfo = new RemoteServerConfigInfo();
                remoteServerConfigInfo.setServiceRef(serviceRefInfo);
                remoteServerConfigInfo.setBackendProtocol("mcp-sse");
                remoteServerConfigInfo.setExportPath("/sse");
                mcpServerInfo.setRemoteServerConfig(remoteServerConfigInfo);
                mcpServerInfo.setType("sse-remote");
            }
            if (this.serverCapabilities.tools() != null) {
                mcpServerInfo.setToolsDescriptionRef(this.serverInfo.name() + toolsConfigSuffix);
            }
            boolean isPublishSuccess = this.configService.publishConfig(this.serverInfo.name() + "-mcp-server.json",
                    configNamespace, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mcpServerInfo));
            if (!isPublishSuccess) {
                log.error("Publish mcp server info to nacos failed.");
                throw new Exception("Publish mcp server info to nacos failed.");
            }
            log.info("Register mcp server info to nacos successfully");
        } catch (Exception e) {
            log.error("Failed to register mcp server to nacos", e);
        }
        
        executorService.scheduleWithFixedDelay(() -> {
            try {
                String toolsInNacosContent = this.configService.getConfig(this.serverInfo.name() + toolsConfigSuffix,
                        toolsGroup, 3000);
                List<McpSchema.Tool> toolsNeedtoRegister = this.tools.stream()
                        .map(McpServerFeatures.AsyncToolRegistration::tool).toList();
                String toolsConfigContent = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(new McpSchema.ListToolsResult(toolsNeedtoRegister, null));
                if (!StringUtils.equals(toolsConfigContent, toolsInNacosContent)) {
                    this.configService.publishConfig(this.serverInfo.name() + toolsConfigSuffix, toolsGroup,
                            toolsConfigContent);
                }
            } catch (Exception e) {
                log.error("Failed to update tools description to nacos", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    private void updateToolsDescription(String toolsInNacosContent) {
        try {
            McpSchema.ListToolsResult toolsInNacosResult = objectMapper.readValue(toolsInNacosContent,
                    McpSchema.ListToolsResult.class);
            List<McpServerFeatures.AsyncToolRegistration> toolsRegistrationNeedToUpdate = new ArrayList<>();
            List<McpSchema.Tool> toolsInNacos = toolsInNacosResult.tools();
            for (McpSchema.Tool toolInNacos : toolsInNacos) {
                for (McpServerFeatures.AsyncToolRegistration toolRegistration : this.tools) {
                    if (!toolRegistration.tool().name().equals(toolInNacos.name())) {
                        continue;
                    }
                    if (toolRegistration.tool().description() != null && !toolRegistration.tool().description()
                            .equals(toolInNacos.description())) {
                        McpSchema.Tool toolNeedtoUpdate = new McpSchema.Tool(toolRegistration.tool().name(),
                                toolInNacos.description(), toolRegistration.tool().inputSchema());
                        toolsRegistrationNeedToUpdate.add(
                                new McpServerFeatures.AsyncToolRegistration(toolNeedtoUpdate, toolRegistration.call()));
                        break;
                    }
                }
            }
            boolean descriptionChanged = false;
            for (McpServerFeatures.AsyncToolRegistration toolRegistration : toolsRegistrationNeedToUpdate) {
                for (int i = 0; i < this.tools.size(); i++) {
                    if (this.tools.get(i).tool().name().equals(toolRegistration.tool().name())) {
                        this.tools.set(i, toolRegistration);
                        descriptionChanged = true;
                        break;
                    }
                }
            }
            if (descriptionChanged) {
                log.info("tools description updated");
            }
            if (descriptionChanged && this.serverCapabilities.tools().listChanged()) {
                this.mcpAsyncServer.notifyToolsListChanged().block();
            }
        } catch (Exception e) {
            log.error("Failed to update tools according to nacos", e);
        }
    }
    
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        if ("stdio".equals(this.type)) {
            log.info("No need to register mcp server service to nacos");
            return;
        }
        try {
            int port = event.getWebServer().getPort();
            NamingService namingService = new NacosNamingService(nacosMcpProperties.getNacosProperties());
            namingService.registerInstance(this.serverInfo.name(), nacosMcpProperties.getGroup(),
                    nacosMcpProperties.getIp(), port);
            log.info("Register mcp server service to nacos successfully");
        } catch (NacosException e) {
            log.error("Failed to register mcp server service to nacos", e);
        }
    }
    
}
