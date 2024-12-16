import { Box } from '@mui/material';
import { useAtom } from 'jotai';
import { useCallback } from 'react';
import ReactFlow, {
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  Background,
  Controls,
  EdgeChange,
  MiniMap,
  NodeChange,
  ReactFlowProvider,
} from 'react-flow-renderer';
import { edgesAtom, nodesAtom, selectedNodeAtom } from '../atoms/flowState';
import { BaseNode } from './Node';
import { NodeEditorDrawer } from './NodeEditorDrawer';
type EdgeParams = {
  source: string | null;
  target: string | null;
  sourceHandle: string | null;
  targetHandle: string | null;
};

const nodeTypes = {
  custom: BaseNode,
};

const FlowEditor = () => {
  const [nodes, setNodes] = useAtom(nodesAtom);
  const [edges, setEdges] = useAtom(edgesAtom);
  const [, setSelectedNode] = useAtom(selectedNodeAtom);

  const onNodesChange = useCallback(
    (changes: NodeChange[]) =>
      setNodes((nds) => applyNodeChanges(changes, nds)),
    [setNodes],
  );

  const onEdgesChange = useCallback(
    (changes: EdgeChange[]) =>
      setEdges((eds) => applyEdgeChanges(changes, eds)),
    [setEdges],
  );

  const onConnect = useCallback(
    (params: EdgeParams) => setEdges((eds) => addEdge(params, eds)),
    [setEdges],
  );

  const handlePaneClick = useCallback(() => {
    setSelectedNode(null);
  }, [setSelectedNode]);

  return (
    <Box display="flex" height="100vh">
      <Box flexGrow={1}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          nodeTypes={nodeTypes}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onPaneClick={handlePaneClick}
          fitView
        >
          <MiniMap
            nodeColor={(node) =>
              node.type === 'custom' ? '#007BFF' : '#FFCC00'
            }
            nodeStrokeWidth={3}
          />
          <Controls />
          <Background />
        </ReactFlow>
      </Box>
      <NodeEditorDrawer />
    </Box>
  );
};

export default () => (
  <ReactFlowProvider>
    <FlowEditor />
  </ReactFlowProvider>
);
