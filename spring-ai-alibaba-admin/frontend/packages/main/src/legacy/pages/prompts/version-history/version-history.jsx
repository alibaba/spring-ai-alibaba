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
import $i18n from '@/i18n';

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
        throw new Error(promptResponse.message || $i18n.get({ id: 'legacy.prompts.failed.to.get.prompt.information', dm: '获取 Prompt 信息失败' }));
      }
      setCurrentPrompt(promptResponse.data);

      // Load version list with pagination
      const versionsResponse = await API.getPromptVersions({
        promptKey,
        pageNo: page,
        pageSize: pageSize // Load all versions for client-side pagination
      });

      if (versionsResponse.code !== 200) {
        throw new Error(versionsResponse.message || $i18n.get({ id: 'legacy.prompts.failed.to.get.version.list', dm: '获取版本列表失败' }));
      }

      setVersions(versionsResponse.data.pageItems || []);
      setPagination({
        ...pagination,
        total: versionsResponse.data.totalCount || 0,
        totalPage: versionsResponse.data.totalPage || 0,
      });
    } catch (err) {
      console.error('Failed to load data:', err);
      handleApiError(err, $i18n.get({ id: 'legacy.prompts.load.version.data', dm: '加载版本数据' }));
      setError(err.message || $i18n.get({ id: 'legacy.prompts.loading.failed.please.try.again.later', dm: '加载失败，请稍后重试' }));
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
        throw new Error(response.message || $i18n.get({ id: 'legacy.prompts.failed.to.get.version.details', dm: '获取版本详情失败' }));
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
      console.error('Failed to load version details:', err);
      handleApiError(err, $i18n.get({ id: 'legacy.prompts.load.version.details', dm: '加载版本详情' }));
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
              <p className="text-gray-600 mt-4">{$i18n.get({ id: 'legacy.prompts.loading.version.data', dm: '加载版本数据中...' })}</p>
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
          title={$i18n.get({ id: 'legacy.prompts.failed.to.load.version.data', dm: '加载版本数据失败' })}
          subTitle={error}
          extra={[
            <Button type="primary" key="retry" onClick={() => loadPromptData()}>
              {$i18n.get({ id: 'legacy.prompts.retry', dm: '重试' })}
            </Button>,
            <Button key="back" onClick={() => navigate('/prompts')}>
              {$i18n.get({ id: 'legacy.prompts.back.to.list', dm: '返回列表' })}
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
          title={$i18n.get({ id: 'legacy.prompts.prompt.not.found', dm: 'Prompt 不存在' })}
          subTitle={$i18n.get({ id: 'legacy.prompts.the.requested.prompt.was.not.found.it.may.have.been.deleted', dm: '未找到指定的 Prompt，可能已被删除或不存在。' })}
          extra={
            <Button type="primary" onClick={() => navigate('/prompts')}>
              {$i18n.get({ id: 'legacy.prompts.back.to.list', dm: '返回列表' })}
            </Button>
          }
        />
      </div>
    );
  }

  const handleVersionClick = (version) => {
    // 点击版本行时加载并显示版本Details
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
        throw new Error($i18n.get({ id: 'legacy.prompts.failed.to.get.version.details', dm: '获取版本详情失败' }));
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
      console.error('Failed to load version comparison data:', err);
      handleApiError(err, $i18n.get({ id: 'legacy.prompts.load.version.comparison.data', dm: '加载版本对比数据' }));
    } finally {
      setLoadingVersionDetail(false);
    }
  };

  const handleCheckboxChange = (version, event) => {
    event.stopPropagation(); // 阻止事件冒泡到行点击

    if (selectedVersions.some(v => v.version === version.version)) {
      // 取消Select
      setSelectedVersions(prev => prev.filter(v => v.version !== version.version));
    } else {
      if (selectedVersions.length < 2) {
        // 添加Select
        const newSelection = [...selectedVersions, version];
        setSelectedVersions(newSelection);

        // 如果Select了两个版本，加载详细信息并开始对比
        if (newSelection.length === 2) {
          loadVersionsAndCompare(newSelection[0], newSelection[1]);
        }
      } else {
        // 已经Select了两个版本，替换第一个并开始对比
        const newSelection = [selectedVersions[1], version];
        setSelectedVersions(newSelection);
        loadVersionsAndCompare(newSelection[0], newSelection[1]);
      }
    }
  };

  const handleVersionDetailCompare = async () => {
    if (!selectedVersion) return;

    // 找到前一个版本 (按Created At排序)
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
          throw new Error($i18n.get({ id: 'legacy.prompts.failed.to.get.previous.version.details', dm: '获取前版本详情失败' }));
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
        console.error('Failed to load version comparison data:', err);
        handleApiError(err, $i18n.get({ id: 'legacy.prompts.load.version.comparison.data', dm: '加载版本对比数据' }));
      } finally {
        setLoadingVersionDetail(false);
      }
    } else {
      notifyError({ message: $i18n.get({ id: 'legacy.prompts.no.previous.version.is.available.for.comparison', dm: '没有可对比的前版本' }) });
    }
  };

  const handleRestoreVersion = () => {
    if (!selectedVersion) return;

    console.log('=== 开始恢复版本 ===');
    console.log('选中版本:', selectedVersion);
    console.log('目标窗口ID:', targetWindowId);
    console.log('当前 Prompt Key:', promptKey);

    // 构建恢复URL，使用 promptKey and version
    let restoreUrl = `/prompt-detail?promptKey=${promptKey}&restoreVersionId=${selectedVersion.version}`;
    if (targetWindowId) {
      restoreUrl += `&targetWindowId=${targetWindowId}`;
    }

    console.log('=== 跳转URL ===', restoreUrl);

    // 跳转到PromptDetails页面，并传递要恢复的版本信息和目标窗口ID
    navigate(restoreUrl);

    // Close版本Details弹窗
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
              <Title level={1} style={{ margin: 0 }}>{$i18n.get({ id: 'legacy.prompts.version.history.3', dm: '版本记录' })}</Title>
            </div>
            <Paragraph style={{ margin: 0, color: '#595959' }}>
              <Text strong>{currentPrompt.promptKey}</Text>{$i18n.get({ id: 'legacy.prompts.version.release.history', dm: ' 的版本发布记录' })}
            </Paragraph>
          </div>
        </div>
      </div>

      {/* Actions提示 */}
      <Alert
        message={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Text>{$i18n.get({ id: 'legacy.prompts.select.two.versions.to.compare.or.click.details.in.the.actio', dm: '勾选两个版本进行对比，或点击操作列的“详情”按钮查看版本详情' })}</Text>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              {selectedVersions.length > 0 && (
                <Button
                  type="link"
                  size="small"
                  onClick={clearSelection}
                >
                  {$i18n.get({ id: 'legacy.prompts.clear.selection.2', dm: '清除选择 ({count})' }, { count: selectedVersions.length })}
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
              title: $i18n.get({ id: 'legacy.prompts.select', dm: '选择' }),
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
              title: $i18n.get({ id: 'legacy.prompts.version', dm: '版本号' }),
              key: 'version',
              width: 150,
              render: (_, record) => (
                <Space direction="vertical" size={4}>
                  <Tag color="blue">{record.version}</Tag>
                  {record.actualIndex === 0 && (
                    <Tag color="success" size="small">{$i18n.get({ id: 'legacy.prompts.current.version', dm: '当前版本' })}</Tag>
                  )}
                  {selectedVersions.some(v => v.version === record.version) && (
                    <Tag color="blue" size="small" icon={<CheckCircleOutlined />}>{$i18n.get({ id: 'legacy.prompts.selected', dm: '已选择' })}</Tag>
                  )}
                </Space>
              )
            },
            {
              title: $i18n.get({ id: 'legacy.prompts.published.at', dm: '发布时间' }),
              key: 'createTime',
              width: 150,
              render: (_, record) => formatTime(record.createTime)
            },
            {
              title: $i18n.get({ id: 'legacy.prompts.version.description', dm: '版本说明' }),
              key: 'description',
              width: 200,
              ellipsis: {
                showTitle: false
              },
              render: (_, record) => (
                <Tooltip title={record.versionDescription || record.cachedDetail?.description || $i18n.get({ id: 'legacy.prompts.no.description.3', dm: '无描述' })}>
                  <Text ellipsis>
                    {record.versionDescription || record.cachedDetail?.description || $i18n.get({ id: 'legacy.prompts.no.description.3', dm: '无描述' })}
                  </Text>
                </Tooltip>
              )
            },
            // {
            //   title: $i18n.get({ id: 'legacy.prompts.model.configuration', dm: '模型配置' }),
            //   key: 'modelConfig',
            //   width: 180,
            //   render: (_, record) => {
            //     if (record.cachedDetail?.modelConfig) {
            //       return (
            //         <div>
            //           <div style={{ fontSize: '12px' }}>
            //             <Text strong>Model: </Text>
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
              title: $i18n.get({ id: 'legacy.prompts.status', dm: '状态' }),
              key: 'status',
              width: 120,
              render: (_, record) => {
                if (record.status === 'release') {
                  return (
                    <Tag color="success" icon={<CheckCircleOutlined />}>
                      {$i18n.get({ id: 'legacy.prompts.release', dm: '正式版本' })}
                    </Tag>
                  );
                }
                return (
                  <Tag color="warning" icon={<ExperimentOutlined />}>
                    {$i18n.get({ id: 'legacy.prompts.pre.release', dm: 'PRE版本' })}
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
              title: $i18n.get({ id: 'legacy.prompts.actions', dm: '操作' }),
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
                  title={$i18n.get({ id: 'legacy.prompts.view.details', dm: '查看详情' })}
                >
                  {$i18n.get({ id: 'legacy.prompts.details', dm: '详情' })}
                </Button>
              )
            }
          ]}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={$i18n.get({ id: 'legacy.prompts.no.version.history.yet', dm: '暂无版本记录' })}
              >
                <Button type="primary" onClick={() => navigate(`/prompt-detail?promptKey=${promptKey}`)}>
                  {$i18n.get({ id: 'legacy.prompts.create.a.version', dm: '开始创建版本' })}
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

      {/* 分页和底部Actions区 */}
      <Space direction="vertical" size={16} style={{ width: '100%', marginTop: 24 }}>
        {/* 底部Actions区 */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text type="secondary">
            {$i18n.get({ id: 'legacy.prompts.total.versions.sorted.by.publish.time.in.descending.order', dm: '共 {total} 个版本，按发布时间倒序排列' }, { total: pagination.total })}
          </Text>

          <Space>
            {selectedVersions.length === 2 && (
              <Button
                type="primary"
                icon={<BranchesOutlined />}
                loading={loadingVersionDetail}
                onClick={() => loadVersionsAndCompare(selectedVersions[0], selectedVersions[1])}
              >
                {loadingVersionDetail ? $i18n.get({ id: 'legacy.prompts.loading', dm: '加载中...' }) : $i18n.get({ id: 'legacy.prompts.compare.selected.versions', dm: '对比选中版本' })}
              </Button>
            )}
            <div>
              {selectedVersions.length === 0 && (
                <Text type="secondary">{$i18n.get({ id: 'legacy.prompts.select.two.versions.to.compare', dm: '请选择两个版本进行对比' })}</Text>
              )}
              {selectedVersions.length === 1 && (
                <Text type="secondary">{$i18n.get({ id: 'legacy.prompts.1.version.selected.select.1.more.version', dm: '已选择1个版本，请再选择1个版本' })}</Text>
              )}
              {selectedVersions.length === 2 && !loadingVersionDetail && (
                <Text type="secondary">{$i18n.get({ id: 'legacy.prompts.selected.versions.pair', dm: '已选择版本 {v1} 和 {v2}' }, { v1: selectedVersions[0].version, v2: selectedVersions[1].version })}</Text>
              )}
              {loadingVersionDetail && (
                <Text type="secondary">
                  <LoadingOutlined style={{ marginRight: 8 }} />
                  {$i18n.get({ id: 'legacy.prompts.loading.version.details.for.comparison', dm: '正在加载版本详情用于对比...' })}
                </Text>
              )}
            </div>
          </Space>
        </div>
      </Space>

      {/* 版本Details模态框 */}
      <Modal
        title={$i18n.get({ id: 'legacy.prompts.version.details', dm: '版本详情 - {selectedVersion}' }, { selectedVersion: selectedVersion?.version })}
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
              {$i18n.get({ id: 'legacy.prompts.restore.to.editor', dm: '恢复到编辑区' })}
            </Button>
            <Button
              icon={<BranchesOutlined />}
              onClick={handleVersionDetailCompare}
            >
              {$i18n.get({ id: 'legacy.prompts.compare.with.previous.version', dm: '与前版本对比' })}
            </Button>
            <Button onClick={() => setShowVersionDetail(false)}>
              {$i18n.get({ id: 'legacy.prompts.close', dm: '关闭' })}
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
                <Text type="secondary">{$i18n.get({ id: 'legacy.prompts.loading.version.details', dm: '加载版本详情中...' })}</Text>
              </div>
            </Spin>
          </div>
        ) : (
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            {/* 版本基本信息 */}
            <Row gutter={24}>
              <Col span={12}>
                <div>
                  <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>{$i18n.get({ id: 'legacy.prompts.version', dm: '版本号' })}</Text>
                  <div style={{ padding: '8px 16px', backgroundColor: '#fafafa', borderRadius: 6 }}>
                    <Tag color="blue">{selectedVersion?.version}</Tag>
                  </div>
                </div>
              </Col>
              <Col span={12}>
                <div>
                  <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>{$i18n.get({ id: 'legacy.prompts.created.at', dm: '创建时间' })}</Text>
                  <div style={{ padding: '8px 16px', backgroundColor: '#fafafa', borderRadius: 6, color: '#262626' }}>
                    {formatTime(selectedVersion?.createTime)}
                  </div>
                </div>
              </Col>
            </Row>

            {/* Version Description */}
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>{$i18n.get({ id: 'legacy.prompts.version.description', dm: '版本说明' })}</Text>
              <div style={{ padding: '12px 16px', backgroundColor: '#fafafa', borderRadius: 6, color: '#262626' }}>
                {selectedVersion?.description || selectedVersion?.versionDescription || $i18n.get({ id: 'legacy.prompts.no.description.3', dm: '无描述' })}
              </div>
            </div>

            {/* Model Configuration */}
            {selectedVersion?.modelConfig && (
              <div>
                <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>{$i18n.get({ id: 'legacy.prompts.model.configuration', dm: '模型配置' })}</Text>
                <div style={{ padding: '12px 16px', backgroundColor: '#fafafa', borderRadius: 6 }}>
                  <Row gutter={16}>
                    <Col span={12}>
                      <Text strong style={{ color: '#595959' }}>Model: </Text>
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

            {/* Parameters */}
            {selectedVersion?.parameters && selectedVersion.parameters.length > 0 && (
              <div>
                <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>{$i18n.get({ id: 'legacy.prompts.parameters', dm: '参数列表' })}</Text>
                <div style={{ padding: '12px 16px', backgroundColor: '#fafafa', borderRadius: 6 }}>
                  <Space size={[8, 8]} wrap>
                    {selectedVersion.parameters.map((param, index) => (
                      <Tag key={index} color="blue">{param}</Tag>
                    ))}
                  </Space>
                </div>
              </div>
            )}

            {/* Version Status */}
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>{$i18n.get({ id: 'legacy.prompts.version.status', dm: '版本状态' })}</Text>
              <div style={{ padding: '8px 16px', backgroundColor: '#fafafa', borderRadius: 6 }}>
                {(selectedVersion?.versionType || selectedVersion?.status) === 'release' ? (
                  <Tag color="success" icon={<CheckCircleOutlined />}>
                    {$i18n.get({ id: 'legacy.prompts.release', dm: '正式版本' })}
                  </Tag>
                ) : (
                  <Tag color="warning" icon={<ExperimentOutlined />}>
                    {$i18n.get({ id: 'legacy.prompts.pre.release', dm: 'PRE版本' })}
                  </Tag>
                )}
              </div>
            </div>

            {/* Version Content */}
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8, color: '#262626' }}>{$i18n.get({ id: 'legacy.prompts.version.content', dm: '版本内容' })}</Text>
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
                {selectedVersion?.content || selectedVersion?.template || $i18n.get({ id: 'legacy.prompts.no.content', dm: '无内容' })}
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
