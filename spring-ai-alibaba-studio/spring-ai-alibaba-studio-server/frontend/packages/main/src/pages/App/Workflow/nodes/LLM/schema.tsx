import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import {
  getDefaultTryCatchConfig,
  LLM_NODE_OUTPUT_PARAMS_DEFAULT,
  RETRY_CONFIG_DEFAULT,
  SELECTED_MODEL_PARAMS_DEFAULT,
  SHORT_MEMORY_CONFIG_DEFAULT,
} from '../../constant';
import { ILLMNodeData, ILLMNodeParam } from '../../types';
import {
  checkLLMData,
  checkShortMemory,
  checkTryCatchConfig,
  getVariablesFromText,
  transformInputParams,
} from '../../utils';

const checkLLMNodeDataValid = (data: ILLMNodeData) => {
  const errorMsg: { label: string; error: string }[] = [];
  checkLLMData(data.node_param.model_config, errorMsg);

  if (!data.node_param.sys_prompt_content) {
    errorMsg.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.LLM.schema.prompt',
        dm: '提示词',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.LLM.schema.requiredField',
        dm: '不能为空',
      }),
    });
  }

  if (!data.node_param.prompt_content) {
    errorMsg.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.LLM.schema.userPrompt',
        dm: '用户提示词',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.nodes.LLM.schema.requiredField',
        dm: '不能为空',
      }),
    });
  }

  checkTryCatchConfig(data.node_param.try_catch_config, errorMsg);

  checkShortMemory(data.node_param.short_memory, errorMsg);
  return errorMsg;
};

const getLLMNodeVariables = (data: ILLMNodeData) => {
  const variableKeyMap: Record<string, boolean> = {};
  const { prompt_content, sys_prompt_content } = data.node_param;
  getVariablesFromText(prompt_content, variableKeyMap);
  getVariablesFromText(sys_prompt_content, variableKeyMap);
  if (data.node_param.model_config.vision_config.enable) {
    transformInputParams(
      data.node_param.model_config.vision_config.params,
      variableKeyMap,
    );
  }

  return Object.keys(variableKeyMap);
};

export const LLMSchema: INodeSchema = {
  type: 'LLM',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.LLM.schema.largeModel',
    dm: '大模型',
  }),
  iconType: 'spark-modePlaza-line',
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.LLM.schema.callLargeModel',
    dm: '调用大模型，依据配置项以及Prompt进行内容生成。',
  }),
  defaultParams: {
    input_params: [],
    output_params: LLM_NODE_OUTPUT_PARAMS_DEFAULT,
    node_param: {
      sys_prompt_content: '',
      prompt_content: '',
      short_memory: SHORT_MEMORY_CONFIG_DEFAULT,
      model_config: SELECTED_MODEL_PARAMS_DEFAULT,
      retry_config: RETRY_CONFIG_DEFAULT,
      try_catch_config: getDefaultTryCatchConfig(
        LLM_NODE_OUTPUT_PARAMS_DEFAULT,
      ),
    } as ILLMNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  groupLabel: $i18n.get({
    id: 'main.pages.App.Workflow.nodes.LLM.schema.basic',
    dm: '基础',
  }),
  bgColor: 'var(--ag-ant-color-purple-hover)',
  getRefVariables: (data) => getLLMNodeVariables(data as ILLMNodeData),
  checkValid: (data) => checkLLMNodeDataValid(data as ILLMNodeData),
};
