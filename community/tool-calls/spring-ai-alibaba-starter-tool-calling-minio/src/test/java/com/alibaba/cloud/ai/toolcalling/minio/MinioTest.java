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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author dahua
 * @since 2025/07/14
 */
@SpringBootTest(classes = { MinioAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("minio tool call Test")
class MinioTest {

	private static final Logger logger = LoggerFactory.getLogger(MinioTest.class);

	@Autowired
	private MinioUploadObjectService minioUploadObjectService;

	@Autowired
	private MinioDownloadObjectService minioDownloadObjectService;

	@Autowired
	private MinioDeleteObjectService minioDeleteObjectService;

	@Autowired
	private MinioCheckObjectExistsService minioCheckObjectExistsService;

	@Test
	@DisplayName("Tool-Calling Test Upload Object")
	@EnabledIfEnvironmentVariable(named = MinioConstants.ENDPOINT, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MinioConstants.ACCESS_KEY, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MinioConstants.SECRET_KEY, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testMinioObjectUpload() {
		Boolean apply = minioUploadObjectService
			.apply(new MinioUploadObjectService.Request("bucket-test", "minio", "minio.txt"));
		logger.info("upload result: {}", apply);
	}

	@Test
	@DisplayName("Tool-Calling Test Download Object")
	@EnabledIfEnvironmentVariable(named = MinioConstants.ENDPOINT, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MinioConstants.ACCESS_KEY, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MinioConstants.SECRET_KEY, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testMinioObjectDownload() {
		Boolean apply = minioDownloadObjectService
			.apply(new MinioDownloadObjectService.Request("bucket-test", "minio", "minio-copy.txt"));
		logger.info("download result: {}", apply);
	}

	@Test
	@DisplayName("Tool-Calling Test Delete Object")
	@EnabledIfEnvironmentVariable(named = MinioConstants.ENDPOINT, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MinioConstants.ACCESS_KEY, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MinioConstants.SECRET_KEY, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testMinioObjectDelete() {
		Boolean apply = minioDeleteObjectService.apply(new MinioDeleteObjectService.Request("bucket-test", "minio"));
		logger.info("delete result: {}", apply);
	}

	@Test
	@DisplayName("Tool-Calling Test Check Object Exists")
	@EnabledIfEnvironmentVariable(named = MinioConstants.ENDPOINT, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MinioConstants.ACCESS_KEY, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MinioConstants.SECRET_KEY, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testMinioObjectCheckExists() {
		Boolean apply = minioCheckObjectExistsService
			.apply(new MinioCheckObjectExistsService.Request("bucket-test", "minio"));
		logger.info("check result: {}", apply);
	}

}
