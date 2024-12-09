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

import FileToolBarNode from '@/pages/Graph/Design/types/FileToolBarNode';
import { CalendarOutlined, MailOutlined } from '@ant-design/icons';
import '@xyflow/react/dist/style.css';
import { GetProp, Menu, MenuProps } from 'antd';
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
let initialNodes: any = [
  {
    id: '1',
    type: 'input',
    sourcePosition: 'right',
    targetPosition: 'left',
    data: {
      label: <div>Start</div>,
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
    type: 'base',
    data: {
      label: 'node 2',
      form: {
        name: '表单数据',
      },
    },
    position: { x: 0, y: 100 },
  },
];

let initialEdges = [
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

type MenuItem = GetProp<MenuProps, 'items'>[number];
const graphMenuItems: MenuItem[] = [
  {
    key: '1',
    icon: <MailOutlined />,
    label: 'Navigation One',
  },
  {
    key: '2',
    icon: <CalendarOutlined />,
    label: 'Navigation Two',
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
  const { fitView } = useReactFlow();
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

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
