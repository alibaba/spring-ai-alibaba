import { IRouterConfig, lazy } from 'ice';
import Layout from '@/Layouts/BasicLayout';

const NotFound = lazy(() => import('@/components/NotFound'));
const Clients = lazy(() => import('@/pages/Clients'));
const ChatModel = lazy(() => import('@/pages/Models/ChatModel'));
const ImageModel = lazy(() => import('@/pages/Models/ImageModel'));

const routerConfig: IRouterConfig[] = [
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '/',
        exact: true,
        component: Clients,
      },
      {
        path: '/Models',
        children: [
          {
            path: '/Models/ChatModel',
            component: ChatModel,
          },
          {
            path: '/Models/ImageModel',
            component: ImageModel,
          },
        ],
      },
      {
        component: NotFound,
      },
    ],
  },
];

export default routerConfig;
