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
 * TypeScript interfaces for Execution Tree API response structure
 * 
 * Maps to Java backend VO classes in com.alibaba.cloud.ai.example.manus.planning.controller.vo
 * 
 * Hierarchy:
 * ExecutionTreeResponse (Main API response)
 *   └── ExecutionTreeNode (Tree root node)
 *       └── steps: ExecutionStepInfo[]
 *           └── Each step contains merged step and agent execution data
 *       └── children: ExecutionTreeNode[] (for future sub-plan support)
 */

/**
 * Execution status enum matching Java backend PlanExecutionStatus
 * 
 * Maps to Java: com.alibaba.cloud.ai.example.manus.planning.controller.vo.PlanExecutionStatus
 */
export enum PlanExecutionStatus {
  /** Plan is waiting to start execution */
  PENDING = 'pending',
  
  /** Plan is currently being executed */
  RUNNING = 'running',
  
  /** Plan execution has completed successfully */
  COMPLETED = 'completed'
}

/**
 * Agent execution status enum matching Java backend ExecutionStatus
 * 
 * Maps to Java: com.alibaba.cloud.ai.example.manus.recorder.entity.ExecutionStatus
 */
export enum AgentExecutionStatus {
  /** Agent is idle and waiting */
  IDLE = 'IDLE',
  
  /** Agent is currently executing */
  RUNNING = 'RUNNING',
  
  /** Agent execution has finished */
  FINISHED = 'FINISHED'
}

/**
 * Represents detailed information about a single execution step in the plan.
 * Contains step metadata and all agent execution details merged into one class.
 * 
 * Maps to Java: com.alibaba.cloud.ai.example.manus.planning.controller.vo.ExecutionStepInfo
 */
export interface ExecutionStepInfo {
  /** The index/position of this step in the execution sequence */
  stepIndex: number
  
  /** Human-readable description of what this step does */
  stepDescription: string
  
  /** Unique identifier for this agent execution */
  id: number
  
  /** Human-readable name of the agent */
  agentName: string
  
  /** Detailed description of what the agent does */
  agentDescription: string
  
  /** Current execution status of the agent */
  status: AgentExecutionStatus
  
  /** When the agent execution started (ISO 8601 string) */
  startTime: string
  
  /** When the agent execution completed, null if still running (ISO 8601 string) */
  endTime: string | null
  
  /** Current step index within the agent's execution sequence */
  currentStep: number
  
  /** Total number of steps the agent needs to complete */
  maxSteps: number
}

/**
 * Represents a node in the execution tree structure.
 * Contains plan information, status, progress, and execution steps.
 * 
 * Maps to Java: com.alibaba.cloud.ai.example.manus.planning.controller.vo.ExecutionTreeNode
 */
export interface ExecutionTreeNode {
  /** The current plan ID for this node */
  currentPlanId: string
  
  /** Human-readable title/description of the plan */
  title: string
  
  /** Current execution status of the plan */
  status: PlanExecutionStatus
  
  /** Execution progress as a percentage (0-100) */
  progress: number
  
  /** When the plan execution started (ISO 8601 string) */
  startTime: string
  
  /** When the plan execution completed, null if still running (ISO 8601 string) */
  endTime: string | null
  
  /** The original user request that triggered this plan */
  userRequest: string
  
  /** List of execution steps for this plan */
  steps: ExecutionStepInfo[]
  
  /** Child plans/sub-plans (currently empty, extensible for future use) */
  children: ExecutionTreeNode[]
}

/**
 * Response object for execution tree API endpoint.
 * Provides a strongly-typed structure for the execution tree data.
 * 
 * Maps to Java: com.alibaba.cloud.ai.example.manus.planning.controller.vo.ExecutionTreeResponse
 */
export interface ExecutionTreeResponse {
  /** The root plan ID that represents the top-level execution plan */
  rootPlanId: string
  
  /** The main execution tree node containing plan details and steps */
  tree: ExecutionTreeNode
}

/**
 * API response type for getExecutionTree method
 * 
 * Used in ManusController.getExecutionTree() response
 */
export type ExecutionTreeResponseData = ExecutionTreeResponse

/**
 * API response wrapper for execution tree endpoint
 * 
 * Standard API response format with data and optional error information
 */
export interface ExecutionTreeApiResponse {
  /** Response data containing the execution tree */
  data: ExecutionTreeResponse
  
  /** Response status code */
  status: number
  
  /** Response message */
  message: string
  
  /** Timestamp of the response */
  timestamp: string
}

/**
 * Error response for execution tree API failures
 */
export interface ExecutionTreeErrorResponse {
  /** Error status code */
  status: number
  
  /** Error message */
  message: string
  
  /** Error details */
  error: string
  
  /** Timestamp of the error */
  timestamp: string
  
