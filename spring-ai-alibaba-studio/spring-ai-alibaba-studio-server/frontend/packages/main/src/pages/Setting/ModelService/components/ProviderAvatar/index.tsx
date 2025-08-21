import { IProviderConfigInfo } from '@/types/modelService';
import { Avatar, IconFont } from '@spark-ai/design';
import classNames from 'classnames';
import styles from './index.module.less';

interface ProviderAvatarProps {
  provider: IProviderConfigInfo | null;
  className?: string;
}

export const ProviderAvatar: React.FC<ProviderAvatarProps> = ({
  provider,
  className,
}) => {
  if (provider?.icon) {
    return (
      <IconFont
        type={provider.icon}
        className={classNames(styles.icon, className)}
      />
    );
  }

  return (
    <Avatar className={classNames(styles['avatar'], className)} shape="square">
      {provider?.name?.charAt(0).toUpperCase() || 'P'}
    </Avatar>
  );
};
