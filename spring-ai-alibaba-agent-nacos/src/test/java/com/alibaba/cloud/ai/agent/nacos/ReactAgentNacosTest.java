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

package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

class ReactAgentNacosTest {
    
    
    private Properties properties = null;
    
    @BeforeEach
    void setUp() {
        properties = new Properties();
        properties.put("serverAddr", "127.0.0.1:8848");
        properties.put("namespace", "71ced124-cb8e-4c2e-9ad1-f124a6f77a93");
        //properties.put("accessKey", System.getProperty("accessKey", ""));
        //properties.put("secretKey", System.getProperty("secretKey", ""));
    }
    
    @Test
    public void testReactAgent() throws Exception {
        NacosOptions nacosOptions = new NacosOptions(properties);
        //nacosOptions.setPromptKey("bookprompt2");
        nacosOptions.setAgentId("agent0001");
        nacosOptions.setModelConfigEncrypted(false);
        nacosOptions.setMcpNamespace("71ced124-cb8e-4c2e-9ad1-f124a6f77a93");
        
        ReactAgent agent = ((NacosReactAgentBuilder) ReactAgent.builder(new NacosAgentBuilderFactory())).nacosOptions(
                nacosOptions).name("agent0001").build();
        
        //Thread.sleep(15000L);
        for (int i = 0; i < 20; i++) {
            var runnableConfig = RunnableConfig.builder().threadId(UUID.randomUUID().toString()).build();
            try {
                Optional<OverAllState> result = agent.invoke(
                        Map.of("messages", List.of(new UserMessage("介绍下沈从文。必须给我回答"))), runnableConfig);
                System.out.println(result.get().data());
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                Thread.sleep(5000L);
            }
        }
        
        Thread.sleep(1000000L);
        
    }
    
}