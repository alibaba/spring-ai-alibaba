/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.autoconfigure.prompt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@ConfigurationProperties(NacosPromptTmplProperties.TEMPLATE_PREFIX)
public class NacosPromptTmplProperties {

	public final static String TEMPLATE_PREFIX = "spring.ai.nacos.prompt.template";

	/**
	 * Default not enabled.
	 */
	private boolean enabled = false;

	public boolean isEnabled() {

		return enabled;
	}

	public void setEnabled(boolean enabled) {

		this.enabled = enabled;
	}

}
