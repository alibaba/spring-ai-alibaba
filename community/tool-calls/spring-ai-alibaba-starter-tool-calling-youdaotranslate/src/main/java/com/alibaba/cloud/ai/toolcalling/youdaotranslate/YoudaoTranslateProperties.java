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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Yeaury
 */
@ConfigurationProperties(prefix = YoudaoTranslateProperties.PREFIX)
public class YoudaoTranslateProperties extends CommonToolCallProperties {

	public static final String PREFIX = "spring.ai.alibaba.toolcalling.youdaotranslate";

	public static final String YOUDAO_TRANSLATE_BASE_URL = "https://openapi.youdao.com";

	public String getAppKey() {
		return getApiKey();
	}

	public String getAppSecret() {
		return getSecretKey();
	}

	public YoudaoTranslateProperties() {
		super(YOUDAO_TRANSLATE_BASE_URL);
		setPropertiesFromEnv("YOUDAO_APP_KEY", "YOUDAO_APP_SECRET", null, null);
	}

}
