import { VariableBaseInput } from '@/components/VariableInput';
import { useStore } from '@/flow/context';
import $i18n from '@/i18n';
import { IWorkFlowNode } from '@/types/work-flow';
import { defaultValueMap } from '@/utils/defaultValues';
import { Button, Drawer, IconFont } from '@spark-ai/design';
import React, { memo, useMemo } from 'react';
import './index.less';

interface ISingleNodeDrawer {
  selectedNodeData: IWorkFlowNode;
  onClose: () => void;
}

const SingleNodeDrawer = (props: ISingleNodeDrawer) => {
  const { selectedNodeData } = props;
  const nodeSchemaMap = useStore((store) => store.nodeSchemaMap);

  const renderTitle = useMemo(() => {
    return (
      <div className="flex items-center gap-[8px]">
        <span className="spark-flow-single-node-drawer-title font-semibold">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.SingleNodeDrawer.index.testRun',
            dm: '测试运行',
          })}
        </span>
        <div className="flex items-center">
          <div className="size-[28px] flex-center">
            <IconFont type={nodeSchemaMap[selectedNodeData.type].iconType} />
          </div>
          <span className="spark-flow-single-node-drawer-title">
            {selectedNodeData.data.label}
          </span>
        </div>
      </div>
    );
  }, [selectedNodeData.type, selectedNodeData.data.label, nodeSchemaMap]);

  return (
    <Drawer
      onClose={props.onClose}
      open
      className="spark-flow-drawer"
      height="90%"
      placement="bottom"
      getContainer={false}
      title={renderTitle}
      maskClassName="spark-flow-drawer-mask"
    >
      <div className="flex flex-col gap-[16px]">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.components.SingleNodeDrawer.index.inputVariables',
            dm: '输入变量',
          })}
        </div>
        <div className="flex flex-col gap-[8px]">
          <div className="flex flex-col gap-[4px]">
            <div className="flex items-center gap-[4px]">
              <span className="spark-flow-single-var-name">user</span>
              <span className="spark-flow-single-var-type">{`[String]`}</span>
            </div>
            <div className="spark-flow-single-var-desc"></div>
          </div>
          <VariableBaseInput value={''} type="String" onChange={() => {}} />
        </div>
        <div className="flex flex-col gap-[8px]">
          <div className="flex flex-col gap-[4px]">
            <div className="flex items-center gap-[4px]">
              <span className="spark-flow-single-var-name">info</span>
              <span className="spark-flow-single-var-type">{`[Object]`}</span>
            </div>
            <div className="spark-flow-single-var-desc">
              {$i18n.get({
                id: 'spark-flow.demos.spark-flow-1.components.SingleNodeDrawer.index.variableDescription',
                dm: '变量描述',
              })}
            </div>
          </div>
          <VariableBaseInput
            value={defaultValueMap['Object']}
            type="Object"
            onChange={() => {}}
          />
        </div>
        <div className="flex gap-[8px]">
          <Button color="default" variant="solid">
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.components.SingleNodeDrawer.index.run',
              dm: '运行',
            })}
          </Button>
          <Button>
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.components.SingleNodeDrawer.index.reset',
              dm: '重置',
            })}
          </Button>
        </div>
      </div>
    </Drawer>
  );
};

export default memo(SingleNodeDrawer);
