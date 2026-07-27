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
      align="center"
      gap={8}
      style={{ background: 'var(--ag-ant-color-fill-tertiary)' }}
      className="w-full rounded-[8px] px-[12px] py-[8px]"
    >
      <Flex
        gap={8}
        align="center"
        className="flex-1 min-w-0"
        style={{ width: 0 }}
      >
        <IconFont type={props.iconType} size="small" />
        <Typography.Text
          ellipsis={{ tooltip: renderTooltip(props.name) }}
          style={{
            color: 'var(--ag-ant-color-text-base)',
            maxWidth: 140,
            flexShrink: 0,
          }}
          className="text-[12px] leading-[20px]"
        >
          {props.name}
        </Typography.Text>
        {props.description ? (
          <Typography.Text
            style={{
              flex: 1,
              minWidth: 0,
              color: 'var(--ag-ant-color-text-tertiary)',
            }}
            ellipsis={{ tooltip: renderTooltip(props.description) }}
            className="text-[12px] leading-[20px]"
          >
            {props.description}
          </Typography.Text>
        ) : null}
        {props.weightInfo ? (
          <Popover content={props.weightInfo.description}>
            <Typography.Text
              style={{
                color: 'var(--ag-ant-color-text-description)',
                fontSize: 12,
                flexShrink: 0,
              }}
            >
              {props.weightInfo.label}
              {props.weightInfo.value}
            </Typography.Text>
          </Popover>
        ) : null}
      </Flex>
      <div style={{ flexShrink: 0 }}>{props.rightArea}</div>
    </Flex>
  );
};
