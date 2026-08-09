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
    { key: 'integration', label: 'Integration Guide', icon: <CodeOutlined /> }
  ];

  const integrationCode1 = `<dependency>
  <groupId>com.alibaba.cloud.ai</groupId>
  <artifactId>spring-ai-alibaba-agent-nacos</artifactId>
  <version>{spring.ai.alibaba.version}</version>
</dependency>
`


  const integrationCode2 = `spring.ai.alibaba.agent.proxy.nacos.serverAddr={ replace with the Nacos address, e.g. 127.0.0.1:8848}
spring.ai.alibaba.agent.proxy.nacos.username={ replace with the Nacos username, e.g. nacos}
spring.ai.alibaba.agent.proxy.nacos.password={ replace with the Nacos password, e.g. nacos}
spring.ai.alibaba.agent.proxy.nacos.promptKey={ replace with the promptKey, e.g. mse-nacos-helper }`;

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
      message.success('Copied successfully');
    }).catch(() => {
      message.error('Copy failed');
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
                <Title level={2} style={{ margin: 0, color: '#262626' }}>Published successfully!</Title>
                <Paragraph style={{ margin: '4px 0 0 0', color: '#595959' }}>
                  Prompt <Text style={{ fontWeight: 500, color: '#52c41a' }}>{prompt.promptKey}</Text>{' '}
                  Version <Text style={{ fontWeight: 500, color: '#52c41a' }}>{version}</Text> published successfully
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
                    Prompt published successfully. <span className='text-xs'>Version type: </span>
                    <Text strong>{prompt.latestVersionStatus === 'release' ? 'Release' : 'Pre-release'}</Text>
                  </div>
                }
                description={
                  prompt.latestVersionStatus === "release" ? (
                    <div>
                      <Text>The current Prompt version has been published to Nacos:</Text>
                      <div style={{
                        marginTop: 8,
                        padding: 12,
                        backgroundColor: '#f6ffed',
                        borderRadius: 6,
                        fontSize: '13px',
                        fontFamily: 'monospace'
                      }}>
                        <div>Group: <Text strong>nacos-ai-meta</Text></div>
                        <div>Data ID: <Text strong>prompt-{prompt.promptKey || '-'}.json</Text></div>
                      </div>
                    </div>
                  )
                  : (
                    <Text>The Prompt pre-release version was published successfully. Evaluate it in the lab, then publish a release version when it meets expectations.</Text>
                  )

                }
                type="success"
                icon={<CheckCircleOutlined />}
                showIcon
              />

              <Alert
                message="Spring AI Alibaba Integration Guide"
                description="Integrate Spring AI Alibaba Agent with Nacos for Prompt loading and dynamic updates."
                type="info"
                icon={<InfoCircleOutlined />}
                showIcon
              />

              <Title level={4} style={{ margin: 0 }}>
                Step 1: Create a Spring Boot project
              </Title>
              <div>
                <span className='text-red-600'>*</span>
                Refer to the Spring AI Alibaba website for the spring.ai.alibaba.version version.
              </div>
              <Card
                title={
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Text strong>1. Add the Spring AI Alibaba Agent Nacos proxy module</Text>
                    <Button
                      type="default"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyToClipboard(integrationCode1)}
                    >
                      Copy Code
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
                    <Text strong>2. Configure the Nacos address and promptKey</Text>
                    <Button
                      type="default"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyToClipboard(integrationCode3)}
                    >
                      Copy Code
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
                    <Text strong>4. Configure observability settings</Text>
                    <Button
                      type="default"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyToClipboard(integrationCode2)}
                    >
                      Copy Code
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
                    <Text strong>3. Add the Spring AI Alibaba observability module</Text>
                    <Button
                      type="default"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyToClipboard(integrationCode4)}
                    >
                      Copy Code
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
                  {`Replace {studio address} with the actual address.`}
                </div>
              </Card>

              <Title level={4} style={{ margin: 0 }}>
                Step 1: Create a Spring Boot project
              </Title>

              <Card>
                <div>
                  <Text>
                    Build ReactAgent with builderFactory set to NacosAgentPromptBuilderFactory
                  </Text>
                </div>
                <div>
                  <Text>
                    ReactAgent.builder(new NacosAgentPromptBuilderFactory(nacosOptions))
                  </Text>
                </div>
                <div>
                  Notes:
                  <ol>
                    <li>
                      nacosOptions is of type NacosOptions and can be injected as a standard Spring Bean.
                    </li>
                    <li>
                      ReactAgent.builder() uses DefaultBuilder by default. With NacosAgentPromptBuilderFactory(nacosOptions), it loads the Prompt template for the promptKey from Nacos during construction and supports dynamic Prompt updates.
                    </li>
                    <li>
                      All other ReactAgent builder parameters are the same as the standard ReactAgent construction approach.
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
            Tip: After configuration updates, the application automatically reloads the latest Prompt configuration.
          </div>
          <Space>
            <Button
              type="primary"
              onClick={onClose}
            >
              Done
            </Button>
          </Space>
        </div>
      </div>
    </Modal>
  );
};

export default PublishSuccessModal;
