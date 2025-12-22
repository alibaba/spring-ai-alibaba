import React, { useState, useEffect } from 'react';
import {
  Modal,
  Card,
  Typography,
  Button,
  Alert,
  Space,
  Row,
  Col,
  Tag,
  Empty,
  Spin,
  message,
  Input,
  Pagination
} from 'antd';
import {
  CloseOutlined,
  DownloadOutlined,
  InfoCircleOutlined,
  FolderOutlined,
  CheckCircleOutlined,
  FileTextOutlined,
  SearchOutlined
} from '@ant-design/icons';
import { getPromptTemplates, getPromptTemplate } from '../services/prompt';

const { Title, Text, Paragraph } = Typography;
const { Search } = Input;

const TemplateImportModal = ({ onImport, onClose }) => {
  const [selectedTemplate, setSelectedTemplate] = useState('');
  const [templates, setTemplates] = useState([]);
  const [selectedTemplateData, setSelectedTemplateData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [templateDetailLoading, setTemplateDetailLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 12,
    total: 0
  });

  // 获取模板列表
  const fetchTemplates = async (params = {}) => {
    setLoading(true);
    try {
      const response = await getPromptTemplates({
        pageNo: pagination.current,
        pageSize: pagination.pageSize,
        search: searchText ? 'blur' : undefined,
        promptTemplateKey: searchText || undefined,
        ...params
      });
      
      if (response.code === 200 && response.data) {
        setTemplates(response.data.pageItems || []);
        setPagination(prev => ({
          ...prev,
          total: response.data.totalCount || 0,
          current: response.data.pageNumber || 1
        }));
      } else {
        message.error(response.message || '获取模板列表失败');
      }
    } catch (error) {
      console.error('获取模板列表失败:', error);
      message.error('获取模板列表失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  // 获取模板详情
  const fetchTemplateDetail = async (promptTemplateKey) => {
    if (!promptTemplateKey) return;
    
    setTemplateDetailLoading(true);
    try {
      const response = await getPromptTemplate({ promptTemplateKey });
      
      if (response.code === 200 && response.data) {
        setSelectedTemplateData(response.data);
      } else {
        message.error(response.message || '获取模板详情失败');
      }
    } catch (error) {
      console.error('获取模板详情失败:', error);
      message.error('获取模板详情失败，请稍后重试');
    } finally {
      setTemplateDetailLoading(false);
    }
  };

  // 初始化加载
  useEffect(() => {
    fetchTemplates();
  }, []);

  // 模板选择变化
  useEffect(() => {
    if (selectedTemplate) {
      fetchTemplateDetail(selectedTemplate);
    } else {
      setSelectedTemplateData(null);
    }
  }, [selectedTemplate]);

  // 搜索处理
  const handleSearch = (value) => {
    setSearchText(value);
    setPagination(prev => ({ ...prev, current: 1 }));
    fetchTemplates({ 
      pageNo: 1,
      search: value ? 'blur' : undefined,
      promptTemplateKey: value || undefined
    });
  };

  // 分页处理
  const handlePageChange = (page, pageSize) => {
    setPagination(prev => ({ ...prev, current: page, pageSize }));
    fetchTemplates({ pageNo: page, pageSize });
  };

  // 导入处理
  const handleImport = () => {
    if (selectedTemplateData) {
      // 解析变量
      let variables = [];
      try {
        if (selectedTemplateData.variables) {
          const varsObj = JSON.parse(selectedTemplateData.variables);
          variables = Object.keys(varsObj);
        }
      } catch (error) {
        console.warn('解析模板变量失败:', error);
      }

      // 解析标签
      let tags = [];
      try {
        if (selectedTemplateData.tags) {
          tags = JSON.parse(selectedTemplateData.tags);
        }
      } catch (error) {
        // 如果解析失败，尝试按逗号分割
        tags = selectedTemplateData.tags ? selectedTemplateData.tags.split(',').map(t => t.trim()) : [];
      }

      const templateData = {
        id: selectedTemplateData.promptTemplateKey,
        name: selectedTemplateData.promptTemplateKey,
        title: selectedTemplateData.templateDescription || selectedTemplateData.promptTemplateKey,
        content: selectedTemplateData.template || '',
        description: selectedTemplateData.templateDescription || '',
        parameters: variables,
        tags: tags
      };
      
      onImport(templateData);
    }
  };

  // 解析标签
  const parseTags = (tagsString) => {
    if (!tagsString) return [];
    try {
      return JSON.parse(tagsString);
    } catch (error) {
      return tagsString.split(',').map(t => t.trim()).filter(t => t);
    }
  };

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 40,
            height: 40,
            backgroundColor: '#e6f7ff',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <DownloadOutlined style={{ color: '#1890ff', fontSize: 20 }} />
          </div>
          <div>
            <Title level={3} style={{ margin: 0 }}>从模板导入</Title>
            <Text type="secondary">选择一个预设模板快速开始创建Prompt</Text>
          </div>
        </div>
      }
      open={true}
      onCancel={onClose}
      width={1200}
      centered
      style={{
        maxHeight: 'calc(100vh - 40px)'
      }}
      bodyStyle={{
        maxHeight: 'calc(100vh - 200px)',
        overflowY: 'auto'
      }}
      footer={[
        <Button key="cancel" onClick={onClose}>
          取消
        </Button>,
        <Button
          key="import"
          type="primary"
          disabled={!selectedTemplate}
          onClick={handleImport}
          icon={<DownloadOutlined />}
        >
          导入模板
        </Button>
      ]}
      closeIcon={<CloseOutlined />}
    >
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        {/* 搜索栏 */}
        <div>
          <Search
            placeholder="搜索模板名称或关键词..."
            allowClear
            onSearch={handleSearch}
            style={{ width: 300 }}
            enterButton={<SearchOutlined />}
          />
        </div>

        <Row gutter={24}>
          {/* 左侧：模板列表 */}
          <Col span={16}>
            <Card 
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <FolderOutlined />
                  <span>选择模板</span>
                  <Text type="secondary">({pagination.total} 个模板)</Text>
                </div>
              }
              size="small"
            >
              <Spin spinning={loading}>
                {templates.length > 0 ? (
                  <div>
                    <Row gutter={[16, 16]}>
                      {templates.map(template => {
                        const tags = parseTags(template.tags);
                        return (
                          <Col span={12} key={template.promptTemplateKey}>
                            <Card
                              size="small"
                              hoverable
                              onClick={() => setSelectedTemplate(template.promptTemplateKey)}
                              className={selectedTemplate === template.promptTemplateKey ? 'ant-card-selected' : ''}
                              style={{
                                borderColor: selectedTemplate === template.promptTemplateKey ? '#1890ff' : '#d9d9d9',
                                backgroundColor: selectedTemplate === template.promptTemplateKey ? '#f0f8ff' : '#fff'
                              }}
                              bodyStyle={{ padding: 16 }}
                            >
                              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                                <Text strong style={{ fontSize: 14, color: '#262626' }}>
                                  {template.templateDescription || template.promptTemplateKey}
                                </Text>
                                {selectedTemplate === template.promptTemplateKey && (
                                  <CheckCircleOutlined style={{ color: '#1890ff', fontSize: 16 }} />
                                )}
                              </div>
                              
                              <Paragraph 
                                style={{ 
                                  fontSize: 12, 
                                  color: '#8c8c8c', 
                                  margin: '0 0 8px 0',
                                  minHeight: 32
                                }}
                                ellipsis={{ rows: 2 }}
                              >
                                {template.templateDescription || '暂无描述'}
                              </Paragraph>
                              
                              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                                {tags.slice(0, 3).map((tag, index) => (
                                  <Tag key={index} size="small" color="blue">
                                    {tag}
                                  </Tag>
                                ))}
                                {tags.length > 3 && (
                                  <Text type="secondary" style={{ fontSize: 12 }}>+{tags.length - 3}</Text>
                                )}
                              </div>
                            </Card>
                          </Col>
                        );
                      })}
                    </Row>
                    
                    {/* 分页 */}
                    {pagination.total > pagination.pageSize && (
                      <div style={{ textAlign: 'center', marginTop: 24 }}>
                        <Pagination
                          current={pagination.current}
                          total={pagination.total}
                          pageSize={pagination.pageSize}
                          showSizeChanger
                          showQuickJumper
                          showTotal={(total, range) => `第 ${range[0]}-${range[1]} 项，共 ${total} 个模板`}
                          onChange={handlePageChange}
                        />
                      </div>
                    )}
                  </div>
                ) : (
                  <Empty 
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description="暂无模板数据"
                  />
                )}
              </Spin>
            </Card>
          </Col>

          {/* 右侧：模板预览 */}
          <Col span={8}>
            <div style={{ position: 'sticky', top: 0 }}>
              <Card 
                title={
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <InfoCircleOutlined />
                    <span>模板预览</span>
                  </div>
                }
                size="small"
              >
                <Spin spinning={templateDetailLoading}>
                  {selectedTemplateData ? (
                    <Space direction="vertical" size={16} style={{ width: '100%' }}>
                      <div>
                        <Text strong style={{ display: 'block', marginBottom: 4 }}>模板名称</Text>
                        <Text>{selectedTemplateData.templateDescription || selectedTemplateData.promptTemplateKey}</Text>
                      </div>

                      <div>
                        <Text strong style={{ display: 'block', marginBottom: 4 }}>Prompt Key</Text>
                        <Text code style={{ fontSize: 12 }}>{selectedTemplateData.promptTemplateKey}</Text>
                      </div>

                      {selectedTemplateData.variables && (() => {
                        try {
                          const variables = JSON.parse(selectedTemplateData.variables);
                          const paramNames = Object.keys(variables);
                          return paramNames.length > 0 ? (
                            <div>
                              <Text strong style={{ display: 'block', marginBottom: 8 }}>参数列表</Text>
                              <Space wrap>
                                {paramNames.map((param, index) => (
                                  <Tag key={index} color="blue">{param}</Tag>
                                ))}
                              </Space>
                            </div>
                          ) : null;
                        } catch (error) {
                          return null;
                        }
                      })()}

                      {selectedTemplateData.template && (
                        <div>
                          <Text strong style={{ display: 'block', marginBottom: 8 }}>内容预览</Text>
                          <div style={{
                            backgroundColor: '#fafafa',
                            padding: 12,
                            borderRadius: 6,
                            fontSize: 12,
                            fontFamily: 'monospace',
                            maxHeight: 200,
                            overflowY: 'auto',
                            border: '1px solid #d9d9d9',
                            whiteSpace: 'pre-wrap'
                          }}>
                            {selectedTemplateData.template}
                          </div>
                        </div>
                      )}

                      {selectedTemplateData.tags && (() => {
                        const tags = parseTags(selectedTemplateData.tags);
                        return tags.length > 0 ? (
                          <div>
                            <Text strong style={{ display: 'block', marginBottom: 8 }}>标签</Text>
                            <Space wrap>
                              {tags.map((tag, index) => (
                                <Tag key={index} color="geekblue">{tag}</Tag>
                              ))}
                            </Space>
                          </div>
                        ) : null;
                      })()}
                    </Space>
                  ) : (
                    <div style={{ textAlign: 'center', padding: '40px 0' }}>
                      <FileTextOutlined style={{ fontSize: 48, color: '#d9d9d9', marginBottom: 16 }} />
                      <br />
                      <Text type="secondary">点击左侧模板查看详情</Text>
                    </div>
                  )}
                </Spin>
              </Card>
            </div>
          </Col>
        </Row>

        {/* 底部提示 */}
        {selectedTemplateData && (
          <Alert
            message={
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <InfoCircleOutlined style={{ color: '#1890ff' }} />
                <span>
                  导入后将替换当前Prompt内容
                  {(() => {
                    try {
                      if (selectedTemplateData.variables) {
                        const variables = JSON.parse(selectedTemplateData.variables);
                        const paramCount = Object.keys(variables).length;
                        return paramCount > 0 ? `，包含 ${paramCount} 个参数` : '';
                      }
                    } catch (error) {
                      return '';
                    }
                    return '';
                  })()} 
                </span>
              </div>
            }
            type="info"
            showIcon={false}
            style={{ backgroundColor: '#f0f8ff', border: '1px solid #d4edda' }}
          />
        )}
      </Space>
    </Modal>
  );
};

export default TemplateImportModal;
