import {
  Background,
  MiniMap,
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
  useRef,
  useState,
} from 'react';

import {
  generateNodeFromKey,
  NODE_TYPE,
} from '@/components/Nodes/Common/manageNodes';
import StartNode from '@/components/Nodes/StartNode';
import FileToolBarNode from '@/pages/Graph/Design/types/FileToolBarNode';
import {
  CopyOutlined,
  FileAddOutlined,
  SnippetsOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import '@xyflow/react/dist/style.css';
import { Menu, MenuProps } from 'antd';
import './index.less';
import './xyTheme.less';

////////////type////////////////
type ContextMenuType = {
  top: number;
  left: number;
  right: number;
  bottom: number;
} | null;
////////////type////////////////

const nodeTypes = {
  base: FileToolBarNode,
};
const initialNodes: any = [
  {
    id: '1',
    type: 'start',
    sourcePosition: 'right',
    targetPosition: 'left',
    data: {
      label: <StartNode />,
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
    type: 'start',
    data: {
      label: 'node 2',
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

const graphSubMenuItems = [
  {
    key: NODE_TYPE.START,
    label: '开始',
    element: <StartNode />,
  },
  // {
  //   key: 'node-branch',
  //   label: '条件分支',
  // },
  // {
  //   key: 'node-llm',
  //   label: 'LLM',
  // },
];

type MenuItem = Required<MenuProps>['items'][number];
const graphMenuItems: MenuItem[] = [
  {
    key: '1',
    icon: <FileAddOutlined />,
    label: '新建节点',
    children: graphSubMenuItems.map((item) => ({
      label: item?.label ?? '',
      key: item?.key,
    })),
  },
  {
    key: '2',
    icon: <CopyOutlined />,
    label: '复制',
  },
  {
    key: '3',
    icon: <SnippetsOutlined />,
    label: '粘贴',
  },
  {
    key: '4',
    icon: <UploadOutlined />,
    label: '导入 DSL',
  },
];
export const LayoutFlow = () => {
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
  // const [reactFlowInstance, setReactFlowInstance] = useState<any>();

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
  const onGrapContextMenu = useCallback(
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

  return (
    <ReactFlow
      ref={ref}
      nodes={nodes}
      onLoad={onLayout}
      edges={edges}
      nodeTypes={nodeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onContextMenu={onGrapContextMenu}
      onClick={clearGraphContextMenu}
      fitView
    >
      <Background />
      <MiniMap></MiniMap>

      {graphContextMenu &&
        `${graphContextMenu.left}_${graphContextMenu.right}_${graphContextMenu.top}_${graphContextMenu.bottom}`}
      {graphContextMenu && (
        <Menu
          onClick={(e) => {
            // const reactFlowBounds = ref.current.getBoundingClientRect();
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
            const newNode = generateNodeFromKey(e.key as NODE_TYPE, {
              x: nodePositionX,
              y: nodePositionY,
            });
            setNodes([...nodes, newNode]);
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
