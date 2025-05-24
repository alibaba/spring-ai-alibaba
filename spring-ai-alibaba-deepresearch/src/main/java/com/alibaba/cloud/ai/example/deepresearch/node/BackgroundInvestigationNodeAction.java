package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.model.BackgroundInvestigationType;
import com.alibaba.cloud.ai.graph.action.NodeAction;

/**
 * @author Allen Hu
 * @date 2025/5/24
 */
public interface BackgroundInvestigationNodeAction extends NodeAction {

	BackgroundInvestigationType of();

}
