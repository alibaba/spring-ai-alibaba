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

import com.alibaba.cloud.ai.a2a.registry.nacos.properties.NacosA2aProperties;
import com.alibaba.cloud.ai.a2a.registry.nacos.utils.AgentCardConverterUtil;
import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import com.alibaba.nacos.maintainer.client.ai.A2aMaintainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nacos operation service for A2A.
 *
 * @author xiweng.yy
 */
public class NacosA2aOperationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NacosA2aOperationService.class);

	private final A2aMaintainerService a2aMaintainerService;

	private final NacosA2aProperties nacosA2aProperties;

	public NacosA2aOperationService(A2aMaintainerService a2aMaintainerService, NacosA2aProperties nacosA2aProperties) {
		this.a2aMaintainerService = a2aMaintainerService;
		this.nacosA2aProperties = nacosA2aProperties;
	}

	public void registerAgent(io.a2a.spec.AgentCard agentCard) {
		AgentCard nacosAgentCard = AgentCardConverterUtil.convertToNacosAgentCard(agentCard);
		try {
			LOGGER.info("Register agent card {} to Nacos namespace {}. ", agentCard.name(),
					nacosA2aProperties.getNamespace());
			a2aMaintainerService.registerAgent(nacosAgentCard, nacosA2aProperties.getNamespace());
			LOGGER.info("Register agent card {} to Nacos namespace {} successfully. ", agentCard.name(),
					nacosA2aProperties.getNamespace());
		}
		catch (NacosException e) {
			LOGGER.error("Register agent card {} to Nacos failed,", agentCard.name(), e);
			throw new NacosRuntimeException(e.getErrCode(), e.getErrMsg());
		}
	}

}
