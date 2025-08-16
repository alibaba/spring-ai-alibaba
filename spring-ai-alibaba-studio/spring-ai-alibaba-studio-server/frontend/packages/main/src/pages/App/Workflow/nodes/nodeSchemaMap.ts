import $i18n from '@/i18n';
import { INodeSchema } from '@spark-ai/flow';
import { APISchema } from './APINode/schema';
import { AppComponentSchema } from './AppComponent/schema';
import { ClassifierSchema } from './Classifier/schema';
import { EndSchema } from './End/schema';
import { InputSchema } from './InputNode/schema';
import { IteratorSchema } from './Iterator/schema';
import { IteratorEndSchema } from './IteratorEnd/schema';
import { IteratorStartSchema } from './IteratorStart/schema';
import { JudgeSchema } from './Judge/schema';
import { LLMSchema } from './LLM/schema';
import { MCPSchema } from './MCP/schema';
import { OutputSchema } from './Output/schema';
import { ParallelSchema } from './Parallel/schema';
import { ParallelEndSchema } from './ParallelEnd/schema';
import { ParallelStartSchema } from './ParallelStart/schema';
import { ParameterExtractorSchema } from './ParameterExtractor/schema';
import { PluginSchema } from './PluginNode/schema';
import { RetrievalSchema } from './Retrieval/schema';
import { ScriptSchema } from './Script/schema';
import { StartSchema } from './Start/schema';
import { VariableAssignSchema } from './VariableAssign/schema';
import { VariableHandleSchema } from './VariableHandle/schema';

export const SystemSchema: INodeSchema = {
  type: 'sys',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.constant.NodeSchema.index.system',
    dm: '系统',
  }),
  iconType: 'spark-summary-line',
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.constant.NodeSchema.index.system',
    dm: '系统',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {},
  },
  bgColor: 'var(--ag-ant-color-purple-hover)',
  hideInMenu: true,
};

export const ConversationSchema: INodeSchema = {
  type: 'conversation',
  title: $i18n.get({
    id: 'main.pages.App.Workflow.constant.NodeSchema.index.conversation',
    dm: '会话',
  }),
  iconType: 'spark-summary-line',
  desc: $i18n.get({
    id: 'main.pages.App.Workflow.constant.NodeSchema.index.conversationVariable',
    dm: '会话变量',
  }),
  defaultParams: {
    input_params: [],
    output_params: [],
    node_param: {},
  },
  bgColor: 'var(--ag-ant-color-purple-hover)',
  hideInMenu: true,
};

export const NODE_SCHEMA_MAP: Record<string, INodeSchema> = {
  sys: SystemSchema,
  conversation: ConversationSchema,
  Start: StartSchema,
  End: EndSchema,
  LLM: LLMSchema,
  Retrieval: RetrievalSchema,
  Input: InputSchema,
  Output: OutputSchema,
  Script: ScriptSchema,
  Judge: JudgeSchema,
  Classifier: ClassifierSchema,
  VariableHandle: VariableHandleSchema,
  VariableAssign: VariableAssignSchema,
  ParameterExtractor: ParameterExtractorSchema,
  MCP: MCPSchema,
  API: APISchema,
  AppComponent: AppComponentSchema,
  Plugin: PluginSchema,
  Iterator: IteratorSchema,
  Parallel: ParallelSchema,
  IteratorStart: IteratorStartSchema,
  IteratorEnd: IteratorEndSchema,
  ParallelStart: ParallelStartSchema,
  ParallelEnd: ParallelEndSchema,
};
