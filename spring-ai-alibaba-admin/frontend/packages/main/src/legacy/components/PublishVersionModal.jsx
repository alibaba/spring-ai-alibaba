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
    status: 'release' // 默认发布正式版本
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  // 从传入的内容中提取参数
  const parameters = Object.entries(variables).map(([key, value]) => ({ key, value }));

  const handleSubmit = async () => {
    if (!formData.version.trim()) {
      handleValidationError('请填写版本号');
      return;
    }

    if (!newContent || !newContent.trim()) {
      handleValidationError('请在编辑区填写 Prompt 内容');
      return;
    }

    setLoading(true);
    setError(null);

    try {

      // 调用发布版本 API
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
          message: '版本发布成功',
          description: `已成功发布${formData.status === 'release' ? '正式' : 'PRE'}版本 ${formData.version}`
        });
        setShowSuccessModal(true);
      } else {
        throw new Error(response.message || '发布失败');
      }
    } catch (err) {
      console.error('发布版本失败:', err);
      handleApiError(err, '发布版本');
      setError(err.message || '发布失败，请稍后重试');
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
            <span>发布新版本</span>
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
            message="发布失败"
            description={error}
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        <div style={{ padding: 24, paddingBottom: 0 }}>
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            {/* 当前 Prompt 信息 */}
            <Card size="small">
              <Row gutter={[16, 16]}>
                <Col span={12}>
                  <div>
                    <Text type="secondary" style={{ fontSize: '12px', textTransform: 'uppercase' }}>当前 Prompt</Text>
                    <div style={{ marginTop: 4 }}>
                      <Text strong>{prompt.promptKey}</Text>
                    </div>
                  </div>
                </Col>
                <Col span={12}>
                  <div>
                    <Text type="secondary" style={{ fontSize: '12px', textTransform: 'uppercase' }}>当前版本</Text>
                    <div style={{ marginTop: 4 }}>
                      {prompt.latestVersion ? (
                        <Tag color="blue">{prompt.latestVersion}</Tag>
                      ) : (
                        <Tag color="default">暂无版本</Tag>
                      )}
                    </div>
                  </div>
                </Col>
              </Row>
            </Card>

            {/* 版本配置 */}
            <Card title="版本配置" size="small">
              <Row gutter={[16, 16]}>
                <Col span={12}>
                  <div>
                    <Text strong style={{ marginBottom: 8, display: 'block' }}>新版本号 *</Text>
                    <Input
                      value={formData.version}
                      onChange={(e) => setFormData(prev => ({ ...prev, version: e.target.value }))}
                      placeholder="1.0.0"
                    />
                  </div>
                </Col>
                <Col span={12}>
                  <div>
                    <Text strong style={{ marginBottom: 8, display: 'block' }}>版本类型 *</Text>
                    <Select
                      value={formData.status}
                      onChange={(value) => setFormData(prev => ({ ...prev, status: value }))}
                      style={{ width: '100%' }}
                    >
                      <Option value="release">正式版本</Option>
                      <Option value="pre">PRE版本</Option>
                    </Select>
                  </div>
                </Col>
              </Row>
            </Card>

            {/* 内容预览 */}
            <Card title="版本内容预览" size="small">
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
                  message="请在编辑区填写Prompt内容"
                  type="warning"
                  showIcon
                  icon={<ExclamationCircleOutlined />}
                />
              )}
            </Card>

            {/* 参数预览 */}
            {parameters.length > 0 && (
              <Card title="检测到的参数: 键值对" size="small">
                <Space size={[8, 8]} wrap>
                  {parameters.map((param, index) => (
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

            {/* 版本类型说明 */}
            <Alert
              message="版本类型说明"
              description={
                <div style={{ marginTop: 8 }}>
                  <div style={{ marginBottom: 4 }}>
                    <Text strong>正式版本：</Text>
                    <Text style={{ marginLeft: 8 }}>稳定的生产环境版本，会更新当前版本指针</Text>
                  </div>
                  <div>
                    <Text strong>PRE版本：</Text>
                    <Text style={{ marginLeft: 8 }}>预发布版本，用于测试和验证</Text>
                  </div>
                </div>
              }
              type="info"
              showIcon
              icon={<InfoCircleOutlined />}
            />

            {/* 版本说明 */}
            <Card title="版本说明" size="small">
              <TextArea
                value={formData.description}
                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                placeholder="描述此版本的变更内容..."
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
              取消
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
                ? '发布中...'
                : `发布${formData.status === 'release' ? '正式' : 'PRE'}版本`
              }
            </Button>
          </Space>
        </div>
      </Modal>

      {/* 发布成功模态框 */}
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
