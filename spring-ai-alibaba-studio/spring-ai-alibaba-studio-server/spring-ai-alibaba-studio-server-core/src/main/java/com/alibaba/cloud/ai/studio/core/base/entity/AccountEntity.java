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

package com.alibaba.cloud.ai.studio.core.base.entity;

import com.alibaba.cloud.ai.studio.runtime.enums.AccountStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.AccountType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Account entity representing user account information.
 *
 * @since 1.0.0.3
 */

@Data
@TableName("account")
public class AccountEntity {

	/** Primary key */
	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	/** Unique account identifier */
	@TableField("account_id")
	private String accountId;

	/** Username for login */
	private String username;

	/** User's email address */
	private String email;

	/** User's mobile number */
	private String mobile;

	/** Encrypted password */
	private String password;

	/** User's display name */
	private String nickname;

	/** User's profile picture URL */
	private String icon;

	/** Account status */
	private AccountStatus status;

	/** Account type */
	private AccountType type;

	/** Creation timestamp */
	@TableField("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@TableField("gmt_modified")
	private Date gmtModified;

	/** Last login timestamp */
	@TableField("gmt_last_login")
	private Date gmtLastLogin;

	/** Creator's identifier */
	private String creator;

	/** Last modifier's identifier */
	private String modifier;

	@TableField(exist = false)
	private String defaultWorkspaceId;

}
