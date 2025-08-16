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

package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.account.Account;
import com.alibaba.cloud.ai.studio.runtime.domain.account.ChangePasswordRequest;
import com.alibaba.cloud.ai.studio.core.base.service.AccountService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.admin.annotation.ApiModelAttribute;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Account management controller that handles user account operations. Provides Restful
 * APIs for account CRUD operations and profile management.
 *
 * @since 1.0.0.3
 */
@RestController
@Tag(name = "account")
@RequestMapping("/console/v1/accounts")
public class AccountController {

	/** Service for handling account-related business logic */
	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	/**
	 * Creates a new user account
	 * @param account Account information including username and password
	 * @return Account ID of the created account
	 */
	@PostMapping()
	public Result<String> createAccount(@RequestBody Account account) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(account)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("account"));
		}

		if (StringUtils.isBlank(account.getUsername())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("username"));
		}

		if (StringUtils.isBlank(account.getPassword())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("password"));
		}

		String accountId = accountService.createAccount(account);
		return Result.success(context.getRequestId(), accountId);
	}

	/**
	 * Updates an existing account's information
	 * @param accountId ID of the account to update
	 * @param account Updated account information
	 * @return Success status
	 */
	@PutMapping("/{accountId}")
	public Result<String> updateAccount(@PathVariable("accountId") String accountId, @RequestBody Account account) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(account)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("account"));
		}

		if (Objects.isNull(accountId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("accountId"));
		}

		account.setAccountId(accountId);
		accountService.updateAccount(account);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes an account by its ID
	 * @param accountId ID of the account to delete
	 * @return Success status
	 */
	@DeleteMapping("/{accountId}")
	public Result<Void> deleteAccount(@PathVariable("accountId") String accountId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(accountId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("accountId"));
		}

		accountService.deleteAccount(accountId);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Retrieves account information by ID
	 * @param accountId ID of the account to retrieve
	 * @return Account information
	 */
	@GetMapping("/{accountId}")
	public Result<Account> getAccount(@PathVariable("accountId") String accountId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(accountId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("accountId"));
		}

		Account account = accountService.getAccount(accountId);
		return Result.success(context.getRequestId(), account);
	}

	/**
	 * Lists accounts with pagination
	 * @param query Query parameters for filtering and pagination
	 * @return Paginated list of accounts
	 */
	@GetMapping()
	public Result<PagingList<Account>> listAccounts(@ApiModelAttribute BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(query)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("query"));
		}

		PagingList<Account> accounts = accountService.listAccounts(query);
		return Result.success(context.getRequestId(), accounts);
	}

	/**
	 * Changes the password for an account
	 * @param request Password change request containing old and new passwords
	 * @return Success status
	 */
	@PutMapping("/change-password")
	public Result<String> changePassword(@RequestBody ChangePasswordRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(request.getPassword())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("password"));
		}

		if (Objects.isNull(request.getNewPassword())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("newPassword"));
		}

		accountService.changePassword(request);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Retrieves the current user's account profile
	 * @return Current user's account information
	 */
	@GetMapping("/profile")
	public Result<Account> getAccountProfile() {
		RequestContext context = RequestContextHolder.getRequestContext();
		Account account = accountService.getAccountProfile();
		return Result.success(context.getRequestId(), account);
	}

}
