import InnerLayout from '@/components/InnerLayout';
import { IChunkItem } from '@/pages/Knowledge/Detail/type';
import { getKnowledgeDetail, getKnowledgeRetrieve } from '@/services/knowledge';
import { useState } from 'react';

import $i18n from '@/i18n';
import { useRequest } from 'ahooks';
import { useParams } from 'react-router-dom';
import ChunkList from './components/ChunkList';
import TestForm from './components/Form';
import styles from './index.module.less';

interface IDetails {
  search_config?: {
    similarity_threshold?: number;
  };
}
export default function () {
  const { kb_id } = useParams();
  const [list, setList] = useState<IChunkItem[]>([]);
  const [hasTest, setHasTest] = useState(false);
  const [details, setDetails] = useState<IDetails>({});
  const handleSubmit = (values: any) => {
    const params = {
      query: values.query,
      search_options: {
        kb_ids: [kb_id as string],
        similarity_threshold: values.similarity_threshold,
      },
    };
    getKnowledgeRetrieve(params).then((res) => {
      setList(res);
      setHasTest(true);
    });
  };
  useRequest(() => getKnowledgeDetail(kb_id as string), {
    onSuccess(res) {
      setDetails(res);
    },
  });
  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Test.index.knowledgeBase',
            dm: '知识库',
          }),
          path: '/knowledge',
        },
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Test.index.hitTest',
            dm: '命中测试',
          }),
        },
      ]}
    >
      <div className={styles['container']}>
        <TestForm
          className={styles['form']}
          onSubmit={handleSubmit}
          similarity_threshold={details?.search_config?.similarity_threshold}
        />

        {hasTest && <ChunkList className={styles['chunk-list']} list={list} />}
      </div>
    </InnerLayout>
  );
}
