/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.messagechannel.dispatcher;

import java.util.Map;

import com.alibaba.cloud.ai.graph.agent.Agent;

import org.springframework.beans.factory.BeanFactory;

/**
 * Resolves the bean name configured under
 * {@code spring.ai.alibaba.message-channel.channels.<name>.bind-agent} to a live
 * {@link Agent} instance.
 *
 * <p>The mapping is captured at startup but Agent lookup happens lazily on each
 * inbound message so that downstream agent re-creation (hot reload, scope=prototype)
 * is honored.</p>
 */
public class AgentBindingRegistry {

	private final BeanFactory beanFactory;

	private final Map<String, String> bindings;

	public AgentBindingRegistry(BeanFactory beanFactory, Map<String, String> bindings) {
		this.beanFactory = beanFactory;
		this.bindings = Map.copyOf(bindings);
	}

	public Agent resolve(String channelName) {
		String beanName = bindings.get(channelName);
		if (beanName == null || beanName.isBlank()) {
			throw new IllegalStateException("No agent bound to channel '" + channelName
					+ "'. Set spring.ai.alibaba.message-channel.channels." + channelName + ".bind-agent");
		}
		return beanFactory.getBean(beanName, Agent.class);
	}

}
