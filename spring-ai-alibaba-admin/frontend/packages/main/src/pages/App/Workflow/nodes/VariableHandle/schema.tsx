import $i18n from '@/i18n';
import { INodeSchema, uniqueId } from '@spark-ai/flow';
import { IVariableHandleNodeData, IVariableHandleNodeParam } from '../../types';
import { checkInputParams } from '../../utils';

const checkValid = (data: IVariableHandleNodeData) => {
  const errorMsg: { label: string; error: string }[] = [];
  if (data.node_param.type === 'template') {
    if (!data.node_param.template_content) {
      errorMsg.push({
        label: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.VariableHandle.schema.outputContent',
          dm: '输出内容',
        }),
        error: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.VariableHandle.schema.notNull',
          dm: '不能为空',
        }),
      });
    }
  } else if (data.node_param.type === 'json') {
    checkInputParams(data.node_param.json_params, errorMsg, {
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.VariableHandle.schema.outputContent',
        dm: '输出内容',
      }),
    });
  } else if (data.node_param.type === 'group') {
    if (
      data.node_param.groups.some((item) =>
        item.variables.some((v) => !v.value),
      )
    ) {
      errorMsg.push({
        label: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.VariableHandle.schema.group',
          dm: '分组',
        }),
        error: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.VariableHandle.schema.notNull',
          dm: '不能为空',
        }),
      });
    }
  }

  return errorMsg;
};

export const VariableHandleSchema: INodeSchema = {
  type: 'VariableHandle',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.VariableHandle.schema.variableProcessing',
    dm: '变量处理',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.VariableHandle.schema.processVariables',
    dm: '对变量进行多种方法的处理。',
  }),
  iconType: 'spark-variableProcessing-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.VariableHandle.schema.variable',
    dm: '变量',
  }),
  defaultParams: {
    input_params: [],
    output_params: [
      {
        key: 'Group1',
        type: 'String',
        desc: void 0,
      },
    ],

    node_param: {
      type: 'group',
      group_strategy: 'firstNotNull',
      groups: [
        {
          group_id: uniqueId(4),
          group_name: 'Group1',
          output_type: 'String',
          variables: [
            {
              id: uniqueId(4),
              value_from: 'refer',
              value: void 0,
              type: 'String',
            },
            {
              id: uniqueId(4),
              value_from: 'refer',
              value: void 0,
              type: 'String',
            },
          ],
        },
      ],

      json_params: [],
      template_content: '',
    } as IVariableHandleNodeParam,
  },
  isSystem: false,
  allowSingleTest: false,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-pink-hover)',
  checkValid: (data) => checkValid(data as IVariableHandleNodeData),
};
