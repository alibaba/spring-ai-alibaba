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

// @ts-ignore
import jsyaml from 'js-yaml';

// @ts-ignore
import demoYamlFile from '!!raw-loader!./demo.yaml';
import { DSLModelType } from '@/models/DSLModel';

/**
 * handle dsl
 * todo
 */
export function loadDSL() {
  // read an app form dsl
  const dsl: DSLModelType = jsyaml.load(demoYamlFile);

  // pre handle
  {
  }

  function getEdges(): any {
    return dsl.spec.graph.edges.map((x) => {
      let newEdge = { ...x };
      newEdge.type = 'smoothstep';
      if (newEdge.sourceHandle === 'source') {
        newEdge.sourceHandle = null;
      }
      if (newEdge.targetHandle === 'target') {
        newEdge.targetHandle = null;
      }
      newEdge.id = `xy_edge_${newEdge.source}_${newEdge.sourceHandle}_to_${newEdge.target}_${newEdge.targetHandle}`;
      return newEdge;
    });
  }

  function getNodes(): any {
    return dsl.spec.graph.nodes.map((x) => {
      return {
        id: x.id,
        selected: x.selected,
        sourcePosition: x.sourcePosition || 'right',
        targetPosition: x.targetPosition || 'left',
        type: x.type,
        data: {
          label: x.title,
          cases: x.data.cases,
          nodeId: x.id,
          org: x,
        },
        position: x.position,
      };
    });
  }

  return {
    getEdges,
    getNodes,
  };
}

export function dumpDSL() {}
