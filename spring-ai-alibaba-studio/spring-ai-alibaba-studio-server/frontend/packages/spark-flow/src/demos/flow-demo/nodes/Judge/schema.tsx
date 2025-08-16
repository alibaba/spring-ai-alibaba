import $i18n from '@/i18n';
import { INodeSchema } from '@/types/work-flow';
import { IJudgeNodeParam } from '../../types/flow';

export const JudgeSchema: INodeSchema = {
  type: 'Judge',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.conditionJudgment',
    dm: '条件判断',
  }),
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.judgmentNode',
    dm: '判断节点',
  }),
  iconType: 'spark-conditionalJudgment-line',
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.logic',
    dm: '逻辑',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      branches: [
        {
          id: 'default',
          label: $i18n.get({
            id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.defaultCondition',
            dm: '默认条件',
          }),
        },
      ],
    } as IJudgeNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--spark-ant-color-orange-hover)',
};
