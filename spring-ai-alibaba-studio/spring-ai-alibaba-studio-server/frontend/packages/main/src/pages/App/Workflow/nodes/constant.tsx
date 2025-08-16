import { IWorkFlowNode, NodeProps } from '@spark-ai/flow';
import ApiNode from './APINode/node';
import AppComponentNode from './AppComponent/node';
import ClassifyNode from './Classifier/node';
import EndNode from './End/node';
import InputNode from './InputNode/node';
import IteratorNode from './Iterator/node';
import IteratorEndNode from './IteratorEnd/node';
import IteratorStartNode from './IteratorStart/node';
import JudgeNode from './Judge/node';
import LLMNode from './LLM/node';
import MCPNode from './MCP/node';
import OutputNode from './Output/node';
import ParallelNode from './Parallel/node';
import ParallelEndNode from './ParallelEnd/node';
import ParallelStartNode from './ParallelStart/node';
import ParameterExtractorNode from './ParameterExtractor/node';
import PluginNode from './PluginNode/node';
import RetrievalNode from './Retrieval/node';
import ScriptNode from './Script/node';
import StartNode from './Start/node';
import VariableAssignNode from './VariableAssign/node';
import VariableHandleNode from './VariableHandle/node';

const NODE_COMPONENT_MAP: Record<
  string,
  React.ComponentType<NodeProps<IWorkFlowNode>>
> = {
  Start: StartNode,
  End: EndNode,
  LLM: LLMNode,
  Output: OutputNode,
  Script: ScriptNode,
  Judge: JudgeNode,
  Classifier: ClassifyNode,
  VariableHandle: VariableHandleNode,
  VariableAssign: VariableAssignNode,
  ParameterExtractor: ParameterExtractorNode,
  API: ApiNode,
  Retrieval: RetrievalNode,
  Input: InputNode,
  AppComponent: AppComponentNode,
  MCP: MCPNode,
  Plugin: PluginNode,
  Iterator: IteratorNode,
  Parallel: ParallelNode,
  IteratorStart: IteratorStartNode,
  IteratorEnd: IteratorEndNode,
  ParallelStart: ParallelStartNode,
  ParallelEnd: ParallelEndNode,
};

export default NODE_COMPONENT_MAP;
