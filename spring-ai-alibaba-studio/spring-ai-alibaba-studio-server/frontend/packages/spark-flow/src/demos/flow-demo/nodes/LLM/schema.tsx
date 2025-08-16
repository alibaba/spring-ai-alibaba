import $i18n from '@/i18n';
import { INodeSchema } from '@/types/work-flow';
import {
  LLM_NODE_OUTPUT_PARAMS_DEFAULT,
  RETRY_CONFIG_DEFAULT,
  SELECTED_MODEL_PARAMS_DEFAULT,
  SHORT_MEMORY_CONFIG_DEFAULT,
  TRY_CATCH_CONFIG_DEFAULT,
} from '../../constant';
import { ILLMNodeParam } from '../../types/flow';

export const LLMSchema: INodeSchema = {
  type: 'LLM',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.largeModel',
    dm: '大模型',
  }),
  iconType: 'spark-modePlaza-line',
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.largeModelNode',
    dm: '大模型节点',
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
      try_catch_config: TRY_CATCH_CONFIG_DEFAULT,
    } as ILLMNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.basic',
    dm: '基础',
  }),
  checkValid: () => [],
  bgColor: 'var(--spark-ant-color-purple-hover)',
};
