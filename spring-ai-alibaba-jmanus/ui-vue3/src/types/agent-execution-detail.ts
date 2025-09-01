/**
 * Detailed agent execution record for frontend display
 */
export interface AgentExecutionRecordDetail {
  // Unique identifier of the record
  id: number;
  
  // Step ID this record belongs to
  stepId: string;
  
  // Name of the agent that created this record
  agentName: string;
  
  // Description information of the agent
  agentDescription: string;
  
  // Timestamp when execution started
  startTime: string;
  
  // Timestamp when execution ended
  endTime?: string;
  
  // Execution status (IDLE, RUNNING, FINISHED)
  status: ExecutionStatus;
  
  // Request content for agent execution
  agentRequest?: string;
  
  // Execution result
  result?: string;
  
  // Error message if execution encounters problems
  errorMessage?: string;
  
  // Actual calling model
  modelName?: string;
  
  // List of ThinkActRecord for detailed execution process
  thinkActSteps: ThinkActRecord[];
}

/**
 * Records the thinking and action process of an agent in a single execution step
 */
export interface ThinkActRecord {
  // Unique identifier of the record
  id: number;
  
  // ID of parent execution record
  parentExecutionId: number;
  
  // Timestamp when thinking started
  thinkStartTime?: string;
  
  // Timestamp when thinking completed
  thinkEndTime?: string;
  
  // Timestamp when action started
  actStartTime?: string;
  
  // Timestamp when action completed
  actEndTime?: string;
  
  // Input context for the thinking process
  thinkInput?: string;
  
  // Output result of the thinking process
  thinkOutput?: string;
  
  // Whether thinking determined that action is needed
  actionNeeded: boolean;
  
  // Description of the action to be taken
  actionDescription?: string;
  
  // Result of action execution
  actionResult?: string;
  
  // Status of this think-act cycle
  status?: ExecutionStatus;
  
  // Error message if the cycle encountered problems
  errorMessage?: string;
  
  // Tool name used for action (if applicable)
  toolName?: string;
  
  // Tool parameters used for action (serialized, if applicable)
  toolParameters?: string;
  
  // Action tool information
  actToolInfoList?: ActToolInfo[];
  
  // Sub-plan execution record if this action created a sub-plan
  subPlanExecutionRecord?: SubPlanExecutionRecord;
}

/**
 * Action tool information
 */
export interface ActToolInfo {
  // Tool name
  name: string;
  
  // Tool parameters (serialized)
  parameters: string;
  
  // Tool call ID
  toolCallId: string;
  
  // Execution result
  result?: string;
}

/**
 * Sub-plan execution record
 */
export interface SubPlanExecutionRecord {
  // Sub-plan ID
  currentPlanId: string;
  
  // Sub-plan title
  title?: string;
  
  // Whether completed
  completed: boolean;
  
  // Execution summary
  summary?: string;
}

/**
 * Execution status enum
 */
export enum ExecutionStatus {
  IDLE = 'IDLE',
  RUNNING = 'RUNNING',
  FINISHED = 'FINISHED'
}

