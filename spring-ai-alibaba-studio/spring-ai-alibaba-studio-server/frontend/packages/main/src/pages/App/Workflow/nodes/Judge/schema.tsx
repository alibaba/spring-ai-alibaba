import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import { IJudgeNodeParam } from '../../types';

export const JudgeSchema: INodeSchema = {
  type: 'Judge',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Judge.schema.conditionJudgment',
    dm: '条件判断',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Judge.schema.decideBranchBasedOnConditions',
    dm: '依据设定条件决定分支，若设定的条件成立则仅运行对应分支，若均不成立则运行“其他”分支。',
  }),
  iconType: 'spark-conditionalJudgment-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Judge.schema.logic',
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
            id: 'main.pages.App.Workflow.nodes.Judge.schema.defaultCondition',
            dm: '默认条件',
          }),
        },
      ],
    } as IJudgeNodeParam,
  },
  isSystem: false,
  allowSingleTest: false,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-orange-hover)',
};
