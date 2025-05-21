package com.alibaba.cloud.ai.dbconnector;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractDBConnectionPool implements DBConnectionPool {

	/**
	 * 方言
	 */
	public abstract DatabaseDialectEnum getDialect();

	/**
	 * 驱动
	 */
	public abstract String getDriver();

	/**
	 * 错误信息映射
	 */
	public abstract ErrorCodeEnum errorMapping(String sqlState);

	public ErrorCodeEnum testConnection(DbConfig config) {
		String jdbcUrl = config.getUrl();
		try (Connection connection = DriverManager.getConnection(jdbcUrl, config.getUsername(), config.getPassword());
				Statement stmt = connection.createStatement();) {
			if (BizDataSourceTypeEnum.isPgDialect(config.getConnectionType())) {
				String sql = "SELECT count(*) FROM information_schema.schemata WHERE schema_name = '%s'";
				ResultSet rs = stmt.executeQuery(String.format(sql, config.getSchema()));
				if (rs.next()) {
					int count = rs.getInt(1);
					rs.close();
					if (count == 0) {
						log.info("the specified schema '{}' does not exist.", config.getSchema());
						return ErrorCodeEnum.SCHEMA_NOT_EXIST_3D070;
					}
				}
				rs.close();
			}
			return ErrorCodeEnum.SUCCESS;
		}
		catch (SQLException e) {
			log.error("test db connection error, url:{}, state:{}, message:{}", jdbcUrl, e.getSQLState(),
					e.getMessage());
			return errorMapping(e.getSQLState());
		}
	}

	public Connection getConnection(DbConfig config) {
		String jdbcUrl = config.getUrl();
		DataSource dataSource = null;
		try {
			dataSource = createdDataSource(jdbcUrl, config.getUsername(), config.getPassword());
			return dataSource.getConnection();
		}
		catch (SQLException e) {
			log.error("create db connection error, e:" + e);
			throw new RuntimeException(e);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public DataSource createdDataSource(String url, String username, String password) throws Exception {
		Map map = new HashMap<>();
		map.put(DruidDataSourceFactory.PROP_DRIVERCLASSNAME, getDriver());
		map.put(DruidDataSourceFactory.PROP_URL, url);
		map.put(DruidDataSourceFactory.PROP_USERNAME, username);
		map.put(DruidDataSourceFactory.PROP_PASSWORD, password);
		map.put(DruidDataSourceFactory.PROP_INITIALSIZE, "1");
		map.put(DruidDataSourceFactory.PROP_MINIDLE, "1");
		map.put(DruidDataSourceFactory.PROP_MAXACTIVE, "3");
		map.put(DruidDataSourceFactory.PROP_MAXWAIT, "6000");
		map.put(DruidDataSourceFactory.PROP_TIMEBETWEENEVICTIONRUNSMILLIS, "60000");
		map.put(DruidDataSourceFactory.PROP_FILTERS, "wall,stat");
		DruidDataSource dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(map);
		dataSource.setBreakAfterAcquireFailure(true);
		dataSource.setConnectionErrorRetryAttempts(2);
		return dataSource;
	}

}
