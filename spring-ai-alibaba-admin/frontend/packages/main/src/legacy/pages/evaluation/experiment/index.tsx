import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Input, Select, Button, Space, Tag, Checkbox, Pagination, Spin, message, Tooltip, Modal, Card, Drawer, Typography } from 'antd';
import { SearchOutlined, PlusOutlined, SyncOutlined, EyeOutlined, StopOutlined, ReloadOutlined, PlayCircleOutlined, DeleteOutlined, BarChartOutlined } from '@ant-design/icons';
import { handleApiError, notifySuccess } from '../../../utils/notification';
import API from '../../../services';
import ExperimentCreate from './experimentCreate';
import usePagination from '../../../hooks/usePagination';
import { getLegacyPath } from '../../../utils/path';
import './index.css';

const { Option } = Select;
const { Title } = Typography;

// 格式化时间显示
const formatDateTime = (dateTimeString: string) => {
    if (!dateTimeString) return '-';
    try {
        const date = new Date(dateTimeString);
        return date.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    } catch {
        return dateTimeString;
    }
};



interface ExperimentRecord {
  id: number;
  name: string;
  description: string;
  datasetId: number;
  datasetVersion: string;
  evaluationObjectConfig: string;
  evaluatorVersionIds: number[];
  evaluatorConfig: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'WAITING' | 'STOPPED';
  progress: number;
  completeTime: string;
  createTime: string;
  updateTime: string;
  result: string;
}

