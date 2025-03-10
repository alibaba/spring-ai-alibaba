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

/**
 * Node Interaction
 */
import { graphState } from '@/store/GraphState';
import { applyNodeChanges } from '@xyflow/react';
import { NodeBase, NodeChange } from '@xyflow/system';
import { produce } from 'immer';

/**
 * e.g.
 * position
 * selected state
 * size
 * @param changes
 */
export function handleNodeChanges(changes: NodeChange[]) {
  graphState.nodes = produce(graphState.nodes, (draft: any) => {
    let tmp_arr: NodeBase[] = applyNodeChanges(changes, draft);
    let idx = 0;
    for (let i = 0; i < draft.length; i++) {
      if (!tmp_arr[i]) {
        continue;
      }
      draft[idx++] = tmp_arr[i];
    }
  });
}
