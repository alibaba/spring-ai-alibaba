package com.alibaba.cloud.ai.example.manus2.nodes;


import com.alibaba.cloud.ai.example.manus.contants.NodeConstants;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphInterruptException;
import com.alibaba.cloud.ai.graph.node.HumanNode;
import com.alibaba.fastjson.JSON;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Map;

public class HumanFeedbackNode extends HumanNode {


    @Override
    public Map<String, Object> apply(OverAllState overAllState) throws GraphInterruptException {
        super.apply(overAllState);
        OverAllState.HumanFeedback humanFeedback = overAllState.humanFeedback();
        if (humanFeedback.data() != null){
            overAllState.updateState(Map.of(NodeConstants.MESSAGES,new UserMessage(humanFeedback.data().get("feedback").toString())));
        }
        return Map.of();
    }
}
