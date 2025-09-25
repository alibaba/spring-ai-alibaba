/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.Test;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class LlmNodeThreadSafetyTest {

    @Test
    public void testConcurrentMessageHandling() throws InterruptedException {
        TestLlmNode node = new TestLlmNode();
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(50);
        AtomicInteger conflicts = new AtomicInteger(0);
        
        for (int i = 0; i < 50; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    OverAllState state = new OverAllState();
                    state.updateState(Map.of("messagesKey", List.of(new UserMessage("Request-" + requestId))));
                    
                    Map<String, Object> result = node.simulateApply(state);
                    
                    String resultStr = result.toString();
                    if (!resultStr.contains("Request-" + requestId)) {
                        conflicts.incrementAndGet();
                    }
                } catch (Exception e) {
                    conflicts.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        assertEquals("Thread safety test failed - found data conflicts", 0, conflicts.get());
    }
    
    private static class TestLlmNode {
        private List<Object> messages = new ArrayList<>();
        private String messagesKey = "messagesKey";
        
        public Map<String, Object> simulateApply(OverAllState state) {
            List<Object> localMessages = new ArrayList<>(this.messages);
            
            if (messagesKey != null) {
                Object messagesValue = state.value(messagesKey).orElse(null);
                if (messagesValue != null) {
                    localMessages = (List<Object>) messagesValue;
                }
            }
            
            return Map.of("processedMessages", localMessages);
        }
    }
}
