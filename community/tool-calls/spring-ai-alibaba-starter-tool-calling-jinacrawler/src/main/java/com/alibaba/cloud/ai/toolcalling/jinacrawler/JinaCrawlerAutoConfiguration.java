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
package com.alibaba.cloud.ai.toolcalling.jinacrawler;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.function.Consumer;

@Configuration
@EnableConfigurationProperties({ JinaCrawlerProperties.class })
@ConditionalOnProperty(prefix = JinaCrawlerConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class JinaCrawlerAutoConfiguration {

	@Bean(name = JinaCrawlerConstants.TOOL_NAME)
	@ConditionalOnMissingBean
	@Description("Jina Reader Service Plugin.")
	public JinaCrawlerService jinaCrawler(JsonParseTool jsonParseTool, JinaCrawlerProperties jinaProperties) {
		Consumer<HttpHeaders> consumer = (httpHeaders) -> {
			httpHeaders.add("Accept", jinaProperties.getAccept().toString());
			httpHeaders.add("Content-Type", "application/json");
			httpHeaders.add("Authorization", "Bearer " + jinaProperties.getApiKey());
			if (StringUtils.hasText(jinaProperties.getLocale())) {
				httpHeaders.add(JinaCrawlerConstants.X_LOCALE, jinaProperties.getLocale());
			}
			if (Objects.nonNull(jinaProperties.getNoCache())) {
				httpHeaders.add(JinaCrawlerConstants.X_NO_CACHE, jinaProperties.getNoCache().toString());
			}
			if (StringUtils.hasText(jinaProperties.getProxyUrl())) {
				httpHeaders.add(JinaCrawlerConstants.X_PROXY_URL, jinaProperties.getProxyUrl());
			}
			if (StringUtils.hasText(jinaProperties.getRemoveSelector())) {
				httpHeaders.add(JinaCrawlerConstants.X_REMOVE_SELECTOR, jinaProperties.getRemoveSelector());
			}
			if (StringUtils.hasText(jinaProperties.getRetainImages())) {
				httpHeaders.add(JinaCrawlerConstants.X_RETAIN_IMAGES, jinaProperties.getRetainImages());
			}
			if (StringUtils.hasText(jinaProperties.getSetCookie())) {
				httpHeaders.add(JinaCrawlerConstants.X_SET_COOKIE, jinaProperties.getSetCookie());
			}
			if (Objects.nonNull(jinaProperties.getWithGeneratedAlt())) {
				httpHeaders.add(JinaCrawlerConstants.X_WITH_GENERATED_ALT,
						jinaProperties.getWithGeneratedAlt().toString());
			}
			if (Objects.nonNull(jinaProperties.getWithIframe())) {
				httpHeaders.add(JinaCrawlerConstants.X_WITH_IFRAME, jinaProperties.getWithIframe().toString());
			}
			if (Objects.nonNull(jinaProperties.getWithShadowDom())) {
				httpHeaders.add(JinaCrawlerConstants.X_WITH_SHADOW_DOM, jinaProperties.getWithShadowDom().toString());
			}
			if (Objects.nonNull(jinaProperties.getWithImagesSummary())) {
				httpHeaders.add(JinaCrawlerConstants.X_WITH_IMAGES_SUMMARY,
						jinaProperties.getWithImagesSummary().toString());
			}
			if (Objects.nonNull(jinaProperties.getWithLinksSummary())) {
				httpHeaders.add(JinaCrawlerConstants.X_WITH_LINKS_SUMMARY,
						jinaProperties.getWithLinksSummary().toString());
			}
			if (StringUtils.hasText(jinaProperties.getTargetSelector())) {
				httpHeaders.add(JinaCrawlerConstants.X_TARGET_SELECTOR, jinaProperties.getTargetSelector());
			}
			if (StringUtils.hasText(jinaProperties.getWaitForSelector())) {
				httpHeaders.add(JinaCrawlerConstants.X_WAIT_FOR_SELECTOR, jinaProperties.getWaitForSelector());
			}
		};
		return new JinaCrawlerService(jsonParseTool,
				WebClientTool.builder(jsonParseTool, jinaProperties).httpHeadersConsumer(consumer).build(),
				jinaProperties);
	}

}
