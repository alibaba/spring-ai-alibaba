import { useStore } from '@/flow/context';
import $i18n from '@/i18n';
import { Tabs } from '@spark-ai/design';
import classNames from 'classnames';
import React, { memo } from 'react';
import CustomIcon from '../CustomIcon';
import NodeMenu from '../NodeMenu';
import NodeTree from '../NodeTree';
import './index.less';

export default memo(function FlowAside() {
  const hiddenMenu = useStore((state) => state.hiddenMenu);
  const setHiddenMenu = useStore((state) => state.setHiddenMenu);
  return (
    <>
      <div
        className={classNames('spark-flow-aside flex flex-col', {
          ['menu-hidden']: hiddenMenu,
        })}
      >
        <div className="spark-flow-aside-header flex-justify-between flex-shrink-0">
          <span className="spark-flow-aside-header-title">
            {$i18n.get({
              id: 'spark-flow.components.FlowAside.index.nodeManagement',
              dm: '节点管理',
            })}
          </span>
          <CustomIcon
            onClick={() => setHiddenMenu(true)}
            className="spark-flow-aside-header-fold-icon cursor-pointer"
            type="spark-operateLeft-line"
          />
        </div>
        <Tabs
          className="spark-flow-aside-tabs flex-1 h-[1px]"
          items={[
            {
              label: $i18n.get({
                id: 'spark-flow.components.FlowAside.index.nodeLibrary',
                dm: '节点库',
              }),
              key: 'node-library',
              children: <NodeMenu />,
            },
            {
              label: $i18n.get({
                id: 'spark-flow.components.FlowAside.index.current',
                dm: '当前',
              }),
              key: 'current',
              children: <NodeTree />,
            },
          ]}
        />
      </div>
      <div
        className={classNames(
          'spark-flow-aside-header-expand-btn absolute cursor-pointer',
          {
            ['menu-hidden']: hiddenMenu,
          },
        )}
        onClick={() => setHiddenMenu(false)}
      >
        <CustomIcon
          className="spark-flow-aside-header-expand-btn-icon"
          type={
            hiddenMenu ? 'spark-operateRight-line' : 'spark-operateLeft-line'
          }
        />
      </div>
    </>
  );
});
