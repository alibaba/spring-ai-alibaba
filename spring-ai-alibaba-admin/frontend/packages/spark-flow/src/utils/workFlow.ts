import { IWorkFlowNode } from '@/types/work-flow';
import { Edge } from '@xyflow/react';

/**
 * Get all upstream adjacent node parameter interfaces for the specified node
 */
export interface GetNeighborNodesParams {
  nodes: IWorkFlowNode[];
  edges: Edge[];
  nodeId: string;
  visited?: Set<string>;
}

/**
 * Get all upstream adjacent nodes of the specified node
 * @param params - Parameter object
 * @returns A mapping of adjacent nodes, with the key as the node ID and the value as the node object
 */
export const getNeighborNodes = (
  params: GetNeighborNodesParams,
): Record<string, IWorkFlowNode> => {
  const { nodes, edges, nodeId, visited = new Set() } = params;

  if (visited.has(nodeId)) {
    return {};
  }

  visited.add(nodeId);

  const edgeItems = edges.filter((item) => item.target === nodeId);
  if (!edgeItems.length) {
    return {};
  }

  const sourceNodeIds = new Set(edgeItems.map((edge) => edge.source));

  const sourceNodes = nodes.filter((item) => sourceNodeIds.has(item.id));

  const nodesMap: Record<string, IWorkFlowNode> = {};
  sourceNodes.forEach((item) => {
    nodesMap[item.id] = item;

    const neighborNodes = getNeighborNodes({
      nodes,
      edges,
      nodeId: item.id,
      visited,
    });
    Object.assign(nodesMap, neighborNodes);
  });

  return nodesMap;
};
