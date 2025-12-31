import classNames from 'classnames';
import React, { useCallback, useRef, useState } from 'react';
import './index.less';

interface DragPanelProps {
  maxWidth: number;
  minWidth: number;
  children?: React.ReactNode;
  defaultWidth?: number;
}

const DragPanel: React.FC<DragPanelProps> = ({
  maxWidth,
  minWidth,
  children,
  defaultWidth = 300,
}) => {
  const [width, setWidth] = useState(defaultWidth);
  const [isDragging, setIsDragging] = useState(false);
  const [isHovering, setIsHovering] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const startXRef = useRef<number>(0);
  const startWidthRef = useRef<number>(0);

  const handleMouseDown = useCallback(
    (e: React.MouseEvent) => {
      setIsDragging(true);
      startXRef.current = e.clientX;
      startWidthRef.current = width;
      e.preventDefault();
    },
    [width],
  );

  const handleMouseMove = useCallback(
    (e: MouseEvent) => {
      if (!isDragging) return;

      const deltaX = startXRef.current - e.clientX;
      const newWidth = startWidthRef.current + deltaX;
      const clampedWidth = Math.min(Math.max(newWidth, minWidth), maxWidth);

      requestAnimationFrame(() => {
        setWidth(clampedWidth);
      });
    },
    [isDragging, maxWidth, minWidth],
  );

  const handleMouseUp = useCallback(() => {
    setIsDragging(false);
  }, []);

  React.useEffect(() => {
    if (isDragging) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
    }
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDragging, handleMouseMove, handleMouseUp]);

  return (
    <div ref={panelRef} className="spark-drag-panel" style={{ width }}>
      <div
        className={classNames('spark-drag-panel-handle', {
          'spark-drag-panel-handle--dragging': isDragging,
          'spark-drag-panel-handle--hovering': isHovering,
        })}
        onMouseDown={handleMouseDown}
        onMouseEnter={() => setIsHovering(true)}
        onMouseLeave={() => setIsHovering(false)}
      />
      {children}
    </div>
  );
};

export default DragPanel;
