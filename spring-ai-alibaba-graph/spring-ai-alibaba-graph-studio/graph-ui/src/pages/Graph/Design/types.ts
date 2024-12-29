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

export enum ZoomType {
  ZOOM_TO_25 = 'zoomTo25',
  ZOOM_TO_50 = 'zoomTo50',
  ZOOM_TO_75 = 'zoomTo75',
  ZOOM_TO_100 = 'zoomTo100',
  ZOOM_TO_150 = 'zoomTo150',
  ZOOM_TO_200 = 'zoomTo200',
}

export enum OperationMode {
  HAND = 'hand',
  POINT = 'pointer',
}
