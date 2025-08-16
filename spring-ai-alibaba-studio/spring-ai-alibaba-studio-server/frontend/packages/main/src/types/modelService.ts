import $i18n from '@/i18n';

// Basic provider information interface
export interface IProvider {
  name: string;
  provider: string;
  description?: string;
  icon?: string;
  supported_model_types?: string[];
  protocol?: string;
  credential_config?: Record<string, any>;
  enable?: boolean;
  model_count?: number;
  source?: 'preset' | 'custom';
  gmt_modified?: string;
}

// Parameters for creating a provider
export interface ICreateProviderParams {
  name: string;
  description?: string;
  icon?: string;
  supported_model_types?: string;
  protocol?: string;
  credential_config?: Record<string, any>;
}

// Parameters for querying provider list
export interface IListProvidersParams {
  name?: string;
}

// Provider configuration information
export interface IProviderConfigInfo {
  name: string;
  provider: string;
  description?: string;
  icon?: string;
  supported_model_types?: string[];
  protocol?: string;
  enable?: boolean;
  credential?: Record<string, any>;
  source?: 'preset' | 'custom';
}

// Basic model information
export interface IModel {
  model_id: string;
  name: string;
  provider: string;
  icon?: string;
  tags?: string[];
  type: string;
  mode?: string;
  enable?: boolean;
  source?: 'preset' | 'custom';
}

// Model configuration information
export interface IModelConfigInfo {
  model_id: string;
  tags?: string[];
  name?: string;
  provider: string;
  icon?: string;
  type: string;
  mode?: string;
}

// Parameters for creating a model
export interface ICreateModelParams {
  model_id?: string;
  name: string;
  provider?: string;
  icon?: string;
  tags?: string[];
  type?: string;
  enable?: boolean;
}

// Parameters for updating a model
export interface IUpdateModelParams {
  name: string;
  icon?: string;
  tags?: string[];
  enable?: boolean;
}

// Model input parameter rules
export interface IModelParameterRule {
  code: string; // Parameter code
  name: string; // Parameter name
  description: string; // Parameter description
  type: TValueType; // Parameter type
  min: number; // Minimum value
  max: number; // Maximum value
  precision: number; // Decimal precision
  required: boolean; // Whether required
  help: {
    zh_Hans: string; // Chinese help text
    en_US: string; // English help text
  };
  default_value: number; // Default value
  options?: string[]; // Available options
}

export interface ISelectedModelParams {
  model_id: string;
  provider: string;
  params: Array<IModelConfigParamItem>;
}

// Value type definition
export type TValueType =
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

// Model configuration parameter item
export interface IModelConfigParamItem {
  key: string;
  type: TValueType;
  value: number | string | boolean;
  enable: boolean;
}

/**
 * Model selector item interface
 * Combines provider information and model list under that provider
 */
export interface IModelSelectorItem {
  /** Provider information */
  provider: IProviderConfigInfo;
  /** Model list under this provider */
  models: IModelConfigInfo[];
}

// Model tags mapping
export const MODEL_TAGS = {
  vision: $i18n.get({
    id: 'main.types.modelService.visual',
    dm: '视觉',
  }),
  web_search: $i18n.get({
    id: 'main.types.modelService.connected',
    dm: '联网',
  }),
  embedding: $i18n.get({
    id: 'main.types.modelService.embedded',
    dm: '嵌入',
  }),
  reasoning: $i18n.get({
    id: 'main.types.modelService.inference',
    dm: '推理',
  }),
  function_call: $i18n.get({
    id: 'main.types.modelService.tool',
    dm: '工具',
  }),
};
