package com.alibaba.cloud.ai.graph.serializer.state;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.fastjson.JSON;
import lombok.NonNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class OverAllStateSerializer extends StateSerializer<OverAllState> {

    public OverAllStateSerializer(@NonNull AgentStateFactory<OverAllState> stateFactory) {
        super(stateFactory);
    }

    @Override
    public void write(OverAllState object, ObjectOutput out) throws IOException {
        if (object == null || out == null) {
            throw new IllegalArgumentException("Input parameters cannot be null");
        }
        out.writeObject(JSON.toJSONString(object));
    }

    @Override
    public OverAllState read(ObjectInput in) throws IOException, ClassNotFoundException {
        if (in == null) {
            throw new IllegalArgumentException("Input parameter cannot be null");
        }
        try {
            String json = (String) in.readObject();
            if (json == null) {
                throw new IllegalStateException("Deserialized object is null");
            }
            OverAllState state = JSON.parseObject(json, OverAllState.class);
            if (state == null) {
                throw new IllegalStateException("Deserialized object is null");
            }
            return state;
        } catch (ClassNotFoundException | IOException e) {
            throw new IOException("Failed to deserialize OverAllState", e);
        }
    }
}
