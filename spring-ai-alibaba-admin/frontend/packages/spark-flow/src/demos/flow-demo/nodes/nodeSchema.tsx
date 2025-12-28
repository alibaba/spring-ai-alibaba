import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import { APISchema } from './Api/schema';
import { ClassifySchema } from './Classify/schema';
import { EndSchema } from './End/schema';
import { IteratorSchema } from './Iterator/schema';
import { IteratorEndSchema } from './IteratorEnd/schema';
import { IteratorStartSchema } from './IteratorStart/schema';
import { JudgeSchema } from './Judge/schema';
import { LLMSchema } from './LLM/schema';
import { OutputSchema } from './Output/schema';
import { ParameterExtractorSchema } from './ParameterExtractor/schema';
import { ScriptSchema } from './Script/schema';
import { StartSchema } from './Start/schema';
import { VariableAssignSchema } from './VariableAssign/schema';
import { VariableHandleSchema } from './VariableHandle/schema';

export const NODE_SCHEMA_MAP: Record<string, INodeSchema> = {
  sys: {
    type: 'sys',
    title: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.system',
      dm: '系统',
    }),
    iconType: 'spark-summary-line',
    desc: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.constant.NodeSchema.systemNode',
      dm: '系统节点',
    }),
    defaultParams: {
      input_params: [],
      output_params: [],
      node_param: {},
    },
    bgColor: '#FA8125',
    hideInMenu: true,
  },
  Start: StartSchema,
  End: EndSchema,
  LLM: LLMSchema,
  Output: OutputSchema,
  Script: ScriptSchema,
  Judge: JudgeSchema,
  Classifier: ClassifySchema,
  Iterator: IteratorSchema,
  VariableHandle: VariableHandleSchema,
  VariableAssign: VariableAssignSchema,
  ParameterExtractor: ParameterExtractorSchema,
  API: APISchema,
  IteratorStart: IteratorStartSchema,
  IteratorEnd: IteratorEndSchema,
};
