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

package com.alibaba.cloud.ai.studio.runtime.domain.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.alibaba.cloud.ai.studio.runtime.enums.AccountStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.AccountType;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Account entity representing user account information.
 *
 * @since 1.0.0.3
 */
@Data
public class Account implements Serializable {

	/** Unique identifier for the account */
	@JsonProperty("account_id")
	private String accountId;

	/** Default workspace ID for the account */
	@JsonProperty("default_workspace_id")
	private String defaultWorkspaceId;

	/** Username for login */
	private String username;

	/** Account password */
	private String password;

	/** User's email address */
	private String email;

	/** User's mobile number */
	private String mobile;

	/** Current status of the account */
	private AccountStatus status;

	/** Type of the account */
	private AccountType type;

	/** User's display name */
	private String nickname;

	/** User's profile picture URL */
	private String icon;

	/** Username of the account creator */
	private String creator;

	/** Username of the last modifier */
	private String modifier;

	/** Account creation timestamp */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

	/** Last login timestamp */
	@JsonProperty("gmt_last_login")
	private Date gmtLastLogin;

}
