import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';

export const ParallelStartSchema: INodeSchema = {
  type: 'ParallelStart',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.ParallelStart.schema.parallelStart',
    dm: '并行开始',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.ParallelStart.schema.parallelStartNode',
    dm: '并行开始节点',
  }),
  iconType: 'spark-processStart-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.ParallelStart.schema.logic',
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
