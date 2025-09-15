import { TopExpandBtn } from '@/components/ExpandBtn';
import { Markdown } from '@spark-ai/chat';
import { CodeBlock, IconFont } from '@spark-ai/design';
import { Flex } from 'antd';
import classNames from 'classnames';
import { memo, useMemo, useState } from 'react';
import styles from './index.module.less';

export interface IJSONViewerProps {
  value?: string;
  label: string;
  type?: 'json' | 'text';
}

export default memo(function JSONViewer(props: IJSONViewerProps) {
  const { type = 'json' } = props;
  const [expand, setExpand] = useState(true);
  const [showType, setShowType] = useState<'md' | 'text'>('md');

  const jsonValue = useMemo(() => {
    if (!props.value) return '';
    try {
      return JSON.stringify(JSON.parse(props.value), null, 2);
    } catch {
      return props.value;
    }
  }, [props.value]);

  const memoContent = useMemo(() => {
    if (type === 'json') {
      return <CodeBlock language="json" readOnly value={jsonValue} />;
    }

    return (
      <div className={styles['text-view-content']}>
        {showType === 'md' ? (
          <Markdown baseFontSize={12} content={props.value} />
        ) : (
          <div
            className={classNames(
              styles['text-view-content'],
              styles['no-padding'],
            )}
          >
            {props.value}
          </div>
        )}
      </div>
    );
  }, [type, jsonValue, props.value, expand, showType]);

  return (
    <div className={styles['viewer-container']}>
      <div className={styles['json-view-header']}>
        <span>{props.label}</span>
        <Flex align="center" gap={8}>
          {type === 'text' && (
            <>
              <IconFont
                onClick={() => setShowType('text')}
                className={styles.icon}
                type="spark-text-line"
              />
              <span onClick={() => setShowType('md')} className={styles.icon}>
                M
              </span>
            </>
          )}
          <TopExpandBtn
            className={styles.icon}
            expand={expand}
            setExpand={setExpand}
          />
        </Flex>
      </div>
      {expand && (
        <div className={styles['json-view-content']}>{memoContent}</div>
      )}
    </div>
  );
});
