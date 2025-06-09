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

/**
 * @author vlsmb
 */
public final class AliTranslateConstants {

	public static final String CONFIG_PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".alitranslate";

	public static final String ACCESS_KEY_SECRET_ENV = "ALITRANSLATE_ACCESS_KEY_SECRET";

	public static final String ACCESS_KEY_ID_ENV = "ALITRANSLATE_ACCESS_KEY_ID";

	public static final String TOOL_NAME = "aliTranslateService";

	/**
	 * version of the api
	 */
	public static final String SCENE = "general";

	/**
	 * FormatType text or html
	 */
	public static final String FORM_TYPE = "text";

	/**
	 * offline doc:
	 * https://help.aliyun.com/zh/machine-translation/support/supported-languages-and-codes?spm=api-workbench.api_explorer.0.0.37a94eecsclZw9
	 */
	public static final String LANGUAGE_CODE_ZH = "zh";

	public static final String LANGUAGE_CODE_EN = "en";

}
