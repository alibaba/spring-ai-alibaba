import { Select, SelectProps } from '@spark-ai/design';
import { Typography } from 'antd';
import React, { memo } from 'react';
import './index.less';

export default memo(function SelectWithDesc(props: SelectProps) {
  return (
    <Select
      {...props}
      optionRender={(item) => {
        return (
          <div className={'spark-flow-select-with-desc-item'}>
            <div>{item.label}</div>
            {!!item.data.desc && (
              <Typography.Paragraph
                ellipsis={{
                  rows: 2,
                  tooltip: item.data.desc,
                }}
                className={'spark-flow-select-with-desc-item-desc'}
              >
                {item.data.desc}
              </Typography.Paragraph>
            )}
          </div>
        );
      }}
    />
  );
});
