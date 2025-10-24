import defaultSettings from '@/defaultSettings';
import $i18n from '@/i18n';
import { KnowledgeSelectorDrawer } from '@/pages/App/components/KnowledgeSelector';
import { IKnowledgeListItem } from '@/types/knowledge';
import { Button, IconFont, Switch, Tag } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Divider, Flex, InputNumber } from 'antd';
import cls from 'classnames';
import { useContext, useEffect } from 'react';
import { AssistantAppContext } from '../../AssistantAppContext';
import { RAG_PROMPT_TEMPLATE } from '../AssistantConfig';
import { promptEventBus } from '../AssistantPromptEditor/eventBus';
import SelectedConfigItem from '../SelectedConfigItem';
import styles from './index.module.less';

export const KNOWLEDGE_BASE_MAX_LIMIT =
  defaultSettings.agentKnowledgeBaseMaxLimit;

export function SelectedKnowledgeBaseItem({
  item,
  handleRemoveKnowledge,
}: {
  item: IKnowledgeListItem;
  handleRemoveKnowledge: (item: IKnowledgeListItem) => void;
}) {
  return (
    <SelectedConfigItem
      iconType="spark-readingPreview-line"
      name={item.name}
      rightArea={
        <Flex gap={12}>
          <IconFont
            type="spark-delete-line"
            isCursorPointer
            onClick={() => {
              handleRemoveKnowledge(item);
            }}
          ></IconFont>
        </Flex>
      }
    ></SelectedConfigItem>
  );
}

