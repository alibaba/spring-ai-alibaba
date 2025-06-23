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
package com.alibaba.cloud.ai.toolcalling.kuaidi100;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <a href="https://api.kuaidi100.com/manager/v2/myinfo/enterprise">Obtain the
 * authorization key and customer value for kuaidi100.com</a>, They correspond to the
 * configuration items apiKey and appId respectively.<br>
 *
 * You can also set it through environment variables:<br>
 * KUAIDI100_KEY<br>
 * KUAIDI100_CUSTOMER<br>
 *
 * @author XiaoYunTao
 * @since 2024/12/25
 */
@ConfigurationProperties(prefix = Kuaidi100Constants.CONFIG_PREFIX)
public class Kuaidi100Properties extends CommonToolCallProperties {

	public static final String QUERY_BASE_URL = "https://www.kuaidi100.com/";

	public Kuaidi100Properties() {
		super(QUERY_BASE_URL);
		this.setPropertiesFromEnv(Kuaidi100Constants.API_KEY_ENV, null, Kuaidi100Constants.APP_ID_ENV, null);
	}

}
