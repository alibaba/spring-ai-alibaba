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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yunlong
 * @author Allen Hu
 */
@ConfigurationProperties(prefix = AliTranslateProperties.PREFIX)
public class AliTranslateProperties extends CommonToolCallProperties {

	public static final String PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".alitranslate";

	private String region;

	public AliTranslateProperties() {
		this.setPropertiesFromEnv("ALITRANSLATE_ACCESS_KEY_ID", "ALITRANSLATE_ACCESS_KEY_SECRET", null, null);
	}

	public String getAccessKeyId() {
		return getApiKey();
	}

	public String getAccessKeySecret() {
		return getSecretKey();
	}

	/**
	 * @param accessKeyId AccessKeyId
	 * @deprecated use {@link #setApiKey(String)} instead
	 */
	@Deprecated
	public void setAccessKeyId(String accessKeyId) {
		this.setApiKey(accessKeyId);
	}

	/**
	 * @param accessKeySecret AccessKeySecret
	 * @deprecated use {@link #setSecretKey(String)} instead
	 */
	@Deprecated
	public void setAccessKeySecret(String accessKeySecret) {
		this.setSecretKey(accessKeySecret);
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

}
