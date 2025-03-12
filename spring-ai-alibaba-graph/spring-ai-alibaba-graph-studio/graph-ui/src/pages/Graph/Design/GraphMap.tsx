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

import { ContextMenu, ContextMenuItem } from '@/pages/Graph/Design/context';
import BottomToolBar from '@/pages/Graph/Design/toolbar/BottomToolBar';
import TopToolBar from '@/pages/Graph/Design/toolbar/TopToolBar';
import { graphState } from '@/store/GraphState';
import * as DSLUtil from '@/utils/DSLUtil';
import { reLayout } from '@/utils/GraphUtil';
import { handleNodeChanges } from '@/utils/NodeUtil';
import { FormattedMessage } from '@@/exports';
import { Icon } from '@iconify/react';
import { useProxy } from '@umijs/max';
import type { Node } from '@xyflow/react';
import {
  Background,
  MiniMap,
  ReactFlow,
  useReactFlow,
  useStoreApi,
  useViewport,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useEventListener } from 'ahooks';
import {
  memo,
  MutableRefObject,
  useCallback,
  useEffect,
  useRef,
  useState,
  type MouseEvent as ReactMouseEvent,
} from 'react';
import FormDrawer from './FormDrawer';
import './index.less';
import { OperationMode } from './types';
import NodeTypes from './types/index';
import './xyTheme.less';

const dsl = DSLUtil.loadDSL();
graphState.nodes.push(...dsl.getNodes());
graphState.edges.push(...dsl.getEdges());

type LayoutFlowProps = {
  operationMode: OperationMode;
};

const LayoutFlow: React.FC<LayoutFlowProps> = memo(({ operationMode }) => {
  const graphStore = useProxy(graphState);

  const { setEdges, onEdgesChange, setNodes }: any = useStoreApi().getState();
  const onConnect = useCallback((params: any) => {
    params.type = 'smoothstep';
    graphStore.edges = [...graphStore.edges, params];
    setEdges(graphStore.edges);
  }, []);
  const onNodesChangeHook = useCallback((changes: any) => {
    if (graphStore.readonly) return;
    handleNodeChanges(changes);
    setNodes(graphStore.nodes);
  }, []);
  const onNodesDeleteHook = useCallback(() => {
    if (graphStore.readonly) return;
  }, []);
  const onNodeClickHook = useCallback((event: ReactMouseEvent, node: Node) => {
    if (graphStore.readonly) return;
    graphStore.currentNodeId = node.id;
    graphStore.formDrawer.isOpen = true;
  }, []);
  const onNodeMouseEnterHook = useCallback(
    (event: ReactMouseEvent, node: Node) => {
      console.debug(event, node);
      if (graphStore.readonly) return;
    },
    [],
  );
  const onNodeMouseLeaveHook = useCallback(
    (event: ReactMouseEvent, node: Node) => {
      console.debug(event, node);
      if (graphStore.readonly) return;
    },
    [],
  );
  const onNodeMouseMoveHook = useCallback(
    (event: ReactMouseEvent, node: Node) => {
      console.debug(event, node);
      if (graphStore.readonly) return;
    },
    [],
  );

  const ref: MutableRefObject<any> = useRef(null);
  const defaultViewport = { x: 0, y: 0, zoom: 1.5 };
  const onGrapContextMenu = useCallback((event: ReactMouseEvent) => {
    event.preventDefault();
    const pane = ref.current.getBoundingClientRect();
    graphStore.contextMenu = {
      top: (event.clientY < pane.height && event.clientY) || event.clientY,
      left: (event.clientX < pane.width && event.clientX) || event.clientX,
      right: (event.clientX >= pane.width && pane.width - event.clientX) || 0,
      bottom:
        (event.clientY >= pane.height && pane.height - event.clientY) || 0,
      show: true,
    };
  }, []);

  return (
    <ReactFlow
      ref={ref}
      className={graphStore.readonly ? 'flow-read-only' : ''}
      nodeOrigin={[0, 0.5]}
      nodes={graphStore.nodes}
      // onLoad={onLayout}
      edges={graphStore.edges}
      nodeTypes={NodeTypes}
      onConnect={onConnect}
      onNodeMouseLeave={onNodeMouseLeaveHook}
      onNodeMouseEnter={onNodeMouseEnterHook}
      onNodeMouseMove={onNodeMouseMoveHook}
      onNodeClick={onNodeClickHook}
      onNodesChange={onNodesChangeHook}
      onNodesDelete={onNodesDeleteHook}
      onEdgesChange={onEdgesChange}
      onContextMenu={onGrapContextMenu}
      panOnDrag={operationMode === OperationMode.HAND}
      selectionOnDrag={operationMode === OperationMode.POINT}
      onClick={() => {
        graphStore.contextMenu.show = false;
        if (graphStore.mode === 'drag') {
          graphStore.mode = 'normal';
          graphStore.readonly = false;
        }
      }}
      defaultViewport={defaultViewport}
      attributionPosition="top-right"
      style={{ backgroundColor: '#F7F9FB' }}
    >
      <Background />

      <MiniMap pannable zoomable></MiniMap>
      <FormDrawer />
    </ReactFlow>
  );
});

