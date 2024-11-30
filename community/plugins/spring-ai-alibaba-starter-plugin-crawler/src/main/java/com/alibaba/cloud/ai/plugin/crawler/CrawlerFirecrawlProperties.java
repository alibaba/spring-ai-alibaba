/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.plugin.crawler;

import java.util.List;

import com.alibaba.cloud.ai.plugin.crawler.constant.CrawlerConstants;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@ConfigurationProperties(prefix = CrawlerFirecrawlProperties.FIRECRAWL_PROPERTIES_PREFIX)
public class CrawlerFirecrawlProperties {

	public static final String FIRECRAWL_PROPERTIES_PREFIX = CrawlerConstants.CONFIG_PREFIX + "jina";

	private String token;

	private Boolean enabled;

	private List<String> excludePaths;

	private ScrapeOptions scrapeOptions;

	private String url;

	private List<String> includePaths;

	private Integer maxDepth;

	private Boolean ignoreSitemap;

	private Integer limit;

	private Boolean allowBackwardLinks;

	private Boolean allowExternalLinks;

	private String webhook;

	// Getters and Setters
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public List<String> getExcludePaths() {
		return excludePaths;
	}

	public void setExcludePaths(List<String> excludePaths) {
		this.excludePaths = excludePaths;
	}

	public ScrapeOptions getScrapeOptions() {
		return scrapeOptions;
	}

	public void setScrapeOptions(ScrapeOptions scrapeOptions) {
		this.scrapeOptions = scrapeOptions;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getIncludePaths() {
		return includePaths;
	}

	public void setIncludePaths(List<String> includePaths) {
		this.includePaths = includePaths;
	}

	public Integer getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(Integer maxDepth) {
		this.maxDepth = maxDepth;
	}

	public Boolean getIgnoreSitemap() {
		return ignoreSitemap;
	}

	public void setIgnoreSitemap(Boolean ignoreSitemap) {
		this.ignoreSitemap = ignoreSitemap;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Boolean getAllowBackwardLinks() {
		return allowBackwardLinks;
	}

	public void setAllowBackwardLinks(Boolean allowBackwardLinks) {
		this.allowBackwardLinks = allowBackwardLinks;
	}

	public Boolean getAllowExternalLinks() {
		return allowExternalLinks;
	}

	public void setAllowExternalLinks(Boolean allowExternalLinks) {
		this.allowExternalLinks = allowExternalLinks;
	}

	public String getWebhook() {
		return webhook;
	}

	public void setWebhook(String webhook) {
		this.webhook = webhook;
	}

	// Inner class for ScrapeOptions
	public static class ScrapeOptions {

		private Boolean mobile;

		private List<String> formats;

		private List<String> includeTags;

		private Integer waitFor;

		private Boolean removeBase64Images;

		private Boolean onlyMainContent;

		private List<String> excludeTags;

		// Getters and Setters
		public Boolean getMobile() {
			return mobile;
		}

		public void setMobile(Boolean mobile) {
			this.mobile = mobile;
		}

		public List<String> getFormats() {
			return formats;
		}

		public void setFormats(List<String> formats) {
			this.formats = formats;
		}

		public List<String> getIncludeTags() {
			return includeTags;
		}

		public void setIncludeTags(List<String> includeTags) {
			this.includeTags = includeTags;
		}

		public Integer getWaitFor() {
			return waitFor;
		}

		public void setWaitFor(Integer waitFor) {
			this.waitFor = waitFor;
		}

		public Boolean getRemoveBase64Images() {
			return removeBase64Images;
		}

		public void setRemoveBase64Images(Boolean removeBase64Images) {
			this.removeBase64Images = removeBase64Images;
		}

		public Boolean getOnlyMainContent() {
			return onlyMainContent;
		}

		public void setOnlyMainContent(Boolean onlyMainContent) {
			this.onlyMainContent = onlyMainContent;
		}

		public List<String> getExcludeTags() {
			return excludeTags;
		}

		public void setExcludeTags(List<String> excludeTags) {
			this.excludeTags = excludeTags;
		}

		@Override
		public String toString() {

			return "ScrapeOptions{" + "mobile=" + mobile
					+ ", formats=" + formats
					+ ", includeTags=" + includeTags
					+ ", waitFor=" + waitFor
					+ ", removeBase64Images=" + removeBase64Images
					+ ", onlyMainContent=" + onlyMainContent
					+ ", excludeTags=" + excludeTags
					+ '}';
		}
	}

	@Override
	public String toString() {

		return "CrawlerFirecrawlProperties{" + "token='" + "*****" + '\''
				+ ", enabled=" + enabled
				+ ", excludePaths=" + excludePaths
				+ ", scrapeOptions=" + scrapeOptions
				+ ", url='" + url + '\''
				+ ", includePaths=" + includePaths
				+ ", maxDepth=" + maxDepth
				+ ", ignoreSitemap=" + ignoreSitemap
				+ ", limit=" + limit
				+ ", allowBackwardLinks=" + allowBackwardLinks
				+ ", allowExternalLinks=" + allowExternalLinks
				+ ", webhook='" + webhook + '\''
				+ '}';
	}
}
