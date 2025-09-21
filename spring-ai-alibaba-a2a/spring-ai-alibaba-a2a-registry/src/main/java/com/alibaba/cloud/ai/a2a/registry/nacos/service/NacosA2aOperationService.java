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

package com.alibaba.cloud.ai.a2a.registry.nacos.service;

import com.alibaba.cloud.ai.a2a.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.registry.nacos.properties.NacosA2aProperties;
import com.alibaba.cloud.ai.a2a.registry.nacos.register.NacosA2aRegistryProperties;
import com.alibaba.cloud.ai.a2a.registry.nacos.utils.AgentCardConverterUtil;
import com.alibaba.nacos.api.ai.A2aService;
import com.alibaba.nacos.api.ai.constant.AiConstants;
import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.ai.model.a2a.AgentEndpoint;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nacos operation service for A2A.
 *
 * @author xiweng.yy
 */
public class NacosA2aOperationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NacosA2aOperationService.class);

	private final A2aService a2aService;

	private final NacosA2aProperties nacosA2aProperties;

	private final A2aServerProperties a2aServerProperties;

	private final NacosA2aRegistryProperties nacosA2aRegistryProperties;

	public NacosA2aOperationService(A2aService a2aService, NacosA2aProperties nacosA2aProperties,
			A2aServerProperties a2aServerProperties, NacosA2aRegistryProperties nacosA2aRegistryProperties) {
		this.a2aService = a2aService;
		this.nacosA2aProperties = nacosA2aProperties;
		this.a2aServerProperties = a2aServerProperties;
		this.nacosA2aRegistryProperties = nacosA2aRegistryProperties;
	}

	public void registerAgent(io.a2a.spec.AgentCard agentCard) {
		AgentCard nacosAgentCard = AgentCardConverterUtil.convertToNacosAgentCard(agentCard);
		try {
			tryReleaseAgentCard(nacosAgentCard);
			registerEndpoint(nacosAgentCard);
		}
		catch (NacosException e) {
			LOGGER.error("Register agent card {} to Nacos failed,", agentCard.name(), e);
			throw new NacosRuntimeException(e.getErrCode(), e.getErrMsg());
		}
	}

	private void tryReleaseAgentCard(AgentCard agentCard) throws NacosException {
		LOGGER.info("Register agent card {} to Nacos namespace {}. ", agentCard.getName(),
				nacosA2aProperties.getNamespace());
		a2aService.releaseAgentCard(agentCard, AiConstants.A2a.A2A_ENDPOINT_TYPE_SERVICE,
				nacosA2aRegistryProperties.isRegisterAsLatest());
		LOGGER.info("Register agent card {} to Nacos namespace {} successfully. ", agentCard.getName(),
				nacosA2aProperties.getNamespace());
	}

	private void registerEndpoint(AgentCard agentCard) throws NacosException {
		AgentEndpoint endpoint = new AgentEndpoint();
		endpoint.setVersion(agentCard.getVersion());
		endpoint.setPath(a2aServerProperties.getMessageUrl());
		endpoint.setTransport(agentCard.getPreferredTransport());
		endpoint.setAddress(a2aServerProperties.getAddress());
		endpoint.setPort(a2aServerProperties.getPort());
		a2aService.registerAgentEndpoint(agentCard.getName(), endpoint);
	}

}
