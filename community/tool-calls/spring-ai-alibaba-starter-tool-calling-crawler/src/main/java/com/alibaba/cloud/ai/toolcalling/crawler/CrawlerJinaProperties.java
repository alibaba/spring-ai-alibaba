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
package com.alibaba.cloud.ai.toolcalling.crawler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;

/**
 * @author yuluo
 */
@EnableConfigurationProperties
@ConfigurationProperties(prefix = CrawlerJinaProperties.JINA_PROPERTIES_PREFIX)
public class CrawlerJinaProperties {

	public static final String JINA_PROPERTIES_PREFIX = CrawlerConstants.CONFIG_PREFIX + "jina";

	private String token;

	private Boolean enabled;

	private MediaType accept = MediaType.asMediaType(MediaType.APPLICATION_JSON);

	private String targetSelector;

	private String waitForSelector;

	private String removeSelector;

	private String retainImages;

	private Boolean withLinksSummary;

	private Boolean withImagesSummary;

	private String setCookie;

	private Boolean withGeneratedAlt;

	private String proxyUrl;

	private Boolean noCache;

	private String locale;

	private Boolean withIframe;

	private Boolean withShadowDom;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public MediaType getAccept() {
		return accept;
	}

	public void setAccept(MediaType accept) {
		this.accept = accept;
	}

	public String getTargetSelector() {
		return targetSelector;
	}

	public void setTargetSelector(String targetSelector) {
		this.targetSelector = targetSelector;
	}

	public String getWaitForSelector() {
		return waitForSelector;
	}

	public void setWaitForSelector(String waitForSelector) {
		this.waitForSelector = waitForSelector;
	}

	public String getRemoveSelector() {
		return removeSelector;
	}

	public void setRemoveSelector(String removeSelector) {
		this.removeSelector = removeSelector;
	}

	public String getRetainImages() {
		return retainImages;
	}

	public void setRetainImages(String retainImages) {
		this.retainImages = retainImages;
	}

	public Boolean getWithLinksSummary() {
		return withLinksSummary;
	}

	public void setWithLinksSummary(Boolean withLinksSummary) {
		this.withLinksSummary = withLinksSummary;
	}

	public Boolean getWithImagesSummary() {
		return withImagesSummary;
	}

	public void setWithImagesSummary(Boolean withImagesSummary) {
		this.withImagesSummary = withImagesSummary;
	}

	public String getSetCookie() {
		return setCookie;
	}

	public void setSetCookie(String setCookie) {
		this.setCookie = setCookie;
	}

	public Boolean getWithGeneratedAlt() {
		return withGeneratedAlt;
	}

	public void setWithGeneratedAlt(Boolean withGeneratedAlt) {
		this.withGeneratedAlt = withGeneratedAlt;
	}

	public String getProxyUrl() {
		return proxyUrl;
	}

	public void setProxyUrl(String proxyUrl) {
		this.proxyUrl = proxyUrl;
	}

	public Boolean getNoCache() {
		return noCache;
	}

	public void setNoCache(Boolean noCache) {
		this.noCache = noCache;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public Boolean getWithIframe() {
		return withIframe;
	}

	public void setWithIframe(Boolean withIframe) {
		this.withIframe = withIframe;
	}

	public Boolean getWithShadowDom() {
		return withShadowDom;
	}

	public void setWithShadowDom(Boolean withShadowDom) {
		this.withShadowDom = withShadowDom;
	}

	@Override
	public String toString() {
		return "CrawlerJinaProperties{" + "token='" + "**********" + '\'' + ", enabled=" + enabled
				+ ", targetSelector='" + targetSelector + '\'' + ", waitForSelector='" + waitForSelector + '\''
				+ ", removeSelector='" + removeSelector + '\'' + ", retainImages='" + retainImages + '\''
				+ ", withLinksSummary=" + withLinksSummary + ", withImagesSummary=" + withImagesSummary
				+ ", setCookie='" + setCookie + '\'' + ", withGeneratedAlt=" + withGeneratedAlt + ", proxyUrl='"
				+ proxyUrl + '\'' + ", noCache=" + noCache + ", locale='" + locale + '\'' + ", withIframe=" + withIframe
				+ ", withShadowDom=" + withShadowDom + '}';
	}

}
