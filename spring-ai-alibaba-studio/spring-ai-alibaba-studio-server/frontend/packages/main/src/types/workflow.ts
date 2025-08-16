import type {
  INodeDataInputParamItem,
  INodeDataOutputParamItem,
} from '@spark-ai/flow';

export interface IBizNode {
  id: string;
  type: string;
  name: string;
  position: {
    x: number;
    y: number;
  };
  config: {
    input_params: INodeDataInputParamItem[];
    output_params: INodeDataOutputParamItem[];
    node_param: any;
  };
  desc?: string;
  width: number;
  height?: number;
  parent_id?: string;
}

export interface IBizEdge {
  id: string;
  source: string;
  target: string;
  source_handle?: string;
  target_handle?: string;
}

export interface IBizFlowData {
  nodes: IBizNode[];
  edges: IBizEdge[];
}
