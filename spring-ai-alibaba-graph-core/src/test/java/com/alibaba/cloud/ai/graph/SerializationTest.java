package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class SerializationTest {

    @Test
    public void testOverAllStateSerialization() throws Exception {
        PlainTextStateSerializer serializer = new StateGraph.JacksonSerializer();

        OverAllState state = new OverAllState();
        Map<String, Object> data = new HashMap<>();
        data.put("test", "value");
        state.updateState(data);

        byte[] bytes = serializer.objectToBytes(state);
        String json = new String(bytes);
        System.out.println("Serialized JSON:");
        System.out.println(json);

        if (json.contains("keyStrategies")) {
            System.out.println("\n⚠️ WARNING: keyStrategies IS being serialized!");
        } else {
            System.out.println("\n✓ OK: keyStrategies is NOT serialized");
        }
    }
}

