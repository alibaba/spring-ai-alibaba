package com.alibaba.cloud.ai.toolcalling.minio;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * 2025/7/23 auth: dahua desc:
 */
public class MinioDeleteObjectService implements Function<MinioDeleteObjectService.Request, Boolean> {

	private static final Logger logger = LoggerFactory.getLogger(MinioDeleteObjectService.class);

	private final MinioClient minioClient;

	public MinioDeleteObjectService(MinioClient minioClient) {
		this.minioClient = minioClient;
	}

	@Override
	public Boolean apply(Request request) {
		try {
			minioClient.removeObject(
					RemoveObjectArgs.builder().bucket(request.bucketName()).object(request.objectName()).build());
		}
		catch (Exception e) {
			logger.error("Delete file from minio exception {}", e);
			return false;
		}
		return true;
	}

	@JsonClassDescription("delete object from minio api")
	public record Request(@JsonPropertyDescription("Minio bucketName") String bucketName,
			@JsonPropertyDescription("Upload objectName") String objectName) {
	}

}
