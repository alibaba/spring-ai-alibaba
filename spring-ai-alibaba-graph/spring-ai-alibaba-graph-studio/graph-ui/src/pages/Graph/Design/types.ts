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
