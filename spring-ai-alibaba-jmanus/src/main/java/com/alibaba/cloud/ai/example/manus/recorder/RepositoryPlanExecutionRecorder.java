package com.alibaba.cloud.ai.example.manus.recorder;

import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecordEntity;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;
import com.alibaba.cloud.ai.example.manus.recorder.repository.PlanExecutionRecordRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存PlanExecutionRecorder 无法在分布式环境下使用，因此需要使用DB持久化PlanExecutionRecord
 *
 * 解决的问题可参考: https://github.com/alibaba/spring-ai-alibaba/issues/1391
 */
public class RepositoryPlanExecutionRecorderV2 implements PlanExecutionRecorder {

	private static final Logger logger = LoggerFactory.getLogger(DefaultPlanExecutionRecorder.class);

	private final AtomicLong agentExecutionIdGenerator = new AtomicLong(0);

	@Resource
	private PlanExecutionRecordRepository planExecutionRecordRepository;

	/**
	 * Find ThinkActRecord by parent plan ID and think-act record ID
	 * @param parentPlanId Parent plan ID
	 * @param thinkActRecordId Think-act record ID
	 * @return ThinkActRecord if found, null otherwise
	 */
	private ThinkActRecord findThinkActRecord(String parentPlanId, Long thinkActRecordId) {
		if (parentPlanId == null || thinkActRecordId == null) {
			return null;
		}

		PlanExecutionRecord parentPlan = getExecutionRecord(parentPlanId);
		if (parentPlan != null) {
			for (AgentExecutionRecord agentRecord : parentPlan.getAgentExecutionSequence()) {
				for (ThinkActRecord thinkActRecord : agentRecord.getThinkActSteps()) {
					if (thinkActRecordId.equals(thinkActRecord.getId())) {
						return thinkActRecord;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Get or create PlanExecutionRecord by planId and optional thinkActRecordId
	 * @param planId Plan ID
	 * @param rootPlanId Root plan ID
	 * @param thinkActRecordId Think-act record ID that contains the sub-plan (null for
	 * main plan)
	 * @param createIfNotExists Whether to create if not exists
	 * @return PlanExecutionRecord instance
	 */
	public PlanExecutionRecord getOrCreatePlanExecutionRecord(String planId, String rootPlanId, Long thinkActRecordId,
			boolean createIfNotExists) {

		// Add detailed logging to debug NPE
		logger.info(
				"Enter getOrCreatePlanExecutionRecord with planId: {}, rootPlanId: {}, thinkActRecordId: {}, createIfNotExists: {}",
				planId, rootPlanId, thinkActRecordId, createIfNotExists);

		if (rootPlanId == null) {
			logger.error("rootPlanId is null, which will cause NPE. PlanId: {}, thinkActRecordId: {}.", planId,
					thinkActRecordId);
			// For further debugging, log the stack trace to understand the call path
			// throw new IllegalArgumentException("rootPlanId cannot be null");
		}

		// Get or create root plan record first
		PlanExecutionRecord rootRecord = getExecutionRecord(rootPlanId);
		if (rootRecord == null) {
			logger.info("Creating root plan with ID: {}", rootPlanId);
			rootRecord = new PlanExecutionRecord(rootPlanId, rootPlanId);
			saveExecutionRecord(rootRecord);
		}

		// If no thinkActRecordId, return root record directly
		if (thinkActRecordId == null) {
			return rootRecord;
		}

		// Find ThinkActRecord in root plan
		ThinkActRecord thinkActRecord = findThinkActRecord(rootPlanId, thinkActRecordId);
		if (thinkActRecord == null) {
			return rootRecord;
		}

		// Check if subPlanExecutionRecord exists
		PlanExecutionRecord subPlan = thinkActRecord.getSubPlanExecutionRecord();
		if (subPlan == null && createIfNotExists) {
			// Create new sub-plan with planId and rootPlanId
			subPlan = new PlanExecutionRecord(planId, rootPlanId);
			subPlan.setThinkActRecordId(thinkActRecordId);
			thinkActRecord.recordSubPlanExecution(subPlan);
			// update sub plan
			updateThinkActRecord(rootRecord, thinkActRecord);
			saveExecutionRecord(rootRecord);
		}

		return subPlan != null ? subPlan : rootRecord;
	}

	/**
	 * Get or create PlanExecutionRecord by planId and optional thinkActRecordId (creates
	 * if not exists)
	 * @param planId Plan ID
	 * @param thinkActRecordId Think-act record ID that contains the sub-plan (null for
	 * main plan)
	 * @return PlanExecutionRecord instance
	 */
	public PlanExecutionRecord getOrCreatePlanExecutionRecord(String planId, String rootPlanId, Long thinkActRecordId) {
		return getOrCreatePlanExecutionRecord(planId, rootPlanId, thinkActRecordId, true);
	}

	@Override
	public String recordPlanExecution(PlanExecutionRecord stepRecord) {
		String planId = stepRecord.getCurrentPlanId();
		String parentPlanId = stepRecord.getRootPlanId();
		Long thinkActRecordId = stepRecord.getThinkActRecordId();

		if (parentPlanId != null && thinkActRecordId != null) {
			// This is a sub-plan, need to attach it to the corresponding think-act record
			ThinkActRecord thinkActRecord = findThinkActRecord(parentPlanId, thinkActRecordId);
			if (thinkActRecord != null) {
				// Found the corresponding think-act record, attach the sub-plan
				thinkActRecord.recordSubPlanExecution(stepRecord);

				// update sub plan
				PlanExecutionRecordEntity entity = planExecutionRecordRepository.findByPlanId(parentPlanId);
				assert entity != null && entity.getPlanExecutionRecord() != null;
				PlanExecutionRecord rootRecord = entity.getPlanExecutionRecord();
				updateThinkActRecord(rootRecord, thinkActRecord);
				entity.setGmtModified(new Date());
				planExecutionRecordRepository.save(entity);
			}
		}
		else {
			// This is a main plan
			saveExecutionRecord(stepRecord);
		}

		return planId;
	}

	/**
	 * Record agent execution with PlanExecutionRecord parameter
	 * @param planExecutionRecord Plan execution record
	 * @param agentRecord Agent execution record
	 * @return Agent execution ID
	 */
	@Override
	public Long recordAgentExecution(PlanExecutionRecord planExecutionRecord, AgentExecutionRecord agentRecord) {
		Long agentExecutionId = agentExecutionIdGenerator.incrementAndGet();
		agentRecord.setId(agentExecutionId);
		if (planExecutionRecord != null) {
			planExecutionRecord.addAgentExecutionRecord(agentRecord);

			saveExecutionRecord(planExecutionRecord);
		}
		return agentExecutionId;
	}

	/**
	 * Record think-act execution with PlanExecutionRecord parameter
	 * @param planExecutionRecord Plan execution record
	 * @param agentExecutionId Agent execution ID
	 * @param thinkActRecord Think-act record
	 */
	@Override
	public void recordThinkActExecution(PlanExecutionRecord planExecutionRecord, Long agentExecutionId,
			ThinkActRecord thinkActRecord) {
		if (planExecutionRecord != null) {
			for (AgentExecutionRecord agentRecord : planExecutionRecord.getAgentExecutionSequence()) {
				if (agentExecutionId.equals(agentRecord.getId())) {
					agentRecord.addThinkActStep(thinkActRecord);

					updateThinkActRecord(planExecutionRecord, thinkActRecord);
					saveExecutionRecord(planExecutionRecord);
					break;
				}
			}
		}
	}

	/**
	 * Record plan completion with PlanExecutionRecord parameter
	 * @param planExecutionRecord Plan execution record
	 * @param summary Execution summary
	 */
	@Override
	public void recordPlanCompletion(PlanExecutionRecord planExecutionRecord, String summary) {
		if (planExecutionRecord != null) {
			planExecutionRecord.complete(summary);

			saveExecutionRecord(planExecutionRecord);
		}
	}

	@Override
	public PlanExecutionRecord getExecutionRecord(String planId, String rootPlanId, Long thinkActRecordId) {
		return getOrCreatePlanExecutionRecord(planId, rootPlanId, thinkActRecordId, false);
	}

	/**
	 * Save execution records of the specified plan ID to persistent storage. This method
	 * will recursively call save methods of PlanExecutionRecord, AgentExecutionRecord and
	 * ThinkActRecord
	 * @param rootPlanId Plan ID to save
	 * @return Returns true if record is found and saved, false otherwise
	 */
	@Override
	public boolean savePlanExecutionRecords(String rootPlanId) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Save all execution records to persistent storage. This method will iterate through
	 * all plan records and call their save methods
	 */
	@Override
	public void saveAllExecutionRecords() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Delete execution record of the specified plan ID
	 * @param planId Plan ID to delete
	 */
	@Override
	public void removeExecutionRecord(String planId) {
		planExecutionRecordRepository.deleteByPlanId(planId);
	}

	/**
	 * Get all plan execution records
	 * @return Map of all plan records
	 */
	public Map<String, PlanExecutionRecord> getAllPlanRecords() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Check if a plan execution record exists
	 * @param planId Plan ID to check
	 * @return true if exists, false otherwise
	 */
	public boolean hasPlanExecutionRecord(String planId) {
		return planExecutionRecordRepository.findByPlanId(planId) != null;
	}

	/**
	 * Get current agent execution record for a specific plan execution record
	 * @param planExecutionRecord Plan execution record
	 * @return Current active agent execution record, or null if none exists
	 */
	@Override
	public AgentExecutionRecord getCurrentAgentExecutionRecord(PlanExecutionRecord planExecutionRecord) {
		if (planExecutionRecord != null) {
			List<AgentExecutionRecord> agentExecutionSequence = planExecutionRecord.getAgentExecutionSequence();
			Integer currentIndex = planExecutionRecord.getCurrentStepIndex();
			if (!agentExecutionSequence.isEmpty() && currentIndex != null
					&& currentIndex < agentExecutionSequence.size()) {
				return agentExecutionSequence.get(currentIndex);
			}
		}
		return null;
	}

	private PlanExecutionRecord getExecutionRecord(String rootPlanId) {
		PlanExecutionRecordEntity entity = planExecutionRecordRepository.findByPlanId(rootPlanId);
		return entity != null ? entity.getPlanExecutionRecord() : null;
	}

	private void saveExecutionRecord(PlanExecutionRecord planExecutionRecord) {
		PlanExecutionRecordEntity entity = planExecutionRecordRepository
			.findByPlanId(planExecutionRecord.getRootPlanId());
		if (entity == null) {
			entity = new PlanExecutionRecordEntity();
			entity.setPlanId(planExecutionRecord.getRootPlanId());
			entity.setGmtCreate(new Date());
		}

		entity.setPlanExecutionRecord(planExecutionRecord);
		entity.setGmtModified(new Date());

		planExecutionRecordRepository.save(entity);
	}

	private void updateThinkActRecord(PlanExecutionRecord parentPlan, ThinkActRecord record) {
		if (parentPlan != null) {
			for (AgentExecutionRecord agentRecord : parentPlan.getAgentExecutionSequence()) {
				for (ThinkActRecord thinkActRecord : agentRecord.getThinkActSteps()) {
					if (record.getId().equals(thinkActRecord.getId())) {
						BeanUtils.copyProperties(record, thinkActRecord);
					}
				}
			}
		}
	}

}
