/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.a2a.autoconfigure.nacos;

import com.alibaba.cloud.ai.a2a.registry.nacos.discovery.NacosAgentCardProvider;
import com.alibaba.cloud.ai.a2a.registry.nacos.properties.NacosA2aProperties;
import com.alibaba.cloud.ai.a2a.autoconfigure.client.A2aClientAgentCardProviderAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.alibaba.nacos.api.ai.A2aService;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.exception.NacosException;

/**
 * The AutoConfiguration for A2A Nacos discovery.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(before = { A2aClientAgentCardProviderAutoConfiguration.class })
@ConditionalOnClass({ A2aClientAgentCardProviderAutoConfiguration.class })
@EnableConfigurationProperties({ NacosA2aProperties.class })
@ConditionalOnProperty(prefix = NacosA2aProperties.PREFIX, value = "discovery.enabled", havingValue = "true",
		matchIfMissing = true)
public class NacosA2aDiscoveryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public A2aService a2aService(NacosA2aProperties nacosA2aProperties) throws NacosException {
		return AiFactory.createAiService(nacosA2aProperties.getNacosProperties());
	}

	@Bean
	public NacosAgentCardProvider nacosAgentCardProvider(A2aService a2aService) throws Exception {
		return new NacosAgentCardProvider(a2aService);
	}

}
