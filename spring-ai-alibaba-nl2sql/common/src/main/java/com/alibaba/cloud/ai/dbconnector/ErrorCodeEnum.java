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
package com.alibaba.cloud.ai.dbconnector;

import lombok.Getter;

/**
 * 数据库连接相关的错误码定义。
 * <p>
 * 包含标准 SQLState 码、自定义码以及对应的中文提示信息。 可作为开源项目的基础错误码模块，便于统一处理异常。
 */
@Getter
public enum ErrorCodeEnum {

	/**
	 * 成功状态码
	 */
	SUCCESS("0", "操作成功"),

	/**
	 * 参数无效
	 */
	INVALID_PARAM("10", "无效参数"),

	/**
	 * 数据源连接失败（SQLState: 08001）
	 */
	DATASOURCE_CONNECTION_FAILURE_08001("08001", "无法建立数据库连接，请检查配置的IP/域名是否正确，或将析言IP加入数据库白名单"),

	/**
	 * 数据源连接失败（SQLState: 08S01）
	 */
	DATASOURCE_CONNECTION_FAILURE_08S01("08S01", "无法建立数据库连接，请检查配置的IP/域名是否正确，或将析言IP加入数据库白名单"),

	/**
	 * 连接丢失（SQLState: 08002）
	 */
	CONNECTION_LOST_08002("08002", "连接丢失：服务器关闭"),

	/**
	 * 在已关闭的连接上执行操作（SQLState: 08003）
	 */
	CONNECTION_CLOSED_08003("08003", "试图在已经关闭的连接上操作"),

	/**
	 * 数据库拒绝连接请求（SQLState: 08004）
	 */
	CONNECTION_DENIED_08004("08004", "数据库拒绝了连接请求"),

	/**
	 * 连接失败（SQLState: 08006）
	 */
	CONNECTION_FAILURE_08006("08006", "连接失败"),

	/**
	 * 权限不足（SQLState: 42501）
	 */
	INSUFFICIENT_PRIVILEGE_42501("42501", "权限不足"),

	/**
	 * 密码错误（SQLState: 28P01）
	 */
	PASSWORD_ERROR_28P01("28P01", "密码错误"),

	/**
	 * 密码错误（SQLState: 28000）
	 */
	PASSWORD_ERROR_28000("28000", "密码错误"),

	/**
	 * 数据库不存在（SQLState: 3D000）
	 */
	DATABASE_NOT_EXIST_3D000("3D000", "数据库不存在"),

	/**
	 * 数据库不存在（SQLState: 42000）
	 */
	DATABASE_NOT_EXIST_42000("42000", "数据库不存在"),

	/**
	 * 时区参数错误（SQLState: 01S00）
	 */
	TIME_ZONE_ERROR_01S00("01S00", "时区参数错误"),

	/**
	 * SSL 连接错误（SQLState: 08P01）
	 */
	SSL_ERROR_08P01("08P01", "进行SSL连线时发生错误"),

	/**
	 * 模式不存在（SQLState: 3D070）
	 */
	SCHEMA_NOT_EXIST_3D070("3D070", "模式不存在"),

	/**
	 * 未知错误（兜底）
	 */
	OTHERS("100", "未知错误，请联系技术人员排查");

	private final String code;

	private final String message;

	ErrorCodeEnum(String code, String message) {
		this.code = code;
		this.message = message;
	}

	/**
	 * 根据错误码查找对应的错误信息
	 * @param code 错误码字符串
	 * @return 对应的错误对象，若未匹配则返回 {@link #OTHERS}
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
	 * 根据错误码查找对应的错误信息，若未找到默认返回 SUCCESS
	 * @param code 错误码字符串
	 * @return 匹配的错误码或默认 SUCCESS
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
	 * 获取错误码描述
	 * @return 错误码 + 错误描述
	 */
	@Override
	public String toString() {
		return "[" + code + "] " + message;
	}

}
