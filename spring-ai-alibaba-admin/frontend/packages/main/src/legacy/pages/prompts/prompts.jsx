import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Spin,
  Button,
  Alert,
  Empty,
  Table,
  Input,
  Card,
  Tag,
  Space,
  Typography,
  Tooltip
} from 'antd';
import {
  PlusOutlined,
  ClearOutlined,
  EyeOutlined,
  DeleteOutlined,
  ShareAltOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExperimentOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { handleApiError, notifySuccess } from '../../utils/notification';
import CreatePromptModal from '../../components/CreatePromptModal';
import DeleteConfirmModal from '../../components/DeleteConfirmModal';
// import ElementSelector from '../../components/ElementSelector';
import API from '../../services';
import usePagination from '../../hooks/usePagination';
import { safeJSONParse } from '../../utils/util';
import { buildLegacyPath } from '../../utils/path';

const { Title, Paragraph } = Typography;
const { Search } = Input;

const PromptsPage = () => {
  const navigate = useNavigate();

  const { pagination, onPaginationChange, onShowSizeChange, setPagination } = usePagination();

  const [prompts, setPrompts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // 搜索和筛选状态
  const [searchName, setSearchName] = useState('');
  const [searchTag, setSearchTag] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [selectedPrompt, setSelectedPrompt] = useState(null);

  // 解析标签字符串为数组
  const parseTags = (tagsString) => {
    if (!tagsString) return [];
    return safeJSONParse(tagsString, () => tagsString.split(',').map(tag => tag.trim()).filter(tag => tag));
  };

  // 加载 Prompts 列表
  const loadPrompts = async (page = 1, pagesize = pagination.pageSize) => {
    setLoading(true);
    setError(null);

    try {
      const params = {
        pageNo: page,
        pageSize: pagesize,
      };

      // 添加搜索条件
      if (searchName) {
        params.promptKey = searchName;
        params.search = 'blur';
      }

      if (searchTag) {
        params.tag = searchTag;
      }

      const response = await API.getPrompts(params);

      if (response.code === 200) {
        setPrompts(response.data.pageItems || []);
        setPagination({
          ...pagination,
          total: response.data.totalCount || 0,
          totalPage: response.data.totalPage || 0,
        });
      } else {
        throw new Error(response.message || 'Failed to load prompts');
      }
    } catch (err) {
      console.error('加载 Prompts 失败:', err);
      handleApiError(err, 'Load prompts');
      setError(err.message || 'Network error. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const handleSearchName = useCallback((searchName) => {
    setSearchName(searchName);
  }, []);

  const handleSearchTag = useCallback((searchTag) => {
    setSearchTag(searchTag);
  }, []);

  // 搜索条件变化时重新加载
  useEffect(() => {
    onPaginationChange(1, pagination.pageSize);
    loadPrompts(1, pagination.pageSize);
  }, [searchName, searchTag]);

  // 页码变化时加载
  useEffect(() => {
    loadPrompts(pagination.current, pagination.pageSize);
  }, [pagination.current, pagination.pageSize]);

  const handleDelete = (prompt, event) => {
    event.stopPropagation();
    setSelectedPrompt(prompt);
    setShowDeleteModal(true);
  };

  const handleView = (prompt) => {
    localStorage.removeItem("prompt-sessions");
    navigate(buildLegacyPath('/prompt-detail', { promptKey: prompt.promptKey }));
  };

  const confirmDelete = async () => {
    if (!selectedPrompt) return;

    try {
      const response = await API.deletePrompt({ promptKey: selectedPrompt.promptKey });

      if (response.code === 200) {
        // 删除成功后重新加载列表
        notifySuccess({
          message: 'Prompt deleted',
          description: `Prompt "${selectedPrompt.promptKey}" was deleted successfully.`
        });
        await loadPrompts(pagination.current);
        setShowDeleteModal(false);
        setSelectedPrompt(null);
      } else {
        throw new Error(response.message || 'Failed to delete prompt');
      }
    } catch (err) {
      console.error('删除 Prompt 失败:', err);
      handleApiError(err, 'Delete prompt');
      setError(err.message || 'Failed to delete prompt. Please try again later.');
    }
  };

  // 渲染状态标签
  const renderStatusBadge = (prompt) => {
    if (!prompt.latestVersion || !prompt.latestVersionStatus) {
      return (
        <Tag color="warning" icon={<ClockCircleOutlined />}>
          No version
        </Tag>
      );
    }

    // 根据 latestVersionStatus 显示不同的状态
    if (prompt.latestVersionStatus === 'release') {
      return (
        <Tag color="success" icon={<CheckCircleOutlined />}>
          Release
        </Tag>
      );
    } else if (prompt.latestVersionStatus === 'pre') {
      return (
        <Tag color="processing" icon={<ExperimentOutlined />}>
          Pre-release
        </Tag>
      );
    } else {
      return (
        <Tag color="default" icon={<QuestionCircleOutlined />}>
          Unknown status
        </Tag>
      );
    }
  };

  // Table columns configuration
  const columns = [
    {
      title: 'Prompt Key',
      dataIndex: 'promptKey',
      key: 'promptKey',
      render: (text) => <span style={{ fontWeight: 500 }}>{text}</span>,
    },
    {
      title: 'Description',
      dataIndex: 'promptDescription',
      key: 'promptDescription',
      ellipsis: {
        showTitle: false,
      },
      render: (text) => (
        <Tooltip placement="topLeft" title={text}>
          {text || 'No description'}
        </Tooltip>
      ),
    },
    {
      title: 'Latest version',
      dataIndex: 'latestVersion',
      key: 'latestVersion',
      render: (version) => (
        version ? (
          <Tag color="blue">{version}</Tag>
        ) : (
          <Tag color="default">No version</Tag>
        )
      ),
    },
    {
      title: 'Status',
      key: 'status',
      render: (_, record) => renderStatusBadge(record),
    },
    {
      title: 'Tags',
      dataIndex: 'tags',
      key: 'tags',
      render: (tags) => (
        <Space size={[0, 4]} wrap>
          {parseTags(tags).map((tag, index) => (
            <Tag key={index} color="geekblue">
              {tag}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: 'Created at',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (time) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: 'Updated at',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (time) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: 'Actions',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Tooltip title="View details">
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                handleView(record);
              }}
            />
          </Tooltip>
          <Tooltip title="View trace">
            <Button
              type="text"
              icon={<ShareAltOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                navigate(buildLegacyPath("/tracing"), {
                  state: {
                    adv: {
                      "spring.ai.alibaba.prompt.key":record.promptKey
                    }
                  }
                });
              }}
            />
          </Tooltip>
          <Tooltip title="Delete">
            <Button
              type="text"
              danger
              icon={<DeleteOutlined />}
              onClick={(e) => handleDelete(record, e)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  const handleElementSelect = (element, elementInfo) => {
  };

  return (
    <div>
    {/* <ElementSelector onSelect={handleElementSelect} debug={true}>*/}
      <div className="p-8 fade-in">
      <div className="mb-8">
        <Title level={2} style={{ marginBottom: 8 }}>Prompt Management</Title>
        <Paragraph type="secondary">Manage and organize your AI prompt templates.</Paragraph>
      </div>

      {/* 错误提示 */}
      {error && (
        <Alert
          message="Loading error"
          description={error}
          type="error"
          showIcon
          style={{ marginBottom: 24 }}
        />
      )}

      {/* 搜索和创建区域 */}
      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', gap: 16, alignItems: 'end', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: 256 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
              Search by Prompt Key
            </label>
            <Search
              placeholder="Enter a Prompt Key..."
              onSearch={handleSearchName}
              allowClear
            />
          </div>

          <div style={{ flex: 1, minWidth: 256 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
              Search by tag
            </label>
            <Search
              placeholder="Enter a tag..."
              onSearch={handleSearchTag}
              allowClear
            />
          </div>

          <Space>
            <Button
              type="primary"
              onClick={() => setShowCreateModal(true)}
              icon={<PlusOutlined />}
            >
              Create Prompt
            </Button>
          </Space>
        </div>
      </Card>

      {/* Prompts列表 */}
      <Card>
        <Table
          columns={columns}
          dataSource={prompts}
          loading={loading}
          rowKey="promptKey"
          onRow={(record) => ({
            onClick: () => handleView(record),
            style: { cursor: 'pointer' },
          })}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="No matching prompts found"
              >
                <Button type="primary" onClick={() => setShowCreateModal(true)}>
                  Create your first prompt
                </Button>
              </Empty>
            ),
          }}
          pagination={{
            ...pagination,
            onChange: onPaginationChange,
            onShowSizeChange: onShowSizeChange
          }}
          scroll={{ x: 800 }}
        />
      </Card>


      {/* 模态框 */}
      {showCreateModal && (
        <CreatePromptModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            setShowCreateModal(false);
            loadPrompts(pagination.current); // 创建成功后刷新列表
          }}
        />
      )}

      {showDeleteModal && selectedPrompt && (
        <DeleteConfirmModal
          prompt={selectedPrompt}
          onConfirm={confirmDelete}
          onClose={() => setShowDeleteModal(false)}
        />
      )}
    </div>
    {/* </ElementSelector>*/}
    </div>
  );
};

export default PromptsPage;
