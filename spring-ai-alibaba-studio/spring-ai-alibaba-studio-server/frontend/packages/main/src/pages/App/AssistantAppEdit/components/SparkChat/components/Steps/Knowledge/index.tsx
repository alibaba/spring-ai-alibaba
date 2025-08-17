import $i18n from '@/i18n';
import { IFileSearchResult } from '@/types/chat';
import { Accordion, Markdown } from '@spark-ai/chat';
import { renderTooltip } from '@spark-ai/design';
import { Typography } from 'antd';
import cls from 'classnames';
import styles from './index.module.less';
const ResultItem = (props: { item: IFileSearchResult }) => {
  const { item } = props;
  return (
    <div className={styles.resultItem}>
      <Accordion
        title={
          <Typography.Text
            ellipsis={{ tooltip: renderTooltip(item.doc_name) }}
            style={{ maxWidth: '400px' }}
          >
            {item.doc_name}
          </Typography.Text>
        }
        rightChildren={
          <div className={styles.header}>
            <span style={{ width: 'max-content' }}>
              {$i18n.get({
                id: 'main.components.SparkChat.components.Steps.Knowledge.index.score',
                dm: '得分：',
              })}
            </span>
            {item.score ? Number(item.score).toFixed(2) : '0%'}
          </div>
        }
        bodyStyle={{
          backgroundColor: 'var(--ag-ant-color-bg-base)',
        }}
      >
        <div className="p-[8px_12px]">
          <div className={cls(styles.resultCon, styles.textContent)}>
            <Markdown content={item.text || ''} />
          </div>
        </div>
      </Accordion>
    </div>
  );
};
export interface IProps {
  data: IFileSearchResult[];
}
export default function Knowledge(props: IProps) {
  const { data } = props;
  return (
    <div className={styles.content}>
      {data?.length > 0 ? (
        data.map?.((item, index) => <ResultItem item={item} key={index} />)
      ) : (
        <span className={styles.textNull}>-</span>
      )}
    </div>
  );
}
