import type { IValueType } from '@spark-ai/flow';

/**
 * Model provider interface
 * Defines the properties of an AI model provider
 */
interface IModelProvider {
  provider: string;
  name: string;
  description: string;
  icon: string;
  source: string;
  enable: boolean;
  supported_model_types: string[];
}

/**
 * Model item interface
 * Defines the properties of a single AI model
 */
interface IModelItem {
  model_id: string;
  name: string;
  provider: string;
  mode: string;
  type: string;
  tags: string[];
  icon: string;
}

/**
 * Model selector item interface
 * Combines provider information and the list of models under the provider
 */
export interface IModelSelectorItem {
  provider: IModelProvider;
  models: IModelItem[];
}

export interface IModelParamsSchema {
  key: string;
  name: string;
  description: string;
  type: IValueType;
  default_value: string | number;
  min?: number;
  max?: number;
  precision?: number;
  required?: boolean;
}
