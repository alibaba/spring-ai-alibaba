package com.alibaba.cloud.ai.memory.message.msql;

import com.alibaba.cloud.ai.memory.message.AbstractPersistentStorageMemory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


public class MySQLPersistentStorageMemory implements AbstractPersistentStorageMemory {

    @Override
    public String get(String id) throws SQLException {
        return null;
    }

    @Override
    public int add(String id, String Messages) throws SQLException {
        return 0;
    }

    @Override
    public int delete(String id) throws SQLException {
        return 0;
    }

    @Override
    public int set(String id, String Messages) throws SQLException {
        return 0;
    }

    public void preCheck() throws IOException {
    }

    public Connection getConnection() throws IOException {
        return null;
    }

    public void close() throws IOException {
    }
}
