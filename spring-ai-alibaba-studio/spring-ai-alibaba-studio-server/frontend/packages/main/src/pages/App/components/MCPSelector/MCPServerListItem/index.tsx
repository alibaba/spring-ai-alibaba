import $i18n from '@/i18n';
import { IMcpServer, IMCPTool, McpStatus } from '@/types/mcp';
import { IconFont, renderTooltip } from '@spark-ai/design';
import { Checkbox, Flex, Radio, Typography } from 'antd';
import classNames from 'classnames';
import { useEffect, useRef, useState } from 'react';
import styles from './index.module.less';

interface IProps {
  item: IMcpServer;
  selectMode: 'server' | 'tool';
  selectedServers?: IMcpServer[];
  onSelectServer?: (val: IMcpServer) => void;
  onRemoveServer?: (val: string) => void;
  onSelectTool?: (item: IMCPTool, server: IMcpServer) => void;
  selectedTool?: IMCPTool;
  fetchList?: () => void;
}

export default (props: IProps) => {
  const { item, selectMode, selectedServers = [], selectedTool } = props;
  const [serverSelected, setServerSelected] = useState<boolean>(false);
  const [folded, setFolded] = useState<boolean>(true);
  const [maxHeight, setMaxHeight] = useState<number>(0);
  const toolsContainerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setServerSelected(
      selectMode === 'server'
        ? !!selectedServers.find(
            (server) => server.server_code === item.server_code,
          )
        : !!item.tools?.find((tool) => tool.name === selectedTool?.name),
    );
  }, [item, selectMode, selectedTool, selectedServers]);

  useEffect(() => {
    if (selectMode === 'tool' && serverSelected) {
      // if the tool is selected, automatically expand the mcp server
      setFolded(false);
    }
  }, [serverSelected, selectMode]);

  useEffect(() => {
    if (toolsContainerRef.current) {
      setMaxHeight(folded ? 0 : toolsContainerRef.current.scrollHeight);
    }
  }, [folded, item.tools]);

  const toggleFold = () => {
    setFolded(!folded);
  };

  return (
    <div>
      <div
        className={classNames(styles['mcp-server-wrapper'], {
          [styles.active]: serverSelected,
          [styles.expanded]: !folded,
        })}
        style={{ padding: '12px 16px' }}
      >
        <Flex gap={8}>
          {selectMode === 'server' && (
            <Checkbox
              checked={serverSelected}
              onChange={(e) => {
                if (e.target.checked) {
                  props.onSelectServer?.(item);
                } else {
                  props.onRemoveServer?.(item.server_code);
                }
              }}
              disabled={item.status === McpStatus.DISABLED}
            ></Checkbox>
          )}
          <Flex gap={8} className="w-full h-[52px] flex-1" align="center">
            <Flex align="center" className="h-[40px] w-[40px]">
              <Flex
                align="center"
                justify="center"
                className="h-[40px] w-[40px] rounded-[6px]"
                style={{
                  border: '1px solid var(--ag-ant-color-border-secondary)',
                }}
              >
                <IconFont
                  className="h-full w-full rounded-[6px]"
                  type="spark-MCP-mcp-line"
                ></IconFont>
              </Flex>
            </Flex>
            <div style={{ width: 'calc(100% - 48px)' }}>
              <Flex
                justify="space-between"
                className="header leading-[22px] h-[22px]"
              >
                <div
                  className="text-[16px] font-semibold  mr-[4px] "
                  style={{
                    color: 'var(--ag-ant-color-text-base)',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    width: 0,
                    flex: 1,
                  }}
                >
                  {item.name}
                </div>
                <Flex style={{ color: 'var(--ag-ant-color-text)' }}>
                  {!!item.tools?.length && (
                    <span
                      className="mr-[4px] cursor-pointer"
                      onClick={toggleFold}
                    >
                      {$i18n.get({
                        id: 'main.pages.App.components.MCPSelector.MCPServerListItem.index.tool',
                        dm: '工具',
                      })}

                      {item.tools.length}
                    </span>
                  )}
                  {item.status === McpStatus.DISABLED ? null : (
                    <IconFont
                      type={'spark-down-line'}
                      isCursorPointer
                      className={`${styles['fold-icon']} ${
                        folded ? '' : styles.rotated
                      }`}
                      onClick={toggleFold}
                    ></IconFont>
                  )}
                </Flex>
              </Flex>
              <Typography.Paragraph
                className={styles.desc}
                style={{ marginBottom: 0 }}
                ellipsis={{ rows: 1, tooltip: renderTooltip(item.description) }}
              >
                {item.description}
              </Typography.Paragraph>
            </div>
          </Flex>
        </Flex>
      </div>
      {!folded && !!item.tools?.length && (
        <div
          ref={toolsContainerRef}
          className={`${styles['tools-container']}`}
          style={{
            borderTop: 'none',
            padding: '8px 12px',
            maxHeight: `${maxHeight}px`,
            overflow: 'hidden',
            display: folded ? 'none' : 'block',
          }}
        >
          {
            <Flex gap={9} style={{ marginLeft: 20 }} vertical>
              {item.tools.map((tool) => (
                <Flex
                  className="h-[60px] flex-1 rounded-[6px]"
                  key={tool.name}
                  align="center"
                >
                  {selectMode === 'tool' && (
                    <Radio
                      checked={selectedTool?.name === tool.name}
                      onChange={(e) => {
                        if (e.target.checked) {
                          props.onSelectTool?.(tool, item);
                        }
                      }}
                      disabled={item.status === McpStatus.DISABLED}
                    ></Radio>
                  )}
                  {selectMode === 'server' && (
                    <Flex
                      gap={8}
                      className="h-[60px] flex-1 rounded-[6px]"
                      style={{
                        border:
                          '1px solid var(--ag-ant-color-border-secondary)',
                        padding: '8px 12px',
                      }}
                      align="center"
                    >
                      <Flex
                        align="center"
                        className="h-[20px] w-[20px] rounded-[4px]"
                        style={{
                          backgroundColor: 'var(--ag-ant-color-text-base)',
                        }}
                      >
                        <IconFont
                          className="w-full h-full rounded-[4px]"
                          type="spark-tool-line"
                          style={{ color: 'var(--ag-ant-color-bg-base)' }}
                        ></IconFont>
                      </Flex>
                      <div className="flex-1">
                        <Flex justify="space-between" className="header">
                          <div
                            className="text-[16px] font-semibold leading-6"
                            style={{ color: 'var(--ag-ant-color-text-base)' }}
                          >
                            <Typography.Paragraph
                              className={styles.desc}
                              style={{ marginBottom: 0, marginTop: 0 }}
                              ellipsis={{
                                rows: 1,
                                tooltip: renderTooltip(tool.name),
                              }}
                            >
                              {tool.name}
                            </Typography.Paragraph>
                          </div>
                        </Flex>
                        <Typography.Paragraph
                          className={styles.desc}
                          style={{ marginBottom: 0 }}
                          ellipsis={{
                            rows: 1,
                            tooltip: renderTooltip(tool.description),
                          }}
                        >
                          {tool.description}
                        </Typography.Paragraph>
                      </div>
                    </Flex>
                  )}
                  {selectMode === 'tool' && (
                    <Flex
                      gap={8}
                      className="h-[72px] flex-1 rounded-[6px]"
                      style={{
                        border:
                          '1px solid var(--ag-ant-color-border-secondary)',
                        padding: '8px 12px',
                      }}
                      vertical
                    >
                      <Flex align="center" gap={12} className="h-[28px]">
                        <Flex
                          align="center"
                          className="h-[20px] w-[20px] rounded-[4px]"
                          style={{
                            backgroundColor: 'var(--ag-ant-color-text-base)',
                          }}
                        >
                          <IconFont
                            className="w-full h-full rounded-[4px]"
                            type="spark-tool-line"
                            style={{ color: 'var(--ag-ant-color-bg-base)' }}
                          ></IconFont>
                        </Flex>
                        <div
                          className="text-[16px] font-semibold leading-6"
                          style={{ color: 'var(--ag-ant-color-text-base)' }}
                        >
                          <Typography.Paragraph
                            className={styles.desc}
                            style={{ marginBottom: 0, marginTop: 0 }}
                            ellipsis={{
                              rows: 1,
                              tooltip: renderTooltip(tool.name),
                            }}
                          >
                            {tool.name}
                          </Typography.Paragraph>
                        </div>
                      </Flex>
                      <Typography.Paragraph
                        className={styles.desc}
                        style={{ marginBottom: 0, marginTop: 0 }}
                        ellipsis={{
                          rows: 1,
                          tooltip: renderTooltip(tool.description),
                        }}
                      >
                        {tool.description}
                      </Typography.Paragraph>
                    </Flex>
                  )}
                </Flex>
              ))}
            </Flex>
          }
        </div>
      )}
    </div>
  );
};
