import $i18n from '@/i18n';
import type { INodeSchema } from '@spark-ai/flow';
import { uniqueId } from '@spark-ai/flow';
import { IVariableHandleNodeParam } from '../../types/flow';

export const VariableHandleSchema: INodeSchema = {
  type: 'VariableHandle',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.variableProcessing',
    dm: '变量处理',
  }),
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.textTransformationNode',
    dm: '文本转换节点',
  }),
  iconType: 'spark-variableProcessing-line',
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.variable',
    dm: '变量',
  }),
  defaultParams: {
    input_params: [],
    output_params: [
      {
        key: 'Group1',
        type: 'String',
        desc: void 0,
      },
    ],

    node_param: {
      type: 'group',
      group_strategy: 'firstNotNull',
      groups: [
        {
          group_id: uniqueId(4),
          group_name: 'Group1',
          output_type: 'String',
          variables: [
            {
              id: uniqueId(4),
              value_from: 'refer',
              value: void 0,
              type: 'String',
            },
            {
              id: uniqueId(4),
              value_from: 'refer',
              value: void 0,
              type: 'String',
            },
          ],
        },
      ],

      json_params: [],
      template_content: '',
    } as IVariableHandleNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: '#EC4899',
};
