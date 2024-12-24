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
package com.alibaba.cloud.ai.reader.feishu.config;

import com.lark.oapi.Client;
import com.lark.oapi.core.enums.BaseUrlEnum;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.util.Assert;

/**
 * @author wblu214
 * @author <a href="mailto:2897718178@qq.com">wblu214</a>
 */
@Configuration
@EnableConfigurationProperties(FeiShuProperties.class)
public class FeiShuPluginConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Build FeiShu Client in Spring AI Alibaba") // description
	@ConditionalOnProperty(prefix = FeiShuProperties.FEISHU_PROPERTIES_PREFIX, name = "enabled", havingValue = "true")
	public Client buildDefaultFeiShuClient(FeiShuProperties feiShuProperties) {
		Assert.notNull(feiShuProperties.getAppId(), "FeiShu AppId must not be empty");
		Assert.notNull(feiShuProperties.getAppSecret(), "FeiShu AppSecret must not be empty");
		return Client.newBuilder(feiShuProperties.getAppId(), feiShuProperties.getAppSecret())
			.openBaseUrl(BaseUrlEnum.FeiShu)
			.logReqAtDebug(true)
			.build();
	}
	// 商店应用自行扩展

}
