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
package com.alibaba.cloud.ai.toolcalling.minio;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * auth: dahua
 */
@Configuration
@ConditionalOnClass(MinioClient.class)
@ConditionalOnProperty(prefix = MinioConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(MinioProperties.class)
public class MinioAutoConfiguration {

	private final MinioProperties minioProperties;

	public MinioAutoConfiguration(MinioProperties minioProperties) {
		this.minioProperties = minioProperties;
	}

	@Bean(name = MinioConstants.TOOL_NAME_UPLOAD)
	@ConditionalOnMissingBean
	@Description("Upload object to minio")
	public MinioUploadObjectService minioUploadObjectService(MinioClient minioClient) {
		return new MinioUploadObjectService(minioClient);
	}

	@Bean(name = MinioConstants.TOOL_NAME_DOWNLOAD)
	@ConditionalOnMissingBean
	@Description("Download object from minio")
	public MinioDownloadObjectService minioDownloadObjectService(MinioClient minioClient) {
		return new MinioDownloadObjectService(minioClient);
	}

	@Bean(name = MinioConstants.TOOL_NAME_DELETE)
	@ConditionalOnMissingBean
	@Description("Delete object from minio")
	public MinioDeleteObjectService minioDeleteObjectService(MinioClient minioClient) {
		return new MinioDeleteObjectService(minioClient);
	}

	@Bean(name = MinioConstants.TOOL_NAME_CHECK_EXISTS)
	@ConditionalOnMissingBean
	@Description("Check object exists from minio")
	public MinioCheckObjectExistsService minioCheckObjectExistsService(MinioClient minioClient) {
		return new MinioCheckObjectExistsService(minioClient);
	}

	@Bean
	@ConditionalOnMissingBean
	public MinioClient minioClient() {
		return MinioClient.builder()
			.endpoint(minioProperties.getEndpoint())
			.credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
			.build();
	}

}
