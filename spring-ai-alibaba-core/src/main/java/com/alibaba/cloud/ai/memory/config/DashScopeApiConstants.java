package com.alibaba.cloud.ai.memory.config;
/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */
public class DashScopeApiConstants {
    /**
     * 记忆存储格式-默认为message
     */
    public static final String MESSAGE_MEMORY_TYPE = "message";
    public static final String TOKEN_MEMORY_TYPE = "token";
    public static final String TIMEWINDOW_MEMORY_TYPE = "timewindow";

    /**
     * 持久化类型-默认为memory
     */
    public static final String MEMORY_PERSISTENT_TYPE = "memory";
    public static final String MYSQL_PERSISTENT_TYPE = "mysql";
    public static final String REDIS_PERSISTENT_TYPE = "redis";

}
