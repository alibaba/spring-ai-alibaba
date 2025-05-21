package com.alibaba.cloud.ai.dbconnector;

import java.sql.Connection;

public interface DBConnectionPool {

	public ErrorCodeEnum testConnection(DbConfig config);

	public Connection getConnection(DbConfig config);

}