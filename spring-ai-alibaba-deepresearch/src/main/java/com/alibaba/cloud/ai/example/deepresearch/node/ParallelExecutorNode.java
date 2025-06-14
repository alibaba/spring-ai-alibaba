package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.example.deepresearch.config.DeepResearchProperties;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author sixiyida
 * @since 2025/6/12
 */

public class ParallelExecutorNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(ParallelExecutorNode.class);

    private final DeepResearchProperties properties;
    
    public ParallelExecutorNode(DeepResearchProperties properties) {
        this.properties = properties;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        long currResearcher = 0;
        long currCoder = 0;

        Plan curPlan = StateUtil.getPlan(state);
        for (Plan.Step step : curPlan.getSteps()) {
            // 跳过不需要处理的步骤
            if (StringUtils.hasText(step.getExecutionRes()) ||
                    StringUtils.hasText(step.getExecutionStatus())) {
                continue;
            }

            Plan.StepType stepType = step.getStepType();

            switch (stepType) {
                case PROCESSING:
                    if (areAllResearchStepsCompleted(curPlan)) {
                        step.setExecutionStatus(assignRole(stepType, currCoder));
                        currCoder = (currCoder + 1) % properties.getCoderNodeCount();
                    }
                    logger.info("Waiting for remaining research steps executed");
                    break;

                case RESEARCH:
                    step.setExecutionStatus(assignRole(stepType, currResearcher));
                    currResearcher = (currResearcher + 1) % properties.getResearcherNodeCount();
                    break;

                // 处理其他可能的StepType
                default:
                    logger.debug("Unhandled step type: {}", stepType);
            }
        }
        return Map.of();
    }

    private String assignRole(Plan.StepType type, long executorId) {
        String role = type == Plan.StepType.PROCESSING ? "coder_" : "researcher_";
        return StateUtil.EXECUTION_STATUS_ASSIGNED_PREFIX + role + executorId;
    }

    private boolean areAllResearchStepsCompleted(Plan plan) {
        if (CollectionUtils.isEmpty(plan.getSteps())) {
            return true;
        }

        return plan.getSteps().stream()
                .filter(step -> step.getStepType() == Plan.StepType.RESEARCH)
                .allMatch(step -> step.getExecutionStatus().startsWith(StateUtil.EXECUTION_STATUS_COMPLETED_PREFIX));
    }
}
