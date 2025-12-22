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
        throw new Error(response.message || '获取评估器列表失败');
      }
    } catch (error) {
      console.error('获取评估器列表失败:', error);
      handleApiError(error, '获取评估器列表');
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
      handleApiError(error, '跳转调试页面');
    }
  };

  const handleDelete = (record: EvaluatorRecord) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <p>确定要删除评估器 <strong>{record.name}</strong> 吗？</p>
          <p className="text-gray-500 text-sm">此操作不可恢复，请谨慎操作。</p>
        </div>
      ),
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await API.deleteEvaluator({ id: record.id });
          if (response.code === 200) {
            notifySuccess({ message: '评估器删除成功' });
            fetchEvaluators(pagination, searchName); // 重新加载列表
          } else {
            throw new Error(response.message || '删除失败');
          }
        } catch (error) {
          handleApiError(error, '删除评估器');
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
      title: '评估器名称',
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
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      render: (text: string) => (
        <div className="text-sm text-gray-600 max-w-xs truncate" title={text}>
          {text || '-'}
        </div>
      ),
    },
    {
      title: '模型',
      dataIndex: 'modelConfig',
      key: 'modelConfig',
      render: (modelConfig: string) => {
        // 优先使用 modelName，如果为空或为 '-' 则从 modelConfig 中提取
        const modelConfigJson = JSON.parse(modelConfig);
        const name = modelNameMap[modelConfigJson?.modelId];
        return name ? (
          <Tag color="geekblue">{name}</Tag>
        ) : "-";
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (text: string) => dayjs(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (text: string) => dayjs(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right' as const,
      render: (_: any, record: EvaluatorRecord) => (
        <Space size="middle">
          <Tooltip title="详情">
            <Button
              type="link"
              icon={<EyeOutlined />}
              onClick={() => handleView(record)}
            />
          </Tooltip>
          <Tooltip title={!record.modelConfig ? "当前暂未发布版本，请先发布版本后调试" : "调试"} >
            <Button
              type="link"
              icon={<BugOutlined />}
              disabled={!record.modelConfig}
              onClick={() => handleDebug(record)}
            />
          </Tooltip>
          <Tooltip title="删除">
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
        <Title level={2} style={{ marginBottom: 8 }}>评估器管理</Title>
      </div>
      <Card className='mb-4'>
        {/* 搜索区域 */}
        <div className="flex gap-4 justify-between" style={{flexWrap: 'wrap'}}>
          <Input.Search
            placeholder="搜索名称"
            allowClear
            style={{ width: 280 }}
            onSearch={handleSearch}
          />
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            新建评估器
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