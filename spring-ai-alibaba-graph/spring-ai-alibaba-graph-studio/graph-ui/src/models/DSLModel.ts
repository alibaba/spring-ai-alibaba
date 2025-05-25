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
