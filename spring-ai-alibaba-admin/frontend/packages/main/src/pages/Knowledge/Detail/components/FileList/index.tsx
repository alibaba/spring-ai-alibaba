import Table from '@/components/Table';
import FileTag from '@/components/Tag/FileTag';
import StatusTag from '@/components/Tag/StatusTag';
import $i18n from '@/i18n';
import { batchDeleteDocuments } from '@/services/knowledge';
import type { FileType } from '@/types/base';
import { AlertDialog, Button, Dropdown, message } from '@spark-ai/design';
import { Space } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { areIndexStatusesEqual } from '../../../utils/constant';
import type { IFileItem } from '../../type';
import BatchOperation from '../BatchOperation';
import styles from './index.module.less';

interface FileListProps {
  /**
   * File list
   */
  list: IFileItem[];
  /**
   * Enable multi-selection
   */
  operationable?: boolean;
  /**
   * Callback when selection changes
   */
  onSelectionChange?: (
    selectedRowKeys: React.Key[],
    selectedRows: IFileItem[],
  ) => void;
  /**
   * Current page number
   */
  current?: number;
  /**
   * Page size
   */
  pageSize?: number;
  /**
   * Total data count
   */
  total?: number;
  /**
   * Callback when pagination changes
   */
  onPaginationChange?: (page: number, pageSize: number) => void;
  /**
   * Callback when exiting batch operation
   */
  onExitOperation?: () => void;

  /**
   * Click action handler
   */
  handleClickAction(key: string, kb_id: string, doc_id: string): void;

  /**
   * Refresh list
   */
  refreshList: () => void;
}

