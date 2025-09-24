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
package com.alibaba.cloud.ai.enums;

/**
 * Database connection related error code definitions.
 * <p>
 * Contains standard SQLState codes, custom codes and corresponding Chinese prompt
 * information. Can be used as a basic error code module for open source projects,
 * facilitating unified exception handling.
 */
public enum ErrorCodeEnum {

	/**
	 * Success status code
	 */
	SUCCESS("0", "操作成功"),

	/**
	 * Invalid parameter
	 */
	INVALID_PARAM("10", "无效参数"),

	/**
	 * Data source connection failed (SQLState: 08001)
	 */
	DATASOURCE_CONNECTION_FAILURE_08001("08001", "无法建立数据库连接，请检查配置的IP/域名是否正确，或将析言IP加入数据库白名单"),

	/**
	 * Data source connection failed (SQLState: 08S01)
	 */
	DATASOURCE_CONNECTION_FAILURE_08S01("08S01", "无法建立数据库连接，请检查配置的IP/域名是否正确，或将析言IP加入数据库白名单"),

	/**
	 * Connection lost (SQLState: 08002)
	 */
	CONNECTION_LOST_08002("08002", "连接丢失：服务器关闭"),

	/**
	 * Operation executed on closed connection (SQLState: 08003)
	 */
	CONNECTION_CLOSED_08003("08003", "试图在已经关闭的连接上操作"),

	/**
	 * Database rejected connection request (SQLState: 08004)
	 */
	CONNECTION_DENIED_08004("08004", "数据库拒绝了连接请求"),

	/**
	 * Connection failed (SQLState: 08006)
	 */
	CONNECTION_FAILURE_08006("08006", "连接失败"),

	/**
	 * Insufficient permissions (SQLState: 42501)
	 */
	INSUFFICIENT_PRIVILEGE_42501("42501", "权限不足"),

	/**
	 * Wrong password (SQLState: 28P01)
	 */
	PASSWORD_ERROR_28P01("28P01", "密码错误"),

	/**
	 * Wrong password (SQLState: 28000)
	 */
	PASSWORD_ERROR_28000("28000", "密码错误"),

	/**
	 * Database does not exist (SQLState: 3D000)
	 */
	DATABASE_NOT_EXIST_3D000("3D000", "数据库不存在"),

	/**
	 * Database does not exist (SQLState: 42000)
	 */
	DATABASE_NOT_EXIST_42000("42000", "数据库不存在"),

	/**
	 * Time zone parameter error (SQLState: 01S00)
	 */
	TIME_ZONE_ERROR_01S00("01S00", "时区参数错误"),

	/**
	 * SSL connection error (SQLState: 08P01)
	 */
	SSL_ERROR_08P01("08P01", "进行SSL连线时发生错误"),

	/**
	 * Schema does not exist (SQLState: 3D070)
	 */
	SCHEMA_NOT_EXIST_3D070("3D070", "模式不存在"),

	/**
	 * Unknown error (fallback)
	 */
	OTHERS("100", "未知错误，请联系技术人员排查");

	private final String code;

	private final String message;

	ErrorCodeEnum(String code, String message) {
		this.code = code;
		this.message = message;
	}

	/**
	 * Find corresponding error information based on error code
	 * @param code error code string
	 * @return corresponding error object, return {@link #OTHERS} if no match
	 */
	public static ErrorCodeEnum fromCode(String code) {
		for (ErrorCodeEnum rc : values()) {
			if (code.equals(rc.getCode())) {
				return rc;
			}
		}
		return OTHERS;
	}

	/**
	 * Find corresponding error information based on error code, return SUCCESS by default
	 * if not found
	 * @param code error code string
	 * @return matched error code or default SUCCESS
	 */
	public static ErrorCodeEnum fromCodeWithSuccess(String code) {
		for (ErrorCodeEnum rc : values()) {
			if (code.equals(rc.getCode())) {
				return rc;
			}
		}
		return SUCCESS;
	}

	/**
	 * Get error code description
	 * @return error code + error description
	 */
	@Override
	public String toString() {
		return "[" + code + "] " + message;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

}
