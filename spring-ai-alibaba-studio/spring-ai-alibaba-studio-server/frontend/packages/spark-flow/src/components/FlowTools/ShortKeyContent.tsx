import $i18n from '@/i18n';
import { Tag } from '@spark-ai/design';
import React, { memo, useMemo } from 'react';
import { isMacOS } from '../../utils';
import './ShortKeyContent.less';

const ShortKeyContent: React.FC = () => {
  const isWindows = useMemo(() => !isMacOS(), []);

  const cmdKey = useMemo(
    () =>
      isWindows
        ? 'Ctrl'
        : $i18n.get({
            id: 'spark-flow.components.FlowTools.ShortKeyContent.command',
            dm: '⌘',
          }),
    [isWindows],
  );
  const shiftKey = useMemo(
    () =>
      isWindows
        ? 'Shift'
        : $i18n.get({
            id: 'spark-flow.components.FlowTools.ShortKeyContent.shift',
            dm: '⇧',
          }),
    [isWindows],
  );

  return (
    <div className="spark-flow-short-key-container flex flex-col gap-[8px]">
      <div className="spark-flow-short-key-row flex items-center h-[20px]">
        <div className="spark-flow-short-key-name">
          {$i18n.get({
            id: 'spark-flow.components.FlowTools.ShortKeyContent.moveCanvas',
            dm: '移动画布',
          })}
        </div>
        <div className="spark-flow-keys-container flex items-center flex-1 justify-end">
          <Tag className="spark-flow-command-key-label">
            {$i18n.get({
              id: 'spark-flow.components.FlowTools.ShortKeyContent.spacebar',
              dm: '空格键',
            })}
          </Tag>
          <Tag className="spark-flow-command-key-label">
            {$i18n.get({
              id: 'spark-flow.components.FlowTools.ShortKeyContent.drag',
              dm: '拖拽',
            })}
          </Tag>
        </div>
      </div>

      <div className="spark-flow-short-key-row flex items-center h-[20px]">
        <div className="spark-flow-short-key-name">
          {$i18n.get({
            id: 'spark-flow.components.FlowTools.ShortKeyContent.zoomIn',
            dm: '放大',
          })}
        </div>
        <div className="spark-flow-keys-container flex items-center flex-1 justify-end">
          <Tag className="spark-flow-command-key">{cmdKey}</Tag>
          <Tag className="spark-flow-command-key-label">
            {$i18n.get({
              id: 'spark-flow.components.FlowTools.ShortKeyContent.scroll',
              dm: '滚动',
            })}
          </Tag>
          <span className="spark-flow-or-text text-[12px] mx-[2px]">
            {$i18n.get({
              id: 'spark-flow.components.FlowTools.ShortKeyContent.or',
              dm: '或',
            })}
          </span>
          <Tag className="spark-flow-command-key">{cmdKey}</Tag>
          <Tag className="spark-flow-command-key-label">+</Tag>
        </div>
      </div>

      <div className="spark-flow-short-key-row flex items-center h-[20px]">
        <div className="spark-flow-short-key-name">
          {$i18n.get({
            id: 'spark-flow.components.FlowTools.ShortKeyContent.zoomOut',
            dm: '缩小',
          })}
        </div>
        <div className="spark-flow-keys-container flex items-center flex-1 justify-end">
          <Tag className="spark-flow-command-key">{cmdKey}</Tag>
          <Tag className="spark-flow-command-key-label">
            {$i18n.get({
              id: 'spark-flow.components.FlowTools.ShortKeyContent.scroll',
              dm: '滚动',
            })}
          </Tag>
          <span className="spark-flow-or-text text-[12px] mx-[2px]">
            {$i18n.get({
              id: 'spark-flow.components.FlowTools.ShortKeyContent.or',
              dm: '或',
            })}
          </span>
          <Tag className="spark-flow-command-key">{cmdKey}</Tag>
          <Tag className="spark-flow-command-key-label">-</Tag>
        </div>
      </div>

      <div className="spark-flow-short-key-row flex items-center h-[20px]">
        <div className="spark-flow-short-key-name">
          {$i18n.get({
            id: 'spark-flow.components.FlowTools.ShortKeyContent.undo',
            dm: '撤销',
          })}
        </div>
        <div className="spark-flow-keys-container flex items-center flex-1 justify-end">
          <Tag className="spark-flow-command-key">{cmdKey}</Tag>
          <Tag className="spark-flow-command-key-label">Z</Tag>
        </div>
      </div>

      <div className="spark-flow-short-key-row flex items-center h-[20px]">
        <div className="spark-flow-short-key-name">
          {$i18n.get({
            id: 'spark-flow.components.FlowTools.ShortKeyContent.redo',
            dm: '恢复',
          })}
        </div>
        <div className="spark-flow-keys-container flex items-center flex-1 justify-end">
          <Tag className="spark-flow-command-key">{cmdKey}</Tag>
          <Tag className="spark-flow-command-key">{shiftKey}</Tag>
          <Tag className="spark-flow-command-key-label">Z</Tag>
        </div>
      </div>

      <div className="spark-flow-short-key-row flex items-center h-[20px]">
        <div className="spark-flow-short-key-name">
          {$i18n.get({
            id: 'spark-flow.components.FlowTools.ShortKeyContent.delete',
            dm: '删除',
          })}
        </div>
        <div className="spark-flow-keys-container flex items-center flex-1 justify-end">
          <Tag className="spark-flow-command-key-label">
            {$i18n.get({
              id: 'spark-flow.components.FlowTools.ShortKeyContent.backspace',
              dm: '退格',
            })}
          </Tag>
          <span className="spark-flow-or-text text-[12px] mx-[2px]">
            {$i18n.get({
              id: 'spark-flow.components.FlowTools.ShortKeyContent.or',
              dm: '或',
            })}
          </span>
          <Tag className="spark-flow-command-key-label">Delete</Tag>
          <span className="spark-flow-or-text text-[12px] mx-[2px]">
            {$i18n.get({
              id: 'spark-flow.components.FlowTools.ShortKeyContent.or',
              dm: '或',
            })}
          </span>
          <Tag className="spark-flow-command-key">{cmdKey}</Tag>
          <Tag className="spark-flow-command-key-label">D</Tag>
        </div>
      </div>
    </div>
  );
};

export default memo(ShortKeyContent);
