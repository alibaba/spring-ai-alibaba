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
package com.alibaba.cloud.ai.dashscope.rag;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import com.alibaba.cloud.ai.dashscope.common.ErrorCodeEnum;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.http.ResponseEntity;

/**
 * @author nuocheng.lxm
 * @since 2024/7/22 14:40 百炼云端文档解析，主要是走当前数据中心逻辑
 *
 */
public class DashScopeDocumentCloudReader implements DocumentReader {

	public static final int MAX_TRY_COUNT = 10;

	private static final Logger logger = LoggerFactory.getLogger(DashScopeDocumentCloudReader.class);

	private final DashScopeApi dashScopeApi;

	private DashScopeDocumentCloudReaderOptions readerConfig;

	private File file;

	public DashScopeDocumentCloudReader(String filePath, DashScopeApi dashScopeApi,
			DashScopeDocumentCloudReaderOptions readerConfig) {
		file = new File(filePath);
		if (!file.exists()) {
			throw new RuntimeException(filePath + " Not Exist");
		}
		if (readerConfig == null) {
			readerConfig = new DashScopeDocumentCloudReaderOptions();
		}
		this.readerConfig = readerConfig;
		this.dashScopeApi = dashScopeApi;
	}

	@Override
	public List<Document> get() {
		String fileMD5;
		FileInputStream fileInputStream;
		try {
			fileInputStream = new FileInputStream(file);
			fileMD5 = DigestUtils.md5Hex(fileInputStream);
			DashScopeApi.UploadRequest uploadRequest = new DashScopeApi.UploadRequest(readerConfig.getCategoryId(),
					file.getName(), file.length(), fileMD5);
			String fileId = dashScopeApi.upload(file, uploadRequest);
			// Polling for results
			int tryCount = 0;
			while (tryCount < MAX_TRY_COUNT) {
				ResponseEntity<DashScopeApi.CommonResponse<DashScopeApi.QueryFileResponseData>> response = dashScopeApi
					.queryFileInfo(readerConfig.getCategoryId(),
							new DashScopeApi.UploadRequest.QueryFileRequest(fileId));
				if (response != null && response.getBody() != null) {
					DashScopeApi.QueryFileResponseData queryFileResponseData = response.getBody().data();
					String fileStatus = queryFileResponseData.status();
					if ("PARSE_SUCCESS".equals(fileStatus)) {
						// download files
						String parseResult = dashScopeApi.getFileParseResult(readerConfig.getCategoryId(),
								new DashScopeApi.UploadRequest.QueryFileRequest(fileId));
						return List.of(toDocument(fileId, parseResult));
					}
					else if ("PARSE_FAILED".equals(fileStatus)) {
						logger.error("File:{} Read Error，ErrorCode:{},ErrorMessage:{}", file.getName(),
								response.getBody().code(), response.getBody().message());
						throw new DashScopeException(ErrorCodeEnum.READER_PARSE_FILE_ERROR);
					}
				}
				tryCount++;
				Thread.sleep(30000L);
			}
			return null;
		}
		catch (Exception exception) {
			throw new RuntimeException("ReadFile Exception", exception);
		}

	}

	private Document toDocument(String fileId, String parseResultText) {
		Map<String, Object> metaData = new HashMap<>();
		metaData.put("parse_fmt_type", "DASHSCOPE_DOCMIND");
		return new Document(fileId, parseResultText, metaData);
	}

}
