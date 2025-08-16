import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import {
  SELECTED_MODEL_PARAMS_DEFAULT,
  SHORT_MEMORY_CONFIG_DEFAULT,
} from '../../constant';
import {
  IParameterExtractorNodeData,
  IParameterExtractorNodeParam,
} from '../../types';
import {
  checkLLMData,
  checkShortMemory,
  getVariablesFromText,
  transformInputParams,
} from '../../utils';

const checkValid = (data: IParameterExtractorNodeData) => {
  const errorMsg: { label: string; error: string }[] = [];
  checkLLMData(data.node_param.model_config, errorMsg);
  if (!data.input_params[0]?.value) {
    errorMsg.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.input',
        dm: '输入',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.notNull',
        dm: '不能为空',
      }),
    });
  }
  if (!data.node_param.extract_params.length) {
    errorMsg.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.extractParameters',
        dm: '提取参数',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.notNull',
        dm: '不能为空',
      }),
    });
  }
  checkShortMemory(data.node_param.short_memory, errorMsg);
  return errorMsg;
};

const getRefVariables = (data: IParameterExtractorNodeData) => {
  const variableKeyMap: Record<string, boolean> = {};
  transformInputParams(data.input_params, variableKeyMap);
  getVariablesFromText(data.node_param.instruction, variableKeyMap);
  if (data.node_param.model_config.vision_config.enable) {
    transformInputParams(
      data.node_param.model_config.vision_config.params,
      variableKeyMap,
    );
  }
  return Object.keys(variableKeyMap);
};

export const ParameterExtractorSchema: INodeSchema = {
  type: 'ParameterExtractor',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.parameterExtraction',
    dm: '参数提取',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.extractStructuredParameters',
    dm: '通过模型提取一段文本中结构化参数。',
  }),
  iconType: 'spark-config-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.variable',
    dm: '变量',
  }),
  defaultParams: {
    input_params: [
      {
        key: 'input',
        value_from: 'refer',
        type: 'String',
        value: '',
      },
    ],

    output_params: [
      {
        key: 'city',
        type: 'String',
        desc: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.city',
          dm: '城市',
        }),
      },
      {
        key: 'date',
        type: 'String',
        desc: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.date',
          dm: '日期',
        }),
      },
      {
        key: '_is_completed',
        type: 'Boolean',
        desc: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.completeParsing',
          dm: '是否完整解析',
        }),
      },
      {
        key: '_reason',
        type: 'String',
        desc: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.unsuccessfulReason',
          dm: '未成功解析的原因',
        }),
      },
    ],

    node_param: {
      model_config: SELECTED_MODEL_PARAMS_DEFAULT,
      instruction: '',
      extract_params: [
        {
          key: 'city',
          type: 'String',
          required: true,
          desc: $i18n.get({
            id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.city',
            dm: '城市',
          }),
        },
        {
          key: 'date',
          type: 'String',
          required: true,
          desc: $i18n.get({
            id: 'main.pages.App.Workflow.nodes.ParameterExtractor.schema.date',
            dm: '日期',
          }),
        },
      ],

      short_memory: SHORT_MEMORY_CONFIG_DEFAULT,
    } as IParameterExtractorNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-pink-hover)',
  checkValid: (data) => checkValid(data as IParameterExtractorNodeData),
  getRefVariables: (data) =>
    getRefVariables(data as IParameterExtractorNodeData),
};