export default memo(() => {
  const graphStore = useProxy(graphState);
  const viewport = useViewport();
  const { screenToFlowPosition, fitView } = useReactFlow();
  const { setNodes } = useStoreApi().getState();
  const graphRef: MutableRefObject<any> = useRef(null);

  const [operationMode, setOperationMode] = useState(OperationMode.HAND);

  const changeOperationMode = (mode: OperationMode) => {
    setOperationMode(mode);
  };

  useEffect(() => {}, [
    screenToFlowPosition,
    graphStore.mousePosition,
    setNodes,
  ]);

  const addNode = useCallback(
    (event: any, nodeType?: string) => {
      const { clientX, clientY } = event;
      let id = String(new Date());

      const newNode: any = {
        id,
        position: screenToFlowPosition({
          x: clientX,
          y: clientY,
        }),
        type: nodeType || 'default',
        data: {
          label: `Node ${id}`,
        },
        // TODO: It conflicts with the `openPanel` method when creating a new node.
        // selected: true,
        origin: [0, 0.0],
      };
      graphStore.currentNodeId = id;
      graphStore.nodes = [...graphStore.nodes, newNode];
      setNodes(graphStore.nodes);
    },
    [screenToFlowPosition],
  );

  const graphMenuItems: ContextMenuItem[] = [
    {
      key: '1',
      icon: <Icon icon="hugeicons:subnode-add" />,
      label: <FormattedMessage id={'page.graph.contextMenu.add-node'} />,
      children: Object.keys(NodeTypes).map((nodeType) => ({
        label: nodeType,
        key: nodeType,
        onClick: (event) => {
          addNode(event.domEvent, nodeType);
          graphStore.mode = 'drag';
          graphStore.readonly = true;
          graphStore.contextMenu.show = false;
        },
      })),
    },
    {
      key: '2',
      icon: <Icon icon="prime:file-import" />,
      label: <FormattedMessage id={'page.graph.contextMenu.import-dsl'} />,
    },
    {
      key: '3',
      icon: <Icon icon="prime:file-export" />,
      label: <FormattedMessage id={'page.graph.contextMenu.export-dsl'} />,
    },
  ];

  const reLayoutCallback = useCallback(() => {
    reLayout();
    fitView();
    setNodes(graphStore.nodes);
  }, [reLayout]);

  useEventListener('mousemove', (e) => {
    const rect = graphRef.current?.getBoundingClientRect();
    if (rect) {
      graphStore.mousePosition = screenToFlowPosition({
        x: e.clientX,
        y: e.clientY,
      });
      let changes: any[] = [];

      if (graphStore.mode === 'drag') {
        changes.push({
          id: graphStore.currentNodeId,
          type: 'position',
          position: graphStore.mousePosition,
        });
        handleNodeChanges(changes);
        setNodes(graphStore.nodes);
      }
    }
  });

  return (
    <div
      style={{
        width: '100%',
        height: '100%',
      }}
      ref={graphRef}
    >
      <LayoutFlow operationMode={operationMode}></LayoutFlow>
      <ContextMenu items={graphMenuItems}></ContextMenu>
      <TopToolBar></TopToolBar>
      <BottomToolBar
        viewport={viewport}
        reLayoutCallback={reLayoutCallback}
        changeOperationMode={changeOperationMode}
      ></BottomToolBar>
    </div>
  );
});
