import { changePassword } from '@/services/account';
import { authLogout } from '@/services/login';
import { Avatar } from '@spark-ai/design';
import React, { useState } from 'react';
import AccountModal from '../AccountModal';

interface UserAccountModalProps {
  trigger?: React.ReactNode;
  avatarProps?: React.ComponentProps<typeof Avatar>;
}

const UserAccountModal: React.FC<UserAccountModalProps> = ({
  trigger,
  avatarProps,
}) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const userInfo = window.g_config.user;

  const defaultTrigger = (
    <Avatar size={32} onClick={() => setIsModalOpen(true)} {...avatarProps}>
      {window.g_config.user?.username.charAt(0).toUpperCase()}
    </Avatar>
  );

  const handleModalOk = async (values: any) => {
    await changePassword({
      password: values.currentPassword,
      new_password: values.newPassword,
    });
    setIsModalOpen(false);
    authLogout();
  };

  return (
    <>
      {trigger || defaultTrigger}

      <AccountModal
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onOk={handleModalOk}
        userInfo={userInfo}
      />
    </>
  );
};

export default UserAccountModal;
