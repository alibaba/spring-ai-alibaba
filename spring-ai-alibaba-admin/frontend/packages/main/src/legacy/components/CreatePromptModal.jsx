import React, { useMemo, useState } from 'react';
import {
  Modal,
  Card,
  Typography,
  Button,
  Input,
  Select,
  Alert,
  Space,
  Row,
  Col,
  Tag,
  Divider,
  Spin,
  message
} from 'antd';
import {
  CloseOutlined,
  RocketOutlined,
  ExclamationCircleOutlined,
  PlusOutlined,
  ExperimentOutlined,
  RobotOutlined,
  GoldOutlined,
  FireOutlined,
  AppstoreOutlined
} from '@ant-design/icons';
import API from '../services';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;
const { Option } = Select;

const CreatePromptModal = (props) => {
  const {
    onClose, onSuccess, initialData = {}, quickCreate = false, modelConfig = null,
    models = [],
  } = props;

  const { variablesWithValues = {} } = initialData;

  const getModelById = (modelId) => {
    return models.find(m => m.id === modelId) || null;
  };

  // Helper function to get model name by ID
  const getModelName = (modelId) => {
    const model = getModelById(modelId);
    return model ? model.name : modelId || '-';
  };

  const variablesWithValueList = useMemo(() => {
    return Object.entries(variablesWithValues).map(([key, value]) => ({
      key, value
    }));
  }, [variablesWithValues]);

  const [formData, setFormData] = useState({
    promptKey: '',
    tags: '',
    promptDescription: ''
  });

  // 快速创建模式下的版本信息
  const [versionData, setVersionData] = useState({
    version: '0.0.1',
    versionDescription: '初始版本',
    status: 'release' // release 或 pre
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // 提取参数的辅助函数
  const extractParameters = (content) => {
    const regex = /\{\{(\w+)\}\}/g;
    const parameters = [];
    let match;
    while ((match = regex.exec(content)) !== null) {
      if (!parameters.includes(match[1])) {
        parameters.push(match[1]);
      }
    }
    return parameters;
  };

  const handleSubmit = async () => {
    if (!formData.promptKey.trim()) {
      message.error('请填写 Prompt Key');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      // 准备标签数据
      const tagsArray = formData.tags.split(',').map(tag => tag.trim()).filter(tag => tag);
      const tagsString = JSON.stringify(tagsArray);

      // 创建 Prompt
      const createParams = {
        promptKey: formData.promptKey.trim(),
        promptDescription: formData.promptDescription.trim(),
        tags: tagsString
      };

      const createResponse = await API.publishPrompt(createParams);

      if (createResponse.code !== 200) {
        throw new Error(createResponse.message || '创建失败');
      }

      // 如果是快速创建且有内容，同时创建版本
      if (quickCreate && initialData.content && initialData.content.trim()) {
        if (!versionData.version.trim()) {
          message.error('请填写版本号');
          setLoading(false);
          return;
        }

        const versionParams = {
          promptKey: formData.promptKey.trim(),
          version: versionData.version,
          versionDescription: versionData.versionDescription,
          template: initialData.content,
          variables: JSON.stringify(variablesWithValues),
          modelConfig: JSON.stringify(modelConfig || {}),
          status: versionData.status
        };

        const versionResponse = await API.publishPromptVersion(versionParams);

        if (versionResponse.code !== 200) {
          throw new Error(versionResponse.message || '版本创建失败');
        }
      }

      // 成功完成
      message.success({
        content: quickCreate ? 'Prompt 创建和版本发布成功' : 'Prompt 创建成功',
        description: quickCreate
          ? `已创建 Prompt "${formData.promptKey}" 并发布版本 ${versionData.version}`
          : `已创建 Prompt "${formData.promptKey}"`
      });

      if (onSuccess) {
        onSuccess();
      } else {
        onClose();
      }
    } catch (err) {
      console.error('创建失败:', err);
      message.error(err.message || '创建失败，请稍后重试');
      setError(err.message || '创建失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

    // Helper function to get display parameters (filtering out model identifiers)
    const getDisplayModelParams = (config) => {
      if (!config || typeof config !== 'object') return {};
  
      // Filter out model identifier fields
      const { model, modelId, ...filteredParams } = config;
      return filteredParams;
    };

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 40,
            height: 40,
            backgroundColor: quickCreate ? '#e6f7ff' : '#f6ffed',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            {quickCreate ? (
              <RocketOutlined style={{ color: '#1890ff', fontSize: 20 }} />
            ) : (
              <PlusOutlined style={{ color: '#52c41a', fontSize: 20 }} />
            )}
          </div>
          <Title level={3} style={{ margin: 0 }}>
            {quickCreate ? '快速创建新Prompt' : '创建新Prompt'}
          </Title>
        </div>
      }
      open={true}
      onCancel={onClose}
      width={800}
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
          key="submit"
          type="primary"
          loading={loading}
          disabled={!formData.promptKey.trim() || (quickCreate && (!initialData.content || !initialData.content.trim() || !versionData.version.trim()))}
          onClick={handleSubmit}
          icon={quickCreate && versionData.status === 'release' ? <RocketOutlined /> :
            quickCreate && versionData.status === 'pre' ? <ExperimentOutlined /> : <PlusOutlined />}
          style={{
            backgroundColor: quickCreate && versionData.status === 'release' ? '#52c41a' :
              quickCreate && versionData.status === 'pre' ? '#fa8c16' : undefined
          }}
        >
          {loading
            ? '创建中...'
            : quickCreate
              ? `快速创建并发布${versionData.status === 'release' ? '正式' : 'PRE'}版本`
              : '创建 Prompt'
          }
        </Button>
      ]}
      closeIcon={<CloseOutlined />}
    >
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        {quickCreate && (
          <Alert
            message="快速创建模式"
            description="将同时创建新Prompt并发布第一个版本"
            type="info"
            icon={<RocketOutlined />}
            showIcon
          />
        )}

        {error && (
          <Alert
            message="创建失败"
            description={error}
            type="error"
            icon={<ExclamationCircleOutlined />}
            showIcon
          />
        )}

        <Card title="基本信息" size="small">
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8 }}>Prompt Key <span className='text-red-700'>*</span></Text>
              <Input
                value={formData.promptKey}
                onChange={(e) => {
                  const value = e.target.value;
                  const validValue = value.replace(/[^a-zA-Z0-9_-]/g, '');
                  setFormData(prev => ({ ...prev, promptKey: validValue }));
                }}
                placeholder="输入Prompt Key（仅支持英文、数字、下划线、横杠）..."
                size="large"
              />
              <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginTop: 4 }}>
                仅支持英文字母、数字、下划线（_）和横杠（-）
              </Text>
            </div>

            <div>
              <Text strong style={{ display: 'block', marginBottom: 8 }}>标签</Text>
              <Input
                value={formData.tags}
                onChange={(e) => setFormData(prev => ({ ...prev, tags: e.target.value }))}
                placeholder="多个标签用逗号分隔，例如：营销，文案，创意"
                size="large"
              />
            </div>

            <div>
              <Text strong style={{ display: 'block', marginBottom: 8 }}>描述</Text>
              <TextArea
                value={formData.promptDescription}
                onChange={(e) => setFormData(prev => ({ ...prev, promptDescription: e.target.value }))}
                placeholder="描述这个Prompt的用途和特点..."
                rows={3}
                size="large"
              />
            </div>
          </Space>
        </Card>

        {quickCreate && (
          <Card title="版本信息" size="small">
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Row gutter={16}>
                <Col span={8}>
                  <Text strong style={{ display: 'block', marginBottom: 8 }}>版本号 *</Text>
                  <Input
                    value={versionData.version}
                    onChange={(e) => setVersionData(prev => ({ ...prev, version: e.target.value }))}
                    placeholder="1.0"
                    size="large"
                  />
                </Col>

                <Col span={8}>
                  <Text strong style={{ display: 'block', marginBottom: 8 }}>版本类型 *</Text>
                  <Select
                    value={versionData.status}
                    onChange={(value) => setVersionData(prev => ({ ...prev, status: value }))}
                    style={{ width: '100%' }}
                    size="large"
                  >
                    <Option value="release">正式版本</Option>
                    <Option value="pre">PRE版本</Option>
                  </Select>
                </Col>

                <Col span={8}>
                  <Text strong style={{ display: 'block', marginBottom: 8 }}>版本说明</Text>
                  <Input
                    value={versionData.versionDescription}
                    onChange={(e) => setVersionData(prev => ({ ...prev, versionDescription: e.target.value }))}
                    placeholder="初始版本"
                    size="large"
                  />
                </Col>
              </Row>

              <div>
                <Text strong style={{ display: 'block', marginBottom: 8 }}>版本内容预览</Text>
                {initialData.content && initialData.content.trim() ? (
                  <div style={{
                    padding: 16,
                    backgroundColor: '#fafafa',
                    borderRadius: 6,
                    fontFamily: 'monospace',
                    fontSize: '13px',
                    maxHeight: 128,
                    overflowY: 'auto',
                    border: '1px solid #d9d9d9',
                    whiteSpace: 'pre-wrap'
                  }}>
                    {initialData.content}
                  </div>
                ) : (
                  <Alert
                    message="请在编辑区填写Prompt内容"
                    type="warning"
                    icon={<ExclamationCircleOutlined />}
                    showIcon
                  />
                )}
              </div>

              {/* 参数预览 */}
              {variablesWithValueList.length > 0 && (
                <Card title="检测到的参数: 键值对" size="small">
                  <Space size={[8, 8]} wrap>
                    {variablesWithValueList.map((param, index) => (
                      <Tag key={index} color="blue">
                        {param.key}{param.value ? `:  ${param.value}` : ''}
                      </Tag>
                    ))}
                  </Space>
                </Card>
              )}

              {/* 模型配置预览 */}
              {modelConfig && (
                <Card title="模型配置" size="small">
                  <Row gutter={[16, 8]}>
                    {/* 显示模型名称而非ID */}
                    <Col span={24} style={{ marginBottom: 8 }}>
                      <Space>
                        <Text strong>模型：</Text>
                        <Text code>{getModelName(modelConfig.modelId)}</Text>
                      </Space>
                    </Col>

                    {/* 动态显示模型参数 */}
                    {(() => {
                      const displayParams = getDisplayModelParams(modelConfig);
                      const paramEntries = Object.entries(displayParams);

                      if (paramEntries.length === 0) {
                        return (
                          <Col span={24}>
                            <Text type="secondary" style={{ fontStyle: 'italic' }}>
                              暂无模型参数配置
                            </Text>
                          </Col>
                        );
                      }

                      return paramEntries.map(([key, value], index) => {
                        return (
                          <Col span={12} key={key}>
                            <Space>
                              <Text strong>{key}：</Text>
                              <Text code>{value}</Text>
                            </Space>
                          </Col>
                        );
                      });
                    })()
                    }
                  </Row>
                </Card>
              )}

              <Alert
                message={versionData.status === 'release' ? '正式版本说明' : 'PRE版本说明'}
                description={
                  <div>
                    {versionData.status === 'release' ? (
                      <div>
                        <Paragraph style={{ margin: 0, marginBottom: 4 }}>
                          <Text strong>正式版本：</Text>稳定的生产环境版本，会更新当前版本指针
                        </Paragraph>
                        <Text>适用于生产环境使用，经过充分测试和验证</Text>
                      </div>
                    ) : (
                      <div>
                        <Paragraph style={{ margin: 0, marginBottom: 4 }}>
                          <Text strong>PRE版本：</Text>预发布版本，用于测试和验证
                        </Paragraph>
                        <Text>适用于测试环境，不会更新当前版本指针</Text>
                      </div>
                    )}
                  </div>
                }
                type={versionData.status === 'release' ? 'success' : 'warning'}
                icon={versionData.status === 'release' ? <RocketOutlined /> : <ExperimentOutlined />}
                showIcon
              />
            </Space>
          </Card>
        )}
      </Space>
    </Modal>
  );
};

export default CreatePromptModal;
