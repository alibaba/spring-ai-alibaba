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

package com.alibaba.cloud.ai.a2a;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A2a server agent card properties.
 *
 * @author xiweng.yy
 */
@ConfigurationProperties(prefix = A2aServerAgentCardProperties.CONFIG_PREFIX)
public class A2aServerAgentCardProperties extends A2aAgentCardProperties {

	private static final Logger log = LoggerFactory.getLogger(A2aServerProperties.class);

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.a2a.server.card";

}
