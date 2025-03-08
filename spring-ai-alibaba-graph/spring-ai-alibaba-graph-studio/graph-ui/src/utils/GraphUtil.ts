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

import { graphState } from '@/store/GraphState';
import { produce } from 'immer';

/**
 * todo
 * re-layout the graph map
 * e.g.
 * Force-Directed Layout
 * Hierarchical Layout
 * Circular Layout
 * Orthogonal Layout
 * Orthogonal Layout
 * @param nodes
 */
export function reLayout(): any {
  let widthGap = 200;
  let heightGap = 100;
  let pre: any = null;
  graphState.nodes = produce(graphState.nodes, (draft: any[]) => {
    draft
      .sort((a: any, b: any) => a.position.x - b.position.x)
      .forEach((x: any) => {
        if (pre === null) {
          pre = x;
          pre.position.x = 0;
          pre.position.y = 150;
          return;
        }
        let offsetW = pre.width || 100;
        let offsetH = pre.height || 100;
        let offsetX = x.position.x - pre.position.x;
        let offsetY = x.position.y - pre.position.y;
        if (Math.abs(offsetX) < offsetW / 2) {
          x.position.x = pre.position.x;
          x.position.y = pre.position.y + offsetH + heightGap;
        } else {
          if (Math.abs(offsetY) < offsetH / 2) {
            x.position.y = pre.position.y;
          } else {
            x.position.y =
              pre.position.y +
              (offsetY > 0 ? offsetH + heightGap : -offsetH - heightGap);
          }
          x.position.x = pre.position.x + offsetW + widthGap;
        }
        pre = x;
      });
  });
}
