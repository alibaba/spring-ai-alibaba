import { SmileOutlined, HeartOutlined } from '@ant-design/icons';

const asideMenuConfig = [
  {
    name: 'ChatClient',
    path: '/Clients',
    icon: SmileOutlined,
  },
  {
    name: 'Chat Model',
    icon: SmileOutlined,
    path: '/Models',
    children: [
      {
        name: 'Chat Model',
        path: '/Models/ChatModel',
        icon: SmileOutlined,
      },
      {
        name: 'Image Model',
        path: '/Models/ImageModel',
        icon: SmileOutlined,
      },
    ],
  },
];

export { asideMenuConfig };
