import UserAccountModal from '@/components/UserAccountModal';
import { Outlet } from 'umi';
import Header from './Header';
import styles from './index.module.less';
import LangSelect from './LangSelect';
import LoginProvider from './LoginProvider';
import MenuList from './MenuList';
import PureLayout from './Pure';
import SettingDropdown from './SettingDropdown';
import ThemeSelect from './ThemeSelect';

export default function Layout() {
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
          <MenuList />
        </Header>
        <div className={styles['body']}>
          <Outlet />
        </div>
      </LoginProvider>
    </PureLayout>
  );
}
