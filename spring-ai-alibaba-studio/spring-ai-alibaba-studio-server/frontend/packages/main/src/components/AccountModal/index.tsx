import $i18n from '@/i18n';
import { authLogout } from '@/services/login';
import { IAccount, USER_TYPE } from '@/types/account';
import { Avatar, Button, Form, Input, Modal, Tag } from '@spark-ai/design';
import { Flex } from 'antd';
import React, { useEffect } from 'react';
import styles from './index.module.less';

interface AccountModalProps {
  open: boolean;
  onCancel: () => void;
  onOk: (values: any) => void;
  userInfo: IAccount | null;
}

const AccountModal: React.FC<AccountModalProps> = ({
  open,
  onCancel,
  onOk,
  userInfo,
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (open && userInfo) {
      form.setFieldsValue({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
    }
  }, [open, userInfo, form]);

  const handleOk = () => {
    form.validateFields().then((values) => {
      onOk(values);
    });
  };

  if (!userInfo) {
    return null;
  }

  return (
    <Modal
      title={$i18n.get({
        id: 'main.components.AccountModal.index.accountManagement',
        dm: '账号管理',
      })}
      open={open}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          {$i18n.get({
            id: 'main.pages.Setting.ModelService.components.ProviderInfoForm.index.cancel',
            dm: '取消',
          })}
        </Button>,
        <Button key="submit" type="primary" onClick={handleOk}>
          {$i18n.get({
            id: 'main.pages.Setting.Account.components.UserEditModal.index.confirm',
            dm: '确定',
          })}
        </Button>,
      ]}
      width={520}
    >
      <div className={styles['user-info-header']}>
        <Avatar size={40}>{userInfo.username.charAt(0).toUpperCase()}</Avatar>
        <div className={styles['user-info-details']}>
          <Flex align="center" gap={24}>
            <div className={styles['user-name']}>{userInfo.username}</div>
            <Button
              size="small"
              iconType="spark-escape-line"
              onClick={() => {
                authLogout();
              }}
            >
              {$i18n.get({
                id: 'main.components.AccountModal.index.logout',
                dm: '退出登录',
              })}
            </Button>
          </Flex>
          <Tag color={userInfo.type === 'admin' ? 'purple' : 'mauve'}>
            {USER_TYPE[userInfo.type as keyof typeof USER_TYPE]}
          </Tag>
        </div>
      </div>

      <Form
        form={form}
        requiredMark={false}
        colon={false}
        labelCol={$i18n.getCurrentLanguage() === 'cn' ? { span: 4 } : undefined}
        labelAlign="right"
      >
        <div className={styles['section-title']}>
          {$i18n.get({
            id: 'main.pages.Setting.Account.components.UserEditModal.index.changePassword',
            dm: '更改密码',
          })}
        </div>
        <Form.Item
          name="currentPassword"
          label={$i18n.get({
            id: 'main.components.AccountModal.index.currentPassword',
            dm: '当前密码',
          })}
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.components.AccountModal.index.enterCurrentPassword',
                dm: '请输入当前密码',
              }),
            },
          ]}
        >
          <Input.Password
            placeholder={$i18n.get({
              id: 'main.components.AccountModal.index.inputCurrentPassword',
              dm: '输入当前密码',
            })}
          />
        </Form.Item>

        <Form.Item
          name="newPassword"
          label={$i18n.get({
            id: 'main.pages.Setting.Account.components.UserEditModal.index.newPassword',
            dm: '新密码',
          })}
        >
          <Input.Password
            placeholder={$i18n.get({
              id: 'main.components.AccountModal.index.inputNewPassword',
              dm: '输入新的密码',
            })}
          />
        </Form.Item>

        <Form.Item
          name="confirmPassword"
          label={$i18n.get({
            id: 'main.pages.Setting.Account.components.UserEditModal.index.newPassword',
            dm: '新密码',
          })}
          dependencies={['newPassword']}
          rules={[
            ({ getFieldValue }) => ({
              validator(_, value) {
                const newPassword = getFieldValue('newPassword');
                if (newPassword && newPassword !== value) {
                  return Promise.reject(
                    new Error(
                      $i18n.get({
                        id: 'main.pages.Setting.Account.components.UserEditModal.index.passwordNotMatch',
                        dm: '两次输入的密码不一致',
                      }),
                    ),
                  );
                }
                return Promise.resolve();
              },
            }),
          ]}
        >
          <Input.Password
            placeholder={$i18n.get({
              id: 'main.pages.Setting.Account.components.UserEditModal.index.confirmNewPassword',
              dm: '确认新密码',
            })}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AccountModal;
