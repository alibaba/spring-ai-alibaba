package com.alibaba.cloud.ai.example.manus.tool.database;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

@Service
public class DataSourceService {

    private static final Logger log = LoggerFactory.getLogger(DataSourceService.class);

    private final DataSource dataSource;

    // Inject MySQL datasource config from application-database-tool.yml
    public DataSourceService(
            @Value("${database_tool.datasource.url}") String url,
            @Value("${database_tool.datasource.username}") String username,
            @Value("${database_tool.datasource.password}") String password,
            @Value("${database_tool.datasource.driver-class-name}") String driverClassName) {
        // Initialize DriverManagerDataSource with injected config
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        this.dataSource = ds;
        log.info("Initialized DataSourceService with url (database_tool): {}", url);
    }

    /**
     * Get a new SQL connection from the datasource
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Get the underlying DataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Close resources if needed (no-op for DriverManagerDataSource)
     */
    public void close() {
        log.info("Closing DataSourceService resources (no-op)");
    }
} 