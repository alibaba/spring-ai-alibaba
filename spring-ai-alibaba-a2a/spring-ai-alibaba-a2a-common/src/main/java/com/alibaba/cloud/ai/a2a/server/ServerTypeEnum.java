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
