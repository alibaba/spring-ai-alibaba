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
