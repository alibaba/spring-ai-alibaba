import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import { IMCPNodeData, IMCPNodeParam } from '../../types';
import { transformInputParams } from '../../utils';

const getMCPNodeVariables = (data: IMCPNodeData) => {
  const variableKeyMap: Record<string, boolean> = {};
  const { input_params } = data;
  transformInputParams(input_params, variableKeyMap);
  return Object.keys(variableKeyMap);
};

export const MCPSchema: INodeSchema = {
  type: 'MCP',
  title: 'MCP',
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.MCP.schema.quickCall',
    dm: '快捷调用符合MCP协议的工具。',
  }),
  iconType: 'spark-MCP-mcp-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.MCP.schema.tool',
    dm: '工具',
  }),
  defaultParams: {
    input_params: [],
    output_params: [
      {
        key: 'output',
        type: 'String',
        desc: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.MCP.schema.result',
          dm: '结果',
        }),
      },
    ],

    node_param: {
      tool_name: '',
      server_code: '',
      server_name: '',
    } as IMCPNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-blue-hover)',
  customAdd: true,
  getRefVariables: (data) => getMCPNodeVariables(data as IMCPNodeData),
};
