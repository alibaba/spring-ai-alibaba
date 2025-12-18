package com.alibaba.cloud.ai.graph.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

public class ReactAgentMutationTest {

    @Test
    public void testMutate() {
        ChatModel chatModel = mock(ChatModel.class);

        ReactAgent agent = ReactAgent.builder()
                .name("original_agent")
                .description("Original Description")
                .model(chatModel)
                .instruction("Original Instruction")
                .build();

        ReactAgent mutatedAgent = agent.mutate()
                .name("mutated_agent")
                .description("Mutated Description")
                .instruction("Mutated Instruction")
                .build();

        assertEquals("original_agent", agent.name());
        assertEquals("Original Description", agent.description());
        assertEquals("Original Instruction", agent.instruction());

        assertEquals("mutated_agent", mutatedAgent.name());
        assertEquals("Mutated Description", mutatedAgent.description());
        assertEquals("Mutated Instruction", mutatedAgent.instruction());

        // Verify model is preserved
        assertNotNull(mutatedAgent.getLlmNode().getChatModel());
        assertEquals(chatModel, mutatedAgent.getLlmNode().getChatModel());
    }
}
