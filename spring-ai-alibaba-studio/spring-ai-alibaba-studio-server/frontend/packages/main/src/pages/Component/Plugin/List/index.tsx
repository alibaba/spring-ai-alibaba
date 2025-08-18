import { useInnerLayout } from '@/components/InnerLayout/utils';
import $i18n from '@/i18n';
import { Button, IconFont } from '@spark-ai/design';
import { history } from 'umi';
import List from './List';

export default function () {
  const portal = useInnerLayout();

  return (
    <>
      {portal.rightPortal(
        <Button
          type="primary"
          icon={<IconFont type="spark-plus-line" />}
          onClick={() => history.push('/component/plugin/create')}
        >
          {$i18n.get({
            id: 'main.pages.Component.Plugin.List.index.createCustomPlugin',
            dm: '创建自定义插件',
          })}
        </Button>,
      )}
      <List />
    </>
  );
}
