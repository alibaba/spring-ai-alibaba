import $i18n from '@/i18n';
import { Dropdown, IconButton, IconFont } from '@spark-ai/design';
import { setWorkFlowLanguage } from '@spark-ai/flow';
import { useMount } from 'ahooks';
import { MenuProps } from 'antd';

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
];

export default function () {
  const language = $i18n.getCurrentLanguage();

  const icon = {
    zh: 'spark-chinese02-line',
    en: 'spark-english02-line',
    ja: 'spark-japan-line',
  }[language];

  useMount(() => {
    setWorkFlowLanguage(language);
  });

  const button = <IconButton bordered={false} icon={icon} shape="default" />;

  return (
    <Dropdown
      menu={{
        items: menuItems,
        onClick: (e) => {
          $i18n.setCurrentLanguage(e.key);
          setWorkFlowLanguage(e.key);
          location.reload();
        },
      }}
      trigger={['click']}
    >
      {button}
    </Dropdown>
  );
}
