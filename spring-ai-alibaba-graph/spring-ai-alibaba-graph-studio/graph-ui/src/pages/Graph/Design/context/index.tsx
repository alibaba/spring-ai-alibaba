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

import { graphState } from '@/store/GraphState';
import { useSnapshot } from '@umijs/max';
import { GetProp, Menu, MenuProps } from 'antd';
import React from 'react';
import './index.less';

export type ContextMenuItem = GetProp<MenuProps, 'items'>[number];

export const ContextMenu: React.FC<{
  items: ContextMenuItem[];
}> = ({ items }) => {
  const store = useSnapshot(graphState);

  return (
    <>
      {store.contextMenu.show && (
        <Menu
          className="graph-menu"
          style={{
            left: store.contextMenu.left,
            top: store.contextMenu.top,
            width: '150px',
            borderRadius: '14px',
          }}
          items={items}
        />
      )}
    </>
  );
};
