package com.alibaba.cloud.ai.example.deepresearch.dispatcher;

import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
import com.alibaba.cloud.ai.example.deepresearch.node.CoderNode;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xiaoyuntao
 * @since 2025/06/11
 */
public class ResearcherDispatcher implements EdgeAction {

    private static final Logger logger = LoggerFactory.getLogger(ResearcherDispatcher.class);

    @Override
    public String apply(OverAllState state) throws Exception {
        String result = state.value("researcher_content", "");
        List<String> observations = StateUtil.getMessagesByType(state, "observations");
        Plan currentPlan = StateUtil.getPlan(state);

        Plan.Step unexecutedStep = null;
        for (Plan.Step step : currentPlan.getSteps()) {
            if (Plan.StepType.RESEARCH.equals(step.getStepType()) && !StringUtils.hasText(step.getExecutionRes())) {
                unexecutedStep = step;
                break;
            }
        }
        unexecutedStep.setExecutionRes(result);

        logger.info("researcher Node response: {}", result);
        Map<String, Object> updated = new HashMap<>();
        observations.add(result);
        updated.put("observations", observations);
        state.input(updated);
        return "research_team";
    }
}
