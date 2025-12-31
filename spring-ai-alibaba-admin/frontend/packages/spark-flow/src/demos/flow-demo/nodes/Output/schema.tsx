import $i18n from '@/i18n';
import { INodeSchema } from '@/types/work-flow';
import { IOutputNodeParam } from '../../types/flow';

export const OutputSchema: INodeSchema = {
  type: 'Output',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.processOutput',
    dm: '流程输出',
  }),
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.processOutputNode',
    dm: '流程输出节点',
  }),
  iconType: 'spark-processOutput-line',
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.interaction',
    dm: '交互',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      output: '',
      stream_switch: true,
    } as IOutputNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--spark-ant-color-teal-hover)',
};
