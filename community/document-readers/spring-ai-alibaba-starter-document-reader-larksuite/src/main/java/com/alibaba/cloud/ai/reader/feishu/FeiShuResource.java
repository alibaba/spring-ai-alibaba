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
package com.alibaba.cloud.ai.reader.feishu;

import com.lark.oapi.Client;
import com.lark.oapi.core.enums.BaseUrlEnum;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class FeiShuResource implements Resource {

	public static final String SOURCE = "source";

	public static final String FEISHU_PROPERTIES_PREFIX = "spring.ai.alibaba.plugin.feishu";

	private final String appId;

	private final String appSecret;

	public String getAppId() {
		return appId;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public FeiShuResource(String appId, String appSecret) {
		this.appId = appId;
		this.appSecret = appSecret;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String appId;

		private String appSecret;

		public Builder appId(String appId) {
			this.appId = appId;
			return this;
		}

		public Builder appSecret(String appSecret) {
			this.appSecret = appSecret;
			return this;
		}

		public FeiShuResource build() {
			Assert.notNull(appId, "FeiShu AppId must not be empty");
			Assert.notNull(appSecret, "FeiShu AppSecret must not be empty");
			return new FeiShuResource(appId, appSecret);
		}

	}

	public Client buildDefaultFeiShuClient() {
		Assert.notNull(this.appId, "FeiShu AppId must not be empty");
		Assert.notNull(this.appSecret, "FeiShu AppSecret must not be empty");
		return Client.newBuilder(this.appId, this.appSecret)
			.openBaseUrl(BaseUrlEnum.FeiShu)
			.logReqAtDebug(true)
			.build();
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public URL getURL() throws IOException {
		return null;
	}

	@Override
	public URI getURI() throws IOException {
		return null;
	}

	@Override
	public File getFile() throws IOException {
		return null;
	}

	@Override
	public long contentLength() throws IOException {
		return 0;
	}

	@Override
	public long lastModified() throws IOException {
		return 0;
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return null;
	}

	@Override
	public String getFilename() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

}
