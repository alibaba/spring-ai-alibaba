package com.alibaba.cloud.ai.memory.message;


import java.sql.SQLException;

public interface AbstractPersistentStorageMemory {
    String get(String id) throws SQLException;
    int add(String id,String Messages) throws SQLException;
    int delete(String id) throws SQLException;
    int set(String id,String Messages) throws SQLException;
}