import InnerLayout from '@/components/InnerLayout';
import {
  deleteDocuments,
  getDocumentsList,
  getKnowledgeDetail,
} from '@/services/knowledge';
import { useRequest, useSetState } from 'ahooks';
import { Modal } from 'antd';
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import FileList from './components/FileList';
import Search from './components/Search';
import styles from './index.module.less';
import type { IFileItem } from './type';

import $i18n from '@/i18n';
import UploadModal from './components/UploadModal';

interface State {
  /** Current page number */
  current: number;
  /** Number of items per page */
  pageSize: number;
  /** Batch operation */
  operationable: boolean;
  /** Selected row key array */
  selectedRowKeys: React.Key[];
  /** File list data */
  list: IFileItem[];
  /** Search keyword */
  name: string;
  /** Total count */
  total: number;
  /** Index status */
  index_status: string;
  /** Document format */
  format: string;
}

const KnowledgeDetail: React.FC = () => {
  const { kb_id } = useParams<{ kb_id: string }>();
  const [state, setState] = useSetState<State>({
    current: 1,
    pageSize: 10,
    operationable: false,
    selectedRowKeys: [],
    list: [],
    name: '',
    total: 0,
    index_status: '',
    format: '',
  });
  const [knowledgeNmae, setKnowledgeNmae] = useState('');
  const [uploadModalVisible, setUploadModalVisible] = useState(false);

  const getList = () => {
    getDocumentsList({
      current: state.current,
      size: state.pageSize,
      kb_id: kb_id || '',
      name: state.name || undefined,
      index_status: state.index_status || undefined,
    }).then((res: any) => {
      setState({
        list: res.records,
        total: res.total,
        current: res.current,
        pageSize: res.size,
      });
    });
  };

  useEffect(() => {
    getList();
  }, [
    state.current,
    state.pageSize,
    state.name,
    state.index_status,
    state.format,
  ]);

  const handlePaginationChange = (newCurrent: number, newPageSize: number) => {
    setState({
      current: newCurrent,
      pageSize: newPageSize,
    });
  };

  const handleSelectionChange = (newSelectedRowKeys: React.Key[]) => {
    setState({ selectedRowKeys: newSelectedRowKeys });
  };

  const handleSearch = (value: string) => {
    setState({
      name: value,
      current: 1,
    });
  };

  const handleFilter = (type: string, value: string | string[]) => {
    setState((prevState) => ({
      ...prevState,
      [type]: value,
      current: 1,
    }));
  };

  const handleBatchOperation = () => {
    setState({
      operationable: !state.operationable,
    });
  };
  const handleDelete = (kb_id: string, doc_id: string) => {
    Modal.confirm({
      title: $i18n.get({
        id: 'main.pages.Knowledge.Detail.index.deleteData',
        dm: '删除数据',
      }),
      content: $i18n.get({
        id: 'main.pages.Knowledge.Detail.index.confirmDeleteData',
        dm: '确定删除该数据吗？',
      }),
      onOk: () => {
        deleteDocuments(kb_id, doc_id).then(() => {
          let current = state.current;
          if (state.list.length === 1 && current > 1) {
            current -= 1;
            setState({
              current,
            });
          }
          getList();
        });
      },
    });
  };
  const handleClickAction = (key: string, kb_id: string, doc_id: string) => {
    switch (key) {
      case 'delete':
        handleDelete(kb_id, doc_id);
        break;
      default:
        break;
    }
  };

  useRequest(() => getKnowledgeDetail(kb_id as string), {
    onSuccess(res) {
      setKnowledgeNmae(res.name);
    },
  });

  const refreshList = () => {
    if (state.current !== 1) {
      setState({
        current: 1,
      });
    } else {
      getList();
    }
    handleSelectionChange([]);
  };

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: knowledgeNmae,
          path: '/knowledge',
        },
        {
          title: $i18n.get({
            id: 'main.pages.Knowledge.Detail.index.fileList',
            dm: '文件列表',
          }),
        },
      ]}
    >
      <div className={styles.container}>
        <Search
          className={styles.search}
          onSearch={handleSearch}
          onFilter={handleFilter}
          onBatchOperation={handleBatchOperation}
          setUploadModalVisible={setUploadModalVisible}
          {...state}
        />

        <FileList
          onSelectionChange={handleSelectionChange}
          onPaginationChange={handlePaginationChange}
          onExitOperation={handleBatchOperation}
          handleClickAction={handleClickAction}
          refreshList={refreshList}
          {...state}
        />
      </div>
      {uploadModalVisible && (
        <UploadModal
          onClose={() => setUploadModalVisible(false)}
          refreshList={refreshList}
        />
      )}
    </InnerLayout>
  );
};

export default KnowledgeDetail;
