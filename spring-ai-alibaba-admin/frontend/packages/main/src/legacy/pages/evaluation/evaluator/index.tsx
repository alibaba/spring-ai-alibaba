import React, { useState, useCallback, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Input, Space, Tag, Tooltip, Card, Modal, Typography } from 'antd';
import { PlusOutlined, EyeOutlined, BugOutlined, DeleteOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { handleApiError, notifySuccess } from '../../../utils/notification';
import API from '../../../services';
import CreateEvaluatorModal from '../../../components/CreateEvaluatorModal';
import usePagination from '../../../hooks/usePagination';
import { getLegacyPath } from '../../../utils/path';
import dayjs from 'dayjs';
import './index.css';
import { ModelsContext } from '../../../context/models';

const { Title } = Typography;


interface EvaluatorRecord {
  id: number;
  name: string;
  description: string;
  createTime: string;
  updateTime: string;
  modelName: string;
  modelConfig?: string; // JSON String
  latestVersion?: string;
  variables?: string; // JSON String
}



const EvaluationEvaluator: React.FC = () => {
  const navigate = useNavigate();
  const [evaluators, setEvaluators] = useState<EvaluatorRecord[]>([]);
  const { pagination, onPaginationChange, onShowSizeChange, setPagination } = usePagination();
  const { models, modelNameMap } = useContext(ModelsContext);
  const [searchName, setSearchName] = useState('');
  const [loading, setLoading] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // 加载评估器列表
  const fetchEvaluators = useCallback(async (pagination: ReturnType<typeof usePagination>["pagination"], searchName: string) => {
    setLoading(true);

    try {
      const response = await API.getEvaluators({
        pageNumber: pagination.current,
        pageSize: pagination.pageSize,
        name: searchName || undefined
      });

      if (response.code === 200) {
        const responseData = response.data;
        setEvaluators(responseData.pageItems || []);
        setPagination(prev => ({
          ...prev,
          total: responseData.totalCount || 0
        }));
      } else {
        throw new Error(response.message || 'Failed to fetch evaluators');
      }
    } catch (error) {
      console.error('Failed to fetch evaluators:', error);
      handleApiError(error, 'Failed to fetch evaluators');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchEvaluators(pagination, searchName);
  }, [pagination.current, pagination.pageSize, searchName]);

  // 当模型列表加载完成后，重新获取评估器列表以正确显示模型名称
  useEffect(() => {
    if (models.length > 0 && evaluators.length > 0) {
      // 触发重新渲染以显示正确的模型名称
      setEvaluators(prev => [...prev]); // 触发组件重新渲染
    }
  }, [models, evaluators.length]);

  // 处理搜索
  const handleSearch = useCallback((searchName: string) => {
    setSearchName(searchName);
  }, []);

  // 操作处理函数
  const handleView = (record: EvaluatorRecord) => {
    // 跳转到评估器详情页面，携带id参数
    navigate(getLegacyPath(`/evaluation/evaluator/${record.id}`));
  };

  const handleDebug = async (record: EvaluatorRecord) => {
    try {

      API.getEvaluator({ id: record.id }).then(({ data }) => {

        navigate(getLegacyPath('/evaluation/debug'), {
          state: {
            evaluatorId: record.id,
            modelConfig: JSON.parse(data?.modelConfig || "{}"),
            variables: JSON.parse(data.variables || "{}"),
            systemPrompt: data.prompt,
          }
        });
      })
      // 跳转到调试页面，携带评估器的实际配置
    } catch (error) {
      console.error('Error in handleDebug:', error);
      handleApiError(error, 'Failed to open the debug page');
    }
  };

  const handleDelete = (record: EvaluatorRecord) => {
    Modal.confirm({
      title: 'Confirm Delete',
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <p>Are you sure you want to delete evaluator <strong>{record.name}</strong>?</p>
          <p className="text-gray-500 text-sm">This action cannot be undone.</p>
        </div>
      ),
      okText: 'Delete',
      okType: 'danger',
      cancelText: 'Cancel',
      onOk: async () => {
        try {
          const response = await API.deleteEvaluator({ id: record.id });
          if (response.code === 200) {
            notifySuccess({ message: 'Evaluator deleted successfully' });
            fetchEvaluators(pagination, searchName); // 重新加载列表
          } else {
            throw new Error(response.message || 'Deletion failed');
          }
        } catch (error) {
          handleApiError(error, 'Failed to delete evaluator');
        }
      },
    });
  };

  const handleCreate = () => {
    setShowCreateModal(true);
  };

  const handleCreateSuccess = () => {
    setShowCreateModal(false);
    fetchEvaluators(pagination, searchName); // 重新加载列表
  };

  // 表格列配置
  const columns = [
    {
      title: 'Evaluator Name',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: EvaluatorRecord) => (
        <div
          className="font-medium text-blue-600 cursor-pointer hover:text-blue-800 hover:underline"
          onClick={() => handleView(record)}
        >
          {text}
        </div>
      ),
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      render: (text: string) => (
        <div className="text-sm text-gray-600 max-w-xs truncate" title={text}>
          {text || '-'}
        </div>
      ),
    },
    {
      title: 'Model',
      dataIndex: 'modelConfig',
      key: 'modelConfig',
      render: (modelConfig: string) => {
        // 未发布或未完整配置的评估器可能没有 modelConfig。
        if (!modelConfig || modelConfig === 'undefined') {
          return '-';
        }

        try {
          const modelConfigJson = JSON.parse(modelConfig);
          const name = modelNameMap[modelConfigJson?.modelId];
          return name ? (
            <Tag color="geekblue">{name}</Tag>
          ) : '-';
        } catch {
          return '-';
        }
      },
    },
    {
      title: 'Created At',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (text: string) => dayjs(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: 'Updated At',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (text: string) => dayjs(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: 'Actions',
      key: 'action',
      width: 160,
      fixed: 'right' as const,
      render: (_: any, record: EvaluatorRecord) => (
        <Space size="middle">
          <Tooltip title="Details">
            <Button
              type="link"
              icon={<EyeOutlined />}
              onClick={() => handleView(record)}
            />
          </Tooltip>
          <Tooltip title={!record.modelConfig ? "No version is published. Publish a version before debugging." : "Debug"} >
            <Button
              type="link"
              icon={<BugOutlined />}
              disabled={!record.modelConfig}
              onClick={() => handleDebug(record)}
            />
          </Tooltip>
          <Tooltip title="Delete">
            <Button
              type="link"
              icon={<DeleteOutlined />}
              onClick={() => handleDelete(record)}
              danger
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <div className="evaluator-page p-8 fade-in">
      {/* 页面标题 */}
      <div className="mb-8">
        <Title level={2} style={{ marginBottom: 8 }}>Evaluator Management</Title>
      </div>
      <Card className='mb-4'>
        {/* 搜索区域 */}
        <div className="flex gap-4 justify-between" style={{flexWrap: 'wrap'}}>
          <Input.Search
            placeholder="Search by name"
            allowClear
            style={{ width: 280 }}
            onSearch={handleSearch}
          />
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            Create Evaluator
          </Button>
        </div>

      </Card>
      {/* 数据表格 */}
      <Card>
        <div className="evaluator-table bg-white rounded-lg">
          <Table
            columns={columns}
            dataSource={evaluators}
            loading={loading}
            rowKey="id"
            pagination={{
              ...pagination,
              onChange: onPaginationChange,
              onShowSizeChange: onShowSizeChange
            }}
            className="border-0"
            scroll={{ x: 800 }}
          />
        </div>
      </Card>

      {/* 创建评估器弹窗 */}
      {showCreateModal && (
        <CreateEvaluatorModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={handleCreateSuccess}
        />
      )}
    </div>
  );
};

export default EvaluationEvaluator;