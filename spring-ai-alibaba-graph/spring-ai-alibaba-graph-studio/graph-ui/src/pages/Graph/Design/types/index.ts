import BranchNode from '@/pages/Graph/Design/types/BranchNode';
import CodeNode from '@/pages/Graph/Design/types/CodeNode';
import CustomNode from '@/pages/Graph/Design/types/CustomNode';
import DefaultNode from '@/pages/Graph/Design/types/DefaultNode';
import EndNode from '@/pages/Graph/Design/types/EndNode';
import KnowledgeRetrievalNode from '@/pages/Graph/Design/types/KnowledgeRetrievalNode';
import LLMNode from '@/pages/Graph/Design/types/LLMNode';
import StartNode from '@/pages/Graph/Design/types/StartNode';
import VariableAggregatorNode from '@/pages/Graph/Design/types/VariableAggregatorNode';

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
