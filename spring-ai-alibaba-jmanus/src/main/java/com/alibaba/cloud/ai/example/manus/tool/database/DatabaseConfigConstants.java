package com.alibaba.cloud.ai.example.manus.tool.database;

/**
 * 数据库配置常量
 */
public class DatabaseConfigConstants {

	// 配置前缀
	public static final String CONFIG_PREFIX = "database_use.datasource.";

	// 配置属性名
	public static final String PROP_TYPE = "type";

	public static final String PROP_ENABLE = "enable";

	public static final String PROP_URL = "url";

	public static final String PROP_DRIVER_CLASS_NAME = "driver-class-name";

	public static final String PROP_USERNAME = "username";

	public static final String PROP_PASSWORD = "password";

	// 配置值
	public static final String ENABLE_TRUE = "true";

	public static final String ENABLE_FALSE = "false";

	private DatabaseConfigConstants() {
		// 工具类，禁止实例化
	}

}