import PureLayout from '@/layouts/Pure';
import { IAppComponentListItem } from '@/types/appComponent';
import { createRoot } from 'react-dom/client';
import ComponentSelectorModal from './index';

interface IShowOptions {
  appCode: string;
  onOk?: (item: IAppComponentListItem) => void;
  onCancel?: () => void;
}

export const ComponentSelectorModalFuncs = {
  show: (options: IShowOptions) => {
    const div = document.createElement('div');
    document.body.appendChild(div);
    const root = createRoot(div);

    const handleClose = () => {
      root.unmount();
      div.remove();
    };

    const handleOk = (item: IAppComponentListItem) => {
      options.onOk?.(item);
      handleClose();
    };

    root.render(
      <PureLayout>
        <ComponentSelectorModal
          appCode={options.appCode}
          onClose={() => {
            handleClose();
            options.onCancel?.();
          }}
          onOk={handleOk}
        />
      </PureLayout>,
    );
  },
};