const Experiment = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [dataSource, setDataSource] = useState<ExperimentRecord[]>([]);
    const [searchText, setSearchText] = useState(''); // 输入框中的文本
    const [queryText, setQueryText] = useState(''); // 实际用于查询的文本
    const [statusFilter, setStatusFilter] = useState<string>('');
    const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
    const [showCreateDrawer, setShowCreateDrawer] = useState(false); // 侧滑面板状态
    const { pagination, setPagination, onPaginationChange, onShowSizeChange } = usePagination();

    // 获取实验列表
    const fetchExperiments = useCallback(async () => {
        try {
            setLoading(true);
            const params = {
                pageNumber: pagination.current,
                pageSize: pagination.pageSize,
                name: queryText || undefined, // 使用查询文本而不是输入文本
                status: statusFilter || undefined,
            };

            const response = await API.getExperiments(params);

            if (response.code === 200) {
                // 优先使用pageItems，如果不存在则使用records作为降级
                const responseData = response.data as any;
                const dataItems = responseData.pageItems || responseData.records || [];
                
                // 使用真实数据
                const experiments: ExperimentRecord[] = dataItems.map((item: any) => ({
                    id: item.id,
                    name: item.name,
                    description: item.description,
                    datasetId: item.datasetId,
                    datasetVersion: item.datasetVersion || '',
                    evaluationObjectConfig: item.evaluationObjectConfig || '',
                    evaluatorVersionIds: item.evaluatorVersionIds || [],
                    evaluatorConfig: item.evaluatorConfig || '',
                    status: item.status,
                    progress: item.progress || 0,
                    completeTime: item.completeTime || '',
                    createTime: item.createTime,
                    updateTime: item.updateTime || item.createTime,
                    result: item.result || ''
                }));

                setDataSource(experiments);
                setPagination(prev => ({
                    ...prev,
                    total: responseData.totalCount || experiments.length,
                    current: responseData.pageNumber || pagination.current
                }));
            } else {
                throw new Error(response.message || 'Failed to load experiments');
            }
        } catch (error) {
            handleApiError(error, 'Failed to retrieve experiments');
            // 发生错误时设置为空列表
            setDataSource([]);
            setPagination(prev => ({
                ...prev,
                total: 0
            }));
        } finally {
            setLoading(false);
        }
    }, [pagination.current, pagination.pageSize, queryText, statusFilter]); // 依赖查询文本而不是输入文本

    useEffect(() => {
        fetchExperiments();
    }, [fetchExperiments]);

    // 处理搜索输入变化（仅更新输入框状态，不触发搜索）
    const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSearchText(e.target.value);
    };

    // 处理搜索（仅在点击搜索按钮或按下回车键时触发）
    const handleSearch = (value: string) => {
        setSearchText(value);
        setQueryText(value); // 更新实际查询参数
        setPagination(prev => ({ ...prev, current: 1 }));
    };

    // 处理状态筛选
    const handleStatusFilter = (value: string) => {
        setStatusFilter(value);
        setPagination(prev => ({ ...prev, current: 1 }));
    };

    // 处理分页
    const handleTableChange = (page: number, pageSize: number) => {
        onPaginationChange(page, pageSize);
    };

    // 处理选择
    const handleSelectChange = (selectedKeys: React.Key[]) => {
        setSelectedRowKeys(selectedKeys);
    };

    // 刷新数据
    const handleRefresh = () => {
        fetchExperiments();
    };

    // 创建新实验 - 打开侧滑面板
    const handleCreateExperiment = () => {
        setShowCreateDrawer(true);
    };

    // 关闭侧滑面板
    const handleCloseCreateDrawer = () => {
        setShowCreateDrawer(false);
    };

    // 查看实验详情
    const handleViewExperiment = (record: ExperimentRecord) => {
        // 跳转到实验详情页面，携带id参数和evaluatorConfig数据
        navigate(getLegacyPath(`/evaluation/experiment/detail/${record.id}`), {
            state: { evaluatorConfig: record.evaluatorConfig }
        });
    };

    // 停止实验
    const handleStopExperiment = async (record: ExperimentRecord) => {
        Modal.confirm({
            title: 'Confirm Stop',
            content: `Are you sure you want to stop experiment "${record.name}"? Its status will change to Failed.`,
            okText: 'Confirm Stop',
            okType: 'danger',
            cancelText: 'Cancel',
            onOk: async () => {
                try {
                    await API.stopExperiment({ experimentId: record.id });
                    notifySuccess({ message: 'Experiment stopped' });
                    fetchExperiments();
                } catch (error) {
                    handleApiError(error, 'Failed to stop experiment');
                }
            }
        });
    };

    // 重新运行实验
    const handleRerunExperiment = async (record: ExperimentRecord) => {
        try {
            // 这里应该调用重新运行实验的API
            // await API.rerunExperiment({ id: record.id });
            message.info(`Rerunning experiment: ${record.name}`);
            // fetchExperiments();
        } catch (error) {
            handleApiError(error, 'Failed to rerun experiment');
        }
    };

    // 查看实验结果
    const handleViewResult = (record: ExperimentRecord) => {
        // 跳转到实验详情页面，并默认展示出评测结果tab的内容，携带id参数和evaluatorConfig数据、activeTab
        navigate(`/evaluation-experiment/detail/${record.id}`, {
            state: { evaluatorConfig: record.evaluatorConfig, activeTab: 'results' }
        });
    };

    // 删除实验
    const handleDeleteExperiment = async (record: ExperimentRecord) => {
        Modal.confirm({
            title: 'Confirm Delete',
            content: `Are you sure you want to delete experiment "${record.name}"? This action cannot be undone.`,
            okText: 'Confirm Delete',
            okType: 'danger',
            cancelText: 'Cancel',
            onOk: async () => {
                try {
                    await API.deleteExperiment({ experimentId: record.id });
                    notifySuccess({ message: 'Experiment deleted successfully' });
                    fetchExperiments();
                } catch (error) {
                    handleApiError(error, 'Failed to delete experiment');
                }
            }
        });
    };

    // 渲染状态标签
    const renderStatus = (status: string, progress?: number) => {
        switch (status) {
            case 'WAITING':
                return <Tag color="default">Waiting</Tag>;
            case 'RUNNING':
                return (
                    <div>
                        <Tag color="blue">Running</Tag>
                        <div style={{fontSize: '12px', color: 'rgb(102, 102, 102)', marginTop: '4px'}}>
                            {progress !== undefined && <span>Progress: {progress}%</span>}
                        </div>
                    </div>
                );
            case 'COMPLETED':
                return <Tag color="green">Completed</Tag>;
            case 'FAILED':
                return <Tag color="red">Failed</Tag>;
            case 'STOPPED':
                return <Tag color="orange">Stopped</Tag>;
            default:
                return <Tag>{status}</Tag>;
        }
    };

    const columns = [
        {
            title: 'Experiment Name',
            dataIndex: 'name',
            key: 'name',
            render: (text: string, record: ExperimentRecord) => (
                <div 
                    className="font-medium text-blue-600 cursor-pointer hover:text-blue-800 hover:underline"
                    onClick={() => handleViewExperiment(record)}
                >
                    {text}
                </div>
            )
        },
        { 
            title: 'Description', 
            dataIndex: 'description', 
            ellipsis: true,
            render: (text: string) => (
                <Tooltip title={text} placement="topLeft">
                    <span>{text}</span>
                </Tooltip>
            )
        },
        {
            title: 'Evaluation Set',
            dataIndex: 'datasetVersion',
            key: 'datasetVersion',
            render: (text: string, record: ExperimentRecord) => (
                <div>
                    <div className="font-medium">{record.datasetId}</div>
                    <div className="text-sm text-gray-500">{text}</div>
                </div>
            )
        },
        {
            title: 'Evaluator',
            dataIndex: 'evaluatorConfig',
            key: 'evaluatorConfig',
            render: (evaluatorConfig: string, record: ExperimentRecord) => {
                // 从evaluatorConfig字段解析评估器信息
                let evaluatorNames: string[] = [];
                try {
                    const evaluatorConfigs = JSON.parse(evaluatorConfig || '[]');
                    evaluatorNames = evaluatorConfigs.map((config: any) => config.evaluatorName || `ID: ${config.evaluatorId}`);
                } catch (e) {
                    // 如果解析失败，回退到使用evaluatorVersionIds
                    if (record.evaluatorVersionIds && record.evaluatorVersionIds.length > 0) {
                        evaluatorNames = record.evaluatorVersionIds.map(id => `ID: ${id}`);
                    }
                }
                
                if (evaluatorNames.length === 0) {
                    return <span className="text-gray-400">None</span>;
                }
                
                // 将所有评估器名称用逗号连接
                const allEvaluatorNames = evaluatorNames.join(', ');
                
                return (
                    <Tooltip title={`All evaluators:\n${allEvaluatorNames}`} placement="topLeft">
                        <div className="text-sm text-gray-600 mt-1 truncate" style={{ maxWidth: '200px' }}>
                            {allEvaluatorNames}
                        </div>
                    </Tooltip>
                );
            }
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status: string, record: ExperimentRecord) => renderStatus(status, record.progress)
        },
        // {
        //     title: '创建人',
        //     dataIndex: 'creator',
        //     key: 'creator'
        // },
        {
            title: 'Created At',
            dataIndex: 'createTime',
            key: 'createTime',
            render: (text: string) => formatDateTime(text)
        },
        {
            title: 'Updated At',
            dataIndex: 'updateTime',
            key: 'updateTime',
            render: (text: string) => formatDateTime(text)
        },
        {
            title: 'Actions',
            key: 'action',
            width: 160,
            fixed: 'right' as const,
            render: (_: any, record: ExperimentRecord) => {
                // 渲染第二个操作按钮（根据状态不同）
                const renderSecondAction = () => {
                    switch (record.status) {
                        case 'RUNNING':
                            return (
                                <Tooltip title="Stop">
                                    <Button
                                        type="link"
                                        icon={<StopOutlined />}
                                        onClick={() => handleStopExperiment(record)}
                                        danger
                                    />
                                </Tooltip>
                            );
                        case 'COMPLETED':
                            return (
                                <Tooltip title="View Results">
                                    <Button
                                        type="link"
                                        icon={<BarChartOutlined />}
                                        onClick={() => handleViewResult(record)}
                                    />
                                </Tooltip>
                            );
                        case 'FAILED':
                            return (
                                <Tooltip title="Rerun">
                                    <Button
                                        type="link"
                                        icon={<PlayCircleOutlined />}
                                        onClick={() => handleRerunExperiment(record)}
                                    />
                                </Tooltip>
                            );
                        case 'WAITING':
                            // 等待中状态暂时不确定，返回空
                            return null;
                        case 'STOPPED':
                            return (
                                <Tooltip title="Rerun">
                                    <Button
                                        type="link"
                                        icon={<PlayCircleOutlined />}
                                        onClick={() => handleRerunExperiment(record)}
                                    />
                                </Tooltip>
                            );
                        default:
                            return null;
                    }
                };

                return (
                    <Space size="middle">
                        <Tooltip title="View Details">
                            <Button
                                type="link"
                                icon={<EyeOutlined />}
                                onClick={() => handleViewExperiment(record)}
                            />
                        </Tooltip>
                        {renderSecondAction()}
                        <Tooltip title="Delete">
                            <Button
                                type="link"
                                icon={<DeleteOutlined />}
                                onClick={() => handleDeleteExperiment(record)}
                                danger
                            />
                        </Tooltip>
                    </Space>
                );
            }
        }
    ];

    const rowSelection = {
        selectedRowKeys,
        onChange: handleSelectChange
    };

    return (
        <div className="experiment-page p-8 fade-in">
            {/* 页面标题 */}
            <div className="mb-8">
                <Title level={2} style={{ marginBottom: 8 }}>Experiment Management</Title>
            </div>

            {/* 搜索和筛选区域 */}
            <Card className='mb-4'>
                <div className="flex gap-4 justify-between" style={{flexWrap: 'wrap'}}>
                    <Input.Search
                        placeholder="Search by name"
                        allowClear
                        style={{ width: 280 }}
                        value={searchText}
                        onChange={handleSearchChange}
                        onSearch={handleSearch}
                    />
                    <Select
                        placeholder="Select status"
                        allowClear
                        style={{ width: 200 }}
                        value={statusFilter}
                        onChange={handleStatusFilter}
                    >
                        <Option value="RUNNING">Running</Option>
                        <Option value="COMPLETED">Completed</Option>
                        <Option value="FAILED">Failed</Option>
                        <Option value="WAITING">Waiting</Option>
                        <Option value="STOPPED">Stopped</Option>
                    </Select>
                    <div style={{flex: 1}}></div>
                    <Button 
                        icon={<SyncOutlined />} 
                        onClick={handleRefresh}
                    >
                        Refresh
                    </Button>
                    <Button 
                        type="primary" 
                        icon={<PlusOutlined />}
                        onClick={handleCreateExperiment}
                    >
                        Create Experiment
                    </Button>
                </div>
            </Card>

            {/* 数据表格 */}
            <Card>
                <div className="experiment-table bg-white rounded-lg">
                    <Table
                        rowSelection={rowSelection}
                        columns={columns}
                        dataSource={dataSource}
                        loading={loading}
                        rowKey="id"
                        className="border-0"
                        pagination={{
                            ...pagination,
                            onChange: onPaginationChange,
                            onShowSizeChange: onShowSizeChange
                        }}
                        scroll={{ x: 800 }}
                    />
                    
                </div>
            </Card>

            {/* 创建实验侧滑面板 */}
            <Drawer
                title="Create Experiment"
                placement="right"
                width="90%"
                open={showCreateDrawer}
                onClose={handleCloseCreateDrawer}
                destroyOnClose={true}
                style={{ zIndex: 1000 }}
                styles={{
                    body: { padding: 0, height: '100%', display: 'flex', flexDirection: 'column' }
                }}
            >
                <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                    <ExperimentCreate 
                      hideTitle={true} // 隐藏标题
                      onCancel={handleCloseCreateDrawer}
                      onSuccess={() => {
                        handleCloseCreateDrawer();
                        fetchExperiments(); // 重新加载数据
                      }}
                    />
                </div>
            </Drawer>
        </div>
    );
};

export default Experiment;