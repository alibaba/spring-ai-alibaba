package com.alibaba.cloud.ai.example.manus2.dispatchers;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.cloud.ai.example.manus.contants.NodeConstants.FinalizerConstants;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

import static com.alibaba.cloud.ai.example.manus.contants.NodeConstants.FULL_CONTEXT;

public class PlannerDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState t) throws Exception {
        Boolean fullContext = (Boolean)t.data().get(FULL_CONTEXT);
        if (fullContext != null && fullContext) {
            return FinalizerConstants.NODE_ID;
        }
        return "Human";
    }

}