export default function KnowledgeBaseSelectorComp() {
  const { appState, onAppConfigChange } = useContext(AssistantAppContext);
  const { file_search } = appState.appBasicConfig?.config || {};
  const { kbs = [] } = file_search || {};
  const [state, setState] = useSetState({
    expand: false,
    selectVisible: false,
  });

  const onSelectKnowledges = (kbs: IKnowledgeListItem[]) => {
    onAppConfigChange({
      file_search: {
        ...appState.appBasicConfig?.config.file_search,
        kbs,
        enable_search: kbs.length > 0 ? file_search?.enable_search : false,
      },
    });
    setState({ selectVisible: false });
  };

  useEffect(() => {
    if (kbs.length) {
      setState({ expand: true });
    }
  }, [kbs]);

  const onRemoveKnowledge = (val: string) => {
    onSelectKnowledges(kbs.filter((kb) => kb.kb_id !== val));
  };

  return (
    <Flex vertical gap={6} className="mb-[20px]">
      <Flex justify="space-between">
        <Flex
          gap={8}
          className="text-[13px] font-medium leading-[20px]"
          style={{ color: 'var(--ag-ant-color-text)' }}
          align="center"
        >
          <span>
            {$i18n.get({
              id: 'main.components.KnowledgeSelectorComp.index.knowledgeBase',
              dm: '知识库',
            })}
          </span>
          <span
            className="text-[12px] leading-[20px]"
            style={{ color: 'var(--ag-ant-color-text-tertiary)' }}
          >
            {kbs.length}/{KNOWLEDGE_BASE_MAX_LIMIT}
          </span>
          <Switch
            size="small"
            className="ml-[8px]"
            checked={file_search?.enable_search}
            onChange={(val) => {
              const prompt = appState.appBasicConfig?.config.instructions || '';
              const file_search = appState.appBasicConfig?.config.file_search;
              if (val && !prompt.includes('${documents}')) {
                promptEventBus.emit(
                  'insertPromptFragment',
                  RAG_PROMPT_TEMPLATE,
                );
              }
              onAppConfigChange({
                file_search: { ...file_search, enable_search: val },
              });
            }}
          ></Switch>
        </Flex>
        <span>
          <Button
            style={{ padding: 0 }}
            onClick={() => setState({ selectVisible: true })}
            iconType="spark-plus-line"
            type="text"
            size="small"
            disabled={!file_search?.enable_search}
          >
            {$i18n.get({
              id: 'main.components.KnowledgeSelectorComp.index.knowledgeBase',
              dm: '知识库',
            })}
          </Button>
          <Divider type="vertical" className="ml-[16px] mr-[16px]"></Divider>
          <IconFont
            onClick={() => setState({ expand: !state.expand })}
            className={cls(
              styles['expand-btn'],
              !state.expand && styles.hidden,
            )}
            type="spark-up-line"
            isCursorPointer
          />
        </span>
      </Flex>
      {state.expand && (
        <>
          <div className={styles.desc}>
            {$i18n.get({
              id: 'main.components.KnowledgeSelectorComp.index.addVariable',
              dm: '提示词中需要增加',
            })}
            <Tag color="mauve" className="ml-[8px]">
              {'${documents}'}
            </Tag>
            {$i18n.get({
              id: 'main.components.KnowledgeSelectorComp.index.variableToEnable',
              dm: '的变量，以实现基于知识库切片的大模型生成。',
            })}
          </div>
          <Flex vertical gap={8}>
            {kbs.map(
              (item) =>
                item && (
                  <SelectedKnowledgeBaseItem
                    handleRemoveKnowledge={() => onRemoveKnowledge(item.kb_id)}
                    item={item}
                    key={item.kb_id}
                  />
                ),
            )}
          </Flex>
          {kbs.length > 0 && (
            <>
              <Divider className="my-[12px]" />
              <Flex vertical gap={12}>
                <Flex align="center" justify="space-between">
                  <Flex align="center" gap={8}>
                    <span
                      className="text-[13px] font-medium leading-[20px]"
                      style={{ color: 'var(--ag-ant-color-text)' }}
                    >
                      Top-K
                    </span>
                    <span
                      className="text-[12px] leading-[20px]"
                      style={{ color: 'var(--ag-ant-color-text-tertiary)' }}
                    >
                      ({$i18n.get({ id: 'main.components.KnowledgeSelectorComp.index.returnDocCount', dm: '返回文档数' })})
                    </span>
                  </Flex>
                  <InputNumber
                    min={1}
                    max={20}
                    placeholder="0"
                    value={file_search?.top_k}
                    onChange={(val) => {
                      onAppConfigChange({
                        file_search: {
                          ...file_search,
                          top_k: val || undefined,
                        },
                      });
                    }}
                    style={{ width: 120 }}
                  />
                </Flex>
                <div className={styles.desc}>
                  {$i18n.get({
                    id: 'main.components.KnowledgeSelectorComp.index.topKDesc',
                    dm: '从所有知识库的检索结果中返回前 K 个最相关的文档',
                  })}
                </div>
                <Flex align="center" justify="space-between">
                  <Flex align="center" gap={8}>
                    <span
                      className="text-[13px] font-medium leading-[20px]"
                      style={{ color: 'var(--ag-ant-color-text)' }}
                    >
                      {$i18n.get({
                        id: 'main.components.KnowledgeSelectorComp.index.similarityThreshold',
                        dm: '相似度阈值',
                      })}
                    </span>
                  </Flex>
                  <InputNumber
                    min={0}
                    max={1}
                    step={0.1}
                    placeholder="0"
                    value={file_search?.similarity_threshold}
                    onChange={(val) => {
                      onAppConfigChange({
                        file_search: {
                          ...file_search,
                          similarity_threshold: val || undefined,
                        },
                      });
                    }}
                    style={{ width: 120 }}
                  />
                </Flex>
                <div className={styles.desc}>
                  {$i18n.get({
                    id: 'main.components.KnowledgeSelectorComp.index.thresholdDesc',
                    dm: '过滤掉相似度低于此阈值的文档，范围 0-1',
                  })}
                </div>
              </Flex>
            </>
          )}
        </>
      )}
      {state.selectVisible && (
        <KnowledgeSelectorDrawer
          value={kbs}
          onClose={() => {
            setState({ selectVisible: false });
          }}
          onOk={onSelectKnowledges}
        ></KnowledgeSelectorDrawer>
      )}
    </Flex>
  );
}
