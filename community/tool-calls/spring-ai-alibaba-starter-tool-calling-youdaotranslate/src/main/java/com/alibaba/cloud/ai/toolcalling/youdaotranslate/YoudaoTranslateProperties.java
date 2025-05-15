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
package com.alibaba.cloud.ai.toolcalling.youdaotranslate;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Yeaury
 */
@ConfigurationProperties(prefix = YoudaoTranslateProperties.PREFIX)
public class YoudaoTranslateProperties extends CommonToolCallProperties {

	public static final String PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".youdaotranslate";

	public static final String YOUDAO_TRANSLATE_BASE_URL = "https://openapi.youdao.com";

	/**
	 * @param appKey youdao AppId
	 * @deprecated use {@link #setAppId(String)} instead
	 */
	@Deprecated
	public void setAppKey(String appKey) {
		this.setAppId(appKey);
	}

	/**
	 * @param appSecret youdao AppSecret
	 * @deprecated use {@link #setSecretKey(String)} instead
	 */
	@Deprecated
	public void setAppSecret(String appSecret) {
		this.setSecretKey(appSecret);
	}

	public String getAppKey() {
		return getAppId();
	}

	public String getAppSecret() {
		return getSecretKey();
	}

	public YoudaoTranslateProperties() {
		super(YOUDAO_TRANSLATE_BASE_URL);
		setPropertiesFromEnv("YOUDAO_APP_ID", "YOUDAO_APP_SECRET", null, null);
	}

}
