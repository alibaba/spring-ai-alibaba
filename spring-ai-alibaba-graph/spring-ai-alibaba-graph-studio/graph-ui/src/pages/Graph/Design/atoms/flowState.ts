import { atom } from 'jotai';
import { Node, Edge, Position } from 'react-flow-renderer';
// 自定义节点数据结构
export const nodesAtom = atom<Node[]>([
  {
    id: 'start',
    type: 'custom', 
    data: { label: '开始', icon: '🏠', iconBg: '#007BFF', background: '#F4F6FA', isStartNode: true },
    position: { x: 100, y: 100 },
    sourcePosition: Position.Right,

  },
  {
    id: '1',
    type: 'custom',
    data: { label: 'LLM', icon: '⚙️', iconBg: '#5B8DEF', background: '#FFFFFF' },
    position: { x: 300, y: 0 },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,

  },
  {
    id: '2',
    type: 'custom',
    data: { label: 'HTTP 请求', icon: '🌐', iconBg: '#8B5CF6', background: '#FFFFFF'},
    position: { x: 500, y: 100 },
    targetPosition: Position.Left,
  },
]);

export const edgesAtom = atom<Edge[]>([
  {
    id: 'e1-2',
    source: 'start',
    target: '1',
    type: 'bezier',
  },
  {
    id: 'e2-3',
    source: '1',
    target: '2',
    type: 'bezier',
  },
]);


export const selectedNodeAtom = atom<string | null>(null);

export const drawerOpenAtom = atom<boolean>(false)
