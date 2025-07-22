package com.alibaba.cloud.ai.studio.runtime.domain.account;

import lombok.Data;

import java.io.Serializable;

/**
 * Request model for user login
 */
@Data
public class LoginRequest implements Serializable {

	/**
	 * Username for authentication
	 */
	private String username;

	/**
	 * Password for authentication
	 */
	private String password;

}
