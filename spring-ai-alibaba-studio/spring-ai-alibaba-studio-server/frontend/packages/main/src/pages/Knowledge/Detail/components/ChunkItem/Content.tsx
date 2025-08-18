import { useSize } from 'ahooks';
import classNames from 'classnames';
import React, { useEffect, useRef } from 'react';
import styles from './index.module.less';

interface IProps {
  /**
   * Custom style
   */
  className?: string;
  /**
   * Content
   */
  content: string;
  /**
   * Show expand
   */
  onShowExpand: (show: boolean) => void;
  /**
   * Is expanded
   */
  isExpanded: boolean;
}

const Content: React.FC<IProps> = ({
  content,
  onShowExpand,
  className,
  isExpanded,
}) => {
  const contentRef = useRef<HTMLDivElement>(null);
  const contentSize = useSize(contentRef);

  useEffect(() => {
    if (contentRef.current && !isExpanded) {
      onShowExpand(
        contentRef.current.scrollHeight > contentRef.current.clientHeight,
      );
    }
  }, [content, contentSize?.width, isExpanded]);

  return (
    <div
      ref={contentRef}
      className={classNames(styles['chunk-content'], className)}
      dangerouslySetInnerHTML={{ __html: content?.replace(/\n/g, '<br/>') }}
    ></div>
  );
};

export default Content;
