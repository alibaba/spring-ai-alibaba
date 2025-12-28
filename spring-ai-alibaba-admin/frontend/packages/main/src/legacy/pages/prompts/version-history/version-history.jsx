import React, { useState, useEffect, useContext, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Spin,
  Result,
  Button,
  Alert,
  Empty,
  Table,
  Checkbox,
  Tag,
  Typography,
  Card,
  Space,
  Modal,
  Row,
  Col,
  Tooltip
} from 'antd';
import {
  LoadingOutlined,
  ArrowLeftOutlined,
  InfoCircleOutlined,
  CheckCircleOutlined,
  ExperimentOutlined,
  BranchesOutlined,
  UndoOutlined,
  EyeOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { handleApiError, notifyError } from '../../../utils/notification';
import VersionCompareModal from '../../../components/VersionCompareModal';
import API from '../../../services';
import { ModelsContext } from '../../../context/models';
import usePagination from '../../../hooks/usePagination';
import { getLegacyPath } from '../../../utils/path';

const { Title, Text, Paragraph } = Typography;

const VersionHistoryPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { models } = useContext(ModelsContext);
  const { pagination, onPaginationChange, onShowSizeChange, setPagination } = usePagination();

  const promptKey = searchParams.get('promptKey');
  const targetWindowId = searchParams.get('targetWindowId');

  // State for API data
  const [currentPrompt, setCurrentPrompt] = useState(null);
  const [versions, setVersions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [versionDetailsCache, setVersionDetailsCache] = useState({}); // Cache for version details

  // State for UI interactions
  const [selectedVersions, setSelectedVersions] = useState([]);
  const [showCompare, setShowCompare] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState(null);
  const [showVersionDetail, setShowVersionDetail] = useState(false);
  const [loadingVersionDetail, setLoadingVersionDetail] = useState(false);

  // Time formatting function
  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss');
  };

  // Load prompt basic info and versions
  const loadPromptData = async (page = pagination.current, pageSize = pagination.pageSize) => {
    if (!promptKey) {
      navigate(getLegacyPath('/prompts'));
      return;
    }

    setLoading(true);
    setError(null);

    try {
      // Load prompt basic info
      const promptResponse = await API.getPrompt({ promptKey });
      if (promptResponse.code !== 200) {
        throw new Error(promptResponse.message || '获取 Prompt 信息失败');
      }
      setCurrentPrompt(promptResponse.data);

      // Load version list with pagination
      const versionsResponse = await API.getPromptVersions({
        promptKey,
        pageNo: page,
        pageSize: pageSize // Load all versions for client-side pagination
      });

      if (versionsResponse.code !== 200) {
        throw new Error(versionsResponse.message || '获取版本列表失败');
      }

      setVersions(versionsResponse.data.pageItems || []);
      setPagination({
        ...pagination,
        total: versionsResponse.data.totalCount || 0,
        totalPage: versionsResponse.data.totalPage || 0,
      });
    } catch (err) {
      console.error('加载数据失败:', err);
      handleApiError(err, '加载版本数据');
      setError(err.message || '加载失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPromptData();
  }, [promptKey]);

  // Load version detail with caching
  const loadVersionDetail = async (version) => {
    const cacheKey = `${promptKey}-${version.version}`;

    // Check cache first
    if (versionDetailsCache[cacheKey]) {
      setSelectedVersion({
        ...version,
        ...versionDetailsCache[cacheKey]
      });
      setShowVersionDetail(true);
      return;
    }

    setLoadingVersionDetail(true);
    try {
      const response = await API.getPromptVersion({
        promptKey,
        version: version.version
      });

      if (response.code !== 200) {
        throw new Error(response.message || '获取版本详情失败');
      }

      const versionDetail = response.data;

      // Parse JSON strings
      const variables = versionDetail.variables ? JSON.parse(versionDetail.variables) : {};
      const modelConfig = versionDetail.modelConfig ? JSON.parse(versionDetail.modelConfig) : null;
      const parameters = Object.keys(variables);

      // Create enhanced version object
      const enhancedVersion = {
        ...version,
        template: versionDetail.template,
        variables,
        modelConfig,
        parameters,
        content: versionDetail.template, // For backward compatibility with existing UI
        description: versionDetail.versionDescription,
        versionType: version.status // Map status to versionType for UI compatibility
      };

      // Cache the result
      setVersionDetailsCache(prev => ({
        ...prev,
        [cacheKey]: {
          template: versionDetail.template,
          variables,
          modelConfig,
          parameters,
          content: versionDetail.template,
          description: versionDetail.versionDescription,
          versionType: version.status
        }
      }));

      setSelectedVersion(enhancedVersion);
      setShowVersionDetail(true);
    } catch (err) {
      console.error('加载版本详情失败:', err);
      handleApiError(err, '加载版本详情');
    } finally {
      setLoadingVersionDetail(false);
    }
  };

  const modelConfig = selectedVersion?.modelConfig || {};

  const { modelId, ...otherModelConfig } = modelConfig;


  const currentModel = useMemo(() => {
    return models.find(model => model.id === modelConfig.modelId);
  }, [models, modelConfig]);


  useEffect(() => {
    loadPromptData();
  }, [pagination.current, pagination.pageSize])

  if (loading) {
    return (
      <div className="p-8 fade-in">
        <div className="flex items-center justify-center h-64">
          <Spin
            indicator={<LoadingOutlined style={{ fontSize: 48 }} spin />}
            size="large"
          >
            <div className="text-center pt-4">
              <p className="text-gray-600 mt-4">加载版本数据中...</p>
            </div>
          </Spin>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 fade-in">
        <Result
          status="error"
          title="加载版本数据失败"
          subTitle={error}
          extra={[
            <Button type="primary" key="retry" onClick={() => loadPromptData()}>
              重试
            </Button>,
            <Button key="back" onClick={() => navigate('/prompts')}>
              返回列表
            </Button>,
          ]}
        />
      </div>
    );
  }

  if (!currentPrompt) {
    return (
      <div className="p-8 fade-in">
        <Result
          status="404"
          title="Prompt 不存在"
          subTitle="未找到指定的 Prompt，可能已被删除或不存在。"
          extra={
            <Button type="primary" onClick={() => navigate('/prompts')}>
              返回列表
            </Button>
          }
        />
      </div>
    );
  }

  const handleVersionClick = (version) => {
    // 点击版本行时加载并显示版本详情
    loadVersionDetail(version);
  };


  // 加载两个版本的详细信息并开始对比
  const loadVersionsAndCompare = async (version1, version2) => {
    setLoadingVersionDetail(true);
    try {
      // 并行加载两个版本的详细信息
      const [detail1Response, detail2Response] = await Promise.all([
        API.getPromptVersion({ promptKey, version: version1.version }),
        API.getPromptVersion({ promptKey, version: version2.version })
      ]);

      if (detail1Response.code !== 200 || detail2Response.code !== 200) {
        throw new Error('获取版本详情失败');
      }

      // 处理版本1数据
      const detail1 = detail1Response.data;
      const variables1 = detail1.variables ? JSON.parse(detail1.variables) : {};
      const modelConfig1 = detail1.modelConfig ? JSON.parse(detail1.modelConfig) : null;

      const enhancedVersion1 = {
        ...version1,
        template: detail1.template,
        variables: variables1,
        modelConfig: modelConfig1,
        parameters: Object.keys(variables1),
        content: detail1.template,
        description: detail1.versionDescription,
        versionType: version1.status
      };

      // 处理版本2数据
      const detail2 = detail2Response.data;
      const variables2 = detail2.variables ? JSON.parse(detail2.variables) : {};
      const modelConfig2 = detail2.modelConfig ? JSON.parse(detail2.modelConfig) : null;

      const enhancedVersion2 = {
        ...version2,
        template: detail2.template,
        variables: variables2,
        modelConfig: modelConfig2,
        parameters: Object.keys(variables2),
        content: detail2.template,
        description: detail2.versionDescription,
        versionType: version2.status
      };

      // 更新缓存
      const cache1Key = `${promptKey}-${version1.version}`;
      const cache2Key = `${promptKey}-${version2.version}`;
      setVersionDetailsCache(prev => ({
        ...prev,
        [cache1Key]: {
          template: detail1.template,
          variables: variables1,
          modelConfig: modelConfig1,
          parameters: Object.keys(variables1),
          content: detail1.template,
          description: detail1.versionDescription,
          versionType: version1.status
        },
        [cache2Key]: {
          template: detail2.template,
          variables: variables2,
          modelConfig: modelConfig2,
          parameters: Object.keys(variables2),
          content: detail2.template,
          description: detail2.versionDescription,
          versionType: version2.status
        }
      }));

      // 设置增强版本并开始对比
      setSelectedVersions([enhancedVersion1, enhancedVersion2]);
      setShowCompare(true);
    } catch (err) {
      console.error('加载版本对比数据失败:', err);
      handleApiError(err, '加载版本对比数据');
    } finally {
      setLoadingVersionDetail(false);
    }
  };

  const handleCheckboxChange = (version, event) => {
    event.stopPropagation(); // 阻止事件冒泡到行点击

    if (selectedVersions.some(v => v.version === version.version)) {
      // 取消选择
      setSelectedVersions(prev => prev.filter(v => v.version !== version.version));
    } else {
      if (selectedVersions.length < 2) {
        // 添加选择
        const newSelection = [...selectedVersions, version];
        setSelectedVersions(newSelection);

        // 如果选择了两个版本，加载详细信息并开始对比
        if (newSelection.length === 2) {
          loadVersionsAndCompare(newSelection[0], newSelection[1]);
        }
      } else {
        // 已经选择了两个版本，替换第一个并开始对比
        const newSelection = [selectedVersions[1], version];
        setSelectedVersions(newSelection);
        loadVersionsAndCompare(newSelection[0], newSelection[1]);
      }
    }
  };

  const handleVersionDetailCompare = async () => {
    if (!selectedVersion) return;

    // 找到前一个版本 (按创建时间排序)
    const sortedVersions = [...versions].sort((a, b) => a.createTime - b.createTime);

    const currentIndex = sortedVersions.findIndex(v => v.version === selectedVersion.version);
    if (currentIndex > 0) {
      const previousVersion = sortedVersions[currentIndex - 1];

      // 加载两个版本的详细信息用于对比
      try {
        setLoadingVersionDetail(true);

        const [prevDetailResponse, currDetailResponse] = await Promise.all([
          API.getPromptVersion({ promptKey, version: previousVersion.version }),
          selectedVersion.template ? Promise.resolve({ code: 200, data: selectedVersion }) :
            API.getPromptVersion({ promptKey, version: selectedVersion.version })
        ]);

        if (prevDetailResponse.code !== 200) {
          throw new Error('获取前版本详情失败');
        }

        // 处理前版本数据
        const prevDetail = prevDetailResponse.data;
        const prevVariables = prevDetail.variables ? JSON.parse(prevDetail.variables) : {};
        const prevModelConfig = prevDetail.modelConfig ? JSON.parse(prevDetail.modelConfig) : null;

        const enhancedPrevVersion = {
          ...previousVersion,
          template: prevDetail.template,
          variables: prevVariables,
          modelConfig: prevModelConfig,
          parameters: Object.keys(prevVariables),
          content: prevDetail.template,
          description: prevDetail.versionDescription,
          versionType: previousVersion.status
        };

        setSelectedVersions([enhancedPrevVersion, selectedVersion]);
        setShowVersionDetail(false);
        setShowCompare(true);
      } catch (err) {
        console.error('加载版本对比数据失败:', err);
        handleApiError(err, '加载版本对比数据');
      } finally {
        setLoadingVersionDetail(false);
      }
    } else {
      notifyError({ message: '没有可对比的前版本' });
    }
  };

  const handleRestoreVersion = () => {
    if (!selectedVersion) return;

    console.log('=== 开始恢复版本 ===');
    console.log('选中版本:', selectedVersion);
    console.log('目标窗口ID:', targetWindowId);
    console.log('当前 Prompt Key:', promptKey);

    // 构建恢复URL，使用 promptKey 和 version
    let restoreUrl = `/prompt-detail?promptKey=${promptKey}&restoreVersionId=${selectedVersion.version}`;
    if (targetWindowId) {
      restoreUrl += `&targetWindowId=${targetWindowId}`;
    }

    console.log('=== 跳转URL ===', restoreUrl);

    // 跳转到Prompt详情页面，并传递要恢复的版本信息和目标窗口ID
    navigate(restoreUrl);

    // 关闭版本详情弹窗
    setShowVersionDetail(false);
  };



  const clearSelection = () => {
    setSelectedVersions([]);
  };


  return (
    <div style={{ padding: 32 }}>
      {/* 页面头部 */}
      <div style={{ marginBottom: 32 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <Button
                type="text"
                icon={<ArrowLeftOutlined />}
                onClick={() => navigate('/prompts')}
                size="large"
              />
              <Title level={1} style={{ margin: 0 }}>版本记录</Title>
            </div>
            <Paragraph style={{ margin: 0, color: '#595959' }}>
              <Text strong>{currentPrompt.promptKey}</Text> 的版本发布记录
            </Paragraph>
          </div>
        </div>
      </div>

      {/* 操作提示 */}
      <Alert
        message={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Text>勾选两个版本进行对比，或点击操作列的“详情”按钮查看版本详情</Text>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              {selectedVersions.length > 0 && (
                <Button
                  type="link"
                  size="small"
                  onClick={clearSelection}
                >
                  清除选择 ({selectedVersions.length})
                </Button>
              )}
            </div>
          </div>
        }
        type="info"
        icon={<InfoCircleOutlined />}
        showIcon
        style={{ marginBottom: 24 }}
      />

      {/* 版本历史列表 */}
      <Card>
        <Table
          dataSource={versions.map((version, index) => ({
            ...version,
            key: version.version,
            actualIndex: index,
            cachedDetail: versionDetailsCache[`${promptKey}-${version.version}`]
          }))}
          columns={[
            {
              title: '选择',
              key: 'select',
              width: 60,
              align: 'center',
              render: (_, record) => (
                <Checkbox
                  checked={selectedVersions.some(v => v.version === record.version)}
                  disabled={selectedVersions.length >= 2 && !selectedVersions.some(v => v.version === record.version)}
                  onChange={(e) => {
                    e.stopPropagation();
                    handleCheckboxChange(record, { stopPropagation: () => { } });
                  }}
                />
              )
            },
            {
              title: '版本号',
              key: 'version',
              width: 150,
              render: (_, record) => (
                <Space direction="vertical" size={4}>
                  <Tag color="blue">{record.version}</Tag>
                  {record.actualIndex === 0 && (
                    <Tag color="success" size="small">当前版本</Tag>
                  )}
                  {selectedVersions.some(v => v.version === record.version) && (
                    <Tag color="blue" size="small" icon={<CheckCircleOutlined />}>已选择</Tag>
                  )}
                </Space>
              )
            },
            {
              title: '发布时间',
              key: 'createTime',
              width: 150,
              render: (_, record) => formatTime(record.createTime)
            },
            {
              title: '版本说明',
              key: 'description',
              width: 200,
              ellipsis: {
                showTitle: false
              },
              render: (_, record) => (
                <Tooltip title={record.versionDescription || record.cachedDetail?.description || '无说明'}>
                  <Text ellipsis>
                    {record.versionDescription || record.cachedDetail?.description || '无说明'}
                  </Text>
                </Tooltip>
              )
            },
            // {
            //   title: '模型配置',
            //   key: 'modelConfig',
            //   width: 180,
            //   render: (_, record) => {
            //     if (record.cachedDetail?.modelConfig) {
            //       return (
            //         <div>
            //           <div style={{ fontSize: '12px' }}>
            //             <Text strong>模型：</Text>
            //             <Text style={{ marginLeft: 4 }}>{record.cachedDetail.modelConfig.modelId}</Text>
            //           </div>
            //           <div style={{ fontSize: '12px', color: '#8c8c8c' }}>
            //             <Text>Tokens: {record.cachedDetail.modelConfig.maxTokens}</Text>
            //             <Text style={{ marginLeft: 8 }}>Temp: {record.cachedDetail.modelConfig.temperature}</Text>
            //           </div>
            //         </div>
            //       );
            //     }
            //     return <Text type="secondary" style={{ fontSize: '12px' }}>点击查看配置</Text>;
            //   }
            // },
            // {
            //   title: '参数配置',
            //   key: 'parameters',
            //   width: 180,
            //   render: (_, record) => {
            //     if (record.cachedDetail?.parameters && record.cachedDetail.parameters.length > 0) {
            //       return (
            //         <Space size={[4, 4]} wrap>
            //           {record.cachedDetail.parameters.slice(0, 3).map((param, index) => (
            //             <Tag key={index} color="blue" size="small">{param}</Tag>
            //           ))}
            //           {record.cachedDetail.parameters.length > 3 && (
            //             <Text type="secondary" style={{ fontSize: '12px' }}>+{record.cachedDetail.parameters.length - 3}</Text>
            //           )}
            //         </Space>
            //       );
            //     }
            //     return <Text type="secondary" style={{ fontSize: '12px' }}>点击查看参数</Text>;
            //   }
            // },
            {
              title: '状态',
              key: 'status',
              width: 120,
              render: (_, record) => {
                if (record.status === 'release') {
                  return (
                    <Tag color="success" icon={<CheckCircleOutlined />}>
                      正式版本
                    </Tag>
                  );
                }
                return (
                  <Tag color="warning" icon={<ExperimentOutlined />}>
                    PRE版本
                  </Tag>
                );
              }
            },
            // {
            //   title: '内容预览',
            //   key: 'content',
            //   width: 250,
            //   ellipsis: {
            //     showTitle: false
            //   },
            //   render: (_, record) => {
            //     const content = record.cachedDetail?.content;
            //     if (content) {
            //       const previewText = `${content.substring(0, 80)}${content.length > 80 ? '...' : ''}`;
            //       return (
            //         <Tooltip title={content}>
            //           <div style={{
            //             fontFamily: 'monospace',
            //             fontSize: '12px',
            //             backgroundColor: '#fafafa',
            //             padding: 8,
            //             borderRadius: 4,
            //             border: '1px solid #f0f0f0'
            //           }}>
            //             {previewText}
            //           </div>
            //         </Tooltip>
            //       );
            //     }
            //     return <Text type="secondary" style={{ fontSize: '12px' }}>点击查看内容</Text>;
            //   }
            // },
            {
              title: '操作',
              key: 'actions',
              width: 100,
              align: 'center',
              render: (_, record) => (
                <Button
                  type="text"
                  size="small"
                  icon={<EyeOutlined />}
                  onClick={(e) => {
                    e.stopPropagation();
                    handleVersionClick(record);
                  }}
                  title="查看详情"
                >
                  详情
                </Button>
              )
            }
          ]}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无版本记录"
              >
                <Button type="primary" onClick={() => navigate(`/prompt-detail?promptKey=${promptKey}`)}>
                  开始创建版本
                </Button>
              </Empty>
            )
          }}
          pagination={{
            ...pagination,
            onChange: onPaginationChange,
            onShowSizeChange: onShowSizeChange
          }}
          scroll={{ x: 1300 }}
        />
      </Card>

      {/* 分页和底部操作区 */}
      <Space direction="vertical" size={16} style={{ width: '100%', marginTop: 24 }}>
        {/* 底部操作区 */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text type="secondary">
            共 {pagination.total} 个版本，按发布时间倒序排列
          </Text>

          <Space>
            {selectedVersions.length === 2 && (
              <Button
                type="primary"
                icon={<BranchesOutlined />}
                loading={loadingVersionDetail}
                onClick={() => loadVersionsAndCompare(selectedVersions[0], selectedVersions[1])}
              >
                {loadingVersionDetail ? '加载中...' : '对比选中版本'}
              </Button>
            )}
            <div>
              {selectedVersions.length === 0 && (
                <Text type="secondary">请选择两个版本进行对比</Text>
              )}
              {selectedVersions.length === 1 && (
                <Text type="secondary">已选择1个版本，请再选择1个版本</Text>
              )}
              {selectedVersions.length === 2 && !loadingVersionDetail && (
                <Text type="secondary">已选择版本 {selectedVersions[0].version} 和 {selectedVersions[1].version}</Text>
              )}
              {loadingVersionDetail && (
                <Text type="secondary">
                  <LoadingOutlined style={{ marginRight: 8 }} />
                  正在加载版本详情用于对比...
                </Text>
              )}
            </div>
          </Space>
        </div>
      </Space>

      {/* 版本详情模态框 */}
      <Modal
        title={`版本详情 - ${selectedVersion?.version}`}
        open={showVersionDetail && selectedVersion}
        onCancel={() => setShowVersionDetail(false)}
        width={1000}
        style={{
          top: 20,
          maxHeight: 'calc(100vh - 40px)'
        }}
        bodyStyle={{
          maxHeight: 'calc(100vh - 200px)',
          overflowY: 'auto'
        }}
        footer={[
          <Space key="actions">
            <Button
              type="primary"
              icon={<UndoOutlined />}
              onClick={handleRestoreVersion}
            >
              恢复到编辑区
            </Button>
            <Button
              icon={<BranchesOutlined />}
              onClick={handleVersionDetailCompare}
            >
              与前版本对比
            </Button>
            <Button onClick={() => setShowVersionDetail(false)}>
              关闭
            </Button>
          </Space>
        ]}
      >
        {loadingVersionDetail ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '48px 0' }}>
            <Spin
              indicator={<LoadingOutlined style={{ fontSize: 32 }} spin />}
              size="large"
            >
              <div style={{ textAlign: 'center', paddingTop: 16 }}>
                <Text type="secondary">加载版本详情中...</Text>
              </div>
            </Spin>
          </div>
        ) : (
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            {/* 版本基本信息 */}
            <Row gutter={24}>
              <Col span={12}>
                <div>
                  <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>版本号</Text>
                  <div style={{ padding: '8px 16px', backgroundColor: '#fafafa', borderRadius: 6 }}>
                    <Tag color="blue">{selectedVersion?.version}</Tag>
                  </div>
                </div>
              </Col>
              <Col span={12}>
                <div>
                  <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>创建时间</Text>
                  <div style={{ padding: '8px 16px', backgroundColor: '#fafafa', borderRadius: 6, color: '#262626' }}>
                    {formatTime(selectedVersion?.createTime)}
                  </div>
                </div>
              </Col>
            </Row>

            {/* 版本说明 */}
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>版本说明</Text>
              <div style={{ padding: '12px 16px', backgroundColor: '#fafafa', borderRadius: 6, color: '#262626' }}>
                {selectedVersion?.description || selectedVersion?.versionDescription || '无说明'}
              </div>
            </div>

            {/* 模型配置 */}
            {selectedVersion?.modelConfig && (
              <div>
                <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>模型配置</Text>
                <div style={{ padding: '12px 16px', backgroundColor: '#fafafa', borderRadius: 6 }}>
                  <Row gutter={16}>
                    <Col span={12}>
                      <Text strong style={{ color: '#595959' }}>模型：</Text>
                      <Tag color="geekblue">{currentModel?.name}</Tag>
                    </Col>
                    {
                      Object.keys(otherModelConfig).map((key) => (
                        <Col span={12}>
                          <Text strong style={{ color: '#595959' }}>{key}：</Text>
                          <Text style={{ marginLeft: 8, color: '#262626' }}>{otherModelConfig[key]}</Text>
                        </Col>
                      ))
                    }
                  </Row>
                </div>
              </div>
            )}

            {/* 参数列表 */}
            {selectedVersion?.parameters && selectedVersion.parameters.length > 0 && (
              <div>
                <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>参数列表</Text>
                <div style={{ padding: '12px 16px', backgroundColor: '#fafafa', borderRadius: 6 }}>
                  <Space size={[8, 8]} wrap>
                    {selectedVersion.parameters.map((param, index) => (
                      <Tag key={index} color="blue">{param}</Tag>
                    ))}
                  </Space>
                </div>
              </div>
            )}

            {/* 版本状态 */}
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>版本状态</Text>
              <div style={{ padding: '8px 16px', backgroundColor: '#fafafa', borderRadius: 6 }}>
                {(selectedVersion?.versionType || selectedVersion?.status) === 'release' ? (
                  <Tag color="success" icon={<CheckCircleOutlined />}>
                    正式版本
                  </Tag>
                ) : (
                  <Tag color="warning" icon={<ExperimentOutlined />}>
                    PRE版本
                  </Tag>
                )}
              </div>
            </div>

            {/* 版本内容 */}
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>版本内容</Text>
              <div style={{
                padding: '12px 16px',
                backgroundColor: '#fafafa',
                borderRadius: 6,
                color: '#262626',
                whiteSpace: 'pre-wrap',
                fontFamily: 'monospace',
                fontSize: '13px',
                maxHeight: 256,
                overflowY: 'auto'
              }}>
                {selectedVersion?.content || selectedVersion?.template || '无内容'}
              </div>
            </div>
          </Space>
        )}
      </Modal>

      {/* 版本对比模态框 */}
      {showCompare && selectedVersions.length === 2 && (
        <VersionCompareModal
          prompt={currentPrompt}
          version1={selectedVersions[0]}
          version2={selectedVersions[1]}
          onClose={() => {
            setShowCompare(false);
            setSelectedVersions([]);
          }}
        />
      )}
    </div>
  );
};

export default VersionHistoryPage;
