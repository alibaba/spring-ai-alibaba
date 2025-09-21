import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';

export const IteratorEndSchema: INodeSchema = {
  type: 'IteratorEnd',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.IteratorEnd.schema.iterationEnd',
    dm: '迭代结束',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.IteratorEnd.schema.iterationEndNode',
    dm: '迭代结束节点',
  }),
  iconType: 'spark-flag-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.IteratorEnd.schema.logic',
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
