import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Input, Select, Button, Space, Tag, Checkbox, Pagination, Spin, message, Tooltip, Modal, Card, Drawer, Typography } from 'antd';
import { SearchOutlined, PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { handleApiError, notifySuccess } from '../../../utils/notification';
import API from '../../../services';
import GatherCreate from './gatherCreate';
import { getLegacyPath } from '../../../utils/path';
import './index.css';
import usePagination from '../../../hooks/usePagination';

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

interface DatasetRecord {
  id: number;
  name: string;
  description: string;
  dataCount?: number; // 数据量（可选）
  version?: string; // 版本信息（可选）
  columnsConfig: string;
  createTime: string;
  updateTime: string;
}



const EvaluationGather = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [dataSource, setDataSource] = useState<DatasetRecord[]>([]);
    const [searchText, setSearchText] = useState(''); // 输入框中的文本
    const [queryText, setQueryText] = useState(''); // 实际用于查询的文本
    const [showCreateDrawer, setShowCreateDrawer] = useState(false); // 侧滑面板状态
    const { pagination, setPagination, onPaginationChange, onShowSizeChange } = usePagination();

    // 获取评测集列表
    const fetchDatasets = useCallback(async () => {
        try {
            setLoading(true);
            const params = {
                pageNumber: pagination.current,
                pageSize: pagination.pageSize,
                datasetName: queryText || undefined, // 使用查询文本而不是输入文本
            };

            const response = await API.getDatasets(params);

            if (response.code === 200) {
                const responseData = response.data as any;
                // 优先使用pageItems，如果不存在则使用records作为降级
                const dataItems = responseData.pageItems || responseData.records || [];
                
                // 使用真实数据，按照新的API结构进行映射
                const datasets: DatasetRecord[] = dataItems.map((item: any, index: number) => ({
                    id: item.id,
                    name: item.name,
                    description: item.description || '',
                    // 由于API暂时不返回dataCount和version，使用默认值
                    // 后续可以通过单独的接口获取这些信息
                    dataCount: item.dataCount || undefined,
                    version: item.version || 'v1.0.0',
                    columnsConfig: item.columnsConfig || '',
                    createTime: item.createTime,
                    updateTime: item.updateTime || item.createTime
                }));

                setDataSource(datasets);
                setPagination(prev => ({
                    ...prev,
                    total: responseData.totalCount || datasets.length,
                    current: responseData.pageNumber || pagination.current
                }));
            } else {
                throw new Error(response.message || '加载失败');
            }
        } catch (error) {
            handleApiError(error, '获取评测集列表失败');
            // 发生错误时设置为空列表
            setDataSource([]);
            setPagination(prev => ({
                ...prev,
                total: 0
            }));
        } finally {
            setLoading(false);
        }
    }, [pagination.current, pagination.pageSize, queryText]); // 依赖查询文本而不是输入文本

    useEffect(() => {
        fetchDatasets();
    }, [fetchDatasets]);

    // 处理分页
    const handleTableChange = (page: number, pageSize: number) => {
        onPaginationChange(page, pageSize);
    };

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

    // 创建新评测集 - 打开侧滑面板
    const handleCreateDataset = () => {
        setShowCreateDrawer(true);
    };

    // 关闭侧滑面板
    const handleCloseCreateDrawer = () => {
        setShowCreateDrawer(false);
    };

    // 查看评测集详情
    const handleViewDataset = (record: DatasetRecord) => {
        // 跳转到评测集详情页面，携带id参数
        navigate(getLegacyPath(`/evaluation/gather/detail/${record.id}`));
    };

    // 删除评测集
    const handleDeleteDataset = async (record: DatasetRecord) => {
        Modal.confirm({
            title: '确认删除',
            content: `确定要删除评测集 "${record.name}" 吗？此操作不可恢复。`,
            okText: '确认删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: async () => {
                try {
                    await API.deleteDataset({ datasetId: record.id });
                    notifySuccess({ message: '评测集已删除' });
                    fetchDatasets();
                } catch (error) {
                    handleApiError(error, '删除评测集失败');
                }
            }
        });
    };

    const columns = [
        {
            title: '评测集名称',
            dataIndex: 'name',
            key: 'name',
            render: (text: string, record: DatasetRecord) => (
                <div 
                    className="font-medium text-blue-600 cursor-pointer hover:text-blue-800 hover:underline"
                    onClick={() => handleViewDataset(record)}
                >
                    {text}
                </div>
            )
        },
        {
            title: '描述',
            dataIndex: 'description',
            key: 'description',
            ellipsis: {
                showTitle: false,
            },
            render: (text: string) => (
                <Tooltip placement="topLeft" title={text || '-'}>
                    <span>{text || '-'}</span>
                </Tooltip>
            ),
        },
        {
            title: '版本',
            dataIndex: 'version',
            key: 'version',
            render: (version: string) => (
                <Tag color="blue">{version || 'v1.0.0'}</Tag>
            )
        },
        // {
        //     title: '创建人',
        //     dataIndex: 'creator',
        //     key: 'creator',
        //     width: 100
        // },
        {
            title: '数据量',
            dataIndex: 'dataCount',
            key: 'dataCount',
            render: (count: number) => (
                <span className="font-medium">
                    {count ? count.toLocaleString() : '-'}
                </span>
            )
        },
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
            width: 120,
            fixed: 'right' as const,
            render: (_: any, record: DatasetRecord) => (
                <Space size="middle">
                    <Tooltip title="详情">
                        <Button
                            type="link"
                            icon={<EyeOutlined />}
                            onClick={() => handleViewDataset(record)}
                        />
                    </Tooltip>
                    <Tooltip title="删除">
                        <Button
                            type="link"
                            icon={<DeleteOutlined />}
                            onClick={() => handleDeleteDataset(record)}
                            danger
                        />
                    </Tooltip>
                </Space>
            )
        }
    ];



    return (
        <div className="evaluation-gather-page p-8 fade-in">
            {/* 页面标题 */}
            <div className="mb-8">
                <Title level={2} style={{ marginBottom: 8 }}>评测集管理</Title>
            </div>

            {/* 搜索区域 */}
            <Card className='mb-4'>
                <div className="flex gap-4 justify-between" style={{flexWrap: 'wrap'}}>
                    <Input.Search
                        placeholder="搜索名称"
                        allowClear
                        style={{ width: 280 }}
                        className='mr-4'
                        value={searchText}
                        onChange={handleSearchChange}
                        onSearch={handleSearch}
                    />
                    {/* <Input
                        placeholder="搜索创建人"
                        allowClear
                        style={{ width: 280 }}
                        value={searchCreator}
                        onChange={(e) => {
                            setSearchCreator(e.target.value);
                            // 实时搜索，当输入框内容改变时触发搜索
                            if (e.target.value !== searchCreator) {
                                setPagination(prev => ({ ...prev, current: 1 }));
                            }
                        }}
                    /> */}
                    <Button 
                        type="primary" 
                        icon={<PlusOutlined />}
                        onClick={handleCreateDataset}
                    >
                        创建评测集
                    </Button>
                </div>
            </Card>

            {/* 数据表格 */}
            <Card>
                <div className="evaluation-gather-table bg-white rounded-lg">
                    <Table
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

            {/* 创建评测集侧滑面板 */}
            <Drawer
                title="创建评测集"
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
                    <GatherCreate 
                      hideTitle={true} // 隐藏标题
                      onCancel={handleCloseCreateDrawer}
                      onSuccess={() => {
                        handleCloseCreateDrawer();
                        fetchDatasets(); // 重新加载数据
                      }}
                    />
                </div>
            </Drawer>
        </div>
    );
};

export default EvaluationGather;