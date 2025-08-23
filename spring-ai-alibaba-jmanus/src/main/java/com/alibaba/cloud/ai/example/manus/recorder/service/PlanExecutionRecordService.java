package com.alibaba.cloud.ai.example.manus.recorder.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.cloud.ai.example.manus.recorder.entity.po.ThinkActRecordEntity;
import com.alibaba.cloud.ai.example.manus.recorder.entity.po.ExecutionStatus;
import com.alibaba.cloud.ai.example.manus.recorder.entity.vo.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.vo.ThinkActRecord;
import com.alibaba.cloud.ai.example.manus.recorder.repository.PlanExecutionRecordRepository;
import com.alibaba.cloud.ai.example.manus.recorder.repository.ThinkActRecordRepository;

import java.util.List;

@Component
public class PlanExecutionRecordService {

    @Autowired
    private PlanExecutionRecordRepository planExecutionRecordRepository;

    @Autowired
    private ThinkActRecordRepository thinkActRecordRepository;

    @Autowired
    private EntityToVoConverter entityToVoConverter;

    /**
     * Save plan execution record (Note: This method needs to be updated to work with PO entities)
     */
    public void savePlanExecutionRecord(PlanExecutionRecord planExecutionRecord) {
        // TODO: This method needs to be updated to convert VO to PO and save
        // For now, it's commented out as it requires proper entity conversion
        // planExecutionRecordRepository.save(planExecutionRecord);
    }

    /**
     * Record the start of thinking phase
     * @param parentExecutionId The ID of the parent agent execution record
     * @param thinkInput The input context for thinking
     * @return The created ThinkActRecordEntity ID
     */
    @Transactional
    public Long recordThinkStart(Long parentExecutionId, String thinkInput) {
        ThinkActRecordEntity thinkActRecord = new ThinkActRecordEntity(parentExecutionId);
        thinkActRecord.startThinking(thinkInput);
        
        ThinkActRecordEntity savedRecord = thinkActRecordRepository.save(thinkActRecord);
        return savedRecord.getId();
    }

    /**
     * Record the completion of thinking phase
     * @param thinkActRecordId The ID of the think-act record
     * @param thinkOutput The output result of thinking
     * @return The updated ThinkActRecordEntity
     */
    @Transactional
    public ThinkActRecordEntity recordThinkFinish(Long thinkActRecordId, String thinkOutput) {
        ThinkActRecordEntity thinkActRecord = thinkActRecordRepository.findById(thinkActRecordId)
                .orElseThrow(() -> new IllegalArgumentException("ThinkActRecord not found with ID: " + thinkActRecordId));
        
        thinkActRecord.finishThinking(thinkOutput);
        return thinkActRecordRepository.save(thinkActRecord);
    }

    /**
     * Record the start of action phase
     * @param thinkActRecordId The ID of the think-act record
     * @param actionDescription Description of the action to be taken
     * @param toolName Name of the tool to be used
     * @param toolParameters Parameters for the tool
     * @return The updated ThinkActRecordEntity
     */
    @Transactional
    public ThinkActRecordEntity recordActStart(Long thinkActRecordId, String actionDescription, String toolName, String toolParameters) {
        ThinkActRecordEntity thinkActRecord = thinkActRecordRepository.findById(thinkActRecordId)
                .orElseThrow(() -> new IllegalArgumentException("ThinkActRecord not found with ID: " + thinkActRecordId));
        
        thinkActRecord.startAction(actionDescription, toolName, toolParameters);
        return thinkActRecordRepository.save(thinkActRecord);
    }

    /**
     * Record the completion of action phase
     * @param thinkActRecordId The ID of the think-act record
     * @param actionResult The result of the action execution
     * @param status The execution status
     * @return The updated ThinkActRecordEntity
     */
    @Transactional
    public ThinkActRecordEntity recordActFinish(Long thinkActRecordId, String actionResult, String status) {
        ThinkActRecordEntity thinkActRecord = thinkActRecordRepository.findById(thinkActRecordId)
                .orElseThrow(() -> new IllegalArgumentException("ThinkActRecord not found with ID: " + thinkActRecordId));
        
        // Convert string status to ExecutionStatus enum
        ExecutionStatus executionStatus = ExecutionStatus.valueOf(status.toUpperCase());
        thinkActRecord.finishAction(actionResult, executionStatus);
        return thinkActRecordRepository.save(thinkActRecord);
    }

    /**
     * Record error information for a think-act record
     * @param thinkActRecordId The ID of the think-act record
     * @param errorMessage The error message to record
     * @return The updated ThinkActRecordEntity
     */
    @Transactional
    public ThinkActRecordEntity recordError(Long thinkActRecordId, String errorMessage) {
        ThinkActRecordEntity thinkActRecord = thinkActRecordRepository.findById(thinkActRecordId)
                .orElseThrow(() -> new IllegalArgumentException("ThinkActRecord not found with ID: " + thinkActRecordId));
        
        thinkActRecord.recordError(errorMessage);
        return thinkActRecordRepository.save(thinkActRecord);
    }

    /**
     * Get a think-act record by ID and convert to VO
     * @param thinkActRecordId The ID of the think-act record
     * @return The ThinkActRecord VO object, or null if not found
     */
    @Transactional(readOnly = true)
    public ThinkActRecord getThinkActRecord(Long thinkActRecordId) {
        ThinkActRecordEntity entity = thinkActRecordRepository.findById(thinkActRecordId).orElse(null);
        return entityToVoConverter.convertToThinkActRecord(entity);
    }

    /**
     * Get all think-act records for a parent execution and convert to VO list
     * @param parentExecutionId The ID of the parent agent execution record
     * @return List of ThinkActRecord VO objects
     */
    @Transactional(readOnly = true)
    public List<ThinkActRecord> getThinkActRecordsByParentExecutionId(Long parentExecutionId) {
        List<ThinkActRecordEntity> entities = thinkActRecordRepository.findByParentExecutionIdOrderByThinkStartTimeAsc(parentExecutionId);
        return entityToVoConverter.convertToThinkActRecordList(entities);
    }
}
