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

package com.alibaba.cloud.ai.studio.runtime.domain;

import com.alibaba.cloud.ai.studio.runtime.enums.AccountType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Request context for tracking request metadata and user information.
 *
 * @since 1.0.0.3
 */

@Data
public class RequestContext implements Serializable {

	/** Request start timestamp */
	private long startTime;

	/** Unique identifier for the request */
	@JsonProperty("request_id")
	private String requestId;

	/** User account identifier */
	@JsonProperty("account_id")
	private String accountId;

	/** User account name */
	private String username;

	/** Type of the user account */
	@JsonProperty("account_type")
	private AccountType accountType;

	/** Workspace identifier */
	@JsonProperty("workspace_id")
	private String workspaceId;

	/** IP address of the request caller */
	@JsonProperty("caller_ip")
	private String callerIp;

	/** Source of the request, defaults to "console" */
	private String source = "console";

}
