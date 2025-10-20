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

package com.alibaba.cloud.ai.a2a.registry.nacos.register;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nacos registry properties for A2A.
 *
 * @author xiweng.yy
 */
@ConfigurationProperties(prefix = NacosA2aRegistryProperties.PREFIX)
public class NacosA2aRegistryProperties {

	public static final String PREFIX = "spring.ai.alibaba.a2a.nacos.registry";

	private boolean registerAsLatest = true;

	public boolean isRegisterAsLatest() {
		return registerAsLatest;
	}

	public void setRegisterAsLatest(boolean registerAsLatest) {
		this.registerAsLatest = registerAsLatest;
	}

}
