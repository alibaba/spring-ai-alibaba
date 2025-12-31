import $i18n from '@/i18n';
import type {
  INodeDataInputParamItem,
  INodeDataOutputParamItem,
  INodeSchema,
} from '@spark-ai/flow';
import { RETRY_CONFIG_DEFAULT, TRY_CATCH_CONFIG_DEFAULT } from '../../constant';
import { IApiNodeData, IApiNodeParam } from '../../types';
import { getVariablesFromText, transformInputParams } from '../../utils';

export const API_OUTPUT_DEFAULT_PARAMS: INodeDataOutputParamItem[] = [
  {
    key: 'output',
    type: 'String',
    desc: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.APINode.schema.outputResult',
      dm: '输出结果',
    }),
  },
];

const checkApiNodeDataValid = (data: IApiNodeData) => {
  const errorMsg: { label: string; error: string }[] = [];
  if (!data.node_param.url) {
    errorMsg.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.APINode.schema.apiAddress',
        dm: 'API地址',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.APINode.schema.required',
        dm: '不能为空',
      }),
    });
  }

  return errorMsg;
};

const getApiVariables = (data: IApiNodeData) => {
  const variableKeyMap: Record<string, boolean> = {};
  const { headers = [], params = [] } = data.node_param;

  getVariablesFromText(data.node_param.url as string, variableKeyMap);
  transformInputParams(headers, variableKeyMap);
  transformInputParams(params, variableKeyMap);
  if (['raw', 'json'].includes(data.node_param.body.type))
    getVariablesFromText(data.node_param.body.data as string, variableKeyMap);
  if (data.node_param.body.type === 'form-data') {
    const bodyData = data.node_param.body.data as INodeDataInputParamItem[];
    transformInputParams(bodyData, variableKeyMap);
  }

  return Object.keys(variableKeyMap);
};

export const APISchema: INodeSchema = {
  type: 'API',
  title: 'API',
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.APINode.schema.callCustomApiService',
    dm: '通过Post或Get的方式，调用自定义API服务，输出API调用结果。',
  }),
  iconType: 'spark-api-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.APINode.schema.tool',
    dm: '工具',
  }),
  defaultParams: {
    input_params: [],
    output_params: API_OUTPUT_DEFAULT_PARAMS,
    node_param: {
      output_type: 'json',
      authorization: {
        auth_type: 'NoAuth',
        auth_config: void 0,
      },
      headers: [
        {
          key: 'header1',
          value_from: 'refer',
          value: void 0,
          type: 'String',
        },
      ],

      method: 'get',
      params: [],
      body: {
        type: 'raw',
        data: '',
      },
      timeout: {
        read: 3,
      },
      retry_config: RETRY_CONFIG_DEFAULT,
      try_catch_config: TRY_CATCH_CONFIG_DEFAULT,
      url: '',
    } as IApiNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-blue-hover)',
  checkValid: (data) => checkApiNodeDataValid(data as IApiNodeData),
  getRefVariables: (data) => getApiVariables(data as IApiNodeData),
};
