package com.alibaba.cloud.ai.memory.token;

/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */
public interface ChatMemoryByToken {
    void add(String conversationId, String tokens);

    String get(String conversationId, int lastN);

    void clear(String conversationId);

    void clearOverLimit(String conversationId, int maxLimit, int deleteSize);
}