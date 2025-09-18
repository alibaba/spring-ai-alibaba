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

package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.cloud.ai.agent.nacos.vo.PartnerAgentsVO;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NacosPartenerAgentsInjector {

	private static final Logger logger = LoggerFactory.getLogger(NacosPartenerAgentsInjector.class);

	public static void registry(LlmNode llmNode, ToolNode toolNode, NacosOptions nacosOptions, String agentName) {

		try {
			nacosOptions.getNacosConfigService()
					.addListener("parterner-agents.json", "ai-agent-" + agentName, new AbstractListener() {
						@Override
						public void receiveConfigInfo(String configInfo) {
							PartnerAgentsVO partnerAgentsVO = JSON.parseObject(configInfo, PartnerAgentsVO.class);
							System.out.println(partnerAgentsVO);
						}
					});
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}

	}

	public static PartnerAgentsVO getPartenerVO(NacosOptions nacosOptions, String agentName) {
		try {
			String config = nacosOptions.getNacosConfigService()
					.getConfig("parterner-agents.json", "ai-agent-" + agentName, 3000L);
			return JSON.parseObject(config, PartnerAgentsVO.class);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

}
