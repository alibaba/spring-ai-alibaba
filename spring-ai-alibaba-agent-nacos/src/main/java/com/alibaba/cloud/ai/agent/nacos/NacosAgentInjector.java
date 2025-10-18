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

import com.alibaba.cloud.ai.agent.nacos.vo.AgentVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;

public class NacosAgentInjector {

	/**
	 * load prompt by agent id.
	 */
	public static AgentVO loadAgentVO(NacosOptions nacosOptions) {
		try {
			String dataIdT = (nacosOptions.isAgentBaseEncrypted() ? "cipher-kms-aes-256-" : "") + "agent-base.json";
			String config = nacosOptions.getNacosConfigService()
					.getConfig(dataIdT, "ai-agent-" + nacosOptions.getAgentName(),
							3000L);
			return JSON.parseObject(config, AgentVO.class);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

}
