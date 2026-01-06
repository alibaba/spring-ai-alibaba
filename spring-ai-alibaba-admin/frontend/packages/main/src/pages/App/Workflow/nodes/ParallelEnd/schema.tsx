import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';

export const ParallelEndSchema: INodeSchema = {
  type: 'ParallelEnd',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.ParallelEnd.schema.parallelEnd',
    dm: '并行结束',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.ParallelEnd.schema.parallelEndNode',
    dm: '并行结束节点',
  }),
  iconType: 'spark-flag-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.ParallelEnd.schema.logic',
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
  bgColor: 'var(--ag-ant-color-purple-hover)',
};
