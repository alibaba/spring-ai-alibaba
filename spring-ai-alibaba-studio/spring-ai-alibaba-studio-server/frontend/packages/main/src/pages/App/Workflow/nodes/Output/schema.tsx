import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import { IOutputNodeData, IOutputNodeParam } from '../../types';

const checkOutputNodeValid = (data: IOutputNodeData) => {
  const errMsgs: { label: string; error: string }[] = [];
  if (!data.node_param.output) {
    errMsgs.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.Output.schema.outputContent',
        dm: '输出内容',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.Output.schema.requiredField',
        dm: '不能为空',
      }),
    });
  }
  return errMsgs;
};

export const OutputSchema: INodeSchema = {
  type: 'Output',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Output.schema.flowOutput',
    dm: '流程输出',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Output.schema.interaction',
    dm: '将流程的中间信息输出。',
  }),
  iconType: 'spark-processOutput-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Output.schema.output',
    dm: '交互',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {
      output: '',
      stream_switch: true,
    } as IOutputNodeParam,
  },
  isSystem: false,
  allowSingleTest: false,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-teal-hover)',
  checkValid: (data) => checkOutputNodeValid(data as IOutputNodeData),
};
