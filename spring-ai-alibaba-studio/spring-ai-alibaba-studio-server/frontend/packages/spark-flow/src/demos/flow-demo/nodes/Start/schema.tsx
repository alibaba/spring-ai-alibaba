import $i18n from '@/i18n';
import { INodeSchema } from '@/types/work-flow';
import { START_NODE_OUTPUT_PARAMS_DEFAULT } from '../../constant';

export const StartSchema: INodeSchema = {
  type: 'Start',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.start',
    dm: '开始',
  }),
  iconType: 'spark-processStart-line',
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.startNode',
    dm: '开始节点',
  }),
  defaultParams: {
    input_params: [],
    output_params: START_NODE_OUTPUT_PARAMS_DEFAULT,
    node_param: {},
  },
  isSystem: true,
  allowSingleTest: false,
  disableConnectSource: true,
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.basic',
    dm: '基础',
  }),
  bgColor: 'var(--spark-ant-color-purple-hover)',
};
