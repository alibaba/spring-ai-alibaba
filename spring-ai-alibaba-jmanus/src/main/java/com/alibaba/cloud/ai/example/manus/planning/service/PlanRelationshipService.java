package com.alibaba.cloud.ai.example.manus.planning.service;

import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanRelationship;
import com.alibaba.cloud.ai.example.manus.planning.repository.PlanRelationshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service implementation for managing plan relationships Provides functionality to record
 * and query parent-child relationships between plans
 */
@Service
public class PlanRelationshipService implements IPlanRelationshipService {

	private static final Logger logger = LoggerFactory.getLogger(PlanRelationshipService.class);

	@Autowired
	private PlanRelationshipRepository planRelationshipRepository;

	@Override
	@Transactional
	public void recordPlanRelationship(String parentPlanId, String childPlanId, String rootPlanId,
			String planTemplateId, String relationshipType) {
		try {
			PlanRelationship relationship = new PlanRelationship(parentPlanId, childPlanId, rootPlanId, planTemplateId,
					relationshipType);

			PlanRelationship savedRelationship = planRelationshipRepository.save(relationship);
			logger.info("Recorded plan relationship: parent={}, child={}, root={}, type={}", parentPlanId, childPlanId,
					rootPlanId, relationshipType);

		}
		catch (Exception e) {
			logger.error("Failed to record plan relationship: parent={}, child={}, root={}", parentPlanId, childPlanId,
					rootPlanId, e);
			// Don't throw exception to avoid breaking the main execution flow
		}
	}

	@Override
	public List<PlanRelationship> getChildrenByParentId(String parentPlanId) {
		if (parentPlanId == null) {
			return Collections.emptyList();
		}
		return planRelationshipRepository.findByParentPlanIdOrderByCreatedTimeAsc(parentPlanId);
	}

	@Override
	public List<PlanRelationship> getChildrenByRootId(String rootPlanId) {
		if (rootPlanId == null) {
			return Collections.emptyList();
		}
		return planRelationshipRepository.findByRootPlanIdOrderByCreatedTimeAsc(rootPlanId);
	}

	@Override
	public PlanRelationship getRelationshipByChildId(String childPlanId) {
		if (childPlanId == null) {
			return null;
		}
		return planRelationshipRepository.findByChildPlanId(childPlanId);
	}

	@Override
	public int getPlanDepth(String planId) {
		if (planId == null) {
			return -1;
		}

		int depth = 0;
		PlanRelationship current = getRelationshipByChildId(planId);

		while (current != null && current.getParentPlanId() != null) {
			depth++;
			current = getRelationshipByChildId(current.getParentPlanId());
		}

		return depth;
	}

}
