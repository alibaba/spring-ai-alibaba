/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.toolcalling.minio;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * auth: dahua
 */
public class MinioCheckObjectExistsService implements Function<MinioCheckObjectExistsService.Request, Boolean> {

	private static final Logger logger = LoggerFactory.getLogger(MinioCheckObjectExistsService.class);

	private final MinioClient minioClient;

	public MinioCheckObjectExistsService(MinioClient minioClient) {
		this.minioClient = minioClient;
	}

	@Override
	public Boolean apply(MinioCheckObjectExistsService.Request request) {
		try {
			minioClient
				.statObject(StatObjectArgs.builder().bucket(request.bucketName()).object(request.objectName()).build());
		}
		catch (Exception e) {
			logger.debug("Check file exists from minio failed. BucketName: {}, ObjectName: {}. Error: {}",
					request.bucketName(), request.objectName(), e.getMessage(), e);
			return false;
		}
		return true;
	}

	@JsonClassDescription("check object exists from minio api")
	public record Request(@JsonPropertyDescription("Minio bucketName") String bucketName,
			@JsonPropertyDescription("Object name to check") String objectName) {
	}

}
