import UserAccountModal from '@/components/UserAccountModal';
import { Outlet, useLocation } from 'umi';
import Header from './Header';
import styles from './index.module.less';
import LangSelect from './LangSelect';
import LoginProvider from './LoginProvider';
import MenuList from './MenuList';
import PureLayout from './Pure';
import SettingDropdown from './SettingDropdown';
import ThemeSelect from './ThemeSelect';

export default function Layout() {
  const location = useLocation();

  // Hide top menu only for the home page and dify converter page
  const shouldHideTopMenu = location.pathname === '/' || location.pathname === '/home' || location.pathname === '/dify';

  return (
    <PureLayout>
      <LoginProvider>
        <Header
          right={
            <>
              <ThemeSelect />
              <LangSelect />
              <SettingDropdown />
              <UserAccountModal avatarProps={{ className: styles.avatar }} />
            </>
          }
        >
          {!shouldHideTopMenu && <MenuList />}
        </Header>
        <div className={styles['body']}>
          <Outlet />
        </div>
      </LoginProvider>
    </PureLayout>
  );
}
