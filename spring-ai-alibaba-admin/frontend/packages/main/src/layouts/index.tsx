import { Outlet } from 'umi';
import SideMenuLayout from './SideMenuLayout';

export default function Layout() {
  return (
    <SideMenuLayout>
      <Outlet />
    </SideMenuLayout>
  );
}
