package com.alibaba.cloud.ai.graph.node.llm;

import com.alibaba.cloud.ai.graph.NodeActionDescriptor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class LLMNodeActionDescriptor extends NodeActionDescriptor {

    private ChatOptions chatOptions;

    private List<PromptTemplate> promptTemplates;

    private List<String> functionNames;

}
