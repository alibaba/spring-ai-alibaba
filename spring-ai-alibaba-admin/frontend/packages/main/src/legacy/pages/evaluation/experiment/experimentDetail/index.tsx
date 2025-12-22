import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { Button, Card, Progress, Tag, Alert, Spin, Table, message, Typography, Tooltip } from 'antd';
import { ArrowLeftOutlined, StopOutlined, ReloadOutlined } from '@ant-design/icons';
import API from '../../../../services';
import usePagination from '../../../../hooks/usePagination';
import './index.css';

const { Title, Text } = Typography;

// 添加隐藏滚动条的CSS样式
const scrollbarHideStyle = `
  .scrollbar-hide::-webkit-scrollbar {
    display: none;
  }
  .scrollbar-hide {
    -ms-overflow-style: none;
    scrollbar-width: none;
  }
`;

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

// 评测结果数据接口
interface EvaluationResult {
  id: number;
  input: string;
  actualOutput: string;
  referenceOutput: string;
  score: number;
  status: 'success' | 'failed';
}

// 执行日志数据接口
interface ExecutionLog {
  id: number;
  timestamp: string;
  message: string;
  type: 'info' | 'success' | 'error' | 'processing';
  score?: number;
}

// 实验概览结果数据接口
interface ExperimentOverview {
  experimentId: number;
  averageScore: number;
  evaluatorVersionId: number;
  progress: number;
  completeItemsCount: number;
  totalItemsCount: number; // 添加总数据项数量字段
}

// Tab类型
type TabType = 'overview' | 'results';

// 实验详情数据接口
interface ExperimentDetail {
  id: number;
  name: string;
  description: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'WAITING' | 'STOPPED';
  progress: number;
  totalProgress: string;
  averageScore: number;
  createTime: string;
  startTime: string;
  endTime?: string;
  completeTime?: string;
  dataset: {
    name: string;
    id: string;
    columns: string[];
  };
  evaluationObject: {
    type: string;
    promptKey: string;
    version: string;
    promptDetail: string;
    promptContent: string;
    inputTemplate: string;
  };
  evaluators: Array<{
    id: string;
    name: string;
    version: string;
    dataCount: number;
    columns: string[];
  }>;
  evaluationResults: {
    schema: string;
    mapping: Record<string, string>;
    progress: number;
  };
}

// 评估器配置接口
interface EvaluatorConfig {
  evaluatorId: number;
  evaluatorVersionId: number;
  evaluatorName: string;
  variableMap: Array<{
    evaluatorVariable: string;
    source: string;
  }>;
  versionName?: string; // 添加版本名称字段
}

