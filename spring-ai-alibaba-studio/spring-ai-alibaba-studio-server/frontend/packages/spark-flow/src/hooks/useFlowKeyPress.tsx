import { isEventInInput, transformToMacKey } from '@/utils';
import { useReactFlow } from '@xyflow/react';
import { useKeyPress } from 'ahooks';
import { useNodesReadOnly } from './useWorkFlow';

export default function useFlowKeyPress() {
  const reactFlow = useReactFlow();
  const { nodesReadOnly } = useNodesReadOnly();

  useKeyPress([`${transformToMacKey('ctrl')}.d`, 'delete'], (event) => {
    event.preventDefault();
    if (isEventInInput(event) || nodesReadOnly) return;
    const needDeleteNodes: { id: string }[] = [];
    const needDeleteEdges: { id: string }[] = [];
    const nodes = reactFlow.getNodes();
    const edges = reactFlow.getEdges();
    nodes.forEach((item) => {
      if (item.selected) needDeleteNodes.push({ id: item.id });
    });
    edges.forEach((item) => {
      if (item.selected) needDeleteEdges.push({ id: item.id });
    });
    if (!needDeleteNodes.length && !needDeleteEdges) return;
    reactFlow.deleteElements({
      nodes: needDeleteNodes,
      edges: needDeleteEdges,
    });
  });
}
