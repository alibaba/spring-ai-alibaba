package com.alibaba.cloud.ai.example.deepresearch.dispatcher;

import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
import com.alibaba.cloud.ai.example.deepresearch.node.CoderNode;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xiaoyuntao
 * @since 2025/06/11
 */
public class CoderDispatcher implements EdgeAction {

    private static final Logger logger = LoggerFactory.getLogger(CoderNode.class);

    @Override
    public String apply(OverAllState state) throws Exception {
        String coderContent = state.value("coder_content", "");
        Plan currentPlan = StateUtil.getPlan(state);
        List<String> observations = StateUtil.getMessagesByType(state, "observations");
        Plan.Step unexecutedStep = null;
        for (Plan.Step step : currentPlan.getSteps()) {
            if (step.getStepType().equals(Plan.StepType.PROCESSING) && step.getExecutionRes() == null) {
                unexecutedStep = step;
                break;
            }
        }
        unexecutedStep.setExecutionRes(coderContent);

        logger.info("coder Node result: {}", coderContent);
        Map<String, Object> updated = new HashMap<>();
        observations.add(coderContent);
        updated.put("observations", observations);
        state.input(updated);
        return "research_team";
    }
}
