package com.alibaba.cloud.ai.memory.token.momory;

import com.alibaba.cloud.ai.memory.token.ChatMemoryByToken;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */
public class InMemoryChatMemoryByToken implements ChatMemoryByToken {
    Map<String, String> conversationHistory = new ConcurrentHashMap<>();

    public InMemoryChatMemoryByToken() {
    }

    @Override
    public void add(String conversationId, String tokens) {
        this.conversationHistory.putIfAbsent(conversationId, "");
        final String updateHistoryToken = (this.conversationHistory.get(conversationId)) + tokens;
        this.conversationHistory.put(conversationId, updateHistoryToken);
    }

    @Override
    public String get(String conversationId, int lastN) {
        String historyToken = this.conversationHistory.get(conversationId);
        return historyToken.length() > lastN ? historyToken.substring(historyToken.length() - lastN) : historyToken;
    }

    @Override
    public void clear(String conversationId) {
        this.conversationHistory.remove(conversationId);
    }

    @Override
    public void clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
        String historyToken = this.conversationHistory.get(conversationId);
        if (historyToken.length() > maxLimit) {
            this.conversationHistory.put(conversationId, historyToken.substring(maxLimit - deleteSize));
        }
    }
}
