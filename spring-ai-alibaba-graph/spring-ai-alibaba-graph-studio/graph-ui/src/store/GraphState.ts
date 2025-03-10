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

import { proxy } from '@umijs/max';
import type { Edge, Node } from '@xyflow/react';

/**
 * graph state
 */
type graphStateType = {
  nodes: Node[];
  edges: Edge[];
  currentNodeId: string | null;

  mode: 'normal' | 'drag';
  // graph  env
  env: 'startup' | 'design' | 'runtime';
  readonly: boolean;
  contextMenu: {
    top: number;
    left: number;
    right: number;
    bottom: number;
    show: boolean;
  };
  mousePosition: {
    x: number;
    y: number;
  };
  formDrawer: {
    isOpen: boolean;
    // nodeId 2 formData
    formDataMap: Record<string, any>;
  };
};
export const graphState: graphStateType = proxy({
  nodes: [],
  edges: [],
  env: 'design',
  currentNodeId: null,
  mode: 'normal',
  readonly: false,
  contextMenu: {
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    show: false,
  },
  mousePosition: {
    x: 0,
    y: 0,
  },
  formDrawer: {
    isOpen: false,
    formDataMap: {},
  },
});
