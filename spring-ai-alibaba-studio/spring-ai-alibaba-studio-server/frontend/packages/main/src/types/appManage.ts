import $i18n from '@/i18n';
import { IAppType } from '@/services/appComponent';
import { IValueType } from '@spark-ai/flow';
import { IAppComponentListItem } from './appComponent';
import { IKnowledgeListItem } from './knowledge';
import { IMcpServer } from './mcp';
import { IModel } from './modelService';
import { PluginTool } from './plugin';
import { IBizEdge, IBizNode } from './workflow';

// Application status mapping (keep i18n strings as is)
export const AppStatus = {
  draft: $i18n.get({
    id: 'main.types.appManage.draft',
    dm: '草稿',
  }),
  published: $i18n.get({
    id: 'main.types.appManage.published',
    dm: '已发布',
  }),
  published_editing: $i18n.get({
    id: 'main.types.appManage.publishedEditing',
    dm: '已发布编辑中',
  }),
};

export type IAppStatus = keyof typeof AppStatus;

// Modality type texts mapping
export const ModalityTypeTexts = {
  textDialog: $i18n.get({
    id: 'main.types.appManage.textDialogue',
    dm: '文本对话',
  }),
  textGenerate: $i18n.get({
    id: 'main.types.appManage.textGeneration',
    dm: '文本生成',
  }),
};

export type ModalityType = keyof typeof ModalityTypeTexts;

// Interface for application card
export interface IAppCard {
  /**
   * Application ID
   */
  app_id: string;
  /**
   * Application name
   */
  name: string;
  /**
   * Last modified time
   */
  gmt_modified: string;
  /**
   * Application description
   */
  description: string;
  /**
   * Application type
   */
  type: IAppType;
  /**
   * Application status
   */
  status: IAppStatus;
}

export interface IAppDetail<T = Record<string, any>, K = T> {
  app_id: string;
  gmt_modified: number;
  name: string;
  description: string;
  status: IAppStatus;
  type: IAppType;
  config: T;
  pub_config: K;
}

export interface IHistoryConfig {
  history_switch: boolean;
  history_max_round: number;
}

export interface IGlobalVariableItem {
  key: string;
  type: IValueType;
  desc: string;
  default_value: string;
}

export interface IWorkFlowConfig {
  nodes: IBizNode[];
  edges: IBizEdge[];
  global_config: {
    history_config: IHistoryConfig;
    variable_config: {
      conversation_params: Array<IGlobalVariableItem>;
    };
  };
}

// Model configuration parameters
export interface IModelConfigParameter {
  max_tokens?: number; // Maximum tokens to generate
  temperature?: number; // Temperature parameter
  top_p?: number;
  repetition_penalty?: number; // Repetition penalty
}

// User prompt parameters
export type UserPromptParams = {
  // User defined variables
  description: string;
  name: string;
  default_value: any;
  type: 'string';
};

/** Assistant configuration details */
export interface IAssistantConfig {
  model_provider?: string; // Model provider
  model: string; // Model name
  instructions?: string; // System prompt
  memory?: {
    // Context configuration
    dialog_round: number; // Conversation rounds
  };
  parameter?: IModelConfigParameter;
  tools?: { id: string }[]; // Tools
  file_search?: {
    kb_ids?: string[]; // Knowledge base IDs
    enable_search?: boolean; // Whether to enable RAG search
    enable_citation?: boolean; // Whether to enable document citation
    top_k?: number; // Retrieved chunks
    similarity_threshold?: number; // Similarity threshold
  };
  mcp_servers?: { id: string }[]; // MCP servers
  agent_components?: string[]; // Agent components
  workflow_components?: string[]; // Workflow components
  modality_type?: ModalityType; // Interaction modality type
  prompt_variables?: UserPromptParams[]; // User defined prompt parameters
  prologue?: {
    prologue_text: string; // Opening remarks
    suggested_questions?: string[]; // Suggested questions
  };
}

export interface IAssistantConfigWithInfos
  extends Omit<
    IAssistantConfig,
    | 'model'
    | 'tools'
    | 'mcp_servers'
    | 'agent_components'
    | 'workflow_components'
    | 'file_search'
  > {
  model?: IModel;
  tools?: PluginTool[];
  mcp_servers?: IMcpServer[];
  agent_components?: IAppComponentListItem[];
  workflow_components?: IAppComponentListItem[];
  file_search?: IAssistantConfig['file_search'] & {
    kbs?: IKnowledgeListItem[];
  };
}

export type IWorkFlowAppDetail = IAppDetail<IWorkFlowConfig>;
export type IAssistantAppDetail = IAppDetail<IAssistantConfig>;
export type IAssistantAppDetailWithInfos = IAppDetail<
  IAssistantConfigWithInfos,
  IAssistantConfig
>;

export interface IAppVersion {
  app_id: string;
  config: IAssistantConfig;
  creator: string;
  gmt_create: number;
  gmt_modified: number;
  modifier: string;
  status: IAppStatus;
  version: string;
  workspace_id: string;
}
