import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';

export const IteratorStartSchema: INodeSchema = {
  type: 'IteratorStart',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.IteratorStart.schema.iterationStart',
    dm: '循环开始',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.IteratorStart.schema.iterationStartNode',
    dm: '循环开始节点',
  }),
  iconType: 'spark-processStart-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.IteratorStart.schema.logic',
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
