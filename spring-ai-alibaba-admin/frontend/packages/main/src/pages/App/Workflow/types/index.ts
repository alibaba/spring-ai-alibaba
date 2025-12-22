import type {
  IBranchItem,
  INodeDataInputParamItem,
  INodeDataOutputParamItem,
  IValueType,
} from '@spark-ai/flow';

/* End node parameter */
export interface IEndNodeParam {
  /* When the output type is json, json_params need to be defined */
  json_params?: INodeDataInputParamItem[];
  /* When the output type is text, text_template needs to be defined */
  text_template?: string;
  /* Output type, json: output json, consumer uses json_params field; text: output text, consumer uses text_template field */
  output_type: 'json' | 'text';
  /* Whether to enable streaming output */
  stream_switch: boolean;
}

export interface IOutputNodeParam {
  /* Template string */
  output?: string;
  /* Whether to enable streaming output */
  stream_switch: boolean;
}

export interface IModelConfigParamItem {
  key: string;
  type: IValueType;
  value: number | string | boolean;
  enable: boolean;
}

export interface IModelVisionConfig {
  enable: boolean;
  params: INodeDataInputParamItem[];
}

export interface ISelectedModelParams {
  model_id: string;
  model_name: string;
  provider: string;
  params: IModelConfigParamItem[];
  mode: 'chat' | 'completion';
  vision_config: IModelVisionConfig;
}

export interface IShortMemoryConfig {
  enabled: boolean;
  /* Memory type, self: cache for this node, custom: custom cache */
  type: 'self' | 'custom';
  /* Memory window, when type is self, memory window needs to be defined */
  round?: number;
  param: INodeDataInputParamItem;
}

export interface IRetryConfig {
  /* Whether to enable retry */
  retry_enabled: boolean;
  /* Maximum number of retries */
  max_retries: number;
  /* Retry interval */
  retry_interval: number;
}

export interface ITryCatchConfig {
  /* Exception handling strategy, defaultValue: default value, failBranch: failure branch, noop: do nothing */
  strategy: 'defaultValue' | 'failBranch' | 'noop';
  /* If strategy is defaultValue, default_value needs to be defined */
  default_values?: Omit<INodeDataInputParamItem, 'value_from'>[];
}

export interface ILLMNodeParam {
  /* System prompt word */
  sys_prompt_content?: string;
  prompt_content?: string;
  model_config: ISelectedModelParams;
  /* Short-term memory */
  short_memory: IShortMemoryConfig;
  /* Retry configuration */
  retry_config: IRetryConfig;
  /* Exception handling configuration */
  try_catch_config: ITryCatchConfig;
}

export interface IParameterExtractorNodeParam {
  model_config: ISelectedModelParams;
  /* Interference prompt */
  instruction: string;
  /* Extract parameters */
  extract_params: INodeDataOutputParamItem[];
  short_memory: IShortMemoryConfig;
  retry_config: IRetryConfig;
  try_catch_config: ITryCatchConfig;
}

export interface IJudgeNodeParam {
  branches: IBranchItem[];
}

export interface IApiNodeParam {
  /* Output type, json: output json, consumer uses json_params field; primitive: output raw data, consumer uses result field */
  output_type: 'json' | 'primitive';
  /* Request URL */
  url: string;
  authorization: {
    auth_type: 'NoAuth' | 'ApiKeyAuth' | 'BearerAuth';
    /* When auth_type is NoAuth, auth_config is invalid
    When auth_type is ApiKeyAuth, the key of auth_config can be customized
    When auth_type is BearerAuth, the key of auth_config must be token
    */
    auth_config?: Record<string, string>;
  };
  headers: Array<INodeDataInputParamItem>;
  method: 'post' | 'get' | 'put' | 'delete';
  params: Array<INodeDataInputParamItem>;
  body: {
    type: 'raw' | 'form-data' | 'json' | 'none';
    data?: string | Array<INodeDataInputParamItem>;
  };
  timeout: {
    /* Read timeout */
    read: number;
  };
  /* Retry configuration */
  retry_config: IRetryConfig;
  /* Exception handling configuration */
  try_catch_config: ITryCatchConfig;
}

export interface IParallelNodeParam {
  /* Batch size */
  batch_size: number;
  /* Concurrency size */
  concurrent_size: number;
  /* Error strategy, terminated: terminate, continueOnError: continue execution, removeErrorOutput: remove error output */
  error_strategy: 'terminated' | 'continueOnError' | 'removeErrorOutput';
}

export interface IIteratorNodeParam {
  /* Iteration type, byArray: iterate by array, byCount: iterate by count */
  iterator_type: 'byArray' | 'byCount';
  /* Iteration count, when iterator_type is byCount, iteration count needs to be defined */
  count_limit?: number;
  /* When iterator_type is byCount, iteration conditions need to be defined */
  terminations: IBranchItem[];
  variable_parameters: INodeDataInputParamItem[];
}

