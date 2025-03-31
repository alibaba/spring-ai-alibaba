package com.alibaba.cloud.ai.example.manus.dynamic.agent;

import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

import com.alibaba.cloud.ai.example.manus.agent.ReActAgent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

public class DynamicAgent extends ReActAgent{

    public DynamicAgent(LlmService llmService, PlanExecutionRecorder planExecutionRecorder,
            ManusProperties manusProperties) {
        super(llmService, planExecutionRecorder, manusProperties);
    }

    @Override
    protected boolean think() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'think'");
    }

    @Override
    protected String act() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'act'");
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getName'");
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDescription'");
    }

    @Override
    protected Message getNextStepMessage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNextStepMessage'");
    }

    @Override
    public List<ToolCallback> getToolCallList() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getToolCallList'");
    }
    
}
