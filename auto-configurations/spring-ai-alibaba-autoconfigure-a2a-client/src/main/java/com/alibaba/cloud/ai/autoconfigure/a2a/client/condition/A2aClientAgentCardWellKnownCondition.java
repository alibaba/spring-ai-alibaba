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

package com.alibaba.cloud.ai.autoconfigure.a2a.client.condition;

import com.alibaba.cloud.ai.a2a.A2aClientAgentCardProperties;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Condition for {@link com.alibaba.cloud.ai.graph.agent.a2a.RemoteAgentCardProvider}.
 *
 * @author xiweng.yy
 */
public class A2aClientAgentCardWellKnownCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String wellKnownUrl = context.getEnvironment()
			.getProperty(A2aClientAgentCardProperties.CONFIG_PREFIX + ".well-known-url", String.class);
		return StringUtils.hasLength(wellKnownUrl) ? ConditionOutcome.match()
				: ConditionOutcome.noMatch(A2aClientAgentCardProperties.CONFIG_PREFIX + ".wellKnownUrl not set.");
	}

}
