import ApiNode from './Api/node';
import ClassifyNode from './Classify/node';
import EndNode from './End/node';
import IteratorNode from './Iterator/node';
import IteratorEndNode from './IteratorEnd/node';
import IteratorStartNode from './IteratorStart/node';
import JudgeNode from './Judge/node';
import LLMNode from './LLM/node';
import OutputNode from './Output/node';
import ParameterExtractorNode from './ParameterExtractor/node';
import ScriptNode from './Script/node';
import StartNode from './Start/node';
import VariableAssignNode from './VariableAssign/node';
import VariableHandleNode from './VariableHandle/node';

const NODE_COMPONENT_MAP = {
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
  Iterator: IteratorNode,
  IteratorStart: IteratorStartNode,
  IteratorEnd: IteratorEndNode,
};

export default NODE_COMPONENT_MAP;
