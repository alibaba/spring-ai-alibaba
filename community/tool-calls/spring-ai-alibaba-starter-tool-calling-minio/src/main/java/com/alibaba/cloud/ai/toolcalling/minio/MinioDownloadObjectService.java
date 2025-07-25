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
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * auth: dahua
 */
public class MinioDownloadObjectService implements Function<MinioDownloadObjectService.Request, Boolean> {

	private static final Logger logger = LoggerFactory.getLogger(MinioDownloadObjectService.class);

	private final MinioClient minioClient;

	public MinioDownloadObjectService(MinioClient minioClient) {
		this.minioClient = minioClient;
	}

	@Override
	public Boolean apply(MinioDownloadObjectService.Request request) {
		try {
			minioClient.downloadObject(DownloadObjectArgs.builder()
				.bucket(request.bucketName())
				.object(request.objectName())
				.filename(request.downloadPath())
				.build());
		}
		catch (Exception e) {
			logger.error("Download file from minio failed. BucketName: {}, ObjectName: {}, DownloadPath: {}. Error: {}",
					request.bucketName(), request.objectName(), request.downloadPath(), e.getMessage(), e);
			return false;
		}
		return true;
	}

	@JsonClassDescription("download object from minio api")
	public record Request(@JsonPropertyDescription("Minio bucketName") String bucketName,
			@JsonPropertyDescription("Object name to download") String objectName,
			@JsonPropertyDescription("Path to save the downloaded object") String downloadPath) {
	}

}
