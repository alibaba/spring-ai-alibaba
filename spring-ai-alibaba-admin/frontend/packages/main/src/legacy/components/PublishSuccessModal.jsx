import React, { useState } from 'react';
import {
  Modal,
  Card,
  Typography,
  Button,
  Alert,
  Space,
  Tabs,
  Avatar,
  message
} from 'antd';
import {
  CheckCircleOutlined,
  CloseOutlined,
  InfoCircleOutlined,
  CopyOutlined,
  BulbOutlined,
  CodeOutlined,
} from '@ant-design/icons';

const { Title, Text, Paragraph } = Typography;

const PublishSuccessModal = ({ prompt, version, onClose }) => {
  console.log(prompt, 'zxc...')
  const [activeTab, setActiveTab] = useState('integration');

  const tabs = [
    { key: 'integration', label: '集成指南', icon: <CodeOutlined /> }
  ];

  const integrationCode1 = `<dependency>
  <groupId>com.alibaba.cloud.ai</groupId>
  <artifactId>spring-ai-alibaba-agent-nacos</artifactId>
  <version>{spring.ai.alibaba.version}</version>
</dependency>
`


  const integrationCode2 = `spring.ai.alibaba.agent.proxy.nacos.serverAddr={ 替换 nacos address, 示例：127.0.0.1:8848}
spring.ai.alibaba.agent.proxy.nacos.username={ 替换 nacos 用户名, 示例：nacos}
spring.ai.alibaba.agent.proxy.nacos.password={ 替换 nacos 密码, 示例：nacos}
spring.ai.alibaba.agent.proxy.nacos.promptKey={ 替换为promptKey，示例：mse-nacos-helper }`;

  const integrationCode3 = `<dependency>
	<groupId>com.alibaba.cloud.ai</groupId>
	<artifactId>spring-ai-alibaba-autoconfigure-arms-observation</artifactId>
	<version>{spring.ai.alibaba.version}</version>
</dependency>


<!-- 用于实现各种 OTel 相关组件，如 Tracer、Exporter 的自动装载 -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- 用于将 micrometer 产生的指标数据对接到 otlp 格式 -->
<dependency>
	<groupId>io.micrometer</groupId>
	<artifactId>micrometer-registry-otlp</artifactId>
</dependency>

<!-- 用于将 micrometer 底层的链路追踪 tracer 替换为 OTel tracer -->
<dependency>
	<groupId>io.micrometer</groupId>
	<artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- 用于将 OTel tracer 产生的 span 按照 otlp 协议进行上报 -->
<dependency>
	<groupId>io.opentelemetry</groupId>
	<artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.ai</groupId>
	<artifactId>spring-ai-autoconfigure-model-tool</artifactId>
	<version>1.0.0</version>
</dependency>`;

  const integrationCode4 = `management.otlp.tracing.export.enabled=true
management.tracing.sampling.probability=1.0
management.otlp.tracing.endpoint=http://{studio address}:4318/v1/traces
management.otlp.metrics.export.enabled=false
management.otlp.logging.export.enabled=false
management.opentelemetry.resource-attributes.service.name=agent-nacos-prompt-test
management.opentelemetry.resource-attributes.service.version=1.0
spring.ai.chat.client.observations.log-prompt=true
spring.ai.chat.observations.log-prompt=true
spring.ai.chat.observations.log-completion=true
spring.ai.image.observations.log-prompt=true
spring.ai.vectorstore.observations.log-query-response=true
spring.ai.alibaba.arms.enabled=true
spring.ai.alibaba.arms.tool.enabled=true
spring.ai.alibaba.arms.model.capture-input=true
spring.ai.alibaba.arms.model.capture-output=true`;

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text).then(() => {
      message.success('复制成功');
    }).catch(() => {
      message.error('复制失败');
    });
  };

  return (
    <Modal
      open={true}
      onCancel={onClose}
      footer={null}
      width={1200}
      style={{
        top: 20,
        maxHeight: 'calc(100vh - 40px)',
        overflow: 'hidden'
      }}
      bodyStyle={{
        maxHeight: 'calc(100vh - 200px)',
        overflowY: 'auto',
        padding: 0
      }}
      closeIcon={<CloseOutlined />}
    >
      <div>
        {/* 成功提示头部 */}
        <div style={{
          padding: '24px',
          borderBottom: '1px solid #f0f0f0',
          background: 'linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <Avatar
                size={48}
                style={{ backgroundColor: '#f6ffed', color: '#52c41a' }}
                icon={<CheckCircleOutlined />}
              />
              <div>
                <Title level={2} style={{ margin: 0, color: '#262626' }}>发布成功！</Title>
                <Paragraph style={{ margin: '4px 0 0 0', color: '#595959' }}>
                  Prompt <Text style={{ fontWeight: 500, color: '#52c41a' }}>{prompt.promptKey}</Text>{' '}
                  版本 <Text style={{ fontWeight: 500, color: '#52c41a' }}>{version}</Text> 已成功发布
                </Paragraph>
              </div>
            </div>
          </div>
        </div>

        {/* Tab导航 */}
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={tabs.map(tab => ({
            key: tab.key,
            label: (
              <span>
                {tab.icon}
                <span style={{ marginLeft: 8 }}>{tab.label}</span>
              </span>
            ),
            children: null
          }))}
          style={{
            borderBottom: '1px solid #f0f0f0',
            backgroundColor: '#fafafa',
            margin: 0
          }}
          tabBarStyle={{
            paddingLeft: 24,
            paddingRight: 24,
            margin: 0
          }}
        />

        {/* 内容区域 */}
        <div style={{ padding: 24 }}>
          {activeTab === 'integration' && (
            <Space direction="vertical" size={24} style={{ width: '100%' }}>
              <Alert
                message={
                  <div>
                    Prompt发布成功，<span className='text-xs'>版本类型：</span>
                    <Text strong>{prompt.latestVersionStatus === 'release' ? '正式版本' : 'PRE版本'}</Text>
                  </div>
                }
                description={
                  prompt.latestVersionStatus === "release" ? (
                    <div>
                      <Text>当前的 Prompt 版本已经发布到Nacos中：</Text>
                      <div style={{
                        marginTop: 8,
                        padding: 12,
                        backgroundColor: '#f6ffed',
                        borderRadius: 6,
                        fontSize: '13px',
                        fontFamily: 'monospace'
                      }}>
                        <div>group：<Text strong>nacos-ai-meta</Text></div>
                        <div>dataID：<Text strong>prompt-{prompt.promptKey || '-'}.json</Text></div>
                      </div>
                    </div>
                  )
                  : (
                    <Text>Prompt 预发版本发布成功，您可以对预发版本 Prompt 进行实验室评估，评估符合预期后发布正式版本</Text>
                  )

                }
                type="success"
                icon={<CheckCircleOutlined />}
                showIcon
              />

              <Alert
                message="Spring AI Alibaba 集成指南"
                description="Spring AI Alibaba Agent集成Nacos实现prompt加载以及动态更新。"
                type="info"
                icon={<InfoCircleOutlined />}
                showIcon
              />

              <Title level={4} style={{ margin: 0 }}>
                Step 1 创建SpringBoot工程
              </Title>
              <div>
                <span className='text-red-600'>*</span>
                spring.ai.alibaba.version版本请参照spring-ai-alibaba官网
              </div>
              <Card
                title={
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Text strong>1. 引入spring ai alibaba agent nacos代理模块</Text>
                    <Button
                      type="default"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyToClipboard(integrationCode1)}
                    >
                      复制代码
                    </Button>
                  </div>
                }
                bodyStyle={{ padding: 0 }}
              >
                <div style={{
                  backgroundColor: '#1f1f1f',
                  padding: 16,
                  overflowX: 'auto'
                }}>
                  <pre style={{
                    color: '#4ade80',
                    fontSize: '13px',
                    fontFamily: 'Consolas, Monaco, "Courier New", monospace',
                    whiteSpace: 'pre-wrap',
                    margin: 0
                  }}>
                    {integrationCode1}
                  </pre>
                </div>
              </Card>

              <Card
                title={
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Text strong>2. 指定nacos地址及prompKey</Text>
                    <Button
                      type="default"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyToClipboard(integrationCode3)}
                    >
                      复制代码
                    </Button>
                  </div>
                }
                bodyStyle={{ padding: 0 }}
              >
                <div style={{
                  backgroundColor: '#1f1f1f',
                  padding: 16,
                  overflowX: 'auto'
                }}>
                  <pre style={{
                    color: '#4ade80',
                    fontSize: '13px',
                    fontFamily: 'Consolas, Monaco, "Courier New", monospace',
                    whiteSpace: 'pre-wrap',
                    margin: 0
                  }}>
                    {integrationCode2}
                  </pre>
                </div>
              </Card>
              <Card
                title={
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Text strong>4. 设置可观测参数</Text>
                    <Button
                      type="default"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyToClipboard(integrationCode2)}
                    >
                      复制代码
                    </Button>
                  </div>
                }
                bodyStyle={{ padding: 0 }}
              >
                <div style={{
                  backgroundColor: '#1f1f1f',
                  padding: 16,
                  overflowX: 'auto'
                }}>
                  <pre style={{
                    color: '#4ade80',
                    fontSize: '13px',
                    fontFamily: 'Consolas, Monaco, "Courier New", monospace',
                    whiteSpace: 'pre-wrap',
                    margin: 0
                  }}>
                    {integrationCode3}
                  </pre>
                </div>
              </Card>
              <Card
                title={
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Text strong>3. 引入spring ai alibaba 模块可观测组件</Text>
                    <Button
                      type="default"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyToClipboard(integrationCode4)}
                    >
                      复制代码
                    </Button>
                  </div>
                }
                bodyStyle={{ padding: 0 }}
              >
                <div style={{
                  backgroundColor: '#1f1f1f',
                  padding: 16,
                  overflowX: 'auto'
                }}>
                  <pre style={{
                    color: '#4ade80',
                    fontSize: '13px',
                    fontFamily: 'Consolas, Monaco, "Courier New", monospace',
                    whiteSpace: 'pre-wrap',
                    margin: 0
                  }}>
                    {integrationCode4}
                  </pre>
                </div>
                <div className='p-2'>
                  <span className='text-red-600'>*</span>
                  {`其中{studio address}请替换为实际的地址`}
                </div>
              </Card>

              <Title level={4} style={{ margin: 0 }}>
                Step 1 创建SpringBoot工程
              </Title>

              <Card>
                <div>
                  <Text>
                    构建ReactAgent指定builderFactory为 NacosAgentPromptBuilderFactory
                  </Text>
                </div>
                <div>
                  <Text>
                    ReactAgent.builder(new NacosAgentPromptBuilderFactory(nacosOptions))
                  </Text>
                </div>
                <div>
                  说明:
                  <ol>
                    <li>
                      nacosOptions类型是NacosOptions，可以通过标准SpringBean模式引入
                    </li>
                    <li>
                      默认ReactAgent.builder() 内部使用DefaultBuilder, 通过指定。NacosAgentPromptBuilderFactory(nacosOptions)，构建时会从nacos中加载promptKey对应的prompt模版，并且支持prompt动态更新。
                    </li>
                    <li>
                      其余构建的ReactAgent的参数和标准的ReactAgent构建方式一致。
                    </li>
                  </ol>
                </div>
              </Card>
            </Space>
          )}

        </div>

        {/* 底部操作区 */}
        <div style={{
          padding: 24,
          borderTop: '1px solid #f0f0f0',
          backgroundColor: '#fafafa',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          position: 'sticky',
          bottom: 0
        }}>
          <div style={{ display: 'flex', alignItems: 'center', color: '#595959', fontSize: '14px' }}>
            <BulbOutlined style={{ color: '#faad14', marginRight: 4 }} />
            提示：配置更新后，应用会自动重新加载最新的Prompt配置
          </div>
          <Space>
            <Button
              type="primary"
              onClick={onClose}
            >
              完成
            </Button>
          </Space>
        </div>
      </div>
    </Modal>
  );
};

export default PublishSuccessModal;
