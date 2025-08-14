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

package com.alibaba.cloud.ai.studio.core.base.service;

import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.account.Account;
import com.alibaba.cloud.ai.studio.runtime.domain.account.ChangePasswordRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.account.LoginRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.account.RefreshTokenRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.account.TokenResponse;
import com.alibaba.cloud.ai.studio.core.base.entity.AccountEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.alibaba.cloud.ai.studio.runtime.domain.account.Oauth2User;

/**
 * Account management service interface. Provides operations for user authentication,
 * account management, and profile handling.
 *
 * @since 1.0.0.3
 */
public interface AccountService extends IService<AccountEntity> {

	/**
	 * Authenticates user and returns access token
	 */
	TokenResponse login(LoginRequest loginRequest);

	/**
	 * Refreshes the access token using refresh token
	 */
	TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

	/**
	 * login with oauth user
	 * @param oauth2User oauth2 user
	 * @return token response
	 */
	TokenResponse login(Oauth2User oauth2User);

	/**
	 * Invalidates the current access token
	 */
	void logout(String accessToken);

	/**
	 * register an account
	 * @param account account
	 * @return account ID
	 */
	String registerAccount(Account account);

	/**
	 * Creates a new account
	 * @return account ID
	 */
	String createAccount(Account account);

	/**
	 * Updates existing account information
	 */
	void updateAccount(Account account);

	/**
	 * Removes an account by ID
	 */
	void deleteAccount(String accountId);

	/**
	 * Retrieves a paginated list of accounts
	 */
	PagingList<Account> listAccounts(BaseQuery query);

	/**
	 * Gets account details by ID
	 */
	Account getAccount(String accountId);

	/**
	 * Updates account password
	 */
	void changePassword(ChangePasswordRequest request);

	/**
	 * Retrieves current user's account profile
	 */
	Account getAccountProfile();

}
