import { useReactFlow, useViewport } from '@xyflow/react';
import { useCallback, useMemo } from 'react';

export const useFlowViewPort = () => {
  const { zoom } = useViewport();
  const reactFlow = useReactFlow();

  const handleSetScale = useCallback(
    (scale: number) => {
      const { x, y } = reactFlow.getViewport();
      reactFlow.setViewport({ zoom: scale, x, y });
    },
    [reactFlow],
  );

  const handleScale = useCallback(
    (options: 1 | -1) => {
      let newZoom = parseFloat(zoom.toFixed(1)) + options * 0.2;
      if (newZoom > 2) newZoom = 2;
      if (newZoom < 0.5) newZoom = 0.5;
      handleSetScale(newZoom);
    },
    [zoom, handleSetScale],
  );

  const scaleRate = useMemo(() => {
    return parseInt(`${zoom * 100}`);
  }, [zoom]);

  return {
    handleScale,
    scaleRate,
    handleSetScale,
  };
};
