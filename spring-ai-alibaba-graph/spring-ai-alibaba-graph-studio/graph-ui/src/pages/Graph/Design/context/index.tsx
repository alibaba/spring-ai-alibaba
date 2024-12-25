import { GetProp, Menu, MenuProps } from 'antd';
import React from 'react';
import './index.less'
import { useSnapshot } from 'umi';
import { graphState } from '@/store/GraphState';

export type ContextMenuItem = GetProp<MenuProps, 'items'>[number];


export const ContextMenu: React.FC<{
  items: ContextMenuItem[]
}> = ({ items }) => {
  const store = useSnapshot(graphState);

  return <>
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

  </>;
};
