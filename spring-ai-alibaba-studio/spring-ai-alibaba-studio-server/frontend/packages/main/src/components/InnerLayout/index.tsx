import { Button, IconButton, IconFont, Tabs, TabsProps, Tooltip } from '@spark-ai/design';
import { Breadcrumb, BreadcrumbProps, Flex, Spin } from 'antd';
import classNames from 'classnames';
import { omit } from 'lodash-es';
import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './index.module.less';

interface TourItem {
  title: string;
  description: string | React.ReactNode;
}

interface InnerLayoutProps {
  breadcrumbLinks: BreadcrumbProps['items'];
  /** Whether to simplify breadcrumb display */
  simplifyBreadcrumb?: boolean;
  /** Left action area */
  left?: React.ReactNode;
  /** Right action area */
  right?: React.ReactNode;
  /** Tabs */
  tabs?: React.ReactNode | TabsProps['items'];
  /** Loading state */
  loading?: boolean;
  /** Currently active tab */
  activeTab?: string;
  /** Tab change handler */
  onTabChange?: (key: string) => void;
  children?: React.ReactNode;
  contentAreaClassName?: string;
  /** Fullscreen mode */
  fullScreen?: boolean;
  /** Tour panel items */
  tours?: TourItem[];
  /** Bottom action area */
  bottom?: React.ReactNode;
  style?: React.CSSProperties;
  /** Whether to destroy inactive tab panels */
  destroyInactiveTabPanel?: boolean;
  /** Custom styles for each part of the component */
  styles?: {
    header?: React.CSSProperties;
    left?: React.CSSProperties;
    middle?: React.CSSProperties;
    right?: React.CSSProperties;
    contentArea?: React.CSSProperties;
    bottom?: React.CSSProperties;
    breadcrumb?: React.CSSProperties;
  };
  /** Custom class names for each part of the component */
  classnames?: {
    header?: string;
    left?: string;
    middle?: string;
    right?: string;
    contentArea?: string;
    bottom?: string;
    breadcrumb?: string;
  };
}

const TEXT_MAX_WIDTH = 200;

const BreadcrumbItem = ({
  item,
  navigate,
  classname,
  style,
}: {
  item: any;
  navigate: any;
  classname?: string;
  style?: React.CSSProperties;
}) => {
  const title = typeof item.title === 'string' ? item.title : '';
  const textRef = useRef<HTMLSpanElement>(null);
  const [shouldShowTooltip, setShouldShowTooltip] = useState(false);

  useEffect(() => {
    if (textRef.current && title.length > 0) {
      const width = textRef.current.offsetWidth;
      setShouldShowTooltip(width >= TEXT_MAX_WIDTH);
    }
  }, [title]);

  const content = (
    <span
      ref={textRef}
      className={classNames(
        classname,
        classNames(styles['breadcrumb-link'], {
          [styles.ellipsis]: typeof item.title === 'string',
        }),
      )}
      style={style}
    >
      {item.onClick ? (
        <a onClick={(e) => item.onClick(e)}>{item.title}</a>
      ) : item.href ? (
        <a href={item.href}>{item.title}</a>
      ) : item.path ? (
        <a onClick={() => navigate(item.path)}>{item.title}</a>
      ) : (
        <span>{item.title}</span>
      )}
    </span>
  );

  return shouldShowTooltip ? (
    <Tooltip mode="dark" title={title} placement="bottom" mouseEnterDelay={0.5}>
      {content}
    </Tooltip>
  ) : (
    content
  );
};