  /** Root plan ID that caused the error */
  rootPlanId?: string
}

/**
 * Utility type for partial execution tree data
 * Useful for form inputs and partial updates
 */
export type PartialExecutionTreeResponse = Partial<ExecutionTreeResponse>

/**
 * Utility type for partial step info
 * Useful for step updates and partial modifications
 */
export type PartialExecutionStepInfo = Partial<ExecutionStepInfo>

/**
 * Utility type for partial tree node
 * Useful for node updates and partial modifications
 */
export type PartialExecutionTreeNode = Partial<ExecutionTreeNode>

/**
 * Type guard to check if an object is an ExecutionTreeResponse
 * 
 * @param obj - Object to check
 * @returns True if obj is an ExecutionTreeResponse
 */
export function isExecutionTreeResponse(obj: any): obj is ExecutionTreeResponse {
  return (
    obj &&
    typeof obj === 'object' &&
    typeof obj.rootPlanId === 'string' &&
    obj.tree &&
    typeof obj.tree === 'object' &&
    typeof obj.tree.currentPlanId === 'string'
  )
}

/**
 * Type guard to check if an object is an ExecutionStepInfo
 * 
 * @param obj - Object to check
 * @returns True if obj is an ExecutionStepInfo
 */
export function isExecutionStepInfo(obj: any): obj is ExecutionStepInfo {
  return (
    obj &&
    typeof obj === 'object' &&
    typeof obj.stepIndex === 'number' &&
    typeof obj.stepDescription === 'string' &&
    typeof obj.id === 'number'
  )
}

/**
 * Type guard to check if an object is an ExecutionTreeNode
 * 
 * @param obj - Object to check
 * @returns True if obj is an ExecutionTreeNode
 */
export function isExecutionTreeNode(obj: any): obj is ExecutionTreeNode {
  return (
    obj &&
    typeof obj === 'object' &&
    typeof obj.currentPlanId === 'string' &&
    typeof obj.title === 'string' &&
    Array.isArray(obj.steps)
  )
}

/**
 * Helper function to get step by index
 * 
 * @param tree - Execution tree response
 * @param stepIndex - Index of the step to find
 * @returns Step info or undefined if not found
 */
export function getStepByIndex(
  tree: ExecutionTreeResponse, 
  stepIndex: number
): ExecutionStepInfo | undefined {
  return tree.tree.steps.find(step => step.stepIndex === stepIndex)
}

/**
 * Helper function to get step by agent ID
 * 
 * @param tree - Execution tree response
 * @param agentId - ID of the agent to find
 * @returns Step info or undefined if not found
 */
export function getStepByAgentId(
  tree: ExecutionTreeResponse, 
  agentId: number
): ExecutionStepInfo | undefined {
  return tree.tree.steps.find(step => step.id === agentId)
}

/**
 * Helper function to check if plan is completed
 * 
 * @param tree - Execution tree response
 * @returns True if plan is completed
 */
export function isPlanCompleted(tree: ExecutionTreeResponse): boolean {
  return tree.tree.status === PlanExecutionStatus.COMPLETED
}

/**
 * Helper function to check if plan is running
 * 
 * @param tree - Execution tree response
 * @returns True if plan is running
 */
export function isPlanRunning(tree: ExecutionTreeResponse): boolean {
  return tree.tree.status === PlanExecutionStatus.RUNNING
}

/**
 * Helper function to check if plan is pending
 * 
 * @param tree - Execution tree response
 * @returns True if plan is pending
 */
export function isPlanPending(tree: ExecutionTreeResponse): boolean {
  return tree.tree.status === PlanExecutionStatus.PENDING
}

/**
 * Helper function to get completed steps count
 * 
 * @param tree - Execution tree response
 * @returns Number of completed steps
 */
export function getCompletedStepsCount(tree: ExecutionTreeResponse): number {
  return tree.tree.steps.filter(step => step.status === AgentExecutionStatus.FINISHED).length
}

/**
 * Helper function to get total steps count
 * 
 * @param tree - Execution tree response
 * @returns Total number of steps
 */
export function getTotalStepsCount(tree: ExecutionTreeResponse): number {
  return tree.tree.steps.length
}

/**
 * Helper function to get running step
 * 
 * @param tree - Execution tree response
 * @returns Currently running step or undefined
 */
export function getRunningStep(tree: ExecutionTreeResponse): ExecutionStepInfo | undefined {
  return tree.tree.steps.find(step => step.status === AgentExecutionStatus.RUNNING)
}

/**
 * Helper function to get next pending step
 * 
 * @param tree - Execution tree response
 * @returns Next pending step or undefined
 */
export function getNextPendingStep(tree: ExecutionTreeResponse): ExecutionStepInfo | undefined {
  return tree.tree.steps.find(step => step.status === AgentExecutionStatus.IDLE)
}
