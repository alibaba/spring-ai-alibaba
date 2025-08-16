import $i18n from '@/i18n';
import { getPluginToolList } from '@/services/plugin';
import { Plugin, PluginTool } from '@/types/plugin';
import { IconFont, renderTooltip } from '@spark-ai/design';
import { Checkbox, Flex, Typography } from 'antd';
import classNames from 'classnames';
import { useEffect, useRef, useState } from 'react';
import styles from './index.module.less';

interface IProps {
  item: Plugin;
  onSelectTool?: (item: PluginTool) => void;
  onRemoveTool?: (tool: PluginTool) => void;
  selectedTools?: PluginTool[];
  fetchList?: () => void;
}

export default (props: IProps) => {
  const { item, selectedTools } = props;
  const [toolsList, setToolsList] = useState<PluginTool[]>([]);
  const [pluginSelected, setPluginSelected] = useState<boolean>(false);
  const [folded, setFolded] = useState<boolean>(true);
  const [maxHeight, setMaxHeight] = useState<number>(0);
  const toolsContainerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!item.plugin_id) return;
    getPluginToolList(item.plugin_id).then((res) => {
      setToolsList(res.data.records);
    });
  }, [item]);
  useEffect(() => {
    setPluginSelected(
      !!selectedTools?.find((tool) => tool.plugin_id === item.plugin_id),
    );
  }, [item, selectedTools]);

  useEffect(() => {
    if (pluginSelected) {
      setFolded(false);
    }
  }, [pluginSelected]);

  useEffect(() => {
    if (toolsContainerRef.current) {
      setMaxHeight(folded ? 0 : toolsContainerRef.current.scrollHeight);
    }
  }, [folded]);

  const toggleFold = () => {
    setFolded(!folded);
  };

  return (
    <div>
      <div
        className={classNames(styles['plugin-list-wrapper'], {
          [styles.expanded]: !folded,
        })}
        style={{ padding: '12px 16px' }}
      >
        <Flex gap={8}>
          <Flex gap={8} className="w-full h-[52px] flex-1" align="center">
            <Flex align="center" className="h-[40px] w-[40px]">
              <Flex
                align="center"
                justify="center"
                className={classNames(
                  'h-[40px] w-[40px] rounded-[6px]',
                  styles['check-item'],
                )}
                style={{
                  border: '1px solid var(--ag-ant-color-border-secondary)',
                }}
              >
                <img src={'/images/plugin.svg'} alt="" />
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
                  {!!toolsList?.length && (
                    <span
                      className="mr-[4px] cursor-pointer"
                      onClick={toggleFold}
                    >
                      {$i18n.get({
                        id: 'main.pages.App.components.PluginSelector.PluginListItem.index.tool',
                        dm: '工具',
                      })}

                      {toolsList.length}
                    </span>
                  )}
                  {
                    <IconFont
                      type={'spark-down-line'}
                      isCursorPointer
                      className={`${styles['fold-icon']} ${
                        folded ? '' : styles.rotated
                      }`}
                      onClick={toggleFold}
                    ></IconFont>
                  }
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
      {!folded && !!toolsList?.length && (
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
              {toolsList.map((tool) => (
                <Flex
                  className="h-[60px] flex-1 rounded-[6px]"
                  key={tool.tool_id}
                  align="center"
                >
                  <Flex gap={8} flex={1}>
                    <Checkbox
                      checked={
                        !!selectedTools?.find((t) => t.tool_id === tool.tool_id)
                      }
                      onChange={(e) => {
                        if (e.target.checked) {
                          props.onSelectTool?.(tool);
                        } else {
                          props.onRemoveTool?.(tool);
                        }
                      }}
                      disabled={!tool.enabled}
                    ></Checkbox>
                    <Flex
                      flex={1}
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
                  </Flex>
                </Flex>
              ))}
            </Flex>
          }
        </div>
      )}
    </div>
  );
};
