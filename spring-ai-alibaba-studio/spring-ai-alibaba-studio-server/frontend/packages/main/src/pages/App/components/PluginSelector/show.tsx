import PureLayout from '@/layouts/Pure';
import { PluginTool } from '@/types/plugin';
import { createRoot } from 'react-dom/client';
import { ToolSelectorModal } from './index';

interface ShowToolSelectorOptions {
  value?: PluginTool[];
  maxLen?: number;
  onOk?: (tools: PluginTool[]) => void;
  onCancel?: () => void;
}

export const ToolSelectorModalFuncs = {
  show: (options: ShowToolSelectorOptions) => {
    const div = document.createElement('div');
    document.body.appendChild(div);
    const root = createRoot(div);

    const handleClose = () => {
      root.unmount();
      div.remove();
      options.onCancel?.();
    };

    const handleOk = (tools: PluginTool[]) => {
      options.onOk?.(tools);
      handleClose();
    };

    root.render(
      <PureLayout>
        <ToolSelectorModal
          value={options.value || []}
          maxLen={options.maxLen}
          onClose={handleClose}
          onOk={handleOk}
        />
      </PureLayout>,
    );
  },
};
