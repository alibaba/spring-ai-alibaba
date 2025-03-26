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

export type AppMeta = {
  description: '';
  icon: '';
  icon_background: '#FFFFFF';
  mode: 'workflow';
  name: '';
  use_icon_as_answer_icon: false;
};

export type GraphEdgeType = {
  cases: null;
  data: any;
  sourceType: string;
  targetType: string;
  id: string;
  selected: boolean;
  source: string;
  sourceHandle: string | null;
  target: string;
  targetHandle: string | null;
  targetMap: null;
  type: string;
  zindex: 0;
  order: number;
};
export type GraphNodeType = {
  id: string;
  height: number;
  width: number;
  type: string;
  title: string;
  selected: boolean;
  position: {
    x: number;
    y: number;
  };
  positionAbsolute: {
    x: number;
    y: number;
  };
  data: any;
  sourcePosition: 'top' | 'right' | 'bottom' | 'left';
  targetPosition: 'top' | 'right' | 'bottom' | 'left';
};
export type GraphType = {
  edges: GraphEdgeType[];
  nodes: GraphNodeType[];
};
export type SpecType = {
  conversationVariables: [];
  environmentVariables: [];
  features: any;
  graph: GraphType;
};

export type DSLModelType = {
  app: AppMeta;
  kind: string;
  version: string;
  spec: SpecType;
};
