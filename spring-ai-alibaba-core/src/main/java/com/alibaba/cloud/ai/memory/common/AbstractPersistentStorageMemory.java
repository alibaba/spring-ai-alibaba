package com.alibaba.cloud.ai.memory.common;
/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */

public interface AbstractPersistentStorageMemory {
    String get(String id) throws Exception;
    int delete(String id) throws Exception;
    int add(String id,String Messages) throws Exception;
    int set(String id,String Messages) throws Exception;
}