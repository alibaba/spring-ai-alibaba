import $i18n from '@/i18n';
import { INodeSchema, uniqueId } from '@spark-ai/flow';
import { IVariableAssignNodeData, IVariableAssignNodeParam } from '../../types';

const checkValid = (data: IVariableAssignNodeData) => {
  const errorMsg: { label: string; error: string }[] = [];
  let hasLeftEmpty = false;
  let hasRightEmpty = false;
  if (!data.node_param.inputs.length) {
    errorMsg.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.VariableAssign.schema.setVariable',
        dm: '设置变量',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.VariableAssign.schema.notNull',
        dm: '不能为空',
      }),
    });
  }

  data.node_param.inputs.forEach((item) => {
    if (!item.left.value && !hasLeftEmpty) {
      hasLeftEmpty = true;
      errorMsg.push({
        label: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.VariableAssign.schema.setVariable',
          dm: '设置变量',
        }),
        error: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.VariableAssign.schema.leftValueNotNull',
          dm: '左值不能为空',
        }),
      });
    }
    if (
      !item.right.value &&
      item.right.value_from !== 'clear' &&
      !hasRightEmpty
    ) {
      hasRightEmpty = true;
      errorMsg.push({
        label: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.VariableAssign.schema.setVariable',
          dm: '设置变量',
        }),
        error: $i18n.get({
          id: 'main.pages.App.Workflow.nodes.VariableAssign.schema.rightValueNotNull',
          dm: '右值不能为空',
        }),
      });
    }
  });

  return errorMsg;
};

export const VariableAssignSchema: INodeSchema = {
  type: 'VariableAssign',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.VariableAssign.schema.variableAssignment',
    dm: '变量赋值',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.VariableAssign.schema.assignVariables',
    dm: '用于对变量进行赋值。',
  }),
  iconType: 'spark-variableSetting-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.VariableAssign.schema.variable',
    dm: '变量',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      inputs: [
        {
          id: uniqueId(4),
          left: {
            value_from: 'refer',
            value: void 0,
            type: 'String',
          },
          right: {
            value_from: 'refer',
            value: void 0,
            type: 'String',
          },
        },
      ],
    } as IVariableAssignNodeParam,
  },
  isSystem: false,
  allowSingleTest: false,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-pink-hover)',
  checkValid: (data) => checkValid(data as IVariableAssignNodeData),
};
