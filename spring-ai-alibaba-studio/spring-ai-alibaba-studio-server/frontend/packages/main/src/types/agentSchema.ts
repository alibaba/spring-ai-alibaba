import { IPageParams, IPageResult } from '@/types/common';

/**
 * Agent type enum
 */
export enum AgentType {
  REACT_AGENT = 'ReactAgent',
  PARALLEL_AGENT = 'ParallelAgent',
  SEQUENTIAL_AGENT = 'SequentialAgent',
  LLM_ROUTING_AGENT = 'LLMRoutingAgent',
  LOOP_AGENT = 'LoopAgent',
}

/**
 * Agent status enum
 */
export enum AgentStatus {
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  CONFIGURING = 'configuring',
  ERROR = 'error',
}

/**
 * Agent schema entity interface
 */
export interface IAgentSchema {
  id?: number;
  agentId?: string;
  workspaceId?: string;
  name: string;
  description?: string;
  type: AgentType;
  instruction: string;
  inputKeys: string[];
  outputKey: string;
  handle: string;
  subAgents?: string;
  yamlSchema?: string;
  status?: AgentStatus;
  enabled?: boolean;
  gmtCreate?: string;
  gmtModified?: string;
  creator?: string;
  modifier?: string;
}

/**
 * Agent schema list query parameters
 */
export interface IAgentSchemaListParams extends IPageParams {
  name?: string;
  type?: AgentType;
  status?: AgentStatus;
}

/**
 * Agent schema list response
 */
export interface IAgentSchemaListResponse extends IPageResult<IAgentSchema> {}

/**
 * Create agent schema request
 */
export interface ICreateAgentSchemaRequest extends Omit<IAgentSchema, 'id' | 'agentId' | 'workspaceId' | 'gmtCreate' | 'gmtModified' | 'creator' | 'modifier'> {}

/**
 * Update agent schema request
 */
export interface IUpdateAgentSchemaRequest extends Partial<ICreateAgentSchemaRequest> {}

/**
 * Agent schema query by ID response
 */
export interface IAgentSchemaResponse {
  id: number;
  agentId: string;
  workspaceId: string;
  name: string;
  description?: string;
  type: AgentType;
  instruction: string;
  inputKeys: string[];
  outputKey: string;
  handle: string;
  subAgents?: string;
  yamlSchema?: string;
  status: AgentStatus;
  enabled: boolean;
  gmtCreate: string;
  gmtModified: string;
  creator: string;
  modifier: string;
}