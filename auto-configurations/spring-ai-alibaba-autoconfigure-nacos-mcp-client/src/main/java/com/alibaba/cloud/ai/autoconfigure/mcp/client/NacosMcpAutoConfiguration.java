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

package com.alibaba.cloud.ai.autoconfigure.mcp.client;

import com.alibaba.cloud.ai.mcp.nacos.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author yingzi
 * @date 2025/6/4 19:16
 */
@AutoConfiguration
@EnableConfigurationProperties({ NacosMcpSseClientProperties.class, NacosMcpProperties.class})
public class NacosMcpAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(NacosMcpAutoConfiguration.class);

    public NacosMcpAutoConfiguration() {
    }

    @Bean(name = "namespace2NacosMcpOperationService")
    public Map<String, NacosMcpOperationService> namespace2NacosMcpOperationService(NacosMcpSseClientProperties nacosMcpSseClientProperties, NacosMcpProperties nacosMcpProperties) {
        Map<String, NacosMcpOperationService> namespace2NacosMcpOperationService = new HashMap<>();
        nacosMcpSseClientProperties.getConnections().forEach((serverKey, nacosSseParameters) -> {
            Properties nacosProperties = nacosMcpProperties.getNacosProperties();
            nacosProperties.put(PropertyKeyConst.NAMESPACE, nacosSseParameters.serviceNamespace());
            try {
                NacosMcpOperationService nacosMcpOperationService = new NacosMcpOperationService(nacosProperties);
                namespace2NacosMcpOperationService.put(nacosSseParameters.serviceNamespace(), nacosMcpOperationService);
            } catch (NacosException e) {
                logger.warn("nacos naming service: {} error", nacosSseParameters.serviceName(), e);
            }

        });
        return namespace2NacosMcpOperationService;
    }

}
