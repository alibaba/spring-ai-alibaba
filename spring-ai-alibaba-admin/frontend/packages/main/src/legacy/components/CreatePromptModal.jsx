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
import $i18n from '@/i18n';

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
    versionDescription: $i18n.get({ id: 'legacy.prompts.initial.version', dm: '初始版本' }),
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
      message.error($i18n.get({ id: 'legacy.prompts.please.enter.a.prompt.key', dm: '请填写 Prompt Key' }));
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
        throw new Error(createResponse.message || $i18n.get({ id: 'legacy.prompts.failed.to.create.prompt', dm: '创建失败' }));
      }

      // 如果是快速创建且有内容，同时创建版本
      if (quickCreate && initialData.content && initialData.content.trim()) {
        if (!versionData.version.trim()) {
          message.error($i18n.get({ id: 'legacy.prompts.please.enter.a.version.number', dm: '请填写版本号' }));
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
          throw new Error(versionResponse.message || $i18n.get({ id: 'legacy.prompts.failed.to.create.version', dm: '版本创建失败' }));
        }
      }

      // 成功完成
      message.success({
        content: quickCreate ? $i18n.get({ id: 'legacy.prompts.prompt.created.and.version.published.successfully', dm: 'Prompt 创建和版本发布成功' }) : $i18n.get({ id: 'legacy.prompts.prompt.created.successfully', dm: 'Prompt 创建成功' }),
        description: quickCreate
          ? $i18n.get({ id: 'legacy.prompts.created.prompt.and.published.version', dm: '已创建 Prompt "{promptKey}" 并发布版本 {version}' }, { promptKey: formData.promptKey, version: versionData.version })
          : $i18n.get({ id: 'legacy.prompts.created.prompt', dm: '已创建 Prompt "{promptKey}"' }, { promptKey: formData.promptKey })
      });

      if (onSuccess) {
        onSuccess();
      } else {
        onClose();
      }
    } catch (err) {
      console.error('创建失败:', err);
      message.error(err.message || $i18n.get({ id: 'legacy.prompts.failed.to.create.prompt.please.try.again.later', dm: '创建失败，请稍后重试' }));
      setError(err.message || $i18n.get({ id: 'legacy.prompts.failed.to.create.prompt.please.try.again.later', dm: '创建失败，请稍后重试' }));
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
            {quickCreate ? $i18n.get({ id: 'legacy.prompts.quickly.create.new.prompt', dm: '快速创建新Prompt' }) : $i18n.get({ id: 'legacy.prompts.create.new.prompt', dm: '创建新Prompt' })}
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
          {$i18n.get({ id: 'legacy.prompts.cancel', dm: '取消' })}
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
            ? $i18n.get({ id: 'legacy.prompts.creating', dm: '创建中...' })
            : quickCreate
              ? $i18n.get({ id: 'legacy.prompts.create.and.publish.version', dm: '快速创建并发布{statusType}版本' }, { statusType: versionData.status === 'release' ? $i18n.get({ id: 'legacy.prompts.release.short', dm: '正式' }) : $i18n.get({ id: 'legacy.prompts.pre.short', dm: 'PRE' }) })
              : $i18n.get({ id: 'legacy.prompts.create.prompt', dm: '创建 Prompt' })
          }
        </Button>
      ]}
      closeIcon={<CloseOutlined />}
    >
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        {quickCreate && (
          <Alert
            message={$i18n.get({ id: 'legacy.prompts.quick.create.mode', dm: '快速创建模式' })}
            description={$i18n.get({ id: 'legacy.prompts.this.creates.a.new.prompt.and.publishes.its.first.version', dm: '将同时创建新Prompt并发布第一个版本' })}
            type="info"
            icon={<RocketOutlined />}
            showIcon
          />
        )}

        {error && (
          <Alert
            message={$i18n.get({ id: 'legacy.prompts.creation.failed', dm: '创建失败' })}
            description={error}
            type="error"
            icon={<ExclamationCircleOutlined />}
            showIcon
          />
        )}

        <Card title={$i18n.get({ id: 'legacy.prompts.basic.information', dm: '基本信息' })} size="small">
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
                placeholder={$i18n.get({ id: 'legacy.prompts.enter.a.prompt.key.letters.numbers.underscores.and.hyphens.o', dm: '输入Prompt Key（仅支持英文、数字、下划线、横杠）...' })}
                size="large"
              />
              <Text type="secondary" style={{ fontSize: '12px', display: 'block', marginTop: 4 }}>
                {$i18n.get({ id: 'legacy.prompts.only.letters.numbers.underscores.and.hyphens.are.supported', dm: '仅支持英文字母、数字、下划线（_）和横杠（-）' })}
              </Text>
            </div>

            <div>
              <Text strong style={{ display: 'block', marginBottom: 8 }}>{$i18n.get({ id: 'legacy.prompts.tags', dm: '标签' })}</Text>
              <Input
                value={formData.tags}
                onChange={(e) => setFormData(prev => ({ ...prev, tags: e.target.value }))}
                placeholder={$i18n.get({ id: 'legacy.prompts.separate.multiple.tags.with.commas.e.g.marketing.copywriting', dm: '多个标签用逗号分隔，例如：营销，文案，创意' })}
                size="large"
              />
            </div>

            <div>
              <Text strong style={{ display: 'block', marginBottom: 8 }}>{$i18n.get({ id: 'legacy.prompts.description', dm: '描述' })}</Text>
              <TextArea
                value={formData.promptDescription}
                onChange={(e) => setFormData(prev => ({ ...prev, promptDescription: e.target.value }))}
                placeholder={$i18n.get({ id: 'legacy.prompts.describe.this.prompt.s.purpose.and.characteristics', dm: '描述这个Prompt的用途和特点...' })}
                rows={3}
                size="large"
              />
            </div>
          </Space>
        </Card>

        {quickCreate && (
          <Card title={$i18n.get({ id: 'legacy.prompts.version.information', dm: '版本信息' })} size="small">
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Row gutter={16}>
                <Col span={8}>
                  <Text strong style={{ display: 'block', marginBottom: 8 }}>{$i18n.get({ id: 'legacy.prompts.version.number', dm: '版本号 *' })}</Text>
                  <Input
                    value={versionData.version}
                    onChange={(e) => setVersionData(prev => ({ ...prev, version: e.target.value }))}
                    placeholder="1.0"
                    size="large"
                  />
                </Col>

                <Col span={8}>
                  <Text strong style={{ display: 'block', marginBottom: 8 }}>{$i18n.get({ id: 'legacy.prompts.version.type', dm: '版本类型 *' })}</Text>
                  <Select
                    value={versionData.status}
                    onChange={(value) => setVersionData(prev => ({ ...prev, status: value }))}
                    style={{ width: '100%' }}
                    size="large"
                  >
                    <Option value="release">{$i18n.get({ id: 'legacy.prompts.release', dm: '正式版本' })}</Option>
                    <Option value="pre">{$i18n.get({ id: 'legacy.prompts.pre.release', dm: 'PRE版本' })}</Option>
                  </Select>
                </Col>

                <Col span={8}>
                  <Text strong style={{ display: 'block', marginBottom: 8 }}>{$i18n.get({ id: 'legacy.prompts.version.description', dm: '版本说明' })}</Text>
                  <Input
                    value={versionData.versionDescription}
                    onChange={(e) => setVersionData(prev => ({ ...prev, versionDescription: e.target.value }))}
                    placeholder={$i18n.get({ id: 'legacy.prompts.initial.version', dm: '初始版本' })}
                    size="large"
                  />
                </Col>
              </Row>

              <div>
                <Text strong style={{ display: 'block', marginBottom: 8 }}>{$i18n.get({ id: 'legacy.prompts.version.content.preview', dm: '版本内容预览' })}</Text>
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
                    message={$i18n.get({ id: 'legacy.prompts.enter.prompt.content.in.the.editor', dm: '请在编辑区填写Prompt内容' })}
                    type="warning"
                    icon={<ExclamationCircleOutlined />}
                    showIcon
                  />
                )}
              </div>

              {/* 参数预览 */}
              {variablesWithValueList.length > 0 && (
                <Card title={$i18n.get({ id: 'legacy.prompts.detected.parameters.key.value.pairs', dm: '检测到的参数: 键值对' })} size="small">
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
                <Card title={$i18n.get({ id: 'legacy.prompts.model.configuration', dm: '模型配置' })} size="small">
                  <Row gutter={[16, 8]}>
                    {/* 显示模型名称而非ID */}
                    <Col span={24} style={{ marginBottom: 8 }}>
                      <Space>
                        <Text strong>{$i18n.get({ id: 'legacy.prompts.model.2', dm: '模型：' })}</Text>
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
                              {$i18n.get({ id: 'legacy.prompts.no.model.parameters.configured', dm: '暂无模型参数配置' })}
                            </Text>
                          </Col>
                        );
                      }

                      return paramEntries.map(([key, value], index) => {
                        return (
                          <Col span={12} key={key}>
                            <Space>
                              <Text strong>{key}{$i18n.get({ id: 'legacy.prompts.text', dm: '：' })}</Text>
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
                message={versionData.status === 'release' ? $i18n.get({ id: 'legacy.prompts.release.version', dm: '正式版本说明' }) : $i18n.get({ id: 'legacy.prompts.pre.release.version', dm: 'PRE版本说明' })}
                description={
                  <div>
                    {versionData.status === 'release' ? (
                      <div>
                        <Paragraph style={{ margin: 0, marginBottom: 4 }}>
                          <Text strong>{$i18n.get({ id: 'legacy.prompts.release.version.2', dm: '正式版本：' })}</Text>{$i18n.get({ id: 'legacy.prompts.a.stable.production.version.that.updates.the.curre', dm: '稳定的生产环境版本，会更新当前版本指针' })}
                        </Paragraph>
                        <Text>{$i18n.get({ id: 'legacy.prompts.use.this.version.in.production.after.thorough.testing.and.va', dm: '适用于生产环境使用，经过充分测试和验证' })}</Text>
                      </div>
                    ) : (
                      <div>
                        <Paragraph style={{ margin: 0, marginBottom: 4 }}>
                          <Text strong>{$i18n.get({ id: 'legacy.prompts.pre.release.version.2', dm: 'PRE版本：' })}</Text>{$i18n.get({ id: 'legacy.prompts.a.pre.release.version.for.testing.and.validation.2', dm: '预发布版本，用于测试和验证' })}
                        </Paragraph>
                        <Text>{$i18n.get({ id: 'legacy.prompts.use.this.version.in.test.environments.it.does.not.update.the', dm: '适用于测试环境，不会更新当前版本指针' })}</Text>
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
