import { MenuProps } from 'antd';

export type TODO = any;

export type IEdge = {
  animated: boolean;
  id: string;
  selected: boolean;
  source: string;
  target: string;
  type: string;
};

export type INode<T> = {
  data: {
    form: T;
    label: string | symbol;
  };
  id: string;
  measured: { width: number; height: number };
  position: { x: number; y: number };
  sourcePosition: string;
  targetPosition: string;
  type: string;
};

export type ISelections = {
  nodes: Array<INode<TODO>>;
  edges: Array<IEdge>;
};

export type ContextMenuType = {
  top: number;
  left: number;
  right: number;
  bottom: number;
} | null;

export type MenuItem = Required<MenuProps>['items'][number];

export type IGraphMenuItems = Array<MenuItem & { onClick: (e: TODO) => void }>;
