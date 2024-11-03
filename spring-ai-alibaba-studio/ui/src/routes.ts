import { IRouterConfig, lazy } from 'ice';
import Layout from '@/Layouts/BasicLayout';

const NotFound = lazy(() => import('@/components/NotFound'));
const Model = lazy(() => import('@/pages/Model'));

const routerConfig: IRouterConfig[] = [
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '/',
        exact: true,
        component: Model,
      }, 
      {
        component: NotFound,
      }
  ],
  },
];

export default routerConfig;
