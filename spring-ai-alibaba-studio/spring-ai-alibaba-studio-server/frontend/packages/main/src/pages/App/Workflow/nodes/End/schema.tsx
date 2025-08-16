import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import { END_NODE_OUTPUT_PARAMS_DEFAULT } from '../../constant';
import { IEndNodeData, IEndNodeParam } from '../../types';
import { checkInputParams } from '../../utils';

const checkEndNodeDataValid = (data: IEndNodeData) => {
  const errorMsg: { label: string; error: string }[] = [];
  if (data.node_param.output_type === 'json') {
    checkInputParams(data.node_param.json_params, errorMsg, {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.End.schema输出',
        dm: '输出',
      }),
    });
  }
  if (data.node_param.output_type === 'text') {
    if (!data.node_param.text_template) {
      errorMsg.push({
        label: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.End.schema文本模板',
          dm: '文本模板',
        }),
        error: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.End.schema不能为空',
          dm: '不能为空',
        }),
      });
    }
  }
  return errorMsg;
};

export const EndSchema: INodeSchema = {
  type: 'End',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.End.schema.end',
    dm: '结束',
  }),
  iconType: 'spark-flag-line',
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.End.schema.endNode',
    dm: '结束节点',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      output_type: 'text',
      text_template: '',
      json_params: END_NODE_OUTPUT_PARAMS_DEFAULT,
      stream_switch: false,
    } as IEndNodeParam,
  },
  isSystem: true,
  hideInMenu: true,
  allowSingleTest: false,
  disableConnectSource: true,
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.End.schema.basic',
    dm: '基础',
  }),
  bgColor: 'var(--ag-ant-color-purple-hover)',
  checkValid: (data) => checkEndNodeDataValid(data as IEndNodeData),
};
