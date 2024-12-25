import { graphState } from '@/store/GraphState';
import { GetProp, Menu, MenuProps } from 'antd';
import React from 'react';
import { useSnapshot } from 'umi';
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
