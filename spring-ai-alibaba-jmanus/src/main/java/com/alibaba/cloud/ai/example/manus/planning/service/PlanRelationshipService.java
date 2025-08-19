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
import java.util.Map;
import java.util.HashMap;

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
	public int recordPlanRelationship(String parentPlanId, String childPlanId, String rootPlanId,
			String planTemplateId, String relationshipType) {
		try {
			PlanRelationship relationship = new PlanRelationship(parentPlanId, childPlanId, rootPlanId, planTemplateId,
					relationshipType);

			PlanRelationship savedRelationship = planRelationshipRepository.save(relationship);
			logger.info("Recorded plan relationship: parent={}, child={}, root={}, type={}", parentPlanId, childPlanId,
					rootPlanId, relationshipType);

			// Calculate and return the depth of the newly created plan using the optimized method
			int depth = getPlanDepth(childPlanId);
			logger.debug("Plan {} recorded at depth {}", childPlanId, depth);
			return depth;

		}
		catch (Exception e) {
			logger.error("Failed to record plan relationship: parent={}, child={}, root={}", parentPlanId, childPlanId,
					rootPlanId, e);
			// Don't throw exception to avoid breaking the main execution flow
			return -1; // Return -1 to indicate failure
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

		// First, get the relationship for the target plan to find its rootPlanId
		PlanRelationship targetRelationship = getRelationshipByChildId(planId);
		if (targetRelationship == null) {
			return -1; // Plan not found
		}

		String rootPlanId = targetRelationship.getRootPlanId();
		
		// Get all relationships for this root plan in one database call
		List<PlanRelationship> allRelationships = getChildrenByRootId(rootPlanId);
		
		// Build a map for efficient parent-child lookup
		Map<String, String> parentChildMap = new HashMap<>();
		for (PlanRelationship rel : allRelationships) {
			parentChildMap.put(rel.getChildPlanId(), rel.getParentPlanId());
		}
		
		// Calculate depth using the in-memory map
		int depth = 0;
		String currentPlanId = planId;
		
		while (parentChildMap.containsKey(currentPlanId) && parentChildMap.get(currentPlanId) != null) {
			depth++;
			currentPlanId = parentChildMap.get(currentPlanId);
		}
		
		return depth;
	}
}
