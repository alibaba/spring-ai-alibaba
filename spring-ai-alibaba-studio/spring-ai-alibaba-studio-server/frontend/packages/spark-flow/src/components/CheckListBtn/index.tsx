import { useStore } from '@/flow/context';
import { useNodesInteraction } from '@/hooks';
import $i18n from '@/i18n';
import { ICheckListItem, IWorkFlowNode } from '@/types/work-flow';
import { Empty, IconButton } from '@spark-ai/design';
import { useStore as useFlowStore } from '@xyflow/react';
import { Badge, Popover, Typography } from 'antd';
import { debounce } from 'lodash-es';
import React, { memo, useCallback, useEffect, useMemo } from 'react';
import CustomIcon from '../CustomIcon';
import FlowIcon from '../FlowIcon';
import './index.less';

export default memo(function CheckListBtn() {
  const showCheckList = useStore((state) => state.showCheckList);
  const setShowCheckList = useStore((state) => state.setShowCheckList);
  const nodes = useFlowStore((store) => store.nodes) as IWorkFlowNode[];
  const edges = useFlowStore((store) => store.edges);
  const nodeSchemaMap = useStore((store) => store.nodeSchemaMap);
  const checkList = useStore((state) => state.checkList);
  const setCheckList = useStore((state) => state.setCheckList);
  const { handleNodeClickByNodeId } = useNodesInteraction();

  const checkNodesList = useCallback(() => {
    const list: ICheckListItem[] = [];

    nodes.forEach((item) => {
      const nodeInfo = nodeSchemaMap[item.type as string];
      if (!nodeInfo.checkValid) return;
      const error_msgs = nodeInfo.checkValid(item.data);
      if (error_msgs.length > 0) {
        list.push({
          node_id: item.id,
          error_msgs,
          node_type: item.type,
          node_name: item.data.label,
        });
      }
    });

    setCheckList(list);
  }, [nodes, edges, nodeSchemaMap, setCheckList]);

  const debouncedCheckNodesList = useMemo(
    () => debounce(checkNodesList, 300),
    [checkNodesList],
  );

  useEffect(() => {
    debouncedCheckNodesList();
    return () => {
      debouncedCheckNodesList.cancel();
    };
  }, [nodes, edges, debouncedCheckNodesList]);

  const memoCheckList = useMemo(() => {
    return (
      <div>
        <div className="spark-flow-check-list-header flex-justify-between">
          <span className="spark-flow-check-list-header-title">
            {$i18n.get(
              {
                id: 'spark-flow.components.CheckListBtn.index.checklistVar1',
                dm: '检查清单 · {var1}',
              },
              { var1: `${checkList.length}` },
            )}
          </span>
          <div className="p-[4px] cursor-pointer">
            <CustomIcon
              onClick={() => setShowCheckList(false)}
              className="spark-flow-operator-icon-with-bg"
              type="spark-false-line"
            />
          </div>
        </div>
        {!checkList.length ? (
          <div className="spark-flow-check-list-empty flex flex-col items-center">
            <Empty
              description={$i18n.get({
                id: 'spark-flow.components.CheckListBtn.index.allResolved',
                dm: '所有清单项已解决',
              })}
              type="success"
            />
          </div>
        ) : (
          <div className="spark-flow-check-list-content px-[8px] pb-[8px] overflow-y-auto flex flex-col gap-[8px]">
            {checkList.map((item) => (
              <div
                onClick={() => {
                  handleNodeClickByNodeId(item.node_id);
                  setShowCheckList(false);
                }}
                key={item.node_id}
                className="spark-flow-node-check-container"
              >
                <div className="spark-flow-node-check-container-header">
                  <FlowIcon size="small" nodeType={item.node_type} />
                  <Typography.Text
                    ellipsis={{ tooltip: item.node_name }}
                    className="spark-flow-node-check-container-header-title"
                  >
                    {item.node_name}
                  </Typography.Text>
                </div>
                <div className="spark-flow-node-check-container-content">
                  {item.error_msgs.map((msg, index) => (
                    <div
                      className="spark-flow-node-check-container-content-item"
                      key={index}
                    >
                      <CustomIcon
                        size="small"
                        className="spark-flow-node-check-container-content-item-icon"
                        type="spark-warningCircle-line"
                      />

                      <span className="spark-flow-node-check-container-content-item-title">
                        {msg.label}
                      </span>
                      <span className="spark-flow-node-check-container-content-item-error">
                        {msg.error}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }, [showCheckList, checkList]);

  return (
    <Popover
      getPopupContainer={(ele) => ele}
      content={memoCheckList}
      trigger={'click'}
      placement="bottomRight"
      open={showCheckList}
      onOpenChange={setShowCheckList}
    >
      <div className="spark-flow-check-list-btn">
        <IconButton
          className="spark-flow-check-list-btn-child"
          icon={<CustomIcon type="spark-organizeAnswer-fill" />}
        >
          {!!checkList.length && (
            <Badge
              className="spark-flow-check-list-btn-count"
              showZero
              count={checkList.length}
              color="orange"
            />
          )}
        </IconButton>
      </div>
    </Popover>
  );
});
