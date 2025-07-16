/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.util.Multiagent;

import com.alibaba.cloud.ai.example.deepresearch.config.SmartAgentProperties;
import com.alibaba.cloud.ai.example.deepresearch.dispatcher.SmartAgentDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.service.mutiagent.QuestionClassifierService;
import com.alibaba.cloud.ai.example.deepresearch.service.mutiagent.SearchPlatformSelectionService;
import com.alibaba.cloud.ai.example.deepresearch.service.mutiagent.SmartAgentSelectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent集成工具类 统一处理智能Agent的集成逻辑，提供智能Agent选择、错误处理等功能
 *
 * @author Makoto
 * @since 2025/01/28
 */
public class AgentIntegrationUtil {

	private static final Logger logger = LoggerFactory.getLogger(AgentIntegrationUtil.class);

	/**
	 * 创建智能Agent选择辅助器
	 */
	public static SmartAgentSelectionHelper createSelectionHelper(SmartAgentProperties smartAgentProperties,
			SmartAgentDispatcher smartAgentDispatcher, QuestionClassifierService questionClassifierService,
			SearchPlatformSelectionService searchPlatformSelectionService) {
		return new SmartAgentSelectionHelper(smartAgentProperties, smartAgentDispatcher, questionClassifierService,
				searchPlatformSelectionService);
	}

	public static boolean isSmartAgentAvailable(SmartAgentProperties smartAgentProperties, Object... services) {
		if (smartAgentProperties == null || !smartAgentProperties.isEnabled()) {
			logger.debug("智能Agent功能未开启");
			return false;
		}

		for (Object service : services) {
			if (service == null) {
				logger.warn("智能Agent必要服务不可用，回退到原有逻辑");
				return false;
			}
		}

		return true;
	}

}
