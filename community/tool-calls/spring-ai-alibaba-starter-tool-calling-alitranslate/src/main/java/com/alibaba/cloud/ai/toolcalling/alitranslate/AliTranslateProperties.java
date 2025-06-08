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
package com.alibaba.cloud.ai.toolcalling.alitranslate;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Aliyun Translation Service Configuration Attributes Class
 *
 * @author Allen Hu
 */
@ConfigurationProperties(prefix = AliTranslateConstants.CONFIG_PREFIX)
public class AliTranslateProperties extends CommonToolCallProperties {

	public AliTranslateProperties() {
		this.setPropertiesFromEnv(null, AliTranslateConstants.ACCESS_KEY_SECRET_ENV, null, null);
		String accessKeyIdEnv = AliTranslateConstants.ACCESS_KEY_ID_ENV;
		if (!StringUtils.hasText(this.getAccessKeyId())) {
			this.setAccessKeyId(System.getenv(accessKeyIdEnv));
		}
	}

}
