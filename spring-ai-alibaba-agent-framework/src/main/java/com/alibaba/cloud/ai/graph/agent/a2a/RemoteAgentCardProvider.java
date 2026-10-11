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
package com.alibaba.cloud.ai.graph.agent.a2a;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.spec.AgentCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAgentCardProvider implements AgentCardProvider {

	private static final Logger logger = LoggerFactory.getLogger(RemoteAgentCardProvider.class);

	private final String url;

	private AgentCard agentCard;

	private RemoteAgentCardProvider(String url) {
		this.url = url;
	}

	public static AgentCardProvider newProvider(String url) {
		return new RemoteAgentCardProvider(url);
	}

	@Override
	public AgentCardWrapper getAgentCard() {
		if (null == agentCard) {
			agentCard = getAgentCardFromUrl();
		}
		return new AgentCardWrapper(agentCard);
	}

	private AgentCard getAgentCardFromUrl() {
		try {
			AgentCard publicAgentCard = A2A.getAgentCard(this.url);
			if (publicAgentCard.capabilities().extendedAgentCard()) {
				logger.info("Agent advertises an authenticated extended card. Using the public card until authenticated client credentials are configured.");
			}
			else {
				logger.info("Public card does not indicate support for an extended card. Using public card.");
			}
			return publicAgentCard;
		}
		catch (Exception e) {
			logger.error("Error building agent card", e);
			throw new RuntimeException(e);
		}
	}

}
