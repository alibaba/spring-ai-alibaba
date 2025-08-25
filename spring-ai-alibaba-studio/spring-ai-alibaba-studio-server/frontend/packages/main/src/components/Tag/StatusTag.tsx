import React from 'react';

import type { TaskStatus } from '@/types/base';
import { IconFont, Tag, TagProps } from '@spark-ai/design';
import classNames from 'classnames';

import $i18n from '@/i18n';
import styles from './StatusTag.module.less';

interface StatusTagProps extends TagProps {
  status: TaskStatus;
}

const statusMap = {
  uploaded: {
    icon: 'spark-process-line',
    text: $i18n.get({
      id: 'main.pages.Knowledge.Detail.components.Search.index.pending',
      dm: '待处理',
    }),
  },
  processing: {
    icon: 'spark-loading-line',
    text: $i18n.get({
      id: 'main.pages.Knowledge.Detail.components.Search.index.processing',
      dm: '处理中',
    }),
  },
  processed: {
    icon: 'spark-checkCircle-fill',
    text: $i18n.get({
      id: 'main.pages.Knowledge.Detail.components.Search.index.completed',
      dm: '处理完成',
    }),
  },
  failed: {
    icon: 'spark-errorCircle-fill',
    text: $i18n.get({
      id: 'main.pages.Knowledge.Detail.components.Search.index.failed',
      dm: '处理失败',
    }),
  },
};

const StatusTag: React.FC<StatusTagProps> = (props) => {
  const { status, className, children, ...rest } = props;
  const icon = (
    <IconFont
      type={statusMap[status]?.icon}
      className={classNames(styles['icon'], styles[status])}
    />
  );

  return (
    <Tag
      icon={icon}
      className={classNames(styles['container'], className)}
      {...rest}
    >
      {children || statusMap[status]?.text}
    </Tag>
  );
};

export default StatusTag;
