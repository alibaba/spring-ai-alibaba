import $i18n from '@/i18n';
import { IValueType } from '@/types/work-flow';
import { Empty, Tag } from '@spark-ai/design';
import { Popover, Typography } from 'antd';
import classNames from 'classnames';
import React, { memo, useMemo, useState } from 'react';
import CustomIcon from '../CustomIcon';
import FlowIcon from '../FlowIcon';
import './index.less';

export interface IVarItem {
  label: string;
  value: string;
  type: IValueType;
  children?: Array<IVarItem>;
}
export interface IVarTreeItem {
  label: string;
  nodeId: string;
  nodeType: string;
  children: Array<IVarItem>;
}

export interface IVariableTreeSelectProps {
  options?: Array<IVarTreeItem>;
  children: React.ReactNode;
  defaultOpen?: boolean;
  onChange?: (value: Pick<IVarItem, 'type' | 'value'>) => void;
  onClose?: () => void;
  disabled?: boolean;
}

export interface IVarTreeProps {
  list: IVarItem[];
}

const VariableTree = memo(
  (
    props: IVarTreeProps & {
      value?: string;
      onChange?: (value: IVarItem) => void;
    },
  ) => {
    const hasProperties = useMemo(() => {
      return props.list.some((item) => !!item.children?.length);
    }, [props.list]);

    return (
      <>
        {props.list.map((item, index) => {
          return (
            <VariableTreeItem
              onChange={(val) => {
                props.onChange?.(val);
              }}
              hasProperties={hasProperties}
              key={index}
              {...item}
            />
          );
        })}
      </>
    );
  },
);

const VariableTreeItem = memo(
  (
    props: IVarItem & {
      hasProperties: boolean;
      onChange?: (val: IVarItem) => void;
    },
  ) => {
    const { hasProperties, onChange, ...restProps } = props;
    const [expand, setExpand] = useState(true);
    return (
      <div
        className={classNames('relative', {
          ['pl-3']: hasProperties,
          ['spark-flow-expand-var-tree-item']: !!props.children?.length,
          ['spark-flow-expand-var-tree-item-hidden']: !expand,
        })}
      >
        <div
          onClick={(e) => {
            e.stopPropagation();
            e.preventDefault();
            onChange?.(restProps);
          }}
          className={classNames(
            'h-[32px] spark-var-tree-item flex-justify-between',
          )}
        >
          <Typography.Text
            style={{ maxWidth: 200 }}
            ellipsis={{ tooltip: props.label }}
          >
            {props.label}
          </Typography.Text>
          <Tag className="spark-var-type">{props.type}</Tag>
        </div>
        {!!props.children?.length && (
          <CustomIcon
            size="small"
            onClick={() => setExpand(!expand)}
            className="spark-flow-inputs-expand-btn cursor-pointer"
            type="spark-up-line"
          />
        )}
        {!!props.children?.length && expand && (
          <VariableTree onChange={props.onChange} list={props.children} />
        )}
      </div>
    );
  },
);

export const VariableTreeNodeItem = memo(
  (
    props: IVarTreeItem & {
      value?: string;
      onChange?: (value: Pick<IVarItem, 'type' | 'value'>) => void;
    },
  ) => {
    const [open, setOpen] = useState(false);
    return (
      <Popover
        rootClassName="spark-flow-small-padding-popover"
        onOpenChange={setOpen}
        open={open}
        content={
          <VariableTree
            onChange={(val) => {
              props.onChange?.(val);
              setOpen(false);
            }}
            list={props.children || []}
          />
        }
        placement="leftTop"
      >
        <div className="flex gap-[4px] px-[8px] py-[4px] spark-flow-var-tree-select-node-props items-center">
          <FlowIcon size="small" nodeType={props.nodeType} />
          <Typography.Text
            className="spark-flow-var-tree-select-node-title flex-1 w-[1px]"
            ellipsis={{ tooltip: props.label }}
          >
            {props.label}
          </Typography.Text>
          <CustomIcon size="small" type="spark-right-line" />
        </div>
      </Popover>
    );
  },
);

const VariableTreeSelect = memo((props: IVariableTreeSelectProps) => {
  const [open, setOpen] = useState(props.defaultOpen || false);

  if (props.disabled) return props.children;

  return (
    <Popover
      placement="bottom"
      trigger={['click']}
      open={open}
      onOpenChange={(val) => {
        setOpen(val);
        if (!val) props.onClose?.();
      }}
      rootClassName="spark-flow-small-padding-popover spark-flow-var-tree-select-popover"
      getPopupContainer={(ele) => ele}
      content={
        !props.options?.length ? (
          <div className="full-center">
            <Empty
              size={160}
              description={$i18n.get({
                id: 'main.pages.Component.Plugin.Tools.List.noData',
                dm: '暂无数据',
              })}
            />
          </div>
        ) : (
          <div className="flex flex-col gap-[4px]">
            {props.options?.map((item) => {
              return (
                <VariableTreeNodeItem
                  onChange={(val) => {
                    props.onChange?.(val);
                    setOpen(false);
                    props.onClose?.();
                  }}
                  key={item.nodeId}
                  {...item}
                />
              );
            })}
          </div>
        )
      }
    >
      {props.children}
    </Popover>
  );
});

export default VariableTreeSelect;
