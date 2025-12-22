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
                throw new Error(response.message || '加载失败');
            }
        } catch (error) {
            handleApiError(error, '获取实验列表失败');
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
            title: '确认停止',
            content: `确定要停止实验 "${record.name}" 吗？停止后实验状态将变为失败。`,
            okText: '确认停止',
            okType: 'danger',
            cancelText: '取消',
            onOk: async () => {
                try {
                    await API.stopExperiment({ experimentId: record.id });
                    notifySuccess({ message: '实验已停止' });
                    fetchExperiments();
                } catch (error) {
                    handleApiError(error, '停止实验失败');
                }
            }
        });
    };

    // 重新运行实验
    const handleRerunExperiment = async (record: ExperimentRecord) => {
        try {
            // 这里应该调用重新运行实验的API
            // await API.rerunExperiment({ id: record.id });
            message.info(`重新运行实验: ${record.name}`);
            // fetchExperiments();
        } catch (error) {
            handleApiError(error, '重新运行实验失败');
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
            title: '确认删除',
            content: `确定要删除实验 "${record.name}" 吗？此操作不可恢复。`,
            okText: '确认删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: async () => {
                try {
                    await API.deleteExperiment({ experimentId: record.id });
                    notifySuccess({ message: '实验删除成功' });
                    fetchExperiments();
                } catch (error) {
                    handleApiError(error, '删除实验失败');
                }
            }
        });
    };

    // 渲染状态标签
    const renderStatus = (status: string, progress?: number) => {
        switch (status) {
            case 'WAITING':
                return <Tag color="default">等待中</Tag>;
            case 'RUNNING':
                return (
                    <div>
                        <Tag color="blue">运行中</Tag>
                        <div style={{fontSize: '12px', color: 'rgb(102, 102, 102)', marginTop: '4px'}}>
                            {progress !== undefined && <span>进度: {progress}%</span>}
                        </div>
                    </div>
                );
            case 'COMPLETED':
                return <Tag color="green">已完成</Tag>;
            case 'FAILED':
                return <Tag color="red">失败</Tag>;
            case 'STOPPED':
                return <Tag color="orange">已停止</Tag>;
            default:
                return <Tag>{status}</Tag>;
        }
    };

    const columns = [
        {
            title: '实验名称',
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
            title: '描述', 
            dataIndex: 'description', 
            ellipsis: true,
            render: (text: string) => (
                <Tooltip title={text} placement="topLeft">
                    <span>{text}</span>
                </Tooltip>
            )
        },
        {
            title: '评测集',
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
            title: '评估器',
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
                    return <span className="text-gray-400">无</span>;
                }
                
                // 将所有评估器名称用逗号连接
                const allEvaluatorNames = evaluatorNames.join('，');
                
                return (
                    <Tooltip title={`全部评估器:\n${allEvaluatorNames}`} placement="topLeft">
                        <div className="text-sm text-gray-600 mt-1 truncate" style={{ maxWidth: '200px' }}>
                            {allEvaluatorNames}
                        </div>
                    </Tooltip>
                );
            }
        },
        {
            title: '状态',
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
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
            render: (text: string) => formatDateTime(text)
        },
        {
            title: '更新时间',
            dataIndex: 'updateTime',
            key: 'updateTime',
            render: (text: string) => formatDateTime(text)
        },
        {
            title: '操作',
            key: 'action',
            width: 160,
            fixed: 'right' as const,
            render: (_: any, record: ExperimentRecord) => {
                // 渲染第二个操作按钮（根据状态不同）
                const renderSecondAction = () => {
                    switch (record.status) {
                        case 'RUNNING':
                            return (
                                <Tooltip title="停止">
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
                                <Tooltip title="查看结果">
                                    <Button
                                        type="link"
                                        icon={<BarChartOutlined />}
                                        onClick={() => handleViewResult(record)}
                                    />
                                </Tooltip>
                            );
                        case 'FAILED':
                            return (
                                <Tooltip title="重新运行">
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
                                <Tooltip title="重新运行">
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
                        <Tooltip title="查看详情">
                            <Button
                                type="link"
                                icon={<EyeOutlined />}
                                onClick={() => handleViewExperiment(record)}
                            />
                        </Tooltip>
                        {renderSecondAction()}
                        <Tooltip title="删除">
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
                <Title level={2} style={{ marginBottom: 8 }}>实验管理</Title>
            </div>

            {/* 搜索和筛选区域 */}
            <Card className='mb-4'>
                <div className="flex gap-4 justify-between" style={{flexWrap: 'wrap'}}>
                    <Input.Search
                        placeholder="搜索名称"
                        allowClear
                        style={{ width: 280 }}
                        value={searchText}
                        onChange={handleSearchChange}
                        onSearch={handleSearch}
                    />
                    <Select
                        placeholder="状态 请选择"
                        allowClear
                        style={{ width: 200 }}
                        value={statusFilter}
                        onChange={handleStatusFilter}
                    >
                        <Option value="RUNNING">运行中</Option>
                        <Option value="COMPLETED">已完成</Option>
                        <Option value="FAILED">失败</Option>
                        <Option value="WAITING">等待中</Option>
                        <Option value="STOPPED">已停止</Option>
                    </Select>
                    <div style={{flex: 1}}></div>
                    <Button 
                        icon={<SyncOutlined />} 
                        onClick={handleRefresh}
                    >
                        刷新
                    </Button>
                    <Button 
                        type="primary" 
                        icon={<PlusOutlined />}
                        onClick={handleCreateExperiment}
                    >
                        新建实验
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
                title="新建实验"
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