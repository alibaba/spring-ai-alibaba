import { useStore } from '@/flow/context';
import { useNodesInteraction, useNodesReadOnly } from '@/hooks';
import $i18n from '@/i18n';
import { IWorkFlowNode, IWorkFlowStatus } from '@/types/work-flow';
import { copy, Dropdown, Tag, Tooltip } from '@spark-ai/design';
import { Background, NodeProps } from '@xyflow/react';
import { ConfigProvider, message, Typography } from 'antd';
import { ItemType } from 'antd/es/menu/interface';
import classNames from 'classnames';
import React, {
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import SourceHandle from '../CustomHandle/SourceHandle';
import TargetHandle from '../CustomHandle/TargetHandle';
import CustomIcon from '../CustomIcon';
import FlowIcon from '../FlowIcon';
import NodeResizer from '../NodeResizer';
import NodeResultPanel from '../NodeResultPanel';
import './index.less';

interface IProps extends NodeProps<IWorkFlowNode> {
  children?: React.ReactNode;
  disableShowSourceHandle?: boolean;
  disableShowTargetHandle?: boolean;
  disableShowDebug?: boolean;
  disableAction?: boolean;
  className?: string;
  hasFailBranch?: boolean;
}

export const NodeStatusIcon = memo(
  ({ status }: { status: IWorkFlowStatus }) => {
    let iconType = '';
    switch (status) {
      case 'success':
        iconType = 'spark-checkCircle-fill';
        break;
      case 'fail':
        iconType = 'spark-warningCircle-fill';
        break;
      case 'executing':
        iconType = 'spark-loading-line';
        break;
      case 'stop':
        iconType = 'spark-stopCircle-line';
        break;
      case 'pause':
        iconType = 'spark-pauseCircle-fill';
        break;
    }

    return (
      <CustomIcon
        className={classNames(
          'spark-flow-status-icon',
          `spark-flow-status-icon-${status}`,
        )}
        type={iconType}
      />
    );
  },
);

function BaseNode(props: IProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const taskStore = useStore((store) => store.taskStore);
  const showResults = useStore((store) => store.showResults);
  const setShowSingleTest = useStore((store) => store.setShowSingleTest);
  const setSelectedNode = useStore((store) => store.setSelectedNode);
  const nodeSchemaMap = useStore((store) => store.nodeSchemaMap);
  const { onNodeDelete, onNodeCopy, updateParentNodeSize } =
    useNodesInteraction();
  const { nodesReadOnly } = useNodesReadOnly();

  const handleClickOperation = useCallback(
    ({ key }: { key: string }) => {
      switch (key) {
        case 'copy':
          onNodeCopy(props.id);
          break;
        case 'delete':
          onNodeDelete(props.id);
          break;
        case 'id':
          copy(props.id);
          message.success(
            $i18n.get({
              id: 'spark-flow.components.BaseNode.index.copySuccess',
              dm: '复制成功',
            }),
          );
          break;
      }
    },
    [props.id],
  );

  const nodeResult = useMemo(() => {
    if (!taskStore) return null;
    return taskStore.node_results.find((item) => item.node_id === props.id);
  }, [props.id, taskStore]);

  const actionMenus = useMemo(() => {
    return [
      {
        label: (
          <div className="flex items-center gap-[8px]">
            <CustomIcon type="spark-copy-line" />
            <span>
              {$i18n.get({
                id: 'spark-flow.components.BaseNode.index.copy',
                dm: '复制',
              })}
            </span>
          </div>
        ),

        key: 'copy',
      },
      {
        label: (
          <div className="flex items-center gap-[8px]">
            <CustomIcon type="spark-delete-line" />
            <span>
              {$i18n.get({
                id: 'spark-flow.components.BaseNode.index.delete',
                dm: '删除',
              })}
            </span>
          </div>
        ),

        key: 'delete',
        danger: true,
      },
      {
        type: 'divider',
      },
      {
        label: (
          <div className="flex items-center spark-flow-node-copy-btn gap-[4px]">
            <Tag
              className="spark-flow-node-copy-btn-tag"
              icon={<CustomIcon type="spark-ID-line" />}
            >
              {props.id}
            </Tag>
            <CustomIcon
              className="spark-flow-node-copy-btn-icon"
              type="spark-copy-line"
            />
          </div>
        ),

        key: 'id',
      },
    ].filter((item) => {
      if (nodeSchemaMap[props.type].isSystem || nodesReadOnly)
        return (
          !['copy', 'delete'].includes(item.key as string) &&
          item.type !== 'divider'
        );

      return true;
    }) as ItemType[];
  }, [nodeSchemaMap, props.type, nodesReadOnly]);

  const nodeSchemaInfo = useMemo(() => {
    return nodeSchemaMap[props.type];
  }, [nodeSchemaMap, props.type]);

  useEffect(() => {
    if (containerRef.current && !!props.parentId) {
      const resizeObserver = new ResizeObserver(() => {
        updateParentNodeSize(props.parentId as string);
      });

      resizeObserver.observe(containerRef.current);

      return () => {
        resizeObserver.disconnect();
      };
    }
  }, [props.parentId, updateParentNodeSize]);

  return (
    <ConfigProvider
      getPopupContainer={() => containerRef.current || document.body}
    >
      <div
        ref={containerRef}
        className={classNames('spark-flow-node-container', props.className, {
          [`spark-flow-node-container-${nodeResult?.node_status}`]:
            !!nodeResult && showResults,
        })}
      >
        <div className="flex-justify-between">
          <div className="flex gap-[8px] items-center flex-1">
            <FlowIcon nodeType={props.type} />
            <Typography.Text
              ellipsis={{ tooltip: true }}
              className="spark-flow-node-label flex-1 w-1"
            >
              {props.data.label}
            </Typography.Text>
          </div>
          {!!nodeResult && showResults ? (
            <NodeStatusIcon status={nodeResult.node_status} />
          ) : props.disableAction ? null : (
            <div
              onClick={(e) => {
                e.stopPropagation();
                e.preventDefault();
              }}
              className="flex gap-[4px] items-center nodrag"
            >
              {nodeSchemaInfo.allowSingleTest && !nodesReadOnly && (
                <Tooltip
                  title={$i18n.get({
                    id: 'spark-flow.components.BaseNode.index.debug',
                    dm: '调试',
                  })}
                >
                  <div
                    onClick={() => {
                      setSelectedNode({
                        id: props.id,
                        data: props.data,
                        type: props.type,
                        position: { x: 0, y: 0 },
                      });
                      setShowSingleTest(true);
                    }}
                    className="spark-flow-operator-icon-with-bg rounded-[6px] size-[24px] cursor-pointer flex-center"
                  >
                    <CustomIcon
                      className="spark-flow-node-action-btn text-[20px]"
                      type="spark-circlePlay-line"
                    />
                  </div>
                </Tooltip>
              )}
              {!!actionMenus.length && (
                <Dropdown
                  placement="bottomRight"
                  trigger={['click']}
                  overlayClassName="spark-flow-node-dropdown"
                  menu={{
                    onClick: handleClickOperation,
                    items: actionMenus,
                  }}
                >
                  <div className="spark-flow-operator-icon-with-bg rounded-[6px] size-[24px] flex-center cursor-pointer">
                    <CustomIcon
                      className="spark-flow-node-action-btn text-[20px]"
                      type="spark-more-line"
                    />
                  </div>
                </Dropdown>
              )}
            </div>
          )}
        </div>
        {!!props.data.desc && (
          <Typography.Paragraph
            ellipsis={{ tooltip: props.data.desc, rows: 2 }}
            className="spark-flow-node-desc"
          >
            {props.data.desc}
          </Typography.Paragraph>
        )}
        {!props.disableShowSourceHandle && (
          <SourceHandle
            nodeType={props.type}
            nodeId={props.id}
            handleId={props.id}
            parentId={props.parentId}
          />
        )}
        {!props.disableShowTargetHandle && <TargetHandle handleId={props.id} />}
        {props.children}
        {props.hasFailBranch && (
          <div
            className={
              'spark-flow-judge-branch flex-justify-between items-center'
            }
          >
            <span className="spark-flow-fail-branch-label">
              {$i18n.get({
                id: 'spark-flow.components.BaseNode.index.whenError',
                dm: '当异常时',
              })}
            </span>
            <span className="spark-flow-fail-branch-label-right">
              {$i18n.get({
                id: 'spark-flow.components.BaseNode.index.errorBranch',
                dm: '异常分支',
              })}
            </span>
            <SourceHandle
              className="spark-flow-fail-branch-handle spark-flow-judge-branch-handle"
              nodeType={props.type}
              nodeId={props.id}
              parentId={props.parentId}
              handleId={`${props.id}_fail`}
            />
          </div>
        )}
        {nodeResult && showResults && <NodeResultPanel data={nodeResult} />}
      </div>
    </ConfigProvider>
  );
}

export interface IGroupNodeProps extends NodeProps<IWorkFlowNode> {
  children?: React.ReactNode;
  disableShowSourceHandle?: boolean;
  disableShowTargetHandle?: boolean;
  disableShowDebug?: boolean;
  disableAction?: boolean;
}

export const GroupNode = memo((props: IGroupNodeProps) => {
  const isDragging = useStore((state) => state.isDragging);
  const [hovered, setHovered] = useState(false);
  const setIsDragging = useStore((store) => store.setIsDragging);
  const { onAddNewNode } = useNodesInteraction();

  const onDrop: React.DragEventHandler<HTMLDivElement> = (event) => {
    setHovered(false);
    setIsDragging(false);
    event.preventDefault();
    event.stopPropagation();
    const type = event.dataTransfer.getData('application/reactflow');

    if (typeof type === 'undefined' || !type) {
      return;
    }

    onAddNewNode(
      { type, parentId: props.id },
      {
        x: event.clientX,
        y: event.clientY,
      },
    );
  };
  const { children, ...extraProps } = props;

  return (
    <BaseNode
      className={classNames('spark-flow-node-container-group', {
        'spark-flow-node-container-dragging': isDragging,
        'spark-flow-node-container-hovered': hovered,
      })}
      {...extraProps}
    >
      {props.children}
      <div
        onDrop={onDrop}
        onDragLeave={(event) => {
          event.preventDefault();
          setHovered(false);
        }}
        onDragEnter={(event) => {
          event.preventDefault();
          setHovered(true);
        }}
        className={'spark-flow-group-node-sub-flow relative'}
      >
        <Background />
      </div>
      <NodeResizer nodeId={props.id} />
    </BaseNode>
  );
});
export default memo(BaseNode);
