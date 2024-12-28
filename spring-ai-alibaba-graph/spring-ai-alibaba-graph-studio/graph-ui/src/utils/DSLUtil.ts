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
