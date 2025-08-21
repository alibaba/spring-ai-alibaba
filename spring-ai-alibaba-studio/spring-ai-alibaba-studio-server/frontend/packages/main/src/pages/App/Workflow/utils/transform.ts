import { IBizEdge, IBizFlowData, IBizNode } from '@/types/workflow';
import { Edge, IWorkFlowNode } from '@spark-ai/flow';
import { NODE_SCHEMA_MAP } from '../nodes/nodeSchemaMap';

export const transformToFlowNode = (node: IBizNode) => {
  return {
    id: node.id,
    type: node.type,
    position: {
      x: node.position.x || 0,
      y: node.position.y || 0,
    },
    width: node.width,
    height: node.height,
    parentId: node.parent_id,
    zIndex: node.parent_id ? 1002 : void 0,
    data: {
      label: node.name,
      desc: node.desc,
      input_params: node.config.input_params,
      output_params: node.config.output_params,
      node_param: node.config.node_param,
    },
  };
};

export const transformToFlowEdge = (edge: IBizEdge) => {
  return {
    id: edge.id,
    source: edge.source,
    target: edge.target,
    targetHandle: edge.target_handle,
    sourceHandle: edge.source_handle,
  };
};

export const transformToBizNode = (node: IWorkFlowNode) => {
  return {
    id: node.id,
    name: node.data.label,
    parent_id: node.parentId,
    desc: node.data.desc,
    config: {
      input_params: node.data.input_params,
      output_params: node.data.output_params,
      node_param: node.data.node_param,
    },
    position: {
      x: node.position.x,
      y: node.position.y,
    },
    width: node.measured?.width || 320,
    // 只有组节点会缓存高度
    height: NODE_SCHEMA_MAP[node.type]?.isGroup
      ? node.measured?.height
      : void 0,
    type: node.type,
  };
};

export const transformToBizEdge = (edge: Edge) => {
  return {
    id: edge.id,
    source: edge.source,
    target: edge.target,
    source_handle: edge.sourceHandle || edge.source,
    target_handle: edge.targetHandle || edge.target,
  };
};

export const transformToBizData = ({
  nodes,
  edges,
}: {
  nodes: IWorkFlowNode[];
  edges: Edge[];
}) => {
  const clearEdges = edges.filter(
    (edgeItem) => !!edgeItem.source && !!edgeItem.target,
  );
  const globalNodes: IWorkFlowNode[] = [];
  const subNodesMap: Record<string, IWorkFlowNode[]> = {};
  nodes.forEach((node) => {
    if (node.parentId) {
      if (!subNodesMap[node.parentId]) subNodesMap[node.parentId] = [];
      subNodesMap[node.parentId].push(node as IWorkFlowNode);
    } else {
      globalNodes.push(node as IWorkFlowNode);
    }
  });

  const globalEdges: IBizEdge[] = [];
  clearEdges.forEach((edge) => {
    if (
      globalNodes.filter((node) => [edge.source, edge.target].includes(node.id))
        .length === 2
    ) {
      globalEdges.push(transformToBizEdge(edge));
    }
  });

  const newNodes: IBizFlowData['nodes'] = (globalNodes as IWorkFlowNode[]).map(
    (node) => {
      const newNode = transformToBizNode(node);
      if (!NODE_SCHEMA_MAP[node.type]?.isGroup) return newNode;
      const subEdges: IBizEdge[] = [];
      clearEdges.forEach((edge) => {
        if (
          subNodesMap[node.id].some((node) =>
            [edge.source, edge.target].includes(node.id),
          )
        ) {
          subEdges.push(transformToBizEdge(edge));
        }
      });
      return {
        ...newNode,
        config: {
          ...newNode.config,
          node_param: {
            ...newNode.config.node_param,
            block: {
              nodes: subNodesMap[node.id].map(transformToBizNode),
              edges: subEdges,
            },
          },
        },
      } as IBizNode;
    },
  );

  return { nodes: newNodes, edges: globalEdges };
};

export const transformToFlowData = (data: IBizFlowData) => {
  const newNodes: IWorkFlowNode[] = [];
  const newEdges: Edge[] = data.edges.map(transformToFlowEdge);
  if (!data) return;
  data.nodes.forEach((item) => {
    const newNode = transformToFlowNode(item);
    if (!NODE_SCHEMA_MAP[item.type]?.isGroup) {
      newNodes.push(newNode);
    } else {
      const { block, ...extraNodeParams } = newNode.data.node_param;
      newNodes.push({
        ...newNode,
        data: {
          ...newNode.data,
          node_param: extraNodeParams,
        },
      });

      const { nodes: subNodes = [], edges: subEdges = [] } = block || {};
      subNodes.forEach((node: IBizNode) => {
        newNodes.push(transformToFlowNode(node));
      });

      subEdges.forEach((edge: IBizEdge) => {
        newEdges.push({
          ...transformToFlowEdge(edge),
          zIndex: 1002,
        });
      });
    }
  });

  return { nodes: newNodes, edges: newEdges };
};
