package com.alibaba.cloud.ai.connector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DB连接池上下文
 */
@Component
public class DBConnectionPoolContext {

    private final Map<String, DBConnectionPool> poolMap = new HashMap<>();

    @Autowired
    public DBConnectionPoolContext(List<DBConnectionPool> pools) {
        for (DBConnectionPool pool : pools) {
            // 获取类上的 @Service("xxx")的Bean名
            String beanName = pool.getClass().getAnnotation(Service.class).value();
            poolMap.put(beanName, pool);
        }
    }

    /**
     * 根据数据库类型获取对应的DB连接池
     *
     * @param type 数据库类型
     * @return DB连接池
     */
    public DBConnectionPool getPoolByType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        return switch (type.toLowerCase()) {
            case "mysql", "mysqljdbcconnectionpool" -> poolMap.get("mysqlJdbcConnectionPool");
            case "postgresql", "postgres", "postgresqljdbcconnectionpool" -> poolMap.get("postgreSqlJdbcConnectionPool");
            default -> null;
        };
    }
}
