import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import ChunkEditItem from '@/pages/Knowledge/Detail/components/ChunkItem/Edit';
import { IChunkItem } from '@/pages/Knowledge/Detail/type';
import {
  deleteChunks,
  getChunksList,
  updateChunksContent,
  updateStatusChunks,
} from '@/services/knowledge';
import { AlertDialog, Empty, Pagination } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Flex } from 'antd';
import classNames from 'classnames';
import React, { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import styles from './index.module.less';

interface ChunkListProps {
  /**
   * Custom style
   */
  className?: string;
  /**
   * List data
   */
  list: IChunkItem[];
}

const SliceEditing: React.FC<ChunkListProps> = ({ className }) => {
  const { kb_id, doc_id } = useParams<{
    kb_id: string;
    doc_id: string;
  }>();

  const [state, setState] = useSetState({
    size: 10,
    current: 1,
    total: 0,
    loading: true,
    list: [] as IChunkItem[],
  });
  const fetchChunksList = () => {
    getChunksList({
      doc_id: doc_id as string,
      current: state.current,
      size: state.size,
    }).then((res: any) => {
      setState({
        list: [...res.records],
        total: res.total,
        loading: false,
      });
    });
  };

  useEffect(() => {
    fetchChunksList();
  }, [doc_id, state.current, state.size]);

  const handleDelete = ({
    doc_id,
    chunk_id,
  }: {
    doc_id: string;
    chunk_id: string;
  }) => {
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.pages.Knowledge.Detail.SliceEditing.index.deleteData',
        dm: '删除数据',
      }),
      children: $i18n.get({
        id: 'main.pages.Knowledge.Detail.SliceEditing.index.confirmDeleteData',
        dm: '确定删除该数据吗？',
      }),
      danger: true,
      okText: $i18n.get({
        id: 'main.pages.Knowledge.Detail.SliceEditing.index.confirmDelete',
        dm: '确认删除',
      }),
      onOk: () => {
        deleteChunks({ doc_id, chunk_id }).then(() => {
          setTimeout(() => {
            fetchChunksList();
          }, 1000);
        });
      },
    });
  };
  const handleUpdate = (data: any): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateChunksContent(data)
        .then(() => {
          fetchChunksList();
          resolve();
        })
        .catch((err) => {
          reject(err);
        });
    });
  };

  const handleDisplay = (chunl_id: string, display: boolean) => {
    const params = {
      doc_id: doc_id as string,
      chunk_ids: [chunl_id],
      enabled: display,
    };

    updateStatusChunks(params).then(() => {
      setTimeout(() => {
        fetchChunksList();
      }, 1000);
    });
  };
  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Detail.SliceEditing.index.fileList',
            dm: '文件列表',
          }),
          path: `/knowledge/${kb_id}`,
        },
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Detail.SliceEditing.index.chunkEditing',
            dm: '切片编辑',
          }),
        },
      ]}
      simplifyBreadcrumb={true}
    >
      <div className={styles['container']}>
        {state?.list.length ? (
          <>
            <div className={classNames(styles['chunk-list'], className)}>
              {state?.list.map((item: any, index) => (
                <ChunkEditItem
                  key={index}
                  index={index}
                  data={{
                    ...item,
                    title: item.title,
                    content: item.text,
                    enabled: item.enabled,
                    chunk_id: item.chunk_id,
                  }}
                  onDelete={() =>
                    handleDelete({
                      doc_id: item.doc_id,
                      chunk_id: item.chunk_id,
                    })
                  }
                  onUpdate={(text) =>
                    handleUpdate({
                      doc_id: item.doc_id,
                      chunk_id: item.chunk_id,
                      text: text,
                    })
                  }
                  onDisplay={(enabled) => handleDisplay(item.chunk_id, enabled)}
                  refreshList={fetchChunksList}
                />
              ))}
            </div>
            <Flex
              align="center"
              justify="flex-end"
              className={styles['pagination-wrapper']}
            >
              <Pagination
                className={styles['pagination']}
                total={state.total}
                pageSize={state.size}
                current={state.current}
                showSizeChanger={false}
                showQuickJumper={false}
                onChange={(page) => setState({ current: page })}
              />
            </Flex>
          </>
        ) : (
          <div className={styles['empty-container']}>
            <Empty
              description={$i18n.get({
                id: 'main.pages.Knowledge.Detail.SliceEditing.index.noData',
                dm: '暂无数据',
              })}
            />
          </div>
        )}
      </div>
    </InnerLayout>
  );
};

export default SliceEditing;
