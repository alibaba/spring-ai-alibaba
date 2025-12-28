import $i18n from '@/i18n';
import { DragPanel, PanelContainer } from '@spark-ai/flow';
import React, { memo } from 'react';

export default memo(function TestPanel(props: {
  open: boolean;
  setOpen: (open: boolean) => void;
}) {
  const { open, setOpen } = props;
  if (!open) return null;
  return (
    <DragPanel minWidth={300} maxWidth={480}>
      <PanelContainer
        title={$i18n.get({
          id: 'spark-flow.demos.spark-flow-1.components.TestPanel.index.testPanel',
          dm: '测试面板',
        })}
        onClose={() => setOpen(false)}
      >
        <div>TestPanel</div>
      </PanelContainer>
    </DragPanel>
  );
});
