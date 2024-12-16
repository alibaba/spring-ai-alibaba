package com.alibaba.cloud.ai.memory.common;


import java.sql.SQLException;
/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */
public class RedisPersistentStorageMemory implements PersistentStorageMemory {
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

}