const FileList: React.FC<FileListProps> = ({
  list,
  operationable = false,
  onSelectionChange,
  current = 1,
  pageSize = 10,
  total = 0,
  onPaginationChange,
  onExitOperation,
  handleClickAction,
  refreshList,
}) => {
  const navigate = useNavigate();
  const { kb_id } = useParams<{ kb_id: string; name: string }>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);
  const [selectedRows, setSelectedRows] = useState<IFileItem[]>([]);
  const [notOperation, setNotOperation] = useState(false);

  const handleSelectionChange = (
    newSelectedRowKeys: React.Key[],
    newSelectedRows: IFileItem[],
  ) => {
    setSelectedRowKeys(newSelectedRowKeys as string[]);
    const mergedRows = [...selectedRows];
    newSelectedRows.forEach((row) => {
      if (!mergedRows.find((item) => item.doc_id === row.doc_id)) {
        mergedRows.push(row);
      }
    });
    const filteredRows = mergedRows.filter((row) =>
      newSelectedRowKeys.includes(row.doc_id),
    );
    setSelectedRows(filteredRows);
    onSelectionChange?.(newSelectedRowKeys, filteredRows);
  };

  const columns: ColumnsType<IFileItem> = [
    {
      title: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.dataName',
        dm: '数据名称',
      }),
      dataIndex: 'name',
      key: 'name',
      width: 500,
    },
    {
      title: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.status',
        dm: '状态',
      }),
      dataIndex: 'index_status',
      key: 'index_status',
      width: 180,
      render: (index_status: IFileItem['index_status']) => (
        <StatusTag status={index_status} />
      ),
    },
    {
      title: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.dataFormat',
        dm: '数据格式',
      }),
      dataIndex: 'format',
      key: 'format',
      width: 100,
      render: (format: IFileItem['format']) => (
        <FileTag format={format?.toUpperCase() as FileType} />
      ),
    },
    {
      title: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.dataSize',
        dm: '数据大小',
      }),
      dataIndex: 'size',
      key: 'size',
      width: 120,
      render: (_, record) => {
        const { size } = record;
        const fileSize =
          size / 1024 / 1024 < 1
            ? `${(size / 1024).toFixed(2)} KB`
            : `${(size / 1024 / 1024).toFixed(2)} MB`;
        return <div>{fileSize}</div>;
      },
    },
    {
      title: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.operation',
        dm: '操作',
      }),
      key: 'action',
      width: 300,
      render: (_, record) => (
        <Space size={12}>
          <Button
            type="link"
            className={styles['operation-btn']}
            onClick={() =>
              navigate(
                `/knowledge/sliceConfiguration/${record.kb_id}/${record.doc_id}`,
              )
            }
          >
            {$i18n.get({
              id: 'main.pages.Knowledge.Detail.components.FileList.index.sliceConfiguration',
              dm: '切片配置',
            })}
          </Button>
          <Button
            type="link"
            className={styles['operation-btn']}
            onClick={() =>
              navigate(
                `/knowledge/sliceEditing/${record.kb_id}/${record.doc_id}`,
              )
            }
          >
            {$i18n.get({
              id: 'main.pages.Knowledge.Detail.components.FileList.index.sliceEdit',
              dm: '切片编辑',
            })}
          </Button>
          <Dropdown
            menu={{
              items: [
                {
                  key: 'delete',
                  label: $i18n.get({
                    id: 'main.pages.Knowledge.Detail.components.FileList.index.delete',
                    dm: '删除',
                  }),
                  danger: true,
                  onClick: () =>
                    handleClickAction &&
                    handleClickAction('delete', record?.kb_id, record?.doc_id),
                },
              ],
            }}
            trigger={['click']}
          >
            <Button type="link" className={styles['operation-btn']}>
              {$i18n.get({
                id: 'main.pages.Knowledge.Detail.components.FileList.index.more',
                dm: '更多',
              })}
            </Button>
          </Dropdown>
        </Space>
      ),
    },
  ];

  useEffect(() => {
    setSelectedRowKeys([]);
    setSelectedRows([]);
  }, [operationable]);

  const handleBatchDisable = () => {
    if (!selectedRows.length) {
      return message.error(
        $i18n.get({
          id: 'main.pages.Knowledge.Detail.components.FileList.index.selectFileFirst',
          dm: '请先选择文件',
        }),
      );
    }
    if (!areIndexStatusesEqual(selectedRows)) {
      setNotOperation(true);
      return;
    }
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.confirmDisableThreeFiles',
        dm: '确认禁用所选的3个文件吗',
      }),
      children: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.disabledFileNotRetrieved',
        dm: '禁用后，文件将不再被检索到',
      }),
    });
  };

  const handleBatchEnable = () => {
    if (!selectedRows.length) {
      return message.error(
        $i18n.get({
          id: 'main.pages.Knowledge.Detail.components.FileList.index.selectFileFirst',
          dm: '请先选择文件',
        }),
      );
    }
    if (!areIndexStatusesEqual(selectedRows)) {
      setNotOperation(true);
      return;
    }
    AlertDialog.warning({
      title: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.confirmEnableThreeFiles',
        dm: '确认启用所选的3个文件吗',
      }),
      children: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.enabledFileRetrieved',
        dm: '启用后，文件将开始被检索到',
      }),
      okText: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.confirm',
        dm: '确认',
      }),
      cancelText: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.cancel',
        dm: '取消',
      }),
    });
  };

  const handleBatchDelete = () => {
    if (!selectedRows.length) {
      return message.error(
        $i18n.get({
          id: 'main.pages.Knowledge.Detail.components.FileList.index.selectFileFirst',
          dm: '请先选择文件',
        }),
      );
    }
    AlertDialog.warning({
      title: $i18n.get(
        {
          id: 'main.pages.Knowledge.Detail.components.FileList.index.confirmDeleteVar1Files',
          dm: '确认删除所选的{var1}个文件吗',
        },
        { var1: selectedRows?.length },
      ),
      danger: true,
      children: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.deletedFileCannotRecover',
        dm: '删除后，文件将无法恢复',
      }),
      okText: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.confirmDelete',
        dm: '确认删除',
      }),
      cancelText: $i18n.get({
        id: 'main.pages.Knowledge.Detail.components.FileList.index.cancel',
        dm: '取消',
      }),
      onOk: () => {
        if (!kb_id) return;
        const params = {
          kb_id,
          doc_ids: selectedRowKeys,
        };
        batchDeleteDocuments(params).then(() => {
          message.success(
            $i18n.get({
              id: 'main.pages.Knowledge.Detail.components.FileList.index.successfullyDeleted',
              dm: '删除成功',
            }),
          );
          refreshList();
          setSelectedRows([]);
        });
      },
    });
  };

  return (
    <>
      {operationable && (
        <BatchOperation
          selectedCount={selectedRows.length}
          onCancelSelect={() => handleSelectionChange([], [])}
          onBatchDisable={handleBatchDisable}
          onBatchDelete={handleBatchDelete}
          onBatchEnable={handleBatchEnable}
          onExitOperation={onExitOperation}
        />
      )}
      <Table
        columns={columns}
        dataSource={list}
        rowKey="doc_id"
        rowSelection={
          operationable
            ? {
                selectedRowKeys,
                onChange: handleSelectionChange,
                preserveSelectedRowKeys: true,
                hideSelectAll: false,
              }
            : undefined
        }
        pagination={{
          current,
          pageSize,
          total,
          showSizeChanger: false,
          showQuickJumper: false,
          onChange: onPaginationChange,
        }}
      />

      {notOperation && (
        <AlertDialog
          type="warning"
          title={$i18n.get({
            id: 'main.pages.Knowledge.Detail.components.FileList.index.confirmSelectedFiles',
            dm: '请确认您选择的文件',
          })}
          open={true}
          footer={
            <Button
              type="primary"
              onClick={() => {
                setNotOperation(false);
              }}
            >
              {$i18n.get({
                id: 'main.pages.Knowledge.Detail.components.FileList.index.close',
                dm: '关闭',
              })}
            </Button>
          }
        >
          {$i18n.get({
            id: 'main.pages.Knowledge.Detail.components.FileList.index.filesHaveMultipleStatuses',
            dm: '您当前所选的文件包含多种状态，无法进行同一个批量操作，请确认！',
          })}
        </AlertDialog>
      )}
    </>
  );
};

export default FileList;
