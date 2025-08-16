import { IconFont, Popover, renderTooltip } from '@spark-ai/design';
import { Flex, Typography } from 'antd';
import React from 'react';

export default (props: {
  iconType: string;
  name: string;
  description?: string;
  rightArea: React.ReactElement;
  weightInfo?: {
    value: number;
    label: string;
    description: string;
  };
}) => {
  return (
    <Flex
      justify="space-between"
      style={{ background: 'var(--ag-ant-color-fill-tertiary)' }}
      className="w-full height-[32px] rounded-[6px] p-[6px_12px]"
    >
      <Flex
        gap={4}
        className="flex flex-1 items-center title"
        style={{ width: 'calc(100% - 24px)' }}
      >
        <IconFont type={props.iconType} size="small"></IconFont>
        <Typography.Text
          ellipsis={{ tooltip: renderTooltip(props.name) }}
          style={{ color: 'var(--ag-ant-color-text-base)', width: '112px' }}
          className="text-[12px] text-normal leading-[20px]"
        >
          {props.name}
        </Typography.Text>
        <Typography.Text
          style={{
            width: 'calc(100% - 140px)',
            color: 'var(--ag-ant-color-text-tertiary)',
          }}
          ellipsis={{ tooltip: renderTooltip(props?.description || '') }}
        >
          {props?.description}
        </Typography.Text>
        <Popover content={props.weightInfo?.description}>
          <Typography.Text
            style={{
              color: 'var(--ag-ant-color-text-description)',
              fontSize: '12px',
              flexShrink: 0,
              marginRight: '12px',
            }}
          >
            {props.weightInfo?.label}
            {props.weightInfo?.value}
          </Typography.Text>
        </Popover>
      </Flex>
      {props.rightArea}
    </Flex>
  );
};
