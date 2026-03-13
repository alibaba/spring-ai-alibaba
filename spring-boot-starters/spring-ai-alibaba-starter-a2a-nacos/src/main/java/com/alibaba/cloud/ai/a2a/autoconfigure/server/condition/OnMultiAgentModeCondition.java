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

package com.alibaba.cloud.ai.a2a.autoconfigure.server.condition;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that checks if multi-agent mode is enabled.
 * <p>
 * Multi-agent mode is enabled when any property starting with
 * "spring.ai.alibaba.a2a.server.agents." is present in the environment.
 *
 * @author xiweng.yy
 */
public class OnMultiAgentModeCondition extends SpringBootCondition {

	private static final String AGENTS_PREFIX = "spring.ai.alibaba.a2a.server.agents.";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Environment environment = context.getEnvironment();
		boolean hasAgentsProperties = hasAgentsProperties(environment);

		ConditionMessage.Builder message = ConditionMessage
			.forCondition("Multi-Agent Mode");

		if (hasAgentsProperties) {
			return ConditionOutcome.match(message.foundExactly("agents configuration"));
		}
		return ConditionOutcome.noMatch(message.didNotFind("agents configuration").atAll());
	}

	private boolean hasAgentsProperties(Environment environment) {
		if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
			for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
				if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
					for (String propertyName : enumerablePropertySource.getPropertyNames()) {
						if (propertyName.startsWith(AGENTS_PREFIX)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

}
