import { IWorkflowDebugInputParamItem } from '@/pages/App/Workflow/context';
import { request } from '@/request';
import { IBizEdge, IBizNode } from '@/types/workflow';
import { IWorkFlowTaskProcess } from '@spark-ai/flow';

/**
 * Parameters for creating workflow task
 */
export interface ICreateTaskParams {
  /** Version number */
  version?: string;
  /** Application ID */
  app_id: string;
  /** Session ID */
  conversation_id?: string;
  /** Input parameters for debugging */
  inputs: Array<IWorkflowDebugInputParamItem>;
}

/**
 * Create a new workflow debug task
 * @param data Task creation parameters
 * @returns Promise containing task ID and related IDs
 */
export const createWorkFlowTask = (data: ICreateTaskParams) => {
  return request({
    url: '/console/v1/apps/workflow/debug/run-task',
    method: 'POST',
    data,
  }).then(
    (res) =>
      res.data.data as {
        task_id: string;
        conversation_id: string;
        request_id: string;
      },
  );
};

/**
 * Parameters for resuming workflow task
 */
export interface IResumeTaskParams {
  /** Application ID */
  app_id: string;
  /** Task ID to resume */
  task_id: string;
  /** Node ID to resume from */
  resume_node_id: string;
  /** Parent node ID of the resuming node */
  resume_parent_id?: string;
  /** Input parameters for resuming */
  input_params: Array<{ key: string; value?: string }>;
}

/**
 * Resume a paused workflow task
 * @param data Task resuming parameters
 * @returns Promise
 */
export const resumeWorkFlowTask = (data: IResumeTaskParams) => {
  return request({
    url: `/console/v1/apps/workflow/debug/resume-task`,
    method: 'POST',
    data,
  });
};

/**
 * Parameters for starting partial graph task
 */
export interface IStartPartGraphTaskParams {
  /** Application ID */
  app_id: string;
  /** Selected nodes for partial execution */
  nodes: Pick<IBizNode, 'id' | 'name' | 'type' | 'config'>[];
  /** Selected edges for partial execution */
  edges?: IBizEdge[];
  /** Input parameters for partial execution */
  input_params: Array<{ key: string; value?: string }>;
}

/**
 * Start a partial graph execution task
 * @param data Partial graph parameters
 * @returns Promise containing task ID and request ID
 */
export const startPartGraphTask = (data: IStartPartGraphTaskParams) => {
  return request({
    url: '/console/v1/apps/workflow/debug/part-graph/run-task',
    method: 'POST',
    data,
  }).then((res) => res.data.data as { task_id: string; request_id: string });
};

/**
 * Parameters for initializing workflow debug session
 */
export interface IInitWorkFlowDebugParams {
  /** Application ID */
  app_id: string;
  /** Version number */
  version?: string;
}

/**
 * Initialize workflow debug session
 * @param data Initialization parameters
 * @returns Promise containing debug input parameters
 */
export const initWorkFlowDebug = (data: IInitWorkFlowDebugParams) => {
  return request({
    url: '/console/v1/apps/workflow/debug/init',
    method: 'POST',
    data,
  }).then((res) => res.data.data as IWorkflowDebugInputParamItem[]);
};

/**
 * Parameters for getting workflow task process
 */
export interface IGetWorkFlowTaskProcessParams {
  /** Task ID to query */
  task_id: string;
}

/**
 * Get workflow task execution process
 * @param data Task process query parameters
 * @returns Promise containing task process details
 */
export const getWorkFlowTaskProcess = (data: IGetWorkFlowTaskProcessParams) => {
  return request({
    url: '/console/v1/apps/workflow/debug/get-task-process',
    method: 'POST',
    data,
  }).then((res) => res.data.data as IWorkFlowTaskProcess);
};
