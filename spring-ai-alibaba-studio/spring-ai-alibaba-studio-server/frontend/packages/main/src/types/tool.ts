import { IPageParams, IPageResult } from '@/types/common';

/**
 * Tool status enum
 */
export enum ToolStatus {
  DELETED = 'deleted',
  DRAFT = 'draft',
  PUBLISHED = 'published',
  PUBLISHED_EDITING = 'published_editing',
}

/**
 * Tool test status enum
 */
export enum ToolTestStatus {
  NOT_TESTED = 'not_tested',
  TESTING = 'testing',
  PASSED = 'passed',
  FAILED = 'failed',
}

/**
 * Tool entity interface
 */
export interface ITool {
  id?: number;
  toolId?: string;
  pluginId?: string;
  workspaceId?: string;
  status?: ToolStatus;
  testStatus?: ToolTestStatus;
  enabled?: boolean;
  name: string;
  description?: string;
  config?: string;
  apiSchema?: string;
  gmtCreate?: string;
  gmtModified?: string;
  creator?: string;
  modifier?: string;
}

/**
 * Tool list query parameters
 */
export interface IToolListParams extends IPageParams {
  name?: string;
  status?: ToolStatus;
  testStatus?: ToolTestStatus;
  pluginId?: string;
  enabled?: boolean;
}

/**
 * Tool list response
 */
export interface IToolListResponse extends IPageResult<ITool> {}

/**
 * Create tool request
 */
export interface ICreateToolRequest extends Omit<ITool, 'id' | 'toolId' | 'workspaceId' | 'gmtCreate' | 'gmtModified' | 'creator' | 'modifier'> {}

/**
 * Update tool request
 */
export interface IUpdateToolRequest extends Partial<ICreateToolRequest> {}

/**
 * Tool query by ID response
 */
export interface IToolResponse {
  id: number;
  toolId: string;
  pluginId?: string;
  workspaceId: string;
  status: ToolStatus;
  testStatus: ToolTestStatus;
  enabled: boolean;
  name: string;
  description?: string;
  config?: string;
  apiSchema?: string;
  gmtCreate: string;
  gmtModified: string;
  creator: string;
  modifier: string;
}