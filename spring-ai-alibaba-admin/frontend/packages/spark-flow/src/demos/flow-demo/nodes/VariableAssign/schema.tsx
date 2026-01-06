import $i18n from '@/i18n';
import type { INodeSchema } from '@spark-ai/flow';
import { uniqueId } from '@spark-ai/flow';
import { IVariableAssignNodeParam } from '../../types/flow';

export const VariableAssignSchema: INodeSchema = {
  type: 'VariableAssign',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.variableAssignment',
    dm: '变量赋值',
  }),
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.variableAssignmentNode',
    dm: '变量赋值节点',
  }),
  iconType: 'spark-variableSetting-line',
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.variable',
    dm: '变量',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      inputs: [
        {
          id: uniqueId(4),
          left: {
            value_from: 'refer',
            value: void 0,
            type: 'String',
          },
          right: {
            value_from: 'refer',
            value: void 0,
            type: 'String',
          },
        },
      ],
    } as IVariableAssignNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: '#EC4899',
};
