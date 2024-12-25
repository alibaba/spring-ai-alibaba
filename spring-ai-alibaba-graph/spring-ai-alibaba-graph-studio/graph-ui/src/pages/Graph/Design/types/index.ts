import LLMNode from '@/pages/Graph/Design/types/LLMNode';
import StartNode from '@/pages/Graph/Design/types/StartNode';
import EndNode from '@/pages/Graph/Design/types/EndNode';
import CustomNode from '@/pages/Graph/Design/types/CustomNode';
import KnowledgeRetrievalNode from '@/pages/Graph/Design/types/KnowledgeRetrievalNode';
import CodeNode from '@/pages/Graph/Design/types/CodeNode';
import DefaultNode from '@/pages/Graph/Design/types/DefaultNode';
import VariableAggregatorNode from '@/pages/Graph/Design/types/VariableAggregatorNode';
import BranchNode from '@/pages/Graph/Design/types/BranchNode';

const NodeTypes = {
  llm: LLMNode,
  start: StartNode,
  end: EndNode,
  branch: BranchNode,
  custom: CustomNode,
  code: CodeNode,
  default: DefaultNode,
  'variable-aggregator': VariableAggregatorNode,
  'knowledge-retrieval': KnowledgeRetrievalNode,
};


export default NodeTypes;
