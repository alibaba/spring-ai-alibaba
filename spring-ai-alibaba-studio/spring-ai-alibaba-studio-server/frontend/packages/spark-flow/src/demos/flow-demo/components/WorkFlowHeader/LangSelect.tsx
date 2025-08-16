import $i18n from '@/i18n';
import { Dropdown, IconButton, IconFont } from '@spark-ai/design';
import { MenuProps } from 'antd';
import React from 'react';

const menuItems: MenuProps['items'] = [
  {
    key: 'en',
    label: (
      <div className="flex items-center gap-[4px]">
        <IconFont type="spark-english02-line" /> English
      </div>
    ),
  },
  {
    key: 'zh',
    label: (
      <div className="flex items-center gap-[4px]">
        <IconFont type="spark-chinese02-line" /> 简体中文
      </div>
    ),
  },
  {
    key: 'ja',
    label: (
      <div className="flex items-center gap-[4px]">
        <IconFont type="spark-japan-line" /> 日本語
      </div>
    ),
  },
];

const lang = $i18n.getCurrentLanguage();

export default function () {
  const icon = {
    zh: 'spark-chinese02-line',
    en: 'spark-english02-line',
    ja: 'spark-japan-line',
  }[lang];

  const button = <IconButton bordered={false} icon={icon} shape="default" />;

  return (
    <Dropdown
      menu={{
        items: menuItems,
        onClick: (e) => {
          $i18n.setCurrentLanguage(e.key);
          location.reload();
        },
      }}
      trigger={['click']}
    >
      {button}
    </Dropdown>
  );
}
