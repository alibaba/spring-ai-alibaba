/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.server;

/**
 * A2A server protocol type enum
 *
 * @author xiweng.yy
 */
public enum ServerTypeEnum {

	JSON_RPC(ServerTypeEnum.JSON_RPC_TYPE), GRPC(ServerTypeEnum.GRPC_TYPE), REST(ServerTypeEnum.REST_TYPE);

	public static final String JSON_RPC_TYPE = "jsonrpc";

	public static final String GRPC_TYPE = "grpc";

	public static final String REST_TYPE = "rest";

	private final String type;

	ServerTypeEnum(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public static ServerTypeEnum valueOfType(String type) {
		for (ServerTypeEnum value : values()) {
			if (value.getType().equals(type)) {
				return value;
			}
		}
		return null;
	}

}
