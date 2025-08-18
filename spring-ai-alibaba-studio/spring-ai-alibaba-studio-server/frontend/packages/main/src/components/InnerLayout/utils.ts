import $i18n from '@/i18n';
import React, { useEffect, useState } from 'react';
import ReactDOM from 'react-dom';

const usePortal = (targetId: string) => {
  const [target, setTarget] = useState<HTMLElement | null>(null);

  // Find target DOM element after component mounts
  useEffect(() => {
    const findTarget = () => {
      const element = document.getElementById(targetId);
      if (element) {
        setTarget(element);
      } else {
        console.warn(
          $i18n.get(
            {
              id: 'main.components.InnerLayout.utils.notFoundTargetElement',
              dm: '找不到目标元素 #{var1}',
            },
            { var1: targetId },
          ),
        );
      }
    };

    findTarget();
  }, [targetId]);

  return (children: React.ReactNode) => {
    return target ? ReactDOM.createPortal(children, target) : null;
  };
};

/**
 * Right action area Portal Hook
 */
const useInnerLayoutRight = () => {
  return usePortal('InnerLayoutRight');
};

/**
 * Bottom area Portal Hook
 */
const useInnerLayoutBottom = () => {
  return usePortal('InnerLayoutBottom');
};

/**
 * Unified InnerLayout Portal Hook
 * Returns an object containing portal functions for right area and bottom area
 * @returns {rightPortal: (children: React.ReactNode) => React.ReactNode, bottomPortal: (children: React.ReactNode) => React.ReactNode}
 */
export const useInnerLayout = (): {
  rightPortal: (children: React.ReactNode) => React.ReactNode;
  bottomPortal: (children: React.ReactNode) => React.ReactNode;
} => {
  const rightPortal = useInnerLayoutRight();
  const baseBottomPortal = useInnerLayoutBottom();

  // Special handling for bottom area - wrap content in a div
  const bottomPortal = (children: React.ReactNode) => {
    // Default styles
    const style: React.CSSProperties = {
      backgroundColor: 'var(--ag-ant-color-bg-base)',
      borderTop: '1px solid var(--ag-ant-color-border-secondary)',
      padding: '16px 24px',
      display: 'flex',
      gap: 8,
    };

    return baseBottomPortal(
      React.createElement(
        'div',
        {
          style,
        },
        children,
      ),
    );
  };

  return { rightPortal, bottomPortal };
};
