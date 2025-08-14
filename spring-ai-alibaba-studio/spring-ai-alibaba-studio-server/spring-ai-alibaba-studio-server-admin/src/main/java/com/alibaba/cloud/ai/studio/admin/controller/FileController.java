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
package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.file.UploadPolicy;
import com.alibaba.cloud.ai.studio.runtime.domain.file.WebUploadPolicy;
import com.alibaba.cloud.ai.studio.runtime.domain.file.WebUploadRequest;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.manager.FileManager;
import com.alibaba.cloud.ai.studio.core.base.manager.OssManager;
import com.alibaba.cloud.ai.studio.core.utils.io.FileUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Controller for handling file operations including upload and download
 */
@RestController
@Tag(name = "file")
@RequestMapping("/console/v1/files")
@RequiredArgsConstructor
public class FileController {

	/** File manager for handling file operations */
	private final FileManager fileManager;

	/** Oss manager */
	private final OssManager ossManager;

	/**
	 * Upload multiple files and return their upload policies
	 * @param files Array of files to upload
	 * @param category Category of the files
	 * @return List of upload policies for the uploaded files
	 */
	@PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result<List<UploadPolicy>> uploadDocument(@RequestPart("files") MultipartFile[] files,
			@RequestPart("category") String category) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(category)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("category"));
		}

		if (ArrayUtils.isEmpty(files)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("files"));
		}

		List<UploadPolicy> policies = new ArrayList<>();
		for (MultipartFile file : files) {
			String path = fileManager.saveFile(file, category, context);
			String ext = FilenameUtils.getExtension(file.getOriginalFilename());
			ext = StringUtils.lowerCase(ext);
			UploadPolicy uploadPolicy = UploadPolicy.builder()
				.path(path)
				.name(file.getOriginalFilename())
				.extension(ext)
				.contentType(file.getContentType())
				.size(file.getSize())
				.build();

			policies.add(uploadPolicy);
		}

		return Result.success(context.getRequestId(), policies);
	}

	/**
	 * Download a file from the server
	 * @param filePath Path of the file to download
	 * @param preview Whether to preview the file in browser
	 * @param response HTTP response object
	 * @throws IOException If file operations fail
	 */
	@GetMapping("/download")
	public void downloadFile(@RequestParam(value = "path") String filePath,
			@RequestParam(value = "preview", defaultValue = "false") boolean preview, HttpServletResponse response)
			throws IOException {
		// TODO check permission, there might be a file management instead?
		if (Objects.isNull(filePath)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("path"));
		}

		Resource resource = fileManager.loadFile(filePath);
		File file = resource.getFile();

		String filename = UriUtils.encode(file.getName(), StandardCharsets.UTF_8);
		if (preview) {
			response.setContentType(MediaType.parseMediaType(FileUtils.getContentType(filename)).toString());
			response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
		}
		else {
			response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		}

		response.setContentLengthLong(file.length());
		try (InputStream inputStream = resource.getInputStream();
				OutputStream outputStream = response.getOutputStream()) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.flush();
		}
	}

	/**
	 * get multiple files upload policies for oss
	 * @param request upload request
	 * @return List of upload policies for the uploaded files
	 */
	@PostMapping(value = "upload-policies")
	public Result<List<WebUploadPolicy>> getUploadPolicies(@RequestBody WebUploadRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(request.getCategory())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("category"));
		}

		if (CollectionUtils.isEmpty(request.getFiles())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("files"));
		}

		List<WebUploadPolicy> policies = new ArrayList<>();
		for (WebUploadRequest.FileMeta fileMeta : request.getFiles()) {
			WebUploadPolicy policy = ossManager.getWebUploadPolicy(context.getAccountId(), request.getCategory(),
					fileMeta.getName());
			policies.add(policy);
		}

		return Result.success(context.getRequestId(), policies);
	}

	/**
	 * get preview url
	 * @param filePath Path of the file to download
	 */
	@GetMapping("/get-preview-url")
	public Result<String> getPreviewUrl(@RequestParam(value = "path") String filePath) {
		// TODO check permission, there might be a file management instead?
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(filePath)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("path"));
		}

		String url = ossManager.generateURL(filePath);
		return Result.success(context.getRequestId(), url);
	}

}
