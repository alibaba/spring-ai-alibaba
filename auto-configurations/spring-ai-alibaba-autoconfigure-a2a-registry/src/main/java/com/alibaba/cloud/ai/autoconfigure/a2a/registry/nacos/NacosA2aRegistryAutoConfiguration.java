/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.autoconfigure.a2a.registry.nacos;

import com.alibaba.cloud.ai.a2a.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.registry.nacos.properties.NacosA2aProperties;
import com.alibaba.cloud.ai.a2a.registry.nacos.register.NacosA2aRegistryProperties;
import com.alibaba.cloud.ai.a2a.registry.nacos.register.NacosAgentRegistry;
import com.alibaba.cloud.ai.a2a.registry.nacos.service.NacosA2aOperationService;
import com.alibaba.cloud.ai.autoconfigure.a2a.server.A2aServerAgentCardAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.a2a.server.A2aServerRegistryAutoConfiguration;
import com.alibaba.nacos.api.ai.A2aService;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.exception.NacosException;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * The AutoConfiguration for A2A Nacos registry.
 *
 * @author xiweng.yy
 */
@ConditionalOnClass({ A2aServerAgentCardAutoConfiguration.class, A2aServerRegistryAutoConfiguration.class })
@AutoConfiguration(after = A2aServerAgentCardAutoConfiguration.class,
		before = { A2aServerRegistryAutoConfiguration.class })
@EnableConfigurationProperties({ NacosA2aProperties.class, NacosA2aRegistryProperties.class })
@ConditionalOnProperty(prefix = NacosA2aRegistryProperties.PREFIX, value = ".enabled", havingValue = "true",
		matchIfMissing = true)
public class NacosA2aRegistryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public A2aService a2aService(NacosA2aProperties nacosA2aProperties) throws NacosException {
		return AiFactory.createAiService(nacosA2aProperties.getNacosProperties());
	}

	@Bean
	public NacosA2aOperationService nacosA2aOperationService(A2aService a2aService,
			NacosA2aProperties nacosA2aProperties, A2aServerProperties a2aServerProperties,
			NacosA2aRegistryProperties nacosA2aRegistryProperties) {
		return new NacosA2aOperationService(a2aService, nacosA2aProperties, a2aServerProperties,
				nacosA2aRegistryProperties);
	}

	@Bean
	public NacosAgentRegistry nacosAgentRegistry(NacosA2aOperationService nacosA2aOperationService,
			NacosA2aProperties nacosA2aProperties) {
		return new NacosAgentRegistry(nacosA2aOperationService, nacosA2aProperties);
	}

}
