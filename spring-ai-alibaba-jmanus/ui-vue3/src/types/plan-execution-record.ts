/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * TypeScript interfaces matching Java backend data structures
 *
 * Hierarchy:
 * PlanExecutionRecord (Main)
 *   └── agentExecutionSequence: AgentExecutionRecord[]
 *       └── subPlanExecutionRecords: PlanExecutionRecord[] (recursive)
 */

/**
 * Execution status enum matching Java ExecutionStatus
 * Maps to Java: com.alibaba.cloud.ai.manus.recorder.entity.vo.ExecutionStatus
 */
export type ExecutionStatus = 'IDLE' | 'RUNNING' | 'FINISHED'

/**
 * Maps to Java: com.alibaba.cloud.ai.manus.recorder.entity.vo.ActToolInfo
 */
export interface ActToolInfo {
  /** Name of the tool */
  name?: string

  /** Description of the tool */
  parameters?: string

  /** Result of tool call */
  result?: string

  /** ID of the tool */
  id?: string
}

/**
 * Maps to Java: com.alibaba.cloud.ai.manus.recorder.entity.ThinkActRecord
 *
 * Records the thinking and action process of an agent in a single execution step.
 */
export interface ThinkActRecord {
  /** Unique identifier of the record */
  id?: number

  /** ID of parent execution record, linked to AgentExecutionRecord */
  parentExecutionId?: number

  /** Timestamp when thinking started */
  thinkStartTime?: string

  /** Timestamp when thinking completed */
  thinkEndTime?: string

  /** Timestamp when action started */
  actStartTime?: string

  /** Timestamp when action completed */
  actEndTime?: string

  /** Input context for the thinking process */
  thinkInput?: string

  /** Output result of the thinking process */
  thinkOutput?: string

  /** Whether thinking determined that action is needed */
  actionNeeded?: boolean

  /** Description of the action to be taken */
  actionDescription?: string

  /** Result of action execution */
  actionResult?: string

  /** Status of this think-act cycle */
  status?: ExecutionStatus

  /** Error message if the cycle encountered problems */
  errorMessage?: string

  /** Tool name used for action (if applicable) */
  toolName?: string

  /** Tool parameters used for action (serialized, if applicable) */
  toolParameters?: string

  /** Tool call information */
  actToolInfoList?: ActToolInfo[]

  /** Sub-plan execution record for tool calls that create new execution plans */
  subPlanExecutionRecord?: PlanExecutionRecord
}

/**
 * Maps to Java: com.alibaba.cloud.ai.manus.recorder.entity.vo.AgentExecutionRecord
 *
 * Unified agent execution record class for tracking and recording detailed information about
 * BaseAgent execution process. This class combines the functionality of both Simple and Detail
 * versions to provide a single, comprehensive record structure.
 */
export interface AgentExecutionRecord {
  /** Unique identifier of the record */
  id?: number

  /** Step ID this record belongs to */
  stepId?: string

  /** Conversation ID this record belongs to */
  conversationId?: string

  /** Name of the agent that created this record */
  agentName?: string

  /** Description information of the agent */
  agentDescription?: string

  /** Timestamp when execution started */
  startTime?: string

  /** Timestamp when execution ended */
  endTime?: string

  /** Maximum allowed number of steps */
  maxSteps?: number

  /** Current execution step number */
  currentStep?: number

  /** Execution status (IDLE, RUNNING, FINISHED) */
  status?: ExecutionStatus

  /** Request content for agent execution */
  agentRequest?: string

  /** Execution result */
  result?: string

  /** Error message if execution encounters problems */
  errorMessage?: string

  /** Model name called */
  modelName?: string

  /** List of ThinkActRecord for detailed execution process */
  thinkActSteps?: ThinkActRecord[]

  /** Sub-plan execution records for this agent */
  subPlanExecutionRecords?: PlanExecutionRecord[]
}

/**
 * Maps to Java: com.alibaba.cloud.ai.manus.runtime.entity.vo.UserInputWaitState
 *
 * User input wait state for handling user interaction during plan execution.
 */
export interface UserInputWaitState {
  /** Plan ID */
  planId?: string

  /** Title to display to user */
  title?: string

  /** Whether the plan is currently waiting for user input */
  waiting?: boolean

  /** Form description */
  formDescription?: string

  /** Form input fields as map list (matching Java structure) */
  formInputs?: Array<Record<string, string>>
}

/**
 * Maps to Java: com.alibaba.cloud.ai.manus.recorder.entity.vo.PlanExecutionRecord
 *
 * Plan execution record class for tracking and recording detailed information about
 * PlanningFlow execution process.
 *
 * This is the main structure returned by /api/executor/details/{planId}
 */
export interface PlanExecutionRecord {
  /** Unique identifier for the record */
  id?: number

  /**
   * Unique identifier for the current plan
   * Maps to Java field: currentPlanId
   * Frontend uses this as: planId
   */
  currentPlanId: string

  /** Root plan ID for sub-plans (null for main plans) */
  rootPlanId?: string

  /** Parent plan ID for sub-plans (null for root plans) */
  parentPlanId?: string

  /** Tool call ID that triggered this plan (for sub-plans) */
  toolCallId?: string

  /** Plan title */
  title?: string

  /** User's original request */
  userRequest?: string

  /** Timestamp when execution started */
  startTime?: string

  /** Timestamp when execution ended */
  endTime?: string



  /** Current step index being executed */
  currentStepIndex?: number

  /** Whether completed */
  completed?: boolean

  /** Execution summary */
  summary?: string

  /**
   * List to maintain the sequence of agent executions
   * This is the main data source for step-by-step execution details
   * Uses AgentExecutionRecord (unified version)
   */
  agentExecutionSequence?: AgentExecutionRecord[]

  /** Field to store user input wait state */
  userInputWaitState?: UserInputWaitState

  /** Actual calling model */
  modelName?: string

  /** Parent tool call information that triggered this sub-plan (for sub-plan detail displaying) */
  parentActToolCall?: ActToolInfo

  // Additional computed fields for frontend compatibility
  /** Current execution status */
  status?: 'pending' | 'running' | 'completed' | 'failed' | 'paused'

  /** Execution progress percentage (0-100) */
  progress?: number

  /** Current progress text description */
  progressText?: string

  /** Detailed execution result */
  result?: string

  /** Error message if execution failed */
  error?: string

  /** Additional message */
  message?: string

  /** Creation timestamp */
  createdAt?: string

  /** Last update timestamp */
  updatedAt?: string

  /** Additional metadata */
  metadata?: Record<string, any>
}

// API response type for getDetails method
export type PlanExecutionRecordResponse = PlanExecutionRecord | null


