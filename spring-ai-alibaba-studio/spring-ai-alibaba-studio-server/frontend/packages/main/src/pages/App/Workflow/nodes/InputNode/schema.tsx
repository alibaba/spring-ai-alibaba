import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import { IInputNodeData } from '../../types';
import { checkInputParams } from '../../utils';

const checkInputNodeValid = (data: IInputNodeData) => {
  const errMsgs: { label: string; error: string }[] = [];
  checkInputParams(data.output_params, errMsgs);
  return errMsgs;
};
export const InputSchema: INodeSchema = {
  type: 'Input',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.InputNode.schema.processInput',
    dm: '流程输入',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.InputNode.schema.supplementInformationInProcess',
    dm: '在流程中进行信息的补充。',
  }),
  iconType: 'spark-processInput-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.InputNode.schema.interaction',
    dm: '交互',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {},
  },
  isSystem: false,
  allowSingleTest: false,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-teal-hover)',
  checkValid: (data) => checkInputNodeValid(data as IInputNodeData),
};
