import $i18n from '@/i18n';
import { Dropdown, IconButton } from '@spark-ai/design';
import type { MenuProps } from 'antd';
import React from 'react';
import { useNavigate } from 'umi';

const menuItems: MenuProps['items'] = [
  {
    key: '/setting/account',
    label: $i18n.get({
      id: 'main.layouts.SettingDropdown.accountManagement',
      dm: '账号管理',
    }),
  },
  {
    key: '/setting/modelService',
    label: $i18n.get({
      id: 'main.pages.Setting.ModelService.index.modelServiceManagement',
      dm: '模型服务管理',
    }),
  },
  {
    key: '/setting/apiKeys',
    label: $i18n.get({
      id: 'main.layouts.SettingDropdown.apiKeyManagement',
      dm: 'API KEY管理',
    }),
  },
];

const SettingDropdown: React.FC = () => {
  const navigate = useNavigate();
  const isAdmin = window.g_config.user?.type === 'admin';

  const handleMenuClick: MenuProps['onClick'] = (e) => {
    navigate(e.key);
  };

  const handleDirectNavigate = () => {
    navigate('/setting/modelService');
  };

  const settingButton = (
    <IconButton
      icon="spark-setting-line"
      bordered={false}
      shape="default"
      onClick={!isAdmin ? handleDirectNavigate : undefined}
    />
  );

  if (isAdmin) {
    return (
      <Dropdown
        menu={{ items: menuItems, onClick: handleMenuClick }}
        trigger={['click']}
      >
        {settingButton}
      </Dropdown>
    );
  } else {
    return settingButton;
  }
};

export default SettingDropdown;
