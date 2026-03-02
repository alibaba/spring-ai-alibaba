/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multimodal.creative;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Service for creative agent (image/audio generation via tools).
 */
@Service
public class CreativeService {

	@Autowired(required = false)
	@Qualifier("creativeAgent")
	private ReactAgent creativeAgent;

	public AssistantMessage creativeAgentCall(String userRequest) throws GraphRunnerException {
		if (creativeAgent == null) {
			throw new IllegalStateException("Creative agent not available (ImageModel may not be configured)");
		}
		return creativeAgent.call(userRequest);
	}

	public boolean isCreativeAgentAvailable() {
		return creativeAgent != null;
	}
}
