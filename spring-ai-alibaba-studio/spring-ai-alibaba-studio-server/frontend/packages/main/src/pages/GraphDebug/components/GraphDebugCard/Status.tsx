import { Tag } from '@spark-ai/design';
import $i18n from '@/i18n';

export type GraphStatus = 'ACTIVE' | 'DRAFT' | 'DISABLED';

interface StatusProps {
  status: GraphStatus;
}

const Status: React.FC<StatusProps> = ({ status }) => {
  const statusConfig = {
    ACTIVE: {
      color: 'green' as const,
      text: $i18n.get({
        id: 'main.pages.GraphDebug.components.Status.active',
        dm: '活跃',
      }),
    },
    DRAFT: {
      color: 'blue' as const,
      text: $i18n.get({
        id: 'main.pages.GraphDebug.components.Status.draft',
        dm: '草稿',
      }),
    },
    DISABLED: {
      color: 'red' as const,
      text: $i18n.get({
        id: 'main.pages.GraphDebug.components.Status.disabled',
        dm: '禁用',
      }),
    },
  };

  const config = statusConfig[status] || statusConfig.DRAFT;

  return <Tag color={config.color}>{config.text}</Tag>;
};

export default Status;