const ExperimentDetail: React.FC = () => {
  // 添加样式标签以隐藏滚动条
  React.useEffect(() => {
    const style = document.createElement('style');
    style.innerHTML = `
      .scrollbar-hide::-webkit-scrollbar {
        display: none;
      }
      .scrollbar-hide {
        -ms-overflow-style: none;
        scrollbar-width: none;
      }
    `;
    document.head.appendChild(style);
    
    return () => {
      document.head.removeChild(style);
    };
  }, []);

  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams<{ id: string }>();
  // 从location.state中获取传递的evaluatorConfig数据
  const passedEvaluatorConfig = location.state?.evaluatorConfig;
  const defaultActiveTab = location.state?.activeTab;
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<ExperimentDetail | null>(null);
  const [stopping, setStopping] = useState(false);
  const [activeTab, setActiveTab] = useState<TabType>(defaultActiveTab || 'overview');
  const [activeEvaluatorTab, setActiveEvaluatorTab] = useState<string>('test'); // 评估器子Tab的状态
  const [overviewData, setOverviewData] = useState<ExperimentOverview[]>([]); // 概览结果数据
  const [overviewLoading, setOverviewLoading] = useState(false); // 概览数据加载状态
  
  // 评测结果相关状态
  const [resultData, setResultData] = useState<EvaluationResult[]>([]); // 当前评估器的结果数据
  const [resultLoading, setResultLoading] = useState(false); // 结果数据加载状态
  const { pagination, setPagination, onPaginationChange, onShowSizeChange } = usePagination();

  // 获取评测结果数据
  const fetchExperimentResult = async (evaluatorVersionId: number, pageNumber?: number, pageSize?: number) => {
    if (!id) return;
    
    try {
      setResultLoading(true);
      
      // 如果没有传入分页参数，则使用当前 pagination 状态
      const current = pageNumber || pagination.current;
      const size = pageSize || pagination.pageSize;
      
      const response = await API.getExperimentResult({
        experimentId: Number(id),
        evaluatorVersionId,
        pageNumber: current,
        pageSize: size
      });
      
      if (response.code === 200 && response.data) {
        const { pageItems, totalCount, pageNumber: currentPage, pageSize: currentPageSize } = response.data as any;
        
        // 转换数据格式
        const results: EvaluationResult[] = (pageItems || []).map((item: any) => ({
          id: item.id,
          input: item.input,
          actualOutput: item.actualOutput,
          referenceOutput: item.referenceOutput,
          score: item.score,
          status: item.score > 0.5 ? 'success' : 'failed',
          reason: item.reason || '暂无理由'
        }));
        
        setResultData(results);
        // 更新分页状态
        setPagination(prev => ({
          ...prev,
          current: currentPage || current,
          pageSize: currentPageSize || size,
          total: totalCount || 0
        }));
      } else {
        throw new Error(response.message || '获取评测结果失败');
      }
    } catch (error) {
      console.error('获取评测结果失败:', error);
      // 错误时使用空数据
      setResultData([]);
      setPagination(prev => ({
        ...prev,
        current: 1,
        total: 0
      }));
    } finally {
      setResultLoading(false);
    }
  };

  // 获取实验概览结果
  const fetchExperimentOverview = async () => {
    if (!id) return;
    
    try {
      setOverviewLoading(true);
      
      const response = await API.getExperimentOverview({ experimentId: Number(id) });
      
      if (response.code === 200 && response.data) {
        // response.data 已经是数组格式
        const overviewList = response.data as any;
        setOverviewData(overviewList);
      } else {
        throw new Error(response.message || '获取概览数据失败');
      }
    } catch (error) {
      console.error('获取概览数据失败:', error);
      // 错误时使用空数据
      setOverviewData([]);
    } finally {
      setOverviewLoading(false);
    }
  };

  // 获取实验详情
  const fetchExperimentDetail = async () => {
    try {
      setLoading(true);
      
      // 调用详情接口
      const response = await API.getExperiment({ experimentId: Number(id) });
      
      if (response.code === 200 && response.data) {
        const apiData = response.data as any;
        
        // 解析 evaluationObjectConfig
        let evaluationObject;
        try {
          evaluationObject = JSON.parse(apiData.evaluationObjectConfig || '{}');
        } catch (e) {
          console.warn('解析 evaluationObjectConfig 失败:', e);
          evaluationObject = { type: '', config: {} };
        }
        
        // 解析 evaluatorConfig
        let evaluatorConfigs: EvaluatorConfig[] = [];
        try {
          evaluatorConfigs = JSON.parse(apiData.evaluatorConfig || '[]');
        } catch (e) {
          console.warn('解析 API 返回的 evaluatorConfig 失败:', e);
          evaluatorConfigs = [];
        }
        
        // 如果有传递过来的evaluatorConfig数据，尝试解析并合并
        if (passedEvaluatorConfig) {
          try {
            const passedConfigs = JSON.parse(passedEvaluatorConfig || '[]');
            // 合并逻辑：使用传递的数据补充API数据中缺失的字段
            evaluatorConfigs = evaluatorConfigs.map(apiConfig => {
              const passedConfig = passedConfigs.find((c: any) => 
                c.evaluatorId === apiConfig.evaluatorId && c.evaluatorVersionId === apiConfig.evaluatorVersionId
              );
              // 如果找到了对应的传递配置，合并数据
              if (passedConfig) {
                return {
                  ...passedConfig,
                  ...apiConfig // API数据优先，覆盖传递的数据
                };
              }
              return apiConfig;
            });
            
            // 添加传递数据中有但API数据中没有的配置
            passedConfigs.forEach((passedConfig: any) => {
              const exists = evaluatorConfigs.some(apiConfig => 
                apiConfig.evaluatorId === passedConfig.evaluatorId && apiConfig.evaluatorVersionId === passedConfig.evaluatorVersionId
              );
              if (!exists) {
                evaluatorConfigs.push(passedConfig);
              }
            });
          } catch (e) {
            console.warn('解析传递的 evaluatorConfig 失败:', e);
          }
        }
        
        // 转换API数据为组件需要的格式
        const detailData: ExperimentDetail = {
          id: apiData.id,
          name: apiData.name,
          description: apiData.description,
          status: apiData.status as 'RUNNING' | 'COMPLETED' | 'FAILED' | 'WAITING' | 'STOPPED',
          progress: apiData.progress || 0,
          totalProgress: `${apiData.progress || 0}%`,
          averageScore: 0.85, // 默认值，后续可以从结果接口获取
          createTime: apiData.createTime,
          startTime: apiData.createTime,
          endTime: apiData.completeTime || undefined,
          completeTime: apiData.completeTime || undefined,
          dataset: {
            name: '默认评测集', // 默认值，后续可以通过 datasetId 查询获取
            id: apiData.datasetId.toString(),
            columns: ['input', 'reference_output']
          },
          evaluationObject: {
            type: evaluationObject.type || 'Prompt',
            promptKey: evaluationObject.config?.promptKey || '',
            version: evaluationObject.config?.versionId || '',
            promptDetail: evaluationObject.config?.promptDescription || '',
            promptContent: '默认Prompt内容', // 默认值
            inputTemplate: '{{input}}'
          },
          evaluators: evaluatorConfigs.map((config: EvaluatorConfig, index: number) => ({
            // 使用唯一的ID，结合索引确保唯一性
            id: `${config.evaluatorId}-${config.evaluatorVersionId}-${index}`,
            name: config.evaluatorName,
            version: config.versionName || (config as any).evaluatorVersionName || '1.0.0', // 优先使用versionName字段，其次evaluatorVersionName字段
            dataCount: 150,
            columns: ['input', 'reference_output']
          })),
          evaluationResults: {
            schema: '字段映射',
            mapping: {
              'evaluator.input': 'dataset.input',
              'evaluator.output': 'evaluation_object.output', // 移除硬编码的actual_output
              'evaluator.reference_output': 'dataset.reference_output'
            },
            progress: apiData.progress || 0
          }
        };
        
        setDetail(detailData);
      } else {
        throw new Error(response.message || '获取实验详情失败');
      }
    } catch (error) {
      console.error('获取实验详情失败:', error);
      
      // 错误处理，设置为空状态
      setDetail(null);
      setLoading(false);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      fetchExperimentDetail();
    }
  }, [id]);

  // 在详情加载完成后，如果是概览tab或评测结果tab则获取概览数据
  useEffect(() => {
    if (detail && (activeTab === 'overview' || activeTab === 'results')) {
      fetchExperimentOverview();
    }
  }, [detail, activeTab]);

  // 在评测结果tab中，当子tab切换时获取对应评估器的结果数据
  useEffect(() => {
    if (detail && activeTab === 'results') {
      // 根据activeEvaluatorTab找到对应的评估器
      const currentEvaluator = detail.evaluators.find((evaluator, index) => 
        `evaluator-${evaluator.id}-${evaluator.version}-${index}` === activeEvaluatorTab
      ) || detail.evaluators[0];
      
      if (currentEvaluator) {
        // 使用当前评估器的ID作为evaluatorVersionId参数调用接口
        // 注意：这里我们使用evaluatorVersionId而不是id
        const evaluatorVersionId = parseInt(currentEvaluator.id.split('-')[1]); // 从id中提取evaluatorVersionId
        // 重置分页到第1页，页面数据量为10
        setPagination(prev => ({
          ...prev,
          current: 1,
          pageSize: 10,
          total: prev.total
        }));
        // 使用重置后的分页获取数据
        fetchExperimentResult(evaluatorVersionId, 1, 10);
      }
    }
  }, [detail, activeTab, activeEvaluatorTab]); // 当activeEvaluatorTab变化时重新获取数据

  // 当从查看实验结果进入且选中评测结果tab时，自动选中第一个评估器子tab
  useEffect(() => {
    if (detail && activeTab === 'results') {
      // 自动选中第一个评估器子tab
      if (detail.evaluators && detail.evaluators.length > 0) {
        const firstEvaluator = detail.evaluators[0];
        // 使用新的唯一tabKey
        const firstTabKey = `evaluator-${firstEvaluator.id}-${firstEvaluator.version}-0`;
        setActiveEvaluatorTab(firstTabKey);
      } else {
        // 如果没有评估器数据，设置默认tab
        setActiveEvaluatorTab('test');
      }
    }
  }, [detail, activeTab]);

  // 返回列表页面
  const handleGoBack = () => {
    navigate('/evaluation-experiment');
  };

  // 停止实验
  const handleStopExperiment = async () => {
    try {
      setStopping(true);
      
      // 调用停止实验的API
      const response = await API.stopExperiment({ experimentId: Number(id) });
      
      if (response.code === 200) {
        // 停止成功，更新实验状态
        if (detail) {
          setDetail({ ...detail, status: 'FAILED' });
        }
        // 重新获取详情以获取最新状态
        await fetchExperimentDetail();
      } else {
        throw new Error(response.message || '停止实验失败');
      }
    } catch (error) {
      console.error('停止实验失败:', error);
      
      // 错误处理
      message.error('停止实验失败，请重试');
    } finally {
      setStopping(false);
    }
  };

  // 渲染状态标签
  const renderStatusTag = (status: string) => {
    const statusConfig = {
      RUNNING: { color: 'blue', text: '运行中' },
      COMPLETED: { color: 'green', text: '已完成' },
      FAILED: { color: 'red', text: '失败' },
      WAITING: { color: 'default', text: '等待中' },
      STOPPED: { color: 'orange', text: '已停止' }
    };
    const config = statusConfig[status as keyof typeof statusConfig];
    return <Tag color={config?.color || 'default'}>{config?.text || status}</Tag>;
  };

  if (loading) {
    return (
      <div className="experiment-detail-page">
        <div className="p-6 text-center" style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          width: '100%',
          position: 'fixed',
          top: 0,
          left: 0,
          zIndex: 1000,
          backgroundColor: 'white'
        }}>
          <Spin size="large" />
        </div>
      </div>
    );
  }

  if (!detail && !loading) {
    return (
      <div className="experiment-detail-page">
        <div className="p-6 text-center" style={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          width: '100%'
        }}>
          <p style={{ marginBottom: '20px' }}>实验详情不存在</p>
          <Button onClick={handleGoBack}>返回列表</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="experiment-detail-page p-8 fade-in">
      {/* 页面头部 */}
      <div className="flex justify-between items-center mb-6">
        <div className="flex">
          <Button 
            type="text" 
            icon={<ArrowLeftOutlined />} 
            onClick={handleGoBack}
            size="large"
          >
          </Button>
          <div className="flex">
            <Title level={2} className="mr-4">{detail?.name}</Title>
            <span className="text-2xl font-semibold mb-0">{detail && renderStatusTag(detail.status)}</span>
          </div>
        </div>
        
        {detail?.status === 'RUNNING' && (
          <div>
            <Button 
              icon={<ReloadOutlined />}
              onClick={fetchExperimentDetail}
              className="mr-4"
              title="刷新"
            >
              刷新
            </Button>
            <Button 
              danger 
              icon={<StopOutlined />}
              loading={stopping}
              onClick={handleStopExperiment}
            >
              停止实验
            </Button>
          </div>
        )}
      </div>

      {/* 实验状态信息 */}
      {/* {detail.status === 'RUNNING' && (
        <Alert
          message="实验正在运行中"
          description={`当前进度: ${detail.progress}%，已处理 ${detail.totalProgress} 条数据`}
          type="info"
          showIcon
          className="mb-6"
        />
      )} */}

      {/* 实验信息区域 */}
      <Card className="mb-6">
        <div className="grid grid-cols-2 gap-8">
          {/* 左侧列 */}
          <div className="space-y-4">
            <div className="flex items-center">
              <span className="text-sm font-medium flex-shrink-0" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>实验名称：</span>
              <span className="text-base text-gray-900" style={{ wordBreak: 'break-word' }}>{detail?.name || '-'}</span>
            </div>
            <div className="flex items-center">
              <span className="text-sm font-medium flex-shrink-0" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>描述：</span>
              <span className="text-base text-gray-900" style={{ wordBreak: 'break-word' }}>{detail?.description || '-'}</span>
            </div>
            <div className="flex items-center">
              <span className="text-sm font-medium flex-shrink-0" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>评测集：</span>
              <span className="text-base text-gray-900" style={{ wordBreak: 'break-word' }}>{detail?.dataset?.name || '-'}</span>
            </div>
            {/* 评估器字段和内容展示在一行 */}
            <div className="flex items-center">
              <span className="text-sm font-medium flex-shrink-0" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>评估器：</span>
              <div>
                {detail?.evaluators && detail?.evaluators?.length > 0 ? (
                  detail?.evaluators.map((evaluator, index) => (
                    <Tag key={index} color="blue" className="mr-2">
                      {evaluator.name}
                    </Tag>
                  ))
                ) : (
                  <span className="text-base text-gray-900">-</span>
                )}
              </div>
            </div>
          </div>
          
          {/* 右侧列 */}
          <div className="space-y-4">
            <div className="flex items-center">
              <span className="text-sm font-medium flex-shrink-0" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>状态：</span>
              <span className="text-base">{detail && renderStatusTag(detail.status)}</span>
            </div>
            {/* <div className="flex items-center">
              <span className="text-sm font-medium" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>创建人：</span>
              <span className="text-base text-gray-900">{detail.creator}</span>
            </div> */}
            <div className="flex items-center">
              <span className="text-sm font-medium flex-shrink-0" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>评测对象：</span>
              <span className="text-base text-gray-900" style={{ wordBreak: 'break-word' }}>{detail?.evaluationObject?.type || '-'}</span>
            </div>
            <div className="flex items-center">
              <span className="text-sm font-medium flex-shrink-0" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>更新时间：</span>
              <span className="text-base text-gray-900">{detail && formatDateTime(detail.startTime)}</span>
            </div>
            <div className="flex items-center">
              <span className="text-sm font-medium flex-shrink-0" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>创建时间：</span>
              <span className="text-base text-gray-900">{detail && formatDateTime(detail.createTime)}</span>
            </div>
          </div>
        </div>
      </Card>

      {/* Tab导航 */}
      <Card>
        <div className="mb-6">
          <div className="border-b border-gray-200 mb-4">
            <div className="flex space-x-8">
              <div 
                className={`pb-2 cursor-pointer font-medium ${
                  activeTab === 'overview' 
                    ? 'border-b-2 border-blue-500 text-blue-600' 
                    : 'text-gray-500 hover:text-gray-700'
                }`}
                onClick={() => setActiveTab('overview')}
              >
                概览
              </div>
              <div 
                className={`pb-2 cursor-pointer font-medium ${
                  activeTab === 'results' 
                    ? 'border-b-2 border-blue-500 text-blue-600' 
                    : 'text-gray-500 hover:text-gray-700'
                }`}
                onClick={() => {
                  setActiveTab('results');
                  // 当切换到评测结果tab时，自动选中第一个评估器子tab
                  if (detail?.evaluators && detail.evaluators.length > 0) {
                    const firstEvaluator = detail.evaluators[0];
                    // 使用新的唯一tabKey
                    const firstTabKey = `evaluator-${firstEvaluator.id}-${firstEvaluator.version}-0`;
                    setActiveEvaluatorTab(firstTabKey);
                  } else {
                    // 如果没有评估器数据，设置默认tab
                    setActiveEvaluatorTab('test');
                  }
                }}
              >
                评测结果
              </div>
            </div>
          </div>

          {/* Tab内容区域 */}
          {activeTab === 'overview' && (
            <>
              {/* 评估器结果概览 */}
              <div className="mb-6">
                <h3 className="text-lg font-medium mb-4">评估器结果概览</h3>
                <div className="grid grid-cols-2 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {detail?.evaluators && detail.evaluators.length > 0 ? (
                    detail.evaluators.map((evaluator, index) => {
                      // 获取对应的概览数据
                      const overview = overviewData[index] || {
                        evaluatorVersionId: parseInt(evaluator.id),
                        averageScore: 0,
                        progress: 0,
                        completeItemsCount: 0,
                        totalItemsCount: 0
                      };
                      
                      // 计算进度百分比
                      const progressPercent = overview.totalItemsCount > 0 
                        ? Math.round((overview.completeItemsCount / overview.totalItemsCount) * 100)
                        : Math.round(overview.progress);
                      
                      // 根据得分确定颜色
                      const scoreColor = overview.averageScore >= 0.8 ? 'text-green-600' : 'text-orange-500';
                      
                      return (
                        <Card key={evaluator.id} className="p-4">
                          <div className="flex justify-between items-start mb-4">
                            <div>
                              <h4 className="text-base font-medium text-gray-900 mb-1">{evaluator.name}</h4>
                              <p className="text-sm" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>评估器描述</p>
                            </div>
                            <Tag color="blue">{evaluator.version}</Tag>
                          </div>
                          
                          <div className="space-y-3">
                            <div>
                              <div className="flex justify-between items-center mb-1">
                                <span className="text-sm" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>完成进度</span>
                                <span className="text-sm font-medium">{overview.completeItemsCount}/{overview.totalItemsCount || evaluator.dataCount}</span>
                              </div>
                              <Progress percent={progressPercent} strokeColor="#1677ff" size="small" />
                              <div className="text-right mt-1">
                                <span className="text-sm font-medium">{progressPercent}%</span>
                              </div>
                            </div>
                            
                            <div className="flex justify-between items-center">
                              <span className="text-sm" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>平均得分</span>
                              <span className={`text-lg font-semibold ${scoreColor}`}>{overview.averageScore.toFixed(2)}</span>
                            </div>
                            
                            <div className="text-xs" style={{ color: 'rgba(0, 0, 0, 0.45)' }}>
                              基于 {overview.completeItemsCount} 条已完成评估
                            </div>
                          </div>
                        </Card>
                      );
                    })
                  ) : (
                    <div className="col-span-full text-center py-8 text-gray-500">
                      暂无评估器数据
                    </div>
                  )}
                </div>
              </div>
            </>
          )}

          {activeTab === 'results' && (
            <>
              {/* 评估器子Tab导航 */}
              <div className="border-b border-gray-200 mb-4 ml-4">
                <div className="flex space-x-8 overflow-x-auto whitespace-nowrap scrollbar-hide -mx-4">
                  {detail?.evaluators?.map((evaluator, index) => {
                    // 使用唯一的tabKey，结合评估器ID、版本和索引确保唯一性
                    const tabKey = `evaluator-${evaluator.id}-${evaluator.version}-${index}`;
                    const tabName = evaluator.name; // 只显示评估器名称，不显示版本号
                    
                    return (
                      <div 
                        key={tabKey}
                        className={`pb-2 cursor-pointer font-medium flex-shrink-0 ${
                          activeEvaluatorTab === tabKey
                            ? 'border-b-2 border-blue-500 text-blue-600' 
                            : 'text-gray-500 hover:text-gray-700'
                        }`}
                        onClick={() => setActiveEvaluatorTab(tabKey)}
                      >
                        {tabName}
                      </div>
                    );
                  })}
                  
                  {/* 如果没有评估器数据，不显示任何Tab */}
                  {(!detail?.evaluators || detail.evaluators.length === 0) && (
                    <div className="text-gray-500 py-2">
                      暂无评估器数据
                    </div>
                  )}
                </div>
              </div>

              {/* 当前评估器的结果内容 */}
              <div className="mb-4">
                {/* 评估器信息 */}
                {detail?.evaluators && detail.evaluators.length > 0 && (() => {
                  // 根据activeEvaluatorTab找到对应的评估器
                  const currentEvaluator = detail.evaluators.find((evaluator, index) => 
                    `evaluator-${evaluator.id}-${evaluator.version}-${index}` === activeEvaluatorTab
                  ) || detail.evaluators[0];
                  
                  if (currentEvaluator) {
                    // 获取对应的概览数据，使用与概览页相同的数据源
                    const evaluatorIndex = detail.evaluators.findIndex((evaluator, index) => 
                      `evaluator-${evaluator.id}-${evaluator.version}-${index}` === activeEvaluatorTab
                    );
                    const overview = overviewData[evaluatorIndex] || {
                      evaluatorVersionId: parseInt(currentEvaluator.id),
                      averageScore: 0,
                      progress: 0,
                      completeItemsCount: 0,
                      totalItemsCount: 0
                    };
                    
                    // 计算进度百分比，与概览页保持一致
                    const progressPercent = overview.totalItemsCount > 0 
                      ? Math.round((overview.completeItemsCount / overview.totalItemsCount) * 100)
                      : Math.round(overview.progress);
                    
                    // 根据得分确定颜色，与概览页保持一致
                    const scoreColor = overview.averageScore >= 0.8 ? 'text-green-600' : 'text-orange-500';
                    
                    return (
                      <div className="mb-4">
                        <div className="text-base font-medium mb-2">{currentEvaluator.name}</div>
                        <div className="flex items-center space-x-6 text-sm">
                          <div>
                            <span style={{ color: 'rgba(0, 0, 0, 0.45)' }}>平均得分：</span>
                            <span className={`text-lg font-semibold ml-2 ${scoreColor}`}>
                              {overview.averageScore.toFixed(2)}
                            </span>
                          </div>
                          <div>
                            <span style={{ color: 'rgba(0, 0, 0, 0.45)' }}>完成进度：</span>
                            <span className="font-medium ml-2">
                              {overview.completeItemsCount}/{overview.totalItemsCount || currentEvaluator.dataCount}
                            </span>
                          </div>
                        </div>
                      </div>
                    );
                  }
                  return null;
                })()}
                
                {/* 详细结果表格 */}
                <Table
                  dataSource={resultData}
                  loading={resultLoading}
                  rowKey="id"
                  pagination={{
                    current: pagination.current,
                    pageSize: pagination.pageSize,
                    total: pagination.total,
                    showTotal: pagination.showTotal,
                    showSizeChanger: pagination.showSizeChanger,
                    showQuickJumper: pagination.showQuickJumper,
                    pageSizeOptions: pagination.pageSizeOptions,
                    onChange: (page, pageSize) => {
                      // 更新分页状态
                      onPaginationChange(page, pageSize);
                      // 根据activeEvaluatorTab找到对应的评估器
                      const currentEvaluator = detail?.evaluators?.find((evaluator, index) => 
                        `evaluator-${evaluator.id}-${evaluator.version}-${index}` === activeEvaluatorTab
                      ) || detail?.evaluators?.[0];
                      
                      if (currentEvaluator) {
                        // 使用当前评估器的ID作为evaluatorVersionId参数调用接口
                        const evaluatorVersionId = parseInt(currentEvaluator.id.split('-')[1]);
                        // 获取数据
                        fetchExperimentResult(evaluatorVersionId, page, pageSize);
                      }
                    },
                    onShowSizeChange: (page, pageSize) => {
                      // 更新分页状态
                      onShowSizeChange(page, pageSize);
                      // 根据activeEvaluatorTab找到对应的评估器
                      const currentEvaluator = detail?.evaluators?.find((evaluator, index) => 
                        `evaluator-${evaluator.id}-${evaluator.version}-${index}` === activeEvaluatorTab
                      ) || detail?.evaluators?.[0];
                      
                      if (currentEvaluator) {
                        // 使用当前评估器的ID作为evaluatorVersionId参数调用接口
                        const evaluatorVersionId = parseInt(currentEvaluator.id.split('-')[1]);
                        // 获取数据
                        fetchExperimentResult(evaluatorVersionId, page, pageSize);
                      }
                    }
                  }}
                  columns={[
                    { 
                      title: '输入', 
                      dataIndex: 'input', 
                      width: '25%',
                      ellipsis: true,
                      render: (text: string) => (
                        <Tooltip title={text} placement="topLeft">
                          <span>{text}</span>
                        </Tooltip>
                      )
                    },
                    { 
                      title: '实际输出', 
                      dataIndex: 'actualOutput', 
                      width: '25%',
                      ellipsis: true,
                      render: (text: string) => (
                        <Tooltip title={text} placement="topLeft">
                          <span>{text}</span>
                        </Tooltip>
                      )
                    },
                    { 
                      title: '参考输出', 
                      dataIndex: 'referenceOutput', 
                      width: '25%',
                      ellipsis: true,
                      render: (text: string) => (
                        <Tooltip title={text} placement="topLeft">
                          <span>{text}</span>
                        </Tooltip>
                      )
                    },
                    { 
                      title: '分数', 
                      dataIndex: 'score', 
                      width: '10%',
                      render: (score: number) => {
                        const scoreColor = score >= 0.8 ? 'text-green-600' : score >= 0.5 ? 'text-orange-500' : 'text-red-500';
                        return (
                          <span className={`font-medium ${scoreColor}`}>
                            {score.toFixed(2)}
                          </span>
                        );
                      }
                    },
                    { 
                      title: '理由', 
                      dataIndex: 'reason', 
                      width: '15%',
                      ellipsis: true,
                      render: (text: string) => (
                        <Tooltip title={text} placement="rightTop">
                          <span>{text}</span>
                        </Tooltip>
                      )
                    }
                  ]}
                />
              </div>
            </>
          )}
        </div>
      </Card>
    </div>
  );
};

export default ExperimentDetail;