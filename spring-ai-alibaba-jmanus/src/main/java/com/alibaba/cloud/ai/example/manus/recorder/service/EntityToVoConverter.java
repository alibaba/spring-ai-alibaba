// /*
//  * Copyright 2025 the original author or authors.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      https://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
// package com.alibaba.cloud.ai.example.manus.recorder.service;

// import com.alibaba.cloud.ai.example.manus.recorder.entity.po.*;
// import com.alibaba.cloud.ai.example.manus.recorder.entity.po.PlanExecutionRecordEntity;
// import com.alibaba.cloud.ai.example.manus.recorder.entity.vo.*;
// import org.springframework.stereotype.Component;

// import java.util.List;
// import java.util.stream.Collectors;

// /**
//  * DTO converter service for converting between JPA entities and VO objects.
//  * This service handles the conversion between the persistence layer (PO) and
//  * the service layer (VO) objects.
//  */
// @Component
// public class EntityToVoConverter {

//     /**
//      * Convert PlanExecutionRecordEntity to PlanExecutionRecord VO
//      */
//     public PlanExecutionRecord convertToPlanExecutionRecord(PlanExecutionRecordEntity entity) {
//         if (entity == null) {
//             return null;
//         }

//         PlanExecutionRecord vo = new PlanExecutionRecord();
//         vo.setId(entity.getId());
//         vo.setCurrentPlanId(entity.getCurrentPlanId());
//         vo.setRootPlanId(entity.getRootPlanId());
//         vo.setTitle(entity.getTitle());
//         vo.setUserRequest(entity.getUserRequest());
//         vo.setStartTime(entity.getStartTime());
//         vo.setEndTime(entity.getEndTime());
//         vo.setSteps(entity.getSteps());
//         vo.setCurrentStepIndex(entity.getCurrentStepIndex());
//         vo.setCompleted(entity.isCompleted());
//         vo.setSummary(entity.getSummary());
//         vo.setModelName(entity.getModelName());
//         vo.setUserInputWaitState(entity.getUserInputWaitState());

//         // Convert agent execution sequence
//         if (entity.getAgentExecutionSequence() != null) {
//             List<AgentExecutionRecord> agentVos = entity.getAgentExecutionSequence().stream()
//                     .map(this::convertToAgentExecutionRecord)
//                     .collect(Collectors.toList());
//             vo.setAgentExecutionSequence(agentVos);
//         }

//         return vo;
//     }

//     /**
//      * Convert PlanExecutionRecordEntity list to PlanExecutionRecord VO list
//      */
//     public List<PlanExecutionRecord> convertToPlanExecutionRecordList(List<PlanExecutionRecordEntity> entities) {
//         if (entities == null) {
//             return null;
//         }
//         return entities.stream()
//                 .map(this::convertToPlanExecutionRecord)
//                 .collect(Collectors.toList());
//     }

//     /**
//      * Convert AgentExecutionRecordEntity to AgentExecutionRecord VO
//      */
//     public AgentExecutionRecord convertToAgentExecutionRecord(AgentExecutionRecordEntity entity) {
//         if (entity == null) {
//             return null;
//         }

//         AgentExecutionRecord vo = new AgentExecutionRecord();
//         vo.setId(entity.getId());
//         vo.setConversationId(entity.getConversationId());
//         vo.setAgentName(entity.getAgentName());
//         vo.setAgentDescription(entity.getAgentDescription());
//         vo.setStartTime(entity.getStartTime());
//         vo.setEndTime(entity.getEndTime());
//         vo.setMaxSteps(entity.getMaxSteps());
//         vo.setCurrentStep(entity.getCurrentStep());
//         // Note: Status conversion needs to be handled separately as PO and VO use different ExecutionStatus types
//         vo.setAgentRequest(entity.getAgentRequest());
//         vo.setResult(entity.getResult());
//         vo.setErrorMessage(entity.getErrorMessage());
//         vo.setModelName(entity.getModelName());

//         // Convert think-act steps
//         if (entity.getThinkActSteps() != null) {
//             List<ThinkActRecord> thinkActVos = entity.getThinkActSteps().stream()
//                     .map(this::convertToThinkActRecord)
//                     .collect(Collectors.toList());
//             vo.setThinkActSteps(thinkActVos);
//         }

//         return vo;
//     }

//     /**
//      * Convert AgentExecutionRecordEntity list to AgentExecutionRecord VO list
//      */
//     public List<AgentExecutionRecord> convertToAgentExecutionRecordList(List<AgentExecutionRecordEntity> entities) {
//         if (entities == null) {
//             return null;
//         }
//         return entities.stream()
//                 .map(this::convertToAgentExecutionRecord)
//                 .collect(Collectors.toList());
//     }

