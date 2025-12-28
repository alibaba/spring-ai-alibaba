import $i18n from '@/i18n';
import {
  INodeDataInputParamItem,
  INodeDataOutputParamItem,
} from '@spark-ai/flow';
import {
  IRetryConfig,
  ISelectedModelParams,
  IShortMemoryConfig,
  ITryCatchConfig,
} from '../types/flow';

export const START_NODE_OUTPUT_PARAMS_DEFAULT: INodeDataOutputParamItem[] = [
  {
    key: 'name',
    type: 'String',
    desc: '',
  },
  {
    key: 'age',
    type: 'Number',
    desc: '',
  },
];

export const END_NODE_OUTPUT_PARAMS_DEFAULT: INodeDataInputParamItem[] = [
  {
    key: 'output',
    value_from: 'refer',
    type: 'String',
    value: void 0,
  },
];

export const LLM_NODE_OUTPUT_PARAMS_DEFAULT: INodeDataOutputParamItem[] = [
  {
    key: 'output',
    type: 'String',
    desc: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.constant.index.textOutput',
      dm: '文本输出',
    }),
  },
];

export const SCRIPT_NODE_INPUT_PARAMS_DEFAULT: INodeDataInputParamItem[] = [
  {
    key: 'pageNo',
    value_from: 'refer',
    type: 'Number',
  },
  {
    key: 'pageSize',
    value_from: 'refer',
    type: 'Number',
  },
];

export const SCRIPT_NODE_OUTPUT_PARAMS_DEFAULT: INodeDataOutputParamItem[] = [
  {
    key: 'list',
    type: 'Array<Object>',
    desc: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.constant.index.listOutput',
      dm: '列表输出',
    }),
    properties: [
      {
        key: 'name',
        type: 'String',
        desc: $i18n.get({
          id: 'spark-flow.demos.spark-flow-1.constant.index.name',
          dm: '名称',
        }),
      },
      {
        key: 'age',
        type: 'Number',
        desc: $i18n.get({
          id: 'spark-flow.demos.spark-flow-1.constant.index.age',
          dm: '年龄',
        }),
      },
    ],
  },
];

export const RETRY_CONFIG_DEFAULT: IRetryConfig = {
  retry_enabled: true,
  max_retries: 3,
  retry_interval: 100,
};

export const TRY_CATCH_CONFIG_DEFAULT: ITryCatchConfig = {
  strategy: 'noop',
};

export const SHORT_MEMORY_CONFIG_DEFAULT: IShortMemoryConfig = {
  enabled: true,
  type: 'self',
  round: 3,
  param: {
    key: 'historyList',
    type: 'Array<String>',
    value_from: 'refer',
    value: void 0,
  },
};

export const SELECTED_MODEL_PARAMS_DEFAULT: ISelectedModelParams = {
  model_id: '',
  model_name: '',
  mode: 'chat',
  provider: '',
  params: [],
  vision_config: {
    enable: false,
    params: [
      {
        key: 'imageContent',
        value_from: 'refer',
        type: 'File',
        value: void 0,
      },
    ],
  },
};
