import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { IChunkItem } from '@/pages/Knowledge/Detail/type';
import { previewChunks, updateChunks } from '@/services/knowledge';
import { Button, message } from '@spark-ai/design';
import { useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ChunkList from './components/ChunkList';
import TestForm from './components/Form';
import styles from './index.module.less';

export default function SliceConfiguration() {
  const [list, setList] = useState<IChunkItem[]>([]);
  const [hasTest, setHasTest] = useState(false);
  const { kb_id = '', doc_id = '' } = useParams<{
    kb_id: string;
    doc_id: string;
  }>();
  const formRef = useRef<any>();
  const navigate = useNavigate();

  const handleSubmit = () => {
    const values = formRef.current.getFieldsValue();
    const params = {
      kb_id,
      doc_id,
      process_config: {
        ...values,
      },
    };
    updateChunks(params).then(() => {
      message.success(
        $i18n.get({
          id: 'main.pages.Knowledge.Detail.SliceConfiguration.index.chunkConfigurationSuccessful',
          dm: '切片配置成功',
        }),
      );
    });
    setHasTest(true);
  };

  const handlePreview = () => {
    const values = formRef.current.getFieldsValue();
    const params = {
      kb_id,
      doc_id,
      process_config: {
        ...values,
      },
    };
    previewChunks(params).then((res) => {
      setList(res);
    });
    setHasTest(true);
  };
  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Detail.SliceConfiguration.index.fileList',
            dm: '文件列表',
          }),
          path: `/knowledge/${kb_id}`,
        },
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Detail.SliceConfiguration.index.chunkConfiguration',
            dm: '切片配置',
          }),
        },
      ]}
      simplifyBreadcrumb={true}
      bottom={
        <div className={styles['footer']}>
          <Button type="primary" onClick={handleSubmit}>
            {$i18n.get({
              id: 'main.pages.Knowledge.Detail.SliceConfiguration.index.save',
              dm: '保存',
            })}
          </Button>
          <Button type="default" onClick={() => navigate(-1)}>
            {$i18n.get({
              id: 'main.pages.Knowledge.Detail.SliceConfiguration.index.cancel',
              dm: '取消',
            })}
          </Button>
        </div>
      }
    >
      <div className={styles['container']}>
        <TestForm
          className={styles['form']}
          formRef={formRef}
          onSubmit={handlePreview}
        />

        {hasTest && <ChunkList className={styles['chunk-list']} list={list} />}
      </div>
    </InnerLayout>
  );
}
