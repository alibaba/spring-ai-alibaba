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
} from '../types';

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
      id: 'main.pages.App.Workflow.constant.index.textOutput',
      dm: '文本输出',
    }),
  },
];

export const LLM_WITH_REASONING_NODE_OUTPUT_PARAMS_DEFAULT: INodeDataOutputParamItem[] =
  [
    {
      key: 'output',
      type: 'String',
      desc: $i18n.get({
        id: 'main.pages.App.Workflow.constant.index.textOutput',
        dm: '文本输出',
      }),
    },
    {
      key: 'reasoning_content',
      type: 'String',
      desc: $i18n.get({
        id: 'main.pages.App.Workflow.constant.index.depthThinkingContent',
        dm: '深度思考内容',
      }),
    },
  ];

export const SCRIPT_NODE_INPUT_PARAMS_DEFAULT: INodeDataInputParamItem[] = [
  {
    key: 'input1',
    value_from: 'input',
    value: '1',
    type: 'Number',
  },
  {
    key: 'input2',
    value_from: 'input',
    value: '2',
    type: 'Number',
  },
];

export const SCRIPT_NODE_OUTPUT_PARAMS_DEFAULT: INodeDataOutputParamItem[] = [
  {
    key: 'output',
    type: 'Number',
    desc: $i18n.get({
      id: 'main.pages.App.Workflow.constant.index.twoNumbersSum',
      dm: '两数相加的结果',
    }),
  },
];

export const RETRY_CONFIG_DEFAULT: IRetryConfig = {
  retry_enabled: false,
  max_retries: 3,
  retry_interval: 500,
};

export const TRY_CATCH_CONFIG_DEFAULT: ITryCatchConfig = {
  strategy: 'noop',
};

export const getDefaultTryCatchConfig = (
  default_values: ITryCatchConfig['default_values'],
): ITryCatchConfig => {
  return {
    strategy: 'noop',
    default_values,
  };
};

export const SHORT_MEMORY_CONFIG_DEFAULT: IShortMemoryConfig = {
  enabled: false,
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

export const OPERATOR_OPTS_MAP = {
  Number: [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.equal',
        dm: '等于',
      }),
      value: 'equals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEqual',
        dm: '不等于',
      }),
      value: 'notEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.greaterThan',
        dm: '大于',
      }),
      value: 'greater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.greaterThanOrEqual',
        dm: '大于等于',
      }),
      value: 'greaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lessThan',
        dm: '小于',
      }),
      value: 'less',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lessThanOrEqual',
        dm: '小于等于',
      }),
      value: 'lessAndEqual',
    },
  ],

  'Array<Boolean>': [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.contains',
        dm: '包含',
      }),
      value: 'contains',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notContains',
        dm: '不包含',
      }),
      value: 'notContains',
    },
  ],

  Boolean: [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.equal',
        dm: '等于',
      }),
      value: 'equals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEqual',
        dm: '不等于',
      }),
      value: 'notEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.isTrue',
        dm: '为true',
      }),
      value: 'isTrue',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.isFalse',
        dm: '为false',
      }),
      value: 'isFalse',
    },
  ],

  'Array<File>': [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
  ],

  'Array<Object>': [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
  ],

  String: [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.equal',
        dm: '等于',
      }),
      value: 'equals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEqual',
        dm: '不等于',
      }),
      value: 'notEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.contains',
        dm: '包含',
      }),
      value: 'contains',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notContains',
        dm: '不包含',
      }),
      value: 'notContains',
    },
  ],

  File: [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
  ],

  'Array<String>': [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.contains',
        dm: '包含',
      }),
      value: 'contains',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notContains',
        dm: '不包含',
      }),
      value: 'notContains',
    },
  ],

  'Array<Number>': [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthEqual',
        dm: '长度等于',
      }),
      value: 'lengthEquals',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThan',
        dm: '长度大于',
      }),
      value: 'lengthGreater',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthGreaterThanOrEqual',
        dm: '长度大于等于',
      }),
      value: 'lengthGreaterAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThan',
        dm: '长度小于',
      }),
      value: 'lengthLess',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.lengthLessThanOrEqual',
        dm: '长度小于等于',
      }),
      value: 'lengthLessAndEqual',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.contains',
        dm: '包含',
      }),
      value: 'contains',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notContains',
        dm: '不包含',
      }),
      value: 'notContains',
    },
  ],

  Object: [
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.empty',
        dm: '为空',
      }),
      value: 'isNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notEmpty',
        dm: '不为空',
      }),
      value: 'isNotNull',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.contains',
        dm: '包含',
      }),
      value: 'contains',
    },
    {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.constant.notContains',
        dm: '不包含',
      }),
      value: 'notContains',
    },
  ],
};
