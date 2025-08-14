import $i18n from '@/i18n';
import { createAccount, updateAccount } from '@/services/account';
import { Button, Form, Input, message, Modal } from '@spark-ai/design';
import React, { useEffect, useState } from 'react';
import styles from './index.module.less';

export interface UserEditData {
  key?: string;
  name: string;
}

interface UserFormValues extends UserEditData {
  newPassword?: string;
  confirmPassword?: string;
}

interface UserEditModalProps {
  open: boolean;
  onCancel: () => void;
  onOk: () => void;
  initialValues?: UserEditData | null;
}

const UserEditModal: React.FC<UserEditModalProps> = ({
  open,
  onCancel,
  onOk,
  initialValues,
}) => {
  const [form] = Form.useForm<UserFormValues>();
  const isEditMode = !!initialValues;
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open) {
      if (initialValues) {
        form.setFieldsValue({
          ...initialValues,
          newPassword: '',
          confirmPassword: '',
        });
      } else {
        form.resetFields();
      }
    } else {
      form.resetFields();
    }
  }, [open, initialValues, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEditMode) {
        if (values.newPassword) {
          await updateAccount(initialValues?.key as string, {
            nickname: values.name,
            password: values.newPassword,
          });
          message.success(
            $i18n.get({
              id: 'main.pages.Setting.Account.components.UserEditModal.index.updateSuccess',
              dm: '更新成功',
            }),
          );
        }
      } else {
        await createAccount({
          username: values.name,
          password: values.newPassword as string,
        });
        message.success(
          $i18n.get({
            id: 'main.pages.Setting.Account.components.UserEditModal.index.userCreateSuccess',
            dm: '用户创建成功',
          }),
        );
      }

      onOk();
    } finally {
      setLoading(false);
    }
  };

  const modalTitle = isEditMode
    ? $i18n.get({
        id: 'main.pages.Setting.Account.components.UserEditModal.index.editUser',
        dm: '编辑用户',
      })
    : $i18n.get({
        id: 'main.pages.Setting.Account.components.UserEditModal.index.addUser',
        dm: '新增用户',
      });

  return (
    <Modal
      title={modalTitle}
      open={open}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          {$i18n.get({
            id: 'main.pages.Setting.Account.components.UserEditModal.index.cancel',
            dm: '取消',
          })}
        </Button>,
        <Button
          key="submit"
          type="primary"
          onClick={handleOk}
          loading={loading}
        >
          {$i18n.get({
            id: 'main.pages.Setting.Account.components.UserEditModal.index.confirm',
            dm: '确定',
          })}
        </Button>,
      ]}
      width={640}
    >
      <Form form={form} layout="vertical" requiredMark={false} colon={false}>
        <Form.Item
          name="name"
          label={$i18n.get({
            id: 'main.pages.Setting.Account.components.UserEditModal.index.userName',
            dm: '用户名称',
          })}
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.Setting.Account.components.UserEditModal.index.enterUserName',
                dm: '请输入用户名称',
              }),
            },
          ]}
        >
          <Input
            placeholder={$i18n.get({
              id: 'main.pages.Setting.Account.components.UserEditModal.index.enterUserName',
              dm: '请输入用户名称',
            })}
            maxLength={30}
            showCount
            disabled={isEditMode}
          />
        </Form.Item>

        <div className={styles['section-title']}>
          {isEditMode
            ? $i18n.get({
                id: 'main.pages.Setting.Account.components.UserEditModal.index.changePassword',
                dm: '更改密码',
              })
            : $i18n.get({
                id: 'main.pages.Setting.Account.components.UserEditModal.index.confirmPassword',
                dm: '确认密码',
              })}
        </div>

        <Form.Item
          name="newPassword"
          label={$i18n.get({
            id: 'main.pages.Setting.Account.components.UserEditModal.index.newPassword',
            dm: '新密码',
          })}
          rules={[
            {
              required: !isEditMode,
              message: $i18n.get({
                id: 'main.pages.Login.components.Register.index.enterPassword',
                dm: '请输入密码',
              }),
            },
          ]}
        >
          <Input.Password
            placeholder={
              isEditMode
                ? $i18n.get({
                    id: 'main.pages.Setting.Account.components.UserEditModal.index.enterNewPassword',
                    dm: '输入新密码以更改',
                  })
                : $i18n.get({
                    id: 'main.pages.Setting.Account.components.UserEditModal.index.enterPassword',
                    dm: '输入密码',
                  })
            }
          />
        </Form.Item>

        <Form.Item
          name="confirmPassword"
          label={$i18n.get({
            id: 'main.pages.Setting.Account.components.UserEditModal.index.confirmPassword',
            dm: '确认密码',
          })}
          dependencies={['newPassword']}
          rules={[
            {
              required: true,
              message: $i18n.get({
                id: 'main.pages.Setting.Account.components.UserEditModal.index.confirmPassword',
                dm: '请确认密码',
              }),
            },
            ({ getFieldValue }) => ({
              validator(_, value) {
                const newPassword = getFieldValue('newPassword');

                if (!newPassword) {
                  return Promise.reject(
                    new Error(
                      $i18n.get({
                        id: 'main.pages.Setting.Account.components.UserEditModal.index.enterPasswordFirst',
                        dm: '请先输入密码',
                      }),
                    ),
                  );
                }

                return newPassword === value
                  ? Promise.resolve()
                  : Promise.reject(
                      new Error(
                        $i18n.get({
                          id: 'main.pages.Setting.Account.components.UserEditModal.index.passwordNotMatch',
                          dm: '两次输入的密码不一致',
                        }),
                      ),
                    );
              },
            }),
          ]}
        >
          <Input.Password
            placeholder={
              isEditMode
                ? $i18n.get({
                    id: 'main.pages.Setting.Account.components.UserEditModal.index.confirmNewPassword',
                    dm: '确认新密码',
                  })
                : $i18n.get({
                    id: 'main.pages.Setting.Account.components.UserEditModal.index.confirmPassword',
                    dm: '确认密码',
                  })
            }
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default UserEditModal;
