import $i18n from '@/i18n';
import type { INodeSchema } from '@spark-ai/flow';

export const IteratorEndSchema: INodeSchema = {
  type: 'IteratorEnd',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.iterationEnd',
    dm: '循环开始',
  }),
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.iterationStartNode',
    dm: '循环开始节点',
  }),
  iconType: 'spark-processStart-line',
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.logic',
    dm: '逻辑',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {},
  },
  isSystem: true,
  hideInMenu: true,
  allowSingleTest: false,
  disableConnectSource: true,
  disableConnectTarget: true,
  notAllowConfig: true,
  bgColor: 'var(--spark-ant-color-purple-hover)',
};
