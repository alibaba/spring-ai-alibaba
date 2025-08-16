import $i18n from '@/i18n';
import { INodeSchema } from '@/types/work-flow';
import {
  RETRY_CONFIG_DEFAULT,
  SCRIPT_NODE_INPUT_PARAMS_DEFAULT,
  SCRIPT_NODE_OUTPUT_PARAMS_DEFAULT,
  TRY_CATCH_CONFIG_DEFAULT,
} from '../../constant';
import { IScriptNodeParam } from '../../types/flow';

export const ScriptSchema: INodeSchema = {
  type: 'Script',
  title: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.script',
    dm: '脚本',
  }),
  desc: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.scriptNode',
    dm: '脚本节点',
  }),
  iconType: 'spark-fileCode-line',
  groupLabel: $i18n.get({
    id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.logic',
    dm: '逻辑',
  }),
  defaultParams: {
    input_params: SCRIPT_NODE_INPUT_PARAMS_DEFAULT,
    output_params: SCRIPT_NODE_OUTPUT_PARAMS_DEFAULT,
    node_param: {
      script_content: '',
      script_type: 'javascript',
      retry_config: RETRY_CONFIG_DEFAULT,
      try_catch_config: TRY_CATCH_CONFIG_DEFAULT,
    } as IScriptNodeParam,
  },
  isSystem: false,
  allowSingleTest: true,
  disableConnectSource: true,
  disableConnectTarget: true,
  bgColor: 'var(--spark-ant-color-orange-hover)',
};
