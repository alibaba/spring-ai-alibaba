/*
* Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.base.manager;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.*;
import com.google.common.collect.ImmutableList;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.enums.UploadType;
import com.alibaba.cloud.ai.studio.runtime.domain.file.WebUploadPolicy;
import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import com.alibaba.cloud.ai.studio.core.utils.common.DateUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * Title oss manager.<br>
 * Description oss manager.<br>
 *
 * @since 1.0.0.3
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OssManager implements InitializingBean {

	/** oss url expire time */
	public static final Duration URl_EXPIRE_TIME = Duration.ofHours(2);

	/** upload expire time */
	public static final Duration UPLOAD_EXPIRE_TIME = Duration.ofMinutes(30);

	/** oss upload file size limit */
	public static final long UPLOAD_FILE_SIZE_LIMIT = 1024 * 1024 * 100;

	List<String> SUPPORT_ENTERPRISE_DATA_MIME_TYPE = ImmutableList.of("application/pdf", "application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel",
			"application/x-xls", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain", // txt
			"application/vnd.ms-powerpoint", // ppt
			"application/vnd.openxmlformats-officedocument.presentationml.presentation", // pptx
			"text/markdown" // md
	);

	private final StudioProperties properties;

	private CredentialsProvider credentialsProvider;

	private OSS ossClient;

	private OSS ossClientInternal;

	/**
	 * get web upload policy for web oss uploading
	 * @param uid user id
	 * @param type file type
	 * @param fileName file name
	 * @return web upload policy
	 * @throws BizException if fail
	 */
	public WebUploadPolicy getWebUploadPolicy(String uid, String type, String fileName) {
		Date expiration = new Date(System.currentTimeMillis() + UPLOAD_EXPIRE_TIME.toMillis());
		String endpoint = getEndpoint();
		String bucket = getBucket();
		String host = "//" + bucket + "." + endpoint;

		String key = getObjectName(uid, type, fileName);
		PolicyConditions conditions = new PolicyConditions();
		conditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, UPLOAD_FILE_SIZE_LIMIT);
		conditions.addConditionItem(MatchMode.Exact, PolicyConditions.COND_KEY, key);

		// // only allow some mime-type
		// conditions.addConditionItem(MatchMode.In, PolicyConditions.COND_CONTENT_TYPE,
		// SUPPORT_ENTERPRISE_DATA_MIME_TYPE.toArray(new String[] {}));

		// TODO use sts in futuer?
		/*
		 * // 获取stsToken StsRole stsRole =
		 * getStsRole(fileStore.getAliyunUid().toString()); if (stsRole == null) { throw
		 * new BizException(ErrorCodeEnum.SDK_FILE_STORE_NO_AUTH); } OSS ossClient = new
		 * OSSClientBuilder().build(endpoint, stsRole.getStsAk(), stsRole.getStsSecret(),
		 * stsRole.getStsSecurityToken()); if (ossClient == null) { throw new
		 * BizException(ErrorCodeEnum.SDK_FILE_STORE_NO_AUTH); }
		 *
		 * String postPolicy = ossClient.generatePostPolicy(expiration, conditions);
		 * byte[] binaryData; binaryData = postPolicy.getBytes(StandardCharsets.UTF_8);
		 * String encodedPolicy = BinaryUtil.toBase64String(binaryData); String
		 * postSignature = ossClient.calculatePostSignature(postPolicy);
		 *
		 * WebUploadPolicy policy = new WebUploadPolicy();
		 * policy.setAccessId(stsRole.getStsAk()); policy.setPolicy(encodedPolicy);
		 * policy.setSignature(postSignature);
		 * policy.setSecurityToken(stsRole.getStsSecurityToken()); policy.setKey(key);
		 * policy.setDir(tenantDir); policy.setHost(host); policy.setExpire(expireEndTime
		 * / Constants.ONE_THOUSAND);
		 */
		String postPolicy = ossClientInternal.generatePostPolicy(expiration, conditions);
		byte[] binaryData;
		binaryData = postPolicy.getBytes(StandardCharsets.UTF_8);
		String encodedPolicy = BinaryUtil.toBase64String(binaryData);
		String postSignature = ossClientInternal.calculatePostSignature(postPolicy);

		WebUploadPolicy policy = new WebUploadPolicy();
		policy.setAccessId(credentialsProvider.getCredentials().getAccessKeyId());
		policy.setName(fileName);
		policy.setPolicy(encodedPolicy);
		policy.setSignature(postSignature);
		policy.setPath(key);
		policy.setHost(host);
		policy.setExpire(expiration.getTime() / 1000);
		return policy;
	}

	/**
	 * upload file to oss
	 * @param type file type
	 * @param uid user id
	 * @param path oss path
	 * @return object name
	 */
	public String uploadFile(String type, String uid, String path) {
		long start = System.currentTimeMillis();

		BufferedInputStream bis = null;
		try {
			File file = new File(path);
			bis = new BufferedInputStream(new FileInputStream(file));
			String name = getObjectName(uid, type, file.getName());

			String bucketName = getBucket();
			PutObjectRequest request = new PutObjectRequest(bucketName, name, bis);
			PutObjectResult putObjectResult = ossClientInternal.putObject(request);
			LogUtils.monitor("ossManager", "uploadFile", start, SUCCESS, path, putObjectResult);

			return name;
		}
		catch (OSSException e) {
			LogUtils.monitor("ossManager", "uploadFile", start, SUCCESS, path, e.getMessage(), e.getRequestId(), e);
			throw new BizException(ErrorCode.OSS_UPLOAD_ERROR.toError(), e);
		}
		catch (Exception e) {
			LogUtils.monitor("ossManager", "uploadFile", start, SUCCESS, path, e);
			throw new BizException(ErrorCode.OSS_UPLOAD_ERROR.toError(), e);
		}
		finally {
			if (bis != null) {
				IOUtils.closeQuietly(bis);
			}
		}
	}

	/**
	 * download file from oss
	 * @param objectName object name
	 * @param path downloaded local path
	 */
	public void downloadFile(String objectName, String path) {
		long start = System.currentTimeMillis();
		try {
			String bucketName = getBucket();
			ossClientInternal.getObject(new GetObjectRequest(bucketName, objectName), new File(path));
			LogUtils.monitor("ossManager", "downloadFile", start, SUCCESS, path, objectName, path);
		}
		catch (Exception e) {
			LogUtils.monitor("ossManager", "downloadFile", start, SUCCESS, path, e.getMessage(), e);
			throw new BizException(ErrorCode.OSS_DOWNLOAD_ERROR.toError(), e);
		}
	}

	public String generateURL(String objectName) {
		long start = System.currentTimeMillis();
		String safeObjectName = trimObjectName(objectName);
		if (StringUtils.isBlank(objectName)) {
			throw new IllegalArgumentException("oss object name is invalid");
		}

		try {
			String bucketName = getBucket();
			Date expiration = new Date(System.currentTimeMillis() + URl_EXPIRE_TIME.toMillis());
			URL url = ossClient.generatePresignedUrl(bucketName, safeObjectName, expiration);
			LogUtils.monitor("ossManager", "generateURL", start, SUCCESS, objectName, url);
			return url.toString().replace("http://", "https://");
		}
		catch (Exception e) {
			LogUtils.monitor("ossManager", "generateURL", start, SUCCESS, objectName, null, e);
			throw new BizException(ErrorCode.OSS_GEN_URL_ERROR.toError(), e);
		}
	}

	/**
	 * download file as string
	 * @param objectName object name
	 * @return file content
	 */
	public String downloadFileAsString(String objectName) {
		long start = System.currentTimeMillis();

		InputStream is = null;
		OSSObject ossObject = null;
		ByteArrayOutputStream bos = null;
		try {
			String bucketName = getBucket();

			ossObject = ossClientInternal.getObject(bucketName, objectName);
			is = ossObject.getObjectContent();
			bos = new ByteArrayOutputStream();
			byte[] readBuffer = new byte[8192];
			int bytesRead;

			long fileBytes = 0;
			while ((bytesRead = is.read(readBuffer)) != -1) {
				bos.write(readBuffer, 0, bytesRead);
				fileBytes += bytesRead;
			}

			LogUtils.monitor("ossManager", "downloadFile", start, SUCCESS, objectName, fileBytes);

			return bos.toString(StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			LogUtils.monitor("ossManager", "downloadFile", start, SUCCESS, objectName, e.getMessage(), e);
			throw new BizException(ErrorCode.OSS_DOWNLOAD_ERROR.toError(), e);
		}
		finally {
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(bos);
			IOUtils.closeQuietly(ossObject);
		}
	}

	@Override
	public void afterPropertiesSet() {
		if (!Objects.equals(properties.getUploadMethod(), UploadType.OSS.getValue())) {
			return;
		}

		String ak = properties.getOss().getAccessKeyId();
		String sk = properties.getOss().getAccessKeySecret();
		String region = properties.getOss().getRegion();
		if (StringUtils.isBlank(ak) || StringUtils.isBlank(sk)) {
			throw new IllegalArgumentException("oss ak or sk should be set.");
		}

		credentialsProvider = new DefaultCredentialProvider(ak, sk);

		// this is for public access like
		ossClient = OSSClientBuilder.create()
			.endpoint(properties.getOss().getEndpoint())
			.credentialsProvider(credentialsProvider)
			.region(region)
			.build();

		// This is for vpc internet access
		ossClientInternal = OSSClientBuilder.create()
			.endpoint(properties.getOss().getInternalEndpoint())
			.credentialsProvider(credentialsProvider)
			.region(region)
			.build();
	}

	/**
	 * get web upload policy
	 * @param uid user id
	 * @param type file type
	 * @param fileName file name
	 * @return web upload policy
	 */
	private String getObjectName(String uid, String type, String fileName) {
		return new StringBuilder(getObjectPath(uid, type)).append(File.separator)
			.append(IdGenerator.uuid32())
			.append("_")
			.append(System.currentTimeMillis())
			.append(".")
			.append(FilenameUtils.getExtension(fileName))
			.toString();
	}

	private String getObjectPath(String uid, String type) {
		return new StringBuilder(type).append(File.separator)
			.append(uid)
			.append(File.separator)
			.append(DateUtils.getYear())
			.append(File.separator)
			.append(DateUtils.getMonth())
			.append(File.separator)
			.append(DateUtils.getDay())
			.toString();
	}

	public static String trimObjectName(String objectName) {
		if (objectName == null || objectName.trim().isEmpty()) {
			return null;
		}
		else {
			return objectName;
		}
	}

	private String getEndpoint() {
		return properties.getOss().getEndpoint();
	}

	private String getBucket() {
		String bucket = properties.getOss().getBucket();
		if (StringUtils.isBlank(bucket)) {
			throw new IllegalArgumentException("oss bucket should be set.");
		}

		return bucket;
	}

}
