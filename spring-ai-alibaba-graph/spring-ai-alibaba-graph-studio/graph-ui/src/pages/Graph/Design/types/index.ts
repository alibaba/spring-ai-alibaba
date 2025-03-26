/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

///
/// Copyright 2024-2025 the original author or authors.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///      https://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
