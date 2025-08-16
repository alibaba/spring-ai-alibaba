import $i18n from '@/i18n';
import { INodeSchema } from '@/types/work-flow';
import { IIteratorNodeParam } from '../../types/flow';

export const IteratorSchema: INodeSchema = {
  type: 'Iterator',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.loop',
    dm: '循环',
  }),
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.iterationNode',
    dm: '迭代节点',
  }),
  iconType: 'spark-cycleDiagram-line',
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.logic',
    dm: '逻辑',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      iterator_type: 'byArray',
      count_limit: 100,
      variable_parameters: [],
      terminations: [],
    } as IIteratorNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--spark-ant-color-orange-hover)',
  isGroup: true,
};
