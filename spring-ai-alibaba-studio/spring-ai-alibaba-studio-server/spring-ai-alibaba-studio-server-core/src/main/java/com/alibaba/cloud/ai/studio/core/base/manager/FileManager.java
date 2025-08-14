/*
 * Copyright 2025 the original author or authors.
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

import com.alibaba.cloud.ai.studio.runtime.enums.UploadType;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import com.alibaba.cloud.ai.studio.core.utils.common.DateUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * File manager for handling file operations including saving, loading and media type
 * detection.
 *
 * @since 1.0.0.3
 */
@Slf4j
@Component
public class FileManager {

	/** Configuration properties for the file manager */
	private final StudioProperties studioProperties;

	/** Oss manager */
	private final OssManager ossManager;

	public FileManager(StudioProperties studioProperties, OssManager ossManager) {
		this.studioProperties = studioProperties;
		this.ossManager = ossManager;
	}

	/**
	 * Saves an uploaded file to the storage system. The file path is constructed using
	 * category, account ID, workspace ID, date and a unique identifier.
	 * @param file The file to be saved
	 * @param category The category of the file
	 * @param context The request context containing account and workspace information
	 * @return The relative path where the file was saved
	 */
	public String saveFile(MultipartFile file, String category, RequestContext context) {
		String storagePath = studioProperties.getStoragePath();
		String filePath = new StringBuilder(category).append(File.separator)
			.append(context.getAccountId())
			.append(File.separator)
			.append(context.getWorkspaceId())
			.append(File.separator)
			.append(DateUtils.getYear())
			.append(File.separator)
			.append(DateUtils.getMonth())
			.append(File.separator)
			.append(DateUtils.getDay())
			.append(File.separator)
			.append(IdGenerator.uuid32())
			.append("_")
			.append(System.currentTimeMillis())
			.append(".")
			.append(FilenameUtils.getExtension(file.getOriginalFilename()))
			.toString();
		File saveFile = new File(storagePath + File.separator + filePath);

		try {
			if (!saveFile.getParentFile().exists()) {
				boolean success = saveFile.getParentFile().mkdirs();
				if (!success) {
					throw new RuntimeException("failed to create dir: " + saveFile.getParentFile());
				}
			}

			file.transferTo(saveFile);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		return filePath;
	}

	/**
	 * Loads a file from the storage system.
	 * @param filePath The relative path of the file
	 * @return A Resource object representing the file
	 * @throws BizException if the file is not found
	 */
	public Resource loadFile(String filePath) {
		if (UploadType.OSS.getValue().equalsIgnoreCase(studioProperties.getUploadMethod())) {
			String url = ossManager.generateURL(filePath);
			try {
				return new UrlResource(url);
			}
			catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}

		String storagePath = studioProperties.getStoragePath();
		File file = new File(storagePath + File.separator + filePath);

		if (!file.exists()) {
			throw new BizException(ErrorCode.FILE_NOT_FOUND.toError("File not found: " + filePath));
		}

		return new FileSystemResource(file);
	}

	/**
	 * Determines the media type of a file from its URL by analyzing the file header.
	 * Supports common formats including JPEG, PNG, GIF, PDF, XML, and JSON.
	 * @param url The URL of the file
	 * @return The detected MediaType
	 * @throws BizException if the media type cannot be determined or is invalid
	 */
	public MediaType getMediaTypeFromUrl(String url) {
		try {
			URL imageUrl = new URL(url);
			// noinspection StartSSRFNetHookCheckingInspection
			java.io.InputStream is = imageUrl.openStream();
			byte[] bytes = new byte[Math.min(is.available(), 1024)]; // 读取前1KB用于识别
			is.read(bytes);
			is.close();
			MediaType mediaType = null;
			// 根据文件头识别MIME类型
			if (bytes.length >= 4) {
				// 图片格式
				if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
					mediaType = MediaType.IMAGE_JPEG;
				}
				else if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E
						&& bytes[3] == (byte) 0x47) {
					mediaType = MediaType.IMAGE_PNG;
				}
				else if (bytes[0] == (byte) 0x47 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46) {
					mediaType = MediaType.IMAGE_GIF;
				}
				// PDF格式
				else if (bytes[0] == (byte) 0x25 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x44
						&& bytes[3] == (byte) 0x46) {
					mediaType = MediaType.APPLICATION_PDF;
				}
				// XML格式
				else if (bytes[0] == (byte) 0x3C && bytes[1] == (byte) 0x3F && bytes[2] == (byte) 0x78
						&& bytes[3] == (byte) 0x6D) {
					mediaType = MediaType.APPLICATION_XML;
				}
				// JSON格式 - 检查开头是否为 { 或 [
				else if (bytes[0] == (byte) 0x7B || bytes[0] == (byte) 0x5B) {
					mediaType = MediaType.APPLICATION_JSON;
				}
			}
			if (mediaType == null) {
				throw new BizException(
						ErrorCode.INVALID_PARAMS.toError("vision param is valid", url + " has invalid media type"));
			}
			return mediaType;
		}
		catch (Exception e) {
			throw new BizException(
					ErrorCode.INVALID_PARAMS.toError("vision param is valid", url + " has invalid media type"));
		}
	}

}
