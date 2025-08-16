import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import {
  RETRY_CONFIG_DEFAULT,
  SELECTED_MODEL_PARAMS_DEFAULT,
  SHORT_MEMORY_CONFIG_DEFAULT,
  TRY_CATCH_CONFIG_DEFAULT,
} from '../../constant';
import { IParameterExtractorNodeParam } from '../../types/flow';

export const ParameterExtractorSchema: INodeSchema = {
  type: 'ParameterExtractor',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.parameterExtraction',
    dm: '参数提取',
  }),
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.parameterExtractionNode',
    dm: '参数提取节点',
  }),
  iconType: 'spark-config-line',
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.variable',
    dm: '变量',
  }),
  defaultParams: {
    input_params: [
      {
        key: 'content',
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
          id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.city',
          dm: '城市',
        }),
      },
      {
        key: 'date',
        type: 'String',
        desc: $i18n.get({
          id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.date',
          dm: '日期',
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
            id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.city',
            dm: '城市',
          }),
        },
        {
          key: 'date',
          type: 'String',
          required: true,
          desc: $i18n.get({
            id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.date',
            dm: '日期',
          }),
        },
      ],

      short_memory: SHORT_MEMORY_CONFIG_DEFAULT,
      retry_config: RETRY_CONFIG_DEFAULT,
      try_catch_config: TRY_CATCH_CONFIG_DEFAULT,
    } as IParameterExtractorNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: '#EC4899',
};