const InnerLayout: React.FC<InnerLayoutProps> = ({
  breadcrumbLinks,
  left,
  right,
  tabs,
  loading = false,
  children,
  activeTab,
  onTabChange,
  contentAreaClassName,
  fullScreen = false,
  simplifyBreadcrumb = false,
  tours = [],
  bottom,
  style,
  destroyInactiveTabPanel = true,
  styles: customStyles = {},
  classnames: customClassnames = {},
}) => {
  const navigate = useNavigate();
  const [showTourPanel, setShowTourPanel] = useState(false);
  const [isFullScreen, setIsFullScreen] = useState(fullScreen);
  const [readied, setReadied] = useState(false);

  useEffect(() => {
    setTimeout(() => {
      setReadied(true);
    });
  }, []);
  const handleTabChange = (key: string) => {
    if (onTabChange) {
      onTabChange(key);
    }
  };

  const renderTabs = () => {
    if (!Array.isArray(tabs)) {
      return null;
    }

    const items = tabs.map((item) => omit(item, ['children']));

    return (
      <Tabs
        activeKey={activeTab}
        onChange={handleTabChange}
        items={items}
        centered
        type="segmented"
        destroyInactiveTabPane={destroyInactiveTabPanel}
        className={styles.tabs}
      />
    );
  };

  const handleBackClick = () => {
    // Navigate to the path of the second last breadcrumb item if there are at least 2 items
    if (breadcrumbLinks && breadcrumbLinks.length >= 2) {
      const prevLink = breadcrumbLinks[breadcrumbLinks.length - 2];
      if (prevLink.path) {
        navigate(prevLink.path);
      }
    }
    cancelFullscreen();
  };

  const renderBreadcrumb = () => {
    if (!breadcrumbLinks || breadcrumbLinks.length === 0) {
      return null;
    }

    const itemRender = (item: any) => {
      return (
        <BreadcrumbItem
          item={item}
          navigate={navigate}
          classname={customClassnames?.breadcrumb}
          style={customStyles?.breadcrumb}
        />
      );
    };

    // Handle fullscreen mode or simplified breadcrumb
    if (isFullScreen || simplifyBreadcrumb) {
      const lastBreadcrumb = breadcrumbLinks[breadcrumbLinks.length - 1];
      return (
        <>
          <Button className={styles['back-icon']} onClick={handleBackClick}>
            <IconFont type="spark-leftArrow-line" />
          </Button>
          <Breadcrumb items={[lastBreadcrumb]} itemRender={itemRender} />
        </>
      );
    }

    // Show full breadcrumb when not in fullscreen mode and not simplified
    return <Breadcrumb items={breadcrumbLinks} itemRender={itemRender} />;
  };

  const toggleFullScreen = () => {
    setIsFullScreen(!isFullScreen);
  };

  const cancelFullscreen = () => {
    setIsFullScreen(false);
  };

  const toggleTourPanel = () => {
    setShowTourPanel(!showTourPanel);
  };

  // Tour panel button
  const renderTourButton = () => {
    if (!tours || tours.length === 0) {
      return null;
    }

    return (
      <div className={styles['tour-button-container']}>
        <IconButton
          bordered={false}
          icon={
            <IconFont
              type={showTourPanel ? 'spark-fold-line-2' : 'spark-fold-line'}
            />
          }
          onClick={toggleTourPanel}
        />

        {showTourPanel && (
          <div className={styles['tour-panel-container']}>
            <div className={styles['tour-panel']}>
              {tours.map((tour, index) => (
                <div key={index} className={styles['tour-item']}>
                  <div className={styles.index}>{index + 1}</div>
                  <div className={styles['tour-title']}>{tour.title}</div>
                  <div className={styles['tour-description']}>
                    {tour.description}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  };

  // Fullscreen button
  const renderFullScreenButton = () => {
    return (
      fullScreen && (
        <>
          <div className={styles['divider']} />
          <IconButton
            bordered={false}
            icon={
              isFullScreen
                ? 'spark-exitFullscreen-line'
                : 'spark-fullscreen-line'
            }
            onClick={toggleFullScreen}
          />
        </>
      )
    );
  };

  return (
    <div
      id="InnerLayout"
      className={classNames(styles['inner-layout'], {
        [styles['full-screen-mode']]: isFullScreen,
      })}
      style={style}
    >
      {/* Header */}
      <div
        className={classNames(styles.header, customClassnames.header)}
        style={customStyles.header}
      >
        {/* Left action area */}
        <div
          className={classNames(styles.left, customClassnames.left)}
          style={customStyles.left}
        >
          {renderBreadcrumb()}
          <div className={styles['left-content']}>{left}</div>
        </div>

        {/* Tabs */}
        <div
          className={classNames(
            styles.middle,
            styles['vertical-center'],
            customClassnames.middle,
          )}
          style={customStyles.middle}
        >
          {renderTabs()}
        </div>

        {/* Right action area */}
        <div
          className={classNames(styles.right, customClassnames.right)}
          style={customStyles.right}
        >
          {renderTourButton()}
          <Flex
            gap={12}
            id="InnerLayoutRight"
            className={styles['right-content']}
          >
            {right}
          </Flex>
          {renderFullScreenButton()}
        </div>
      </div>

      {/* Content area */}
      {readied && (
        <div
          className={classNames(
            styles['content-area'],
            contentAreaClassName,
            customClassnames.contentArea,
          )}
          style={customStyles.contentArea}
        >
          <Spin spinning={loading} rootClassName={styles.loading}>
            {Array.isArray(tabs)
              ? tabs.find((item) => item.key === activeTab)?.children
              : null}
            {children}
          </Spin>
        </div>
      )}

      {/* Fixed bottom area */}
      <div
        id="InnerLayoutBottom"
        className={classNames(
          { [styles['bottom']]: !!bottom },
          customClassnames.bottom,
        )}
        style={customStyles.bottom}
      >
        {bottom}
      </div>
    </div>
  );
};

export default InnerLayout;
