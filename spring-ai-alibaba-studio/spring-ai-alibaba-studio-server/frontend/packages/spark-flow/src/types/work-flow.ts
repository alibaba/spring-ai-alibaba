import { IVarTreeItem } from '@/components/VariableTreeSelect';
import { Node } from '@xyflow/react';

export type INodeProps = {
  id: string;
  data: IWorkFlowNodeData;
};

export interface INodeSchema {
  /* Node type */
  type: string;
  /* Node icon */
  iconType: string;
  /* Node title */
  title: string;
  /* Node description */
  desc: string;
  /* Default parameters, according to backend protocol */
  defaultParams: Omit<IWorkFlowNodeData, 'label'>;
  /* Whether it is a system preset node, if true, deletion and copying are not allowed */
  isSystem?: boolean;
  /* Whether single-point debugging is allowed */
  allowSingleTest?: boolean;
  /* Upstream node type restrictions */
  allowSourceNodeTypes?: string[];
  /* Downstream node type restrictions */
  allowTargetNodeTypes?: string[];
  /* Whether to prohibit connecting upstream nodes */
  disableConnectSource?: boolean;
  /* Whether to prohibit connecting downstream nodes */
  disableConnectTarget?: boolean;
  /* Used to calculate the position of the new node */
  defaultHeight?: number;
  /* Validation method */
  checkValid?: (data: IWorkFlowNodeData) => { label: string; error: string }[];
  /* Get variables referenced by the node */
  getRefVariables?: (data: IWorkFlowNodeData) => string[];
  getParentNodeVariableList?: (
    data: IWorkFlowNode,
    options?: { disableShowVariableParameters?: boolean },
  ) => IVarTreeItem[];
  /* Node group type */
  groupLabel?: string;
  /* Node background color */
  bgColor: string;
  /* Whether custom addition is allowed */
  customAdd?: boolean;
  /* Whether to hide in the menu */
  hideInMenu?: boolean;
  /** Whether configuration is prohibited */
  notAllowConfig?: boolean;
  /** Whether it is a group node */
  isGroup?: boolean;
  /** Whether it is prohibited to appear on group nodes */
  disableInGroup?: boolean;
}

export interface IPointItem {
  type: string;
  id: string;
  handleId: string;
}

export interface IEdgeData {
  _hover?: boolean;
  _source_node_status?: IWorkFlowStatus;
  _target_node_status?: IWorkFlowStatus;
}

/* Node output parameter */
export interface INodeDataOutputParamItem {
  /* Parameter id */
  id?: string;
  /* Parameter name */
  key: string;
  /* Parameter type */
  type?: IValueType;
  /* Parameter description */
  desc?: string;
  /* When defining input parameters for the start node, it is necessary to specify whether it is required */
  required?: boolean;
  value_from?: 'refer' | 'input' | 'clear';
  /* If the type is Object, sub-parameters need to be defined */
  properties?: INodeDataOutputParamItem[];
}

/* Node input parameter */
export interface INodeDataInputParamItem {
  /* Parameter id */
  id?: string;
  /* Parameter name */
  key: string;
  /* Parameter type */
  type?: IValueType;
  /* Parameter value */
  value?: string;
  /* Value source type, refer: reference; input: input; clear: clear, when value_from is clear, value is invalid */
  value_from?: 'refer' | 'input' | 'clear';
}

export type IWorkFlowNodeData<T = any> = {
  /* Node name */
  label: string;
  /* Node input parameters */
  input_params: INodeDataInputParamItem[];
  /* Node output parameters */
  output_params: INodeDataOutputParamItem[];
  /* Node business parameters */
  node_param: T;
  /* Node description */
  desc?: string;
};

export interface IConditionItem {
  operator?: string;
  left: Omit<INodeDataInputParamItem, 'key'>;
  right: Omit<INodeDataInputParamItem, 'key'>;
}

export interface IBranchItem {
  id: string;
  label: string;
  logic?: 'and' | 'or';
  conditions?: IConditionItem[];
}

export interface ICheckListItem {
  node_id: string;
  node_type: string;
  node_name: string;
  error_msgs: { label: string; error: string }[];
}

export interface IWorkFlowNode extends Node<IWorkFlowNodeData> {
  type: string;
}

export type IValueType =
  | 'Array<Object>'
  | 'Array<File>'
  | 'Array<String>'
  | 'Array<Number>'
  | 'Array<Boolean>'
  | 'Object'
  | 'File'
  | 'String'
  | 'Number'
  | 'Boolean';

export type IValueTypeOption = {
  label: string;
  value: IValueType | 'Array';
  disabled?: boolean;
  children?: IValueTypeOption[];
};

export type IWorkFlowStatus =
  | 'pause'
  | 'success'
  | 'fail'
  | 'skip'
  | 'executing'
  | 'stop';

export interface IUserInputItem {
  key: string;
  type: IValueType;
  desc: string;
  value?: string;
  required?: boolean;
}
export interface IWorkFlowTaskResultItem {
  node_type: string;
  node_name: string;
  node_id: string;
  node_content: string | IUserInputItem[];
  node_status: IWorkFlowStatus;
  parent_node_id?: string;
}

export interface IWorkFlowNodeResultItem {
  is_batch: boolean;
  retry?: {
    happened: boolean;
    retry_times: number;
  };
  input?: string;
  output?: string;
  usages?: {
    prompt_tokens: number;
    completion_tokens: number;
    total_tokens: number;
  }[];
  batches: IWorkFlowNodeResultItem[];
  is_multi_branch: boolean;
  multi_branch_results?: {
    condition_id: string;
    target_ids: string[];
  }[];
  node_id: string;
  node_name: string;
  node_type: string;
  node_status: IWorkFlowStatus;
  parent_node_id?: string;
  output_type: 'json' | 'text';
  node_exec_time: string;
  try_catch?: {
    happened: boolean;
    strategy: string;
  };
  short_memory?: {
    round: number;
    current_self_chat_messages: {
      role?: string;
      content?: any;
      name?: string;
      tool_calls?: Array<{
        id: string;
        type: string;
        index: number;
        function: {
          name: string;
          arguments: string;
          outputs: string;
        };
      }>;
      audio?: {
        id: string;
        data: string;
        expires_at: number;
        transcript: string;
      };
    }[];
  };
  error_info?: string;
}

export interface IWorkFlowTaskProcess {
  task_id: string;
  conversation_id?: string;
  request_id: string;
  task_status: IWorkFlowStatus;
  task_results: IWorkFlowTaskResultItem[];
  error_code?: string;
  error_info?: string;
  task_exec_time: string;
  node_results: IWorkFlowNodeResultItem[];
}
