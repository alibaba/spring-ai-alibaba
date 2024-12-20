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
package com.alibaba.cloud.ai.dashscope.common;

/**
 * @author nuocheng.lxm
 * @since 1.0.0-M2
 */
public enum ErrorCodeEnum {

	READER_APPLY_LEASE_ERROR("ApplyLeaseError", "ApplyLease Error,Please Check Your Params"),

	READER_UPLOAD_OSS_ERROR("UploadOssError", "UploadOss Error,Please Check Your Params"),

	READER_ADD_FILE_ERROR("AddFileError", "AddFile Error,Please Check File"),

	READER_PARSE_FILE_ERROR("ParseFileError", "ParseFile Error,Please Check File"),

	SPLIT_DOCUMENT_ERROR("SplitDocumentError", "SplitDocumentError,Please Check Your Params"),

	CREATE_INDEX_ERROR("CreateIndexError", "CreateIndexError,Please Check Your Params"),

	INDEX_ADD_DOCUMENT_ERROR("IndexAddDocumentError", "IndexAddDocumentError,Please Check Your Params"),

	RETRIEVER_DOCUMENT_ERROR("RetrieverError", "RetrieverError,Please Check Your Params");

	private String code;

	private String message;

	ErrorCodeEnum(String code, String message) {
		this.code = code;
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public String message() {
		return message;
	}

}
