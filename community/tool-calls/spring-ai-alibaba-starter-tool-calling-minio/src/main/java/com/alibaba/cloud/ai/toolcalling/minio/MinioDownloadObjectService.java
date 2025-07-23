package com.alibaba.cloud.ai.toolcalling.minio;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * 2025/7/23 22:15 auth: dahua desc:
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
			logger.error("Download file from minio exception {}", e);
			return false;
		}
		return true;
	}

	@JsonClassDescription("download object to minio api")
	public record Request(@JsonPropertyDescription("Minio bucketName") String bucketName,
			@JsonPropertyDescription("Upload objectName") String objectName,
			@JsonPropertyDescription("Upload downloadPath") String downloadPath) {
	}

}
