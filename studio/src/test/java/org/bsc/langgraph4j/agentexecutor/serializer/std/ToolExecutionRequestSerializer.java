package org.bsc.langgraph4j.agentexecutor.serializer.std;

import com.alibaba.cloud.ai.graph.serializer.Serializer;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ToolExecutionRequestSerializer implements Serializer<ToolExecutionRequest> {

    @Override
    public void write(ToolExecutionRequest object, ObjectOutput out) throws IOException {
        out.writeUTF( object.id() );
        out.writeUTF( object.name() );
        out.writeUTF( object.arguments() );


    }

    @Override
    public ToolExecutionRequest read(ObjectInput in) throws IOException, ClassNotFoundException {
        return ToolExecutionRequest.builder()
                .id(in.readUTF())
                .name(in.readUTF())
                .arguments(in.readUTF())
                .build();
    }
}
