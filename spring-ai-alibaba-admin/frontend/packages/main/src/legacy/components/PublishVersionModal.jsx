import React, { useState } from 'react';
import {
  Modal,
  Card,
  Typography,
  Input,
  Select,
  Button,
  Alert,
  Tag,
  Space,
  Row,
  Col,
  Divider,
  Spin
} from 'antd';
import {
  CloseOutlined,
  ExclamationCircleOutlined,
  RocketOutlined,
  ExperimentOutlined,
  RobotOutlined,
  GoldOutlined,
  FireOutlined,
  AppstoreOutlined,
  InfoCircleOutlined
} from '@ant-design/icons';
import { handleApiError, handleValidationError, notifySuccess } from '../utils/notification';
import PublishSuccessModal from './PublishSuccessModal';
import API from '../services';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;

const PublishVersionModal = ({ prompt, newContent, modelConfig, models = [], onClose, onSuccess, variables }) => {
  // Helper function to get model information by ID
  const getModelById = (modelId) => {
    return models.find(m => m.id === modelId) || null;
  };

  // Helper function to get model name by ID
  const getModelName = (modelId) => {
    const model = getModelById(modelId);
    return model ? model.name : modelId || '-';
  };

  // Helper function to get display parameters (filtering out model identifiers)
  const getDisplayModelParams = (config) => {
    if (!config || typeof config !== 'object') return {};

    // Filter out model identifier fields
    const { model, modelId, ...filteredParams } = config;
    return filteredParams;
  };


  // 计算新版本号的函数
  const calculateNextVersion = (currentVersion) => {
    if (!currentVersion) return '1.0.0';

    // 尝试解析版本号
    let versionStr = String(currentVersion).trim();

    // 处理版本号前缀 (如 v1.5.0)
    if (versionStr.toLowerCase().startsWith('v')) {
      versionStr = versionStr.substring(1);
    }

    // 处理常见的版本号格式
    if (versionStr.includes('.')) {
      const parts = versionStr.split('.');

      if (parts.length >= 3) {
        // 三位版本号格式 (如 1.5.0, 2.1.3)
        const major = parseInt(parts[0]) || 0;
        const minor = parseInt(parts[1]) || 0;
        const patch = parseInt(parts[2]) || 0;
        return `${major}.${minor}.${patch + 1}`;
      } else if (parts.length === 2) {
        // 两位版本号格式 (如 1.5, 2.1) - 转换为三位并递增补丁号
        const major = parseInt(parts[0]) || 0;
        const minor = parseInt(parts[1]) || 0;
        return `${major}.${minor}.1`;
      } else if (parts.length === 1) {
        // 单版本号格式，但有小数点 (如 "1.")
        const major = parseInt(parts[0]) || 0;
        return `${major}.0.1`;
      }
    }

    // 如果是纯数字，当作主版本号处理，默认递增补丁版本
    const num = parseInt(versionStr);
    if (!isNaN(num)) {
      return `${num}.0.1`;
    }

    // 默认情况
    return '0.0.1';
  };

  const [formData, setFormData] = useState({
    version: calculateNextVersion(prompt.latestVersion),
    description: '',
    status: 'release' // 默认Publish Release
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  // 从传入的内容中提取参数
  const parameters = Object.entries(variables).map(([key, value]) => ({ key, value }));

  const handleSubmit = async () => {
    if (!formData.version.trim()) {
      handleValidationError('Enter a version number');
      return;
    }

    if (!newContent || !newContent.trim()) {
      handleValidationError('Enter Prompt content in the editor.');
      return;
    }

    setLoading(true);
    setError(null);

    try {

      // 调用Publish version API
      const response = await API.publishPromptVersion({
        promptKey: prompt.promptKey,
        version: formData.version,
        versionDescription: formData.description,
        template: newContent,
        variables: JSON.stringify(variables),
        modelConfig: JSON.stringify(modelConfig || {}),
        status: formData.status
      });

      if (response.code === 200) {
        notifySuccess({
          message: 'Version published successfully',
          description: `Successfully published ${formData.status === 'release' ? 'release ' : 'PRE'}version ${formData.version}`
        });
        setShowSuccessModal(true);
      } else {
        throw new Error(response.message || 'Publishing failed');
      }
    } catch (err) {
      console.error('Publishing version failed:', err);
      handleApiError(err, 'Publish version');
      setError(err.message || 'Publishing failed. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const handleSuccessClose = () => {
    setShowSuccessModal(false);
    if (onSuccess) {
      onSuccess();
    } else {
      onClose();
    }
  };

  return (
    <>
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <RocketOutlined />
            <span>Publish New Version</span>
          </div>
        }
        open={true}
        onCancel={onClose}
        width={800}
        footer={null}
        destroyOnHidden
        style={{ top: 20 }}
        styles={{
          body: {
            maxHeight: 'calc(100vh - 200px)',
            overflowY: 'auto',
            padding: 0
          }
        }}
      >
        {error && (
          <Alert
            message="Publishing failed"
            description={error}
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        <div style={{ padding: 24, paddingBottom: 0 }}>
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            {/* Current Prompt 信息 */}
            <Card size="small">
              <Row gutter={[16, 16]}>
                <Col span={12}>
                  <div>
                    <Text type="secondary" style={{ fontSize: '12px', textTransform: 'uppercase' }}>Current Prompt</Text>
                    <div style={{ marginTop: 4 }}>
                      <Text strong>{prompt.promptKey}</Text>
                    </div>
                  </div>
                </Col>
                <Col span={12}>
                  <div>
                    <Text type="secondary" style={{ fontSize: '12px', textTransform: 'uppercase' }}>Current Version</Text>
                    <div style={{ marginTop: 4 }}>
                      {prompt.latestVersion ? (
                        <Tag color="blue">{prompt.latestVersion}</Tag>
                      ) : (
                        <Tag color="default">No Version</Tag>
                      )}
                    </div>
                  </div>
                </Col>
              </Row>
            </Card>

            {/* Version Settings */}
            <Card title="Version Settings" size="small">
              <Row gutter={[16, 16]}>
                <Col span={12}>
                  <div>
                    <Text strong style={{ marginBottom: 8, display: 'block' }}>New Version Number *</Text>
                    <Input
                      value={formData.version}
                      onChange={(e) => setFormData(prev => ({ ...prev, version: e.target.value }))}
                      placeholder="1.0.0"
                    />
                  </div>
                </Col>
                <Col span={12}>
                  <div>
                    <Text strong style={{ marginBottom: 8, display: 'block' }}>Version Type *</Text>
                    <Select
                      value={formData.status}
                      onChange={(value) => setFormData(prev => ({ ...prev, status: value }))}
                      style={{ width: '100%' }}
                    >
                      <Option value="release">Release</Option>
                      <Option value="pre">Pre-release</Option>
                    </Select>
                  </div>
                </Col>
              </Row>
            </Card>

            {/* 内容预览 */}
            <Card title="Version Content Preview" size="small">
              {newContent && newContent.trim() ? (
                <div style={{
                  padding: 12,
                  backgroundColor: '#f5f5f5',
                  borderRadius: 6,
                  fontFamily: 'monospace',
                  fontSize: '12px',
                  maxHeight: 150,
                  overflowY: 'auto',
                  whiteSpace: 'pre-wrap',
                  border: '1px solid #d9d9d9'
                }}>
                  {newContent}
                </div>
              ) : (
                <Alert
                  message="Enter Prompt content in the editor."
                  type="warning"
                  showIcon
                  icon={<ExclamationCircleOutlined />}
                />
              )}
            </Card>

            {/* 参数预览 */}
            {parameters.length > 0 && (
              <Card title="Detected Parameters: Key-Value Pairs" size="small">
                <Space size={[8, 8]} wrap>
                  {parameters.map((param, index) => (
                    <Tag key={index} color="blue">
                      {param.key}{param.value ? `:  ${param.value}` : ''}
                    </Tag>
                  ))}
                </Space>
              </Card>
            )}

            {/* Model Configuration预览 */}
            {modelConfig && (
              <Card title="Model Configuration" size="small">
                <Row gutter={[16, 8]}>
                  {/* 显示模型名称而非ID */}
                  <Col span={24} style={{ marginBottom: 8 }}>
                    <Space>
                      <Text strong>Model: </Text>
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
                            No model parameters configured
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

            {/* Version Type Guide */}
            <Alert
              message="Version Type Guide"
              description={
                <div style={{ marginTop: 8 }}>
                  <div style={{ marginBottom: 4 }}>
                    <Text strong>Release：</Text>
                    <Text style={{ marginLeft: 8 }}>A stable production version that updates the current version pointer.</Text>
                  </div>
                  <div>
                    <Text strong>Pre-release：</Text>
                    <Text style={{ marginLeft: 8 }}>A pre-release version for testing and validation.</Text>
                  </div>
                </div>
              }
              type="info"
              showIcon
              icon={<InfoCircleOutlined />}
            />

            {/* Version Description */}
            <Card title="Version Description" size="small">
              <TextArea
                value={formData.description}
                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                placeholder="Describe the changes in this version..."
                rows={3}
              />
            </Card>
          </Space>
        </div>

        {/* 底部按钮 */}
        <div style={{
          padding: 24,
          paddingTop: 16,
          borderTop: '1px solid #f0f0f0',
          textAlign: 'right',
          marginTop: 16,
          backgroundColor: '#fff',
          position: 'sticky',
          bottom: 0
        }}>
          <Space>
            <Button onClick={onClose}>
              Cancel
            </Button>
            <Button
              type="primary"
              icon={loading ? <Spin size="small" /> : (formData.status === 'release' ? <RocketOutlined /> : <ExperimentOutlined />)}
              onClick={handleSubmit}
              disabled={loading || !newContent || !newContent.trim() || !formData.version.trim()}
              style={{
                backgroundColor: formData.status === 'release' ? '#52c41a' : '#fa8c16',
                borderColor: formData.status === 'release' ? '#52c41a' : '#fa8c16'
              }}
            >
              {loading
                ? 'Publishing...'
                : `Publish ${formData.status === 'release' ? 'release ' : 'PRE'}版本`
              }
            </Button>
          </Space>
        </div>
      </Modal>

      {/* Publish 成功模态框 */}
      {showSuccessModal && (
        <PublishSuccessModal
          prompt={{
            ...prompt,
            latestVersionStatus: formData.status
          }}
          version={formData.version}
          onClose={handleSuccessClose}
        />
      )}
    </>
  );
};

export default PublishVersionModal;
