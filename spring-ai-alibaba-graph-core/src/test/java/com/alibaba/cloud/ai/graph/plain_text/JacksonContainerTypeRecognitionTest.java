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
package com.alibaba.cloud.ai.graph.plain_text;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for Jackson generic container deserialization to verify
 * recognition of both "@type" and "@class" markers when restoring nested elements.
 */
class JacksonContainerTypeRecognitionTest {

    private SpringAIJacksonStateSerializer serializer;

    @BeforeEach
    void setUp() {
        AgentStateFactory<OverAllState> stateFactory = OverAllState::new;
        serializer = new SpringAIJacksonStateSerializer(stateFactory);
}



// EOF


    @Test
    void deserializeFromTypePropertyInMap() throws Exception {
        // JSON value uses explicit @type mapping (SYSTEM -> SystemMessage)
        String json = "{" +
                "\"msg\": {" +
                "\"@type\": \"SYSTEM\"," +
                "\"text\": \"hello type mapping\"," +
                "\"metadata\": { \"k\": \"v\" }" +
                "}" +
                "}";

        ObjectMapper mapper = serializer.objectMapper();
        Map<String, Object> data = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

        assertNotNull(data);
        assertTrue(data.get("msg") instanceof SystemMessage);
        SystemMessage sm = (SystemMessage) data.get("msg");
        assertEquals("hello type mapping", sm.getText());
        assertEquals("v", sm.getMetadata().get("k"));
    }

    @Test
    void deserializeFromClassPropertyInList() throws Exception {
        // JSON value uses Jackson default typing marker @class for list elements
        String json = "{" +
                "\"messages\": [" +
                "{" +
                "\"@class\": \"org.springframework.ai.chat.messages.UserMessage\"," +
                "\"text\": \"hi class typing\"," +
                "\"metadata\": { \"foo\": \"bar\" }" +
                "}" +
                "]" +
                "}";

        ObjectMapper mapper = serializer.objectMapper();
        Map<String, Object> data = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

        assertNotNull(data);
        assertTrue(data.get("messages") instanceof List<?>);
        List<?> list = (List<?>) data.get("messages");
        assertEquals(1, list.size());
        assertTrue(list.get(0) instanceof UserMessage);
        UserMessage um = (UserMessage) list.get(0);
        assertEquals("hi class typing", um.getText());
        assertEquals("bar", um.getMetadata().get("foo"));
    }

    @Test
    void defaultTypingPreservesCustomPojo() throws Exception {
        // Insert a custom POJO into state; default typing should preserve type via @class
        SamplePojo pojo = new SamplePojo();
        pojo.name = "neo";
        pojo.age = 42;

        Map<String, Object> data = new HashMap<>();
        data.put("pojo", pojo);

        // Round-trip through serializer writeData/readData
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            serializer.writeData(data, oos);
            oos.flush();
        }

        Map<String, Object> restored;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            restored = serializer.readData(ois);
        }

        assertNotNull(restored);
        assertTrue(restored.get("pojo") instanceof SamplePojo);
        SamplePojo r = (SamplePojo) restored.get("pojo");
        assertEquals("neo", r.name);
        assertEquals(42, r.age);
    }

    @Test
    void fallbackToGenericObjectWhenTypeAbsent() throws Exception {
        // No @type or @class -> should degrade to generic Map structure
        String json = "{" +
                "\"unknown\": { \"field\": \"value\" }" +
                "}";

        ObjectMapper mapper = serializer.objectMapper();
        Map<String, Object> data = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

        assertNotNull(data);
        assertTrue(data.get("unknown") instanceof Map<?,?>);
        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) data.get("unknown");
        assertEquals("value", inner.get("field"));
    }

    /**
     * Simple non-final POJO with public fields to let Jackson bind without setters.
     */
    static class SamplePojo {
        public String name;
        public int age;
    }
}
