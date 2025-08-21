import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { createMcpServer, getMcpServer, updateMcpServer } from '@/services/mcp';
import { ICreateMcpParams, IUpdateMcpParams, McpStatus } from '@/types/mcp';
import {
  AlertDialog,
  Button,
  CodeBlock,
  Form,
  getCommonConfig,
  Input,
  message,
} from '@spark-ai/design';
import { useMount } from 'ahooks';
import { Flex } from 'antd';
import classNames from 'classnames';
import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import TipBox from '../../components/TipBox';
import RadioItem from './components/RadioItem';
import styles from './Create.module.less';
import { installTypeOptions, MCP_TIP_SECTIONS } from './utils/constant';

export default function McpCreate() {
  const navigate = useNavigate();
  const { id: server_code } = useParams<{ id: string }>();

  const [form] = Form.useForm();
  const darkMode = getCommonConfig().isDarkMode;

  const [loading, setLoading] = useState(!!server_code);
  const [saveLoading, setSaveLoading] = useState(false);
  const [deployStatus, setDeployStatus] = useState(McpStatus.DISABLED);
  const [installType, setInstallType] = useState(installTypeOptions[0].value);
  const [initialData, setInitialData] = useState<any>(null);

  // Get MCP service details
  useMount(() => {
    if (server_code) {
      getMcpServer({
        server_code,
        need_tools: false,
      })
        .then((res) => {
          if (res && res.data) {
            const mcpData = res.data;
            setDeployStatus(mcpData.status);
            setInitialData(mcpData);

            // Set form values
            form.setFieldsValue({
              serverName: mcpData.name,
              description: mcpData.description,
              deployConfig: mcpData.deploy_config,
            });
          }
        })
        .finally(() => {
          setLoading(false);
        });
    }
  });

  const statusText = useMemo(() => {
    switch (deployStatus) {
      case McpStatus.ENABLED:
        return $i18n.get({
          id: 'main.pages.MCP.Create.enabled',
          dm: '已启用',
        });
      case McpStatus.DISABLED:
        return $i18n.get({
          id: 'main.pages.MCP.Create.disabled',
          dm: '未启用',
        });
      case McpStatus.DELETED:
        return $i18n.get({
          id: 'main.pages.MCP.Create.deleted',
          dm: '已删除',
        });
      default:
        return $i18n.get({
          id: 'main.pages.MCP.Create.unknownStatus',
          dm: '未知状态',
        });
    }
  }, [deployStatus]);

  // Save or deploy MCP service
  const handleOk = async () => {
    if (saveLoading) return;

    try {
      const formValues = await form.validateFields();
      let _deployConfig = formValues.deployConfig;

      // Validate deployConfig is a valid JSON
      try {
        if (typeof formValues.deployConfig === 'string') {
          const deployConfig = JSON.parse(formValues.deployConfig);
          _deployConfig = JSON.stringify(deployConfig);
        }
      } catch (error) {
        message.error(
          $i18n.get({
            id: 'main.pages.MCP.Create.mcpServiceConfigurationInvalidJsonFormat',
            dm: 'MCP服务配置不是有效的JSON格式',
          }),
        );
        return;
      }

      setSaveLoading(true);

      // Configure API parameters
      const apiParams = {
        name: formValues.serverName,
        description: formValues.description || '',
        deploy_config: _deployConfig,
        type: 'CUSTOMER',
        install_type: installType,
      };

      if (server_code) {
        // Update MCP service
        const updateParams: IUpdateMcpParams = {
          ...apiParams,
          server_code,
          status: deployStatus,
        };

        await updateMcpServer(updateParams);
        message.success(
          $i18n.get({
            id: 'main.pages.MCP.Create.mcpServiceUpdatedSuccessfully',
            dm: 'MCP服务更新成功',
          }),
        );
      } else {
        // Create MCP service
        const createParams: ICreateMcpParams = {
          ...apiParams,
        };

        await createMcpServer(createParams);
        message.success(
          $i18n.get({
            id: 'main.pages.MCP.Create.mcpServiceCreatedSuccessfully',
            dm: 'MCP服务创建成功',
          }),
        );
      }

      navigate('/mcp');
    } finally {
      setSaveLoading(false);
    }
  };

  const isFormChanged = () => {
    try {
      const currentValues = form.getFieldsValue();

      // For new entries, check if any content has been entered
      if (!server_code) {
        return !!(
          currentValues.serverName ||
          currentValues.description ||
          currentValues.deployConfig
        );
      }

      // For editing scenarios, compare the current value with the initial value
      if (initialData) {
        return (
          currentValues.serverName !== initialData.name ||
          currentValues.description !== initialData.description ||
          currentValues.deployConfig !== initialData.deploy_config
        );
      }

      return false;
    } catch (e) {
      return false;
    }
  };

  const onBack = () => {
    if (isFormChanged()) {
      AlertDialog.warning({
        title: $i18n.get({
          id: 'main.pages.MCP.Create.confirmReturn',
          dm: '确认返回吗',
        }),
        children: $i18n.get({
          id: 'main.pages.MCP.Create.returnWillNotSaveCurrentEditingContentConfirmReturn',
          dm: '返回将不会保存当前编辑的内容，确认返回吗？',
        }),
        onOk: () => {
          navigate('/mcp');
        },
      });
    } else {
      // If the form is not edited, return directly
      navigate('/mcp');
    }
  };

  const renderOkText = useMemo(() => {
    if (saveLoading) {
      return server_code
        ? $i18n.get({
            id: 'main.pages.MCP.Create.saving',
            dm: '保存中...',
          })
        : $i18n.get({
            id: 'main.pages.MCP.Create.creating',
            dm: '创建中...',
          });
    }
    return server_code
      ? $i18n.get({ id: 'main.pages.MCP.Create.save', dm: '保存' })
      : $i18n.get({
          id: 'main.pages.MCP.Create.create',
          dm: '创建',
        });
  }, [saveLoading, server_code]);

  return (
    <InnerLayout
      loading={loading}
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.MCP.Create.mcpManagement',
            dm: 'MCP管理',
          }),
          onClick: onBack,
        },
        {
          title: server_code ? (
            <Flex gap={8} align="center">
              <span>
                {$i18n.get({
                  id: 'main.pages.MCP.Create.editMcpService',
                  dm: '编辑MCP服务',
                })}
              </span>
              <Flex
                className={classNames(styles['status'], {
                  [styles['deploy']]: deployStatus === McpStatus.ENABLED,
                  [styles['deploy-error']]: deployStatus === McpStatus.DISABLED,
                })}
                gap={6}
                align="center"
              >
                <span className={styles['dot']}></span>
                <span>{statusText}</span>
              </Flex>
            </Flex>
          ) : (
            $i18n.get({
              id: 'main.pages.MCP.Create.createMcpService',
              dm: '创建MCP服务',
            })
          ),
        },
      ]}
      bottom={
        <div className={styles['bottom-container']}>
          <Button loading={saveLoading} onClick={handleOk} type="primary">
            {renderOkText}
          </Button>
          <Button onClick={onBack}>
            {$i18n.get({
              id: 'main.pages.MCP.Create.cancel',
              dm: '取消',
            })}
          </Button>
        </div>
      }
    >
      <div className={styles['page']}>
        <Flex className={styles['container']} vertical>
          <div className={styles['content-wrap']}>
            <Form className={styles['content']} form={form} layout="vertical">
              <Form.Item
                required
                label={$i18n.get({
                  id: 'main.pages.MCP.Create.serviceName',
                  dm: '服务名称',
                })}
                name="serverName"
                rules={[
                  {
                    required: true,
                    message: $i18n.get({
                      id: 'main.pages.MCP.Create.enterServiceName',
                      dm: '请输入服务名称',
                    }),
                  },
                ]}
              >
                <Input
                  className={styles['fixed-width']}
                  showCount
                  maxLength={30}
                  placeholder={$i18n.get({
                    id: 'main.pages.MCP.Create.mcpServiceName',
                    dm: 'MCP服务名称',
                  })}
                />
              </Form.Item>
              <Form.Item
                name="description"
                label={$i18n.get({
                  id: 'main.pages.MCP.Create.description',
                  dm: '描述',
                })}
              >
                <Input.TextArea
                  className={styles['fixed-width']}
                  showCount
                  maxLength={100}
                  autoSize={{ minRows: 2, maxRows: 2 }}
                  placeholder={$i18n.get({
                    id: 'main.pages.MCP.Create.describeYourMcpService',
                    dm: '描述你的MCP服务',
                  })}
                />
              </Form.Item>
              <Form.Item
                name="installType"
                label={$i18n.get({
                  id: 'main.pages.MCP.Create.installType',
                  dm: '安装类型',
                })}
              >
                {installTypeOptions.map((item) => (
                  <RadioItem
                    className={styles['mcp-install-type-item']}
                    onSelect={() => setInstallType(item.value)}
                    isActive={installType === item.value}
                    disabled={true}
                    {...item}
                    key={item.value}
                  />
                ))}
              </Form.Item>

              <Form.Item
                name="deployConfig"
                label={
                  <div className={styles['custom-label-wrap']}>
                    <span className={styles['custom-label']}>
                      {$i18n.get({
                        id: 'main.pages.MCP.Create.mcpServiceConfiguration',
                        dm: 'MCP服务配置',
                      })}

                      <span className={styles['mark']}>*</span>
                      <span className={styles['label-desc']}>
                        {$i18n.get({
                          id: 'main.pages.MCP.Create.mcpServiceUsesJsonFormatSubmitBeforeEnsureFormatIsCorrect',
                          dm: 'MCP服务使用Json表示，提交前请保证格式正确',
                        })}
                      </span>
                    </span>
                    <TipBox
                      title={$i18n.get({
                        id: 'main.pages.MCP.Create.howToGetMcpService',
                        dm: '如何获取MCP服务？',
                      })}
                      sections={MCP_TIP_SECTIONS}
                    />
                  </div>
                }
                required={false}
                rules={[
                  {
                    required: true,
                    message: $i18n.get({
                      id: 'main.pages.MCP.Create.enterMcpServiceConfiguration',
                      dm: '请输入MCP服务配置',
                    }),
                  },
                ]}
              >
                <CodeBlock
                  value={JSON.stringify(initialData?.deploy_config, null, 2)}
                  className={styles.coder}
                  theme={darkMode ? 'dark' : 'light'}
                  language="json"
                  readOnly={deployStatus === McpStatus.ENABLED}
                />
              </Form.Item>
            </Form>
          </div>
        </Flex>
      </div>
    </InnerLayout>
  );
}
