import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import { SHORT_MEMORY_CONFIG_DEFAULT } from '../../constant';
import { IAppComponentNodeData, IAppComponentNodeParam } from '../../types';
import { transformInputParams } from '../../utils';

const getAppComponentVariables = (data: IAppComponentNodeData) => {
  const variableKeyMap: Record<string, boolean> = {};
  const { input_params = [] } = data;
  transformInputParams(input_params, variableKeyMap);
  return Object.keys(variableKeyMap);
};

export const AppComponentSchema: INodeSchema = {
  type: 'AppComponent',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.AppComponent.schema.appComponent',
    dm: '应用组件',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.AppComponent.schema.referencePublishedWorkflowOrAgent',
    dm: '引用发布为组件的工作流/智能体。',
  }),
  iconType: 'spark-osWidget-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.AppComponent.schema.tool',
    dm: '工具',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      name: '',
      code: '',
      type: 'basic',
      output_type: 'json',
      stream_switch: false,
      short_memory: SHORT_MEMORY_CONFIG_DEFAULT,
    } as IAppComponentNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-blue-hover)',
  customAdd: true,
  getRefVariables: (data) =>
    getAppComponentVariables(data as IAppComponentNodeData),
};
