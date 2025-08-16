import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import { RETRY_CONFIG_DEFAULT, TRY_CATCH_CONFIG_DEFAULT } from '../../constant';
import { IPluginNodeData, IPluginNodeParam } from '../../types';
import { transformInputParams } from '../../utils';

const getPluginNodeVariables = (data: IPluginNodeData) => {
  const variableKeyMap: Record<string, boolean> = {};
  transformInputParams(data.input_params, variableKeyMap);
  return Object.keys(variableKeyMap);
};

export const PluginSchema: INodeSchema = {
  type: 'Plugin',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.PluginNode.schema.plugin',
    dm: '插件',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.PluginNode.schema.extendFunctionality',
    dm: '通过添加插件扩展应用能力，执行外部操作。',
  }),
  iconType: 'spark-plugin-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.PluginNode.schema.tool',
    dm: '工具',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      plugin_id: '',
      tool_id: '',
      tool_name: '',
      plugin_name: '',
      retry_config: RETRY_CONFIG_DEFAULT,
      try_catch_config: TRY_CATCH_CONFIG_DEFAULT,
    } as IPluginNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-blue-hover)',
  customAdd: true,
  getRefVariables: (data) => getPluginNodeVariables(data as IPluginNodeData),
};
