import React from 'react';

import IconFile from '@/components/Icon/IconFile';
import type { FileType } from '@/types/base';
import { Tag, TagProps } from '@spark-ai/design';
import classNames from 'classnames';
import styles from './FileTag.module.less';

interface FileTagProps extends TagProps {
  format: FileType;
}

const imageMap = {
  PDF: {
    text: 'PDF',
  },
  MD: {
    text: 'MD',
  },
  Excel: {
    text: 'Excel',
  },
  PPT: {
    text: 'PPT',
  },
  TXT: {
    text: 'TXT',
  },
  DOC: {
    text: 'DOC',
  },
  DOCX: {
    text: 'DOCX',
  },
  PPTX: {
    text: 'PPTX',
  },
};

const FileTag: React.FC<FileTagProps> = (props) => {
  const { format, className, children, ...rest } = props;
  const icon = <IconFile type={format} className={styles['icon']} />;

  return (
    <Tag
      icon={icon}
      className={classNames(styles['container'], className)}
      {...rest}
    >
      {children || imageMap[format]?.text}
    </Tag>
  );
};

export default FileTag;
