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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;

/**
 * auth: dahua
 */
public final class MinioConstants {

	public static final String CONFIG_PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".minio";

	public static final String TOOL_NAME_UPLOAD = "minio-upload";

	public static final String TOOL_NAME_DOWNLOAD = "minio-download";

	public static final String TOOL_NAME_DELETE = "minio-delete";

	public static final String TOOL_NAME_CHECK_EXISTS = "minio-checkExists";

	public static final String ENDPOINT = "MINIO-ENDPOINT";

	public static final String ACCESS_KEY = "MINIO-ACCESS-KEY";

	public static final String SECRET_KEY = "MINIO-SECRET-KEY";

}
