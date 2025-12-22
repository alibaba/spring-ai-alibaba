package com.alibaba.cloud.ai.studio.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.elasticsearch")
public class ElasticsearchProperties {

    /**
     * Elasticsearch服务地址
     */
    private String url = "http://localhost:9200";

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectTimeout = 5000;

    /**
     * Socket超时时间（毫秒）
     */
    private Integer socketTimeout = 60000;

    /**
     * 连接池配置
     */
    private ConnectionPool connectionPool = new ConnectionPool();

    @Data
    public static class ConnectionPool {
        /**
         * 最大连接数
         */
        private Integer maxConnections = 100;

        /**
         * 最大空闲连接数
         */
        private Integer maxIdleConnections = 50;

        /**
         * 连接保活时间（毫秒）
         */
        private Long keepAlive = 300000L;
    }
}