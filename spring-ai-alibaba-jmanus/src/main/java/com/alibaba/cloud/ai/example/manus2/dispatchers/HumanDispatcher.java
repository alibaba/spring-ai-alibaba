package com.alibaba.cloud.ai.example.manus2.dispatchers;

import com.alibaba.cloud.ai.example.manus.contants.NodeConstants;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

import java.util.Arrays;
import java.util.Map;

import static com.alibaba.cloud.ai.example.manus.contants.NodeConstants.*;

public class HumanDispatcher implements EdgeAction {


    @Override
    public String apply(OverAllState overAllState) throws Exception {
        Map<String, Object> feedback = overAllState.humanFeedback().data();
        if (feedback.toString().contains("ACCEPT_PLAN")) {
            return EXECUTOR_ID;
        }
        return PLANNER_ID;
    }
}
