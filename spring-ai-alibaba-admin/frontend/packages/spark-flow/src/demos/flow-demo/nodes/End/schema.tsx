import $i18n from '@/i18n';
import { INodeSchema } from '@/types/work-flow';
import { END_NODE_OUTPUT_PARAMS_DEFAULT } from '../../constant';
import { IEndNodeParam } from '../../types/flow';

export const EndSchema: INodeSchema = {
  type: 'End',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.end',
    dm: '结束',
  }),
  iconType: 'spark-flag-line',
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.endNode',
    dm: '结束节点',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      output_type: 'text',
      text_template: '',
      json_params: END_NODE_OUTPUT_PARAMS_DEFAULT,
      stream_switch: false,
    } as IEndNodeParam,
  },
  isSystem: true,
  allowSingleTest: false,
  disableConnectSource: true,
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.basic',
    dm: '基础',
  }),
  checkValid: () => [],
  bgColor: 'var(--spark-ant-color-purple-hover)',
};
