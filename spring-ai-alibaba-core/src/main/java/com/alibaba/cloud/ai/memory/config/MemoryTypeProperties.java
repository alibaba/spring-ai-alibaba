package com.alibaba.cloud.ai.memory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import static com.alibaba.cloud.ai.memory.config.DashScopeApiConstants.MESSAGE_MEMORY_TYPE;
/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */
@ConfigurationProperties(MemoryTypeProperties.CONFIG_PREFIX)
public class MemoryTypeProperties {
    public static final String CONFIG_PREFIX = "spring.ai.memory";

    private String memorytype;
    private String persistenttype;

    public MemoryTypeProperties() {
        memorytype = MESSAGE_MEMORY_TYPE;
        persistenttype = MESSAGE_MEMORY_TYPE;
    }

    public String getMemoryType() {
        return memorytype;
    }

    public void setMemoryType(String memorytype) {
        this.memorytype = memorytype;
    }

    public String getPersistentType() {
        return persistenttype;
    }

    public void setPersistentType(String persistenttype) {
        this.persistenttype = persistenttype;
    }
}
