import $i18n from '@/i18n';
import { NavLink } from 'umi';
import styles from './index.module.less';

const menus: {
  to: string;
  title: string;
}[] = [
  {
    to: 'app',
    title: $i18n.get({
      id: 'main.layouts.MenuList.application',
      dm: '应用',
    }),
  },
  { to: 'mcp', title: 'MCP' },
  {
    to: 'component',
    title: $i18n.get({
      id: 'main.pages.Component.AppComponent.index.component',
      dm: '组件',
    }),
  },
  {
    to: 'knowledge',
    title: $i18n.get({
      id: 'main.pages.Knowledge.Test.index.knowledgeBase',
      dm: '知识库',
    }),
  },
];

export default function () {
  return (
    <div className={styles['menu-list']}>
      {menus.map((item, index) => {
        return (
          <NavLink className={styles['menu-item']} to={item.to} key={index}>
            {item.title}
          </NavLink>
        );
      })}
    </div>
  );
}
