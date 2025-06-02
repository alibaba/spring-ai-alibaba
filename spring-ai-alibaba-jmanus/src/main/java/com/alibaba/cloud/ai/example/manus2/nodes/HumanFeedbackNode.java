/*
 * Copyright 2025 the original author or authors.
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