export interface IAppComponentNodeParam {
  /* Component type, workflow: workflow, basic: Agent */
  type: 'basic' | 'workflow';
  /* Whether to enable streaming output */
  stream_switch: boolean;
  code: string;
  name: string;
  short_memory?: IShortMemoryConfig;
  output_type: string;
}

export interface IPluginNodeParam {
  tool_name: string;
  tool_id: string;
  plugin_name: string;
  plugin_id: string;
  retry_config: IRetryConfig;
  try_catch_config: ITryCatchConfig;
}

export interface IClassifierNodeParam {
  model_config: ISelectedModelParams;
  /* Classification prompt word */
  instruction: string;
  /* Classification mode, efficient: efficient mode, advanced: advanced mode */
  mode_switch: 'efficient' | 'advanced';
  conditions: {
    id: string;
    subject: string;
  }[];
  short_memory: IShortMemoryConfig;
}

export interface IScriptNodeParam {
  /* Script content */
  script_content: string;
  /* Script type, javascript: javascript, python: python */
  script_type: 'javascript' | 'python';
  /* Retry configuration */
  retry_config: IRetryConfig;
  /* Exception handling configuration */
  try_catch_config: ITryCatchConfig;
}

export interface IVariableAssignNodeParam {
  inputs: {
    id: string;
    left: Omit<INodeDataInputParamItem, 'key'>;
    right: Omit<INodeDataInputParamItem, 'key'>;
  }[];
}

export interface IVariable extends Omit<INodeDataInputParamItem, 'key'> {
  id: string;
}

export interface IVariableHandleGroupItem {
  group_id: string;
  group_name: string;
  output_type: IValueType;
  variables: IVariable[];
}

export interface IVariableHandleNodeParam {
  type: 'group' | 'json' | 'template';
  /* When type is not group, group_strategy and groups need to be filled in */
  group_strategy: 'firstNotNull' | 'lastNotNull';
  groups: IVariableHandleGroupItem[];
  /* When type is json, json_params need to be filled in */
  json_params: INodeDataInputParamItem[];
  /* When type is template, template_content needs to be filled in */
  template_content: string;
}

export type IStartNodeParam = Record<string, never>;

export type IInputNodeParam = Record<string, never>;

export type IRetrievalNodeParam = {
  /* KnowledgeBase id list  */
  knowledge_base_ids: string[];
  /* Retrieval strategy, top_k: sort by similarity */
  prompt_strategy: string;
  /* prompt_strategy is top_k takes effect */
  top_k: number;
  /* Similarity threshold */
  similarity_threshold: number;
};

export interface IMCPNodeParam {
  tool_name: string;
  server_code: string;
  server_name: string;
}

/* Node business parameter */
export type INodeDataNodeParam =
  | IStartNodeParam
  | IInputNodeParam
  | IEndNodeParam
  | IOutputNodeParam
  | ILLMNodeParam
  | IParameterExtractorNodeParam
  | IJudgeNodeParam
  | IApiNodeParam
  | IParallelNodeParam
  | IIteratorNodeParam
  | IAppComponentNodeParam
  | IPluginNodeParam
  | IClassifierNodeParam
  | IScriptNodeParam
  | IVariableAssignNodeParam
  | IVariableHandleNodeParam
  | IRetrievalNodeParam
  | IMCPNodeParam;

export type IWorkFlowNodeData<T = INodeDataNodeParam> = {
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

export type IStartNodeData = IWorkFlowNodeData<IStartNodeParam>;

export type IInputNodeData = IWorkFlowNodeData<IInputNodeParam>;

export type IEndNodeData = IWorkFlowNodeData<IEndNodeParam>;

export type IOutputNodeData = IWorkFlowNodeData<IOutputNodeParam>;

export type ILLMNodeData = IWorkFlowNodeData<ILLMNodeParam>;

export type IParameterExtractorNodeData =
  IWorkFlowNodeData<IParameterExtractorNodeParam>;

export type IJudgeNodeData = IWorkFlowNodeData<IJudgeNodeParam>;

export type IApiNodeData = IWorkFlowNodeData<IApiNodeParam>;

export type IParallelNodeData = IWorkFlowNodeData<IParallelNodeParam>;

export type IIteratorNodeData = IWorkFlowNodeData<IIteratorNodeParam>;

export type IAppComponentNodeData = IWorkFlowNodeData<IAppComponentNodeParam>;

export type IPluginNodeData = IWorkFlowNodeData<IPluginNodeParam>;

export type IClassifierNodeData = IWorkFlowNodeData<IClassifierNodeParam>;

export type IScriptNodeData = IWorkFlowNodeData<IScriptNodeParam>;

export type IRetrievalNodeData = IWorkFlowNodeData<IRetrievalNodeParam>;

export type IMCPNodeData = IWorkFlowNodeData<IMCPNodeParam>;

export type IVariableAssignNodeData =
  IWorkFlowNodeData<IVariableAssignNodeParam>;

export type IVariableHandleNodeData =
  IWorkFlowNodeData<IVariableHandleNodeParam>;
