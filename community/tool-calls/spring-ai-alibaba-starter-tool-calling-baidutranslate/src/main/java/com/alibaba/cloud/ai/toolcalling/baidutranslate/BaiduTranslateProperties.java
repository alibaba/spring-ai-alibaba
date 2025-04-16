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

package com.alibaba.cloud.ai.toolcalling.baidutranslate;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.alibaba.cloud.ai.toolcalling.baidutranslate.BaiduTranslateProperties.BaiDuTranslatePrefix;
import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX;

/**
 * @author SCMRCORE
 */
@ConfigurationProperties(prefix = BaiDuTranslatePrefix)
public class BaiduTranslateProperties extends CommonToolCallProperties {

	protected static final String BaiDuTranslatePrefix = TOOL_CALLING_CONFIG_PREFIX + ".baidu.translate";

	private static final String TRANSLATE_HOST_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate/";

	public BaiduTranslateProperties() {
		super(TRANSLATE_HOST_URL);
	}

	@PostConstruct
	private void initProperties() {
		this.setPropertiesFromEnv(null, "BAIDU_TRANSLATE_SECRET_KEY", "BAIDU_TRANSLATE_APP_ID", null);
	}

}