//     /**
//      * Convert ThinkActRecordEntity to ThinkActRecord VO
//      */
//     public ThinkActRecord convertToThinkActRecord(ThinkActRecordEntity entity) {
//         if (entity == null) {
//             return null;
//         }

//         ThinkActRecord vo = new ThinkActRecord();
//         vo.setId(entity.getId());
//         vo.setParentExecutionId(entity.getParentExecutionId());
//         vo.setThinkStartTime(entity.getThinkStartTime());
//         vo.setThinkEndTime(entity.getThinkEndTime());
//         vo.setActStartTime(entity.getActStartTime());
//         vo.setActEndTime(entity.getActEndTime());
//         vo.setThinkInput(entity.getThinkInput());
//         vo.setThinkOutput(entity.getThinkOutput());
//         vo.setActionNeeded(entity.isActionNeeded());
//         vo.setActionDescription(entity.getActionDescription());
//         vo.setActionResult(entity.getActionResult());
//         // Note: Status conversion needs to be handled separately as PO and VO use different ExecutionStatus types
//         vo.setErrorMessage(entity.getErrorMessage());
//         vo.setToolName(entity.getToolName());
//         vo.setToolParameters(entity.getToolParameters());
//         // Note: ModelName is not available in ThinkActRecordEntity

//         // Convert act tool info list
//         if (entity.getActToolInfoList() != null) {
//             List<ActToolInfo> actToolInfoVos = entity.getActToolInfoList().stream()
//                     .map(this::convertToActToolInfo)
//                     .collect(Collectors.toList());
//             vo.setActToolInfoList(actToolInfoVos);
//         }

//         // Note: Sub-plan execution record conversion is not implemented as the entity doesn't have this field

//         return vo;
//     }

//     /**
//      * Convert ThinkActRecordEntity list to ThinkActRecord VO list
//      */
//     public List<ThinkActRecord> convertToThinkActRecordList(List<ThinkActRecordEntity> entities) {
//         if (entities == null) {
//             return null;
//         }
//         return entities.stream()
//                 .map(this::convertToThinkActRecord)
//                 .collect(Collectors.toList());
//     }

//     /**
//      * Convert ActToolInfoEntity to ActToolInfo VO
//      */
//     public ActToolInfo convertToActToolInfo(ActToolInfoEntity entity) {
//         if (entity == null) {
//             return null;
//         }

//         ActToolInfo vo = new ActToolInfo();
//         vo.setId(entity.getId() != null ? entity.getId().toString() : null);
//         vo.setName(entity.getName());
//         vo.setParameters(entity.getParameters());
//         vo.setResult(entity.getResult());

//         return vo;
//     }

//     /**
//      * Convert ActToolInfoEntity list to ActToolInfo VO list
//      */
//     public List<ActToolInfo> convertToActToolInfoList(List<ActToolInfoEntity> entities) {
//         if (entities == null) {
//             return null;
//         }
//         return entities.stream()
//                 .map(this::convertToActToolInfo)
//                 .collect(Collectors.toList());
//     }

//     /**
//      * Convert PlanExecutionRecordEntity to PlanExecutionRecordEntity (VO wrapper)
//      */
//     public com.alibaba.cloud.ai.example.manus.recorder.entity.vo.PlanExecutionRecordEntity convertToPlanExecutionRecordEntityVo(
//             PlanExecutionRecordEntity entity) {
//         if (entity == null) {
//             return null;
//         }

//         com.alibaba.cloud.ai.example.manus.recorder.entity.vo.PlanExecutionRecordEntity vo = 
//                 new com.alibaba.cloud.ai.example.manus.recorder.entity.vo.PlanExecutionRecordEntity();
//         vo.setId(entity.getId());
//         vo.setPlanId(entity.getCurrentPlanId());
//         vo.setGmtCreate(java.sql.Date.valueOf(entity.getStartTime().toLocalDate()));
//         vo.setGmtModified(java.sql.Date.valueOf(
//                 entity.getEndTime() != null ? entity.getEndTime().toLocalDate() : entity.getStartTime().toLocalDate()));
        
//         // Convert the nested PlanExecutionRecord
//         vo.setPlanExecutionRecord(convertToPlanExecutionRecord(entity));

//         return vo;
//     }

//     /**
//      * Convert PlanExecutionRecordEntity list to PlanExecutionRecordEntity (VO wrapper) list
//      */
//     public List<com.alibaba.cloud.ai.example.manus.recorder.entity.vo.PlanExecutionRecordEntity> 
//             convertToPlanExecutionRecordEntityVoList(List<PlanExecutionRecordEntity> entities) {
//         if (entities == null) {
//             return null;
//         }
//         return entities.stream()
//                 .map(this::convertToPlanExecutionRecordEntityVo)
//                 .collect(Collectors.toList());
//     }
// }
