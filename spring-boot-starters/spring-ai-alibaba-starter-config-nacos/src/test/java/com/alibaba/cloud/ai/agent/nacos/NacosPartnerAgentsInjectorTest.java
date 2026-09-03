/*
 * Copyright 2024-2026 the original author or authors.
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
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.client.config.NacosConfigService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NacosPartnerAgentsInjectorTest {

	@Test
	void registryShouldListenToPartnerAgentsDataId() throws Exception {
		NacosConfigService configService = mock(NacosConfigService.class);
		NacosOptions nacosOptions = mock(NacosOptions.class);
		when(nacosOptions.getNacosConfigService()).thenReturn(configService);

		NacosPartnerAgentsInjector.registry(null, null, nacosOptions, "demo");

		verify(configService).addListener(eq("partner-agents.json"), eq("ai-agent-demo"), any(Listener.class));
	}

	@Test
	void getPartnerVOShouldReadPartnerAgentsDataId() throws Exception {
		NacosConfigService configService = mock(NacosConfigService.class);
		NacosOptions nacosOptions = mock(NacosOptions.class);
		when(nacosOptions.getNacosConfigService()).thenReturn(configService);
		when(configService.getConfig("partner-agents.json", "ai-agent-demo", 3000L))
			.thenReturn("{\"agents\":[{\"agentName\":\"partner\"}]}");

		PartnerAgentsVO partnerAgentsVO = NacosPartnerAgentsInjector.getPartnerVO(nacosOptions, "demo");

		assertThat(partnerAgentsVO.getAgents()).hasSize(1);
		assertThat(partnerAgentsVO.getAgents().get(0).getAgentName()).isEqualTo("partner");
	}

}
