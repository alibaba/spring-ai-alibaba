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

import {
  Background,
  MiniMap,
  OnSelectionChangeParams,
  ReactFlow,
  useEdgesState,
  useNodesState,
  useReactFlow,
} from '@xyflow/react';
import {
  type MouseEvent as ReactMouseEvent,
  MutableRefObject,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

import {
  copy,
  generateNodeFromKey,
  NODE_TITLE,
  NODE_TYPE,
  paste,
} from '@/components/Nodes/Common/manageNodes';
import FileToolBarNode from '@/pages/Graph/Design/types/FileToolBarNode';
import {
  CopyOutlined,
  FileAddOutlined,
  SnippetsOutlined,
} from '@ant-design/icons';
import { useIntl } from '@umijs/max';
import '@xyflow/react/dist/style.css';
import { Menu } from 'antd';
import './index.less';
import { ContextMenuType, IGraphMenuItems, ISelections, TODO } from './types';
import './xyTheme.less';

const nodeTypes = {
  base: FileToolBarNode,
};
const initialNodes: any = [
  {
    id: '1',
    type: NODE_TYPE.START,
    sourcePosition: 'right',
    targetPosition: 'left',
    data: {
      label: NODE_TITLE[NODE_TYPE.START],
      form: {
        name: 1,
      },
    },
    position: { x: 0, y: 0 },
  },
  {
    id: '2',
    sourcePosition: 'right',
    targetPosition: 'left',
    type: NODE_TYPE.START,
    data: {
      label: NODE_TITLE[NODE_TYPE.START],
      form: {
        name: '表单数据',
      },
    },
    position: { x: 0, y: 100 },
  },
];

const initialEdges = [
  {
    id: 'e12',
    type: 'smoothstep',
    source: '1',
    target: '2',
    animated: true,
  },
];

const getLayoutedElements = (nodes: any, edges: any) => {
  return { nodes, edges };
};

export const LayoutFlow = () => {
  const intl = useIntl();
  const graphSubMenuItems = [
    {
      key: NODE_TYPE.START,
      label: intl.formatMessage({ id: 'page.graph.start' }),
    },
    {
      key: NODE_TYPE.LLM,
      label: intl.formatMessage({ id: 'page.graph.llm' }),
    },
  ];
  const handleContext = (e: MouseEvent) => {
    e.preventDefault();
  };
  useEffect(() => {
    document.addEventListener('contextmenu', handleContext);
    return () => {
      document.removeEventListener('contextmenu', handleContext);
    };
  }, []);
  const reactFlowInstance = useReactFlow();
  const { fitView } = reactFlowInstance;
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [selections, setSelections] = useState<ISelections>();

  const onLayout = useCallback(() => {
    const layouted = getLayoutedElements(nodes, edges);
    setNodes([...layouted.nodes]);
    setEdges([...layouted.edges]);
    window.requestAnimationFrame(() => {
      fitView();
    });
  }, [nodes, edges]);

  const ref: MutableRefObject<any> = useRef(null);

  const [graphContextMenu, setGraphContextMenu] =
    useState<ContextMenuType>(null);
  const onGraphContextMenu = useCallback(
    (event: ReactMouseEvent) => {
      // Prevent native context menu from showing
      event.preventDefault();
      // Calculate position of the context menu. We want to make sure it
      // doesn't get positioned off-screen.
      const pane = ref.current.getBoundingClientRect();
      setGraphContextMenu({
        top: (event.clientY < pane.height && event.clientY) || event.clientY,
        left: (event.clientX < pane.width && event.clientX) || event.clientX,
        right: (event.clientX >= pane.width && pane.width - event.clientX) || 0,
        bottom:
          (event.clientY >= pane.height && pane.height - event.clientY) || 0,
      });

      console.log(pane);
    },
    [setGraphContextMenu],
  );
  const clearGraphContextMenu = useCallback(() => {
    setGraphContextMenu(null);
  }, [setGraphContextMenu]);

  const onSelectionChange = useCallback(
    (selections: OnSelectionChangeParams) => {
      console.log(selections);
      setSelections(selections as ISelections);
    },
    [setSelections],
  );

  const getMenuOperationPosition = (e: TODO) => {
    const domEvent = e.domEvent as unknown as ReactMouseEvent<
      HTMLElement,
      MouseEvent
    >;
    const { x, y } = reactFlowInstance.getViewport();
    const scale = reactFlowInstance.getZoom();
    const clientX = domEvent.clientX;
    const clientY = domEvent.clientY;
    const nodePositionX = (clientX - x - 600) / scale;
    const nodePositionY = (clientY - y - 200) / scale;
    return {
      x: nodePositionX,
      y: nodePositionY,
    };
  };

  const graphMenuItems: IGraphMenuItems = useMemo(
    () => [
      {
        key: 'create',
        icon: <FileAddOutlined />,
        label: intl.formatMessage({ id: 'page.graph.createNode' }),
        children: graphSubMenuItems.map((item) => ({
          label: item?.label ?? '',
          key: item?.key,
        })),
        onClick: (e) => {
          const { x, y } = getMenuOperationPosition(e);
          const newNode = generateNodeFromKey(e.key as NODE_TYPE, {
            x,
            y,
          });
          setNodes([...nodes, newNode]);
        },
      },
      {
        key: 'copy',
        icon: <CopyOutlined />,
        label: intl.formatMessage({ id: 'page.graph.copy' }),
        disabled: selections?.nodes.length === 0,
        onClick: (e) => {
          const { nodes } = selections ?? {};
          if (nodes?.length) {
            copy(nodes[0], 'node');
          }
          console.log(e);
        },
      },
      {
        key: 'paste',
        icon: <SnippetsOutlined />,
        label: intl.formatMessage({ id: 'page.graph.paste' }),
        onClick: async (e) => {
          const { x, y } = getMenuOperationPosition(e);
          const newNode = await paste({ x, y });
          if (newNode) {
            setNodes([...nodes, newNode]);
          }
        },
      },
      // {
      //   key: 'importFromDSL',
      //   icon: <UploadOutlined />,
      //   label: '导入 DSL',
      //   onClick: (e) => {
      //     console.log(e);
      //   },
      // },
    ],
    [nodes, edges, selections],
  );

  return (
    <ReactFlow
      ref={ref}
      nodes={nodes}
      onLoad={onLayout}
      edges={edges}
      nodeTypes={nodeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onContextMenu={onGraphContextMenu}
      onClick={clearGraphContextMenu}
      onSelectionChange={onSelectionChange}
      fitView
    >
      <Background />
      <MiniMap></MiniMap>

      {graphContextMenu &&
        `${graphContextMenu.left}_${graphContextMenu.right}_${graphContextMenu.top}_${graphContextMenu.bottom}`}
      {graphContextMenu && (
        <Menu
          onClick={(e) => {
            console.log('click', e);
            const menuTargetKey = e.keyPath[e.keyPath.length - 1];
            const menuTarget = graphMenuItems.find(
              (i) => i?.key === menuTargetKey,
            );
            menuTarget?.onClick(e);
          }}
          className="graph-menu"
          style={{
            left: graphContextMenu.left,
            top: graphContextMenu.top,
          }}
          defaultSelectedKeys={['1']}
          defaultOpenKeys={['sub1']}
          items={graphMenuItems}
        />
      )}
    </ReactFlow>
  );
};
