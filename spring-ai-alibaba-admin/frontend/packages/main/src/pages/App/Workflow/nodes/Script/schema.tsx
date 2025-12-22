import $i18n from '@/i18n';
import { CODE_DEMO_MAP, INodeSchema } from '@spark-ai/flow';
import {
  RETRY_CONFIG_DEFAULT,
  SCRIPT_NODE_INPUT_PARAMS_DEFAULT,
  SCRIPT_NODE_OUTPUT_PARAMS_DEFAULT,
  TRY_CATCH_CONFIG_DEFAULT,
} from '../../constant';
import { IScriptNodeData, IScriptNodeParam } from '../../types';
import { checkInputParams, transformInputParams } from '../../utils';

const getScriptRefVariables = (data: IScriptNodeData) => {
  const variableKeyMap: Record<string, boolean> = {};
  transformInputParams(data.input_params, variableKeyMap);

  return Object.keys(variableKeyMap);
};

const checkScriptNodeValid = (data: IScriptNodeData) => {
  const errMsgs: { label: string; error: string }[] = [];
  checkInputParams(data.input_params, errMsgs);
  if (!data.node_param.script_content) {
    errMsgs.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.Script.schema.scriptContent',
        dm: '脚本内容',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.Script.schema.requiredField',
        dm: '不能为空',
      }),
    });
  }
  checkInputParams(data.output_params, errMsgs, {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Script.schema.output',
      dm: '输出',
    }),
  });
  return errMsgs;
};

export const ScriptSchema: INodeSchema = {
  type: 'Script',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Script.schema.script',
    dm: '脚本',
  }),
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Script.schema.2',
    dm: '提供自定义逻辑扩展，用脚本规则处理数据。',
  }),
  iconType: 'spark-fileCode-line',
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.Script.schema.logic',
    dm: '逻辑',
  }),
  defaultParams: {
    input_params: SCRIPT_NODE_INPUT_PARAMS_DEFAULT,
    output_params: SCRIPT_NODE_OUTPUT_PARAMS_DEFAULT,
    node_param: {
      script_content: CODE_DEMO_MAP.javascript,
      script_type: 'javascript',
      retry_config: RETRY_CONFIG_DEFAULT,
      try_catch_config: TRY_CATCH_CONFIG_DEFAULT,
    } as IScriptNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--ag-ant-color-orange-hover)',
  getRefVariables: (val) => getScriptRefVariables(val as IScriptNodeData),
  checkValid: (data) => checkScriptNodeValid(data as IScriptNodeData),
};
