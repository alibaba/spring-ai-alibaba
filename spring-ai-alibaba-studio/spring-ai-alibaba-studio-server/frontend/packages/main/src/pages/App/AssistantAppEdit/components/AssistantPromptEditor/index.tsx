import $i18n from '@/i18n';
import { IAssistantAppDetailWithInfos } from '@/types/appManage';
import { useMount, useUnmount } from 'ahooks';
import { Flex } from 'antd';
import cls from 'classnames';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AssistantPromptEditor } from './editor';
import { promptEventBus } from './eventBus';
import styles from './index.module.less';
import Variables from './Variables';

interface IPromptProps {
  prompt: string;
  changePrompt: (val: string) => void;
  appBasicConfig: IAssistantAppDetailWithInfos | null;
  autoSaveConfig?: (val?: any, extraOpts?: any) => void;
  maxTokenContext: number;
  size?: string;
  className?: string;
}

export function AssistantPromptEditorWrap(props: IPromptProps) {
  const { prompt = '', appBasicConfig } = props;
  const editorRef = useRef(null as any);
  const promptCache = useRef(prompt);
  const [userDefinedVariables, setUserDefinedVariables] = useState<
    { label: string; code: string }[]
  >([]);

  useEffect(() => {
    promptCache.current = prompt;
  }, [prompt]);

  const { prompt_variables } = appBasicConfig?.config || {};

  const variables = useMemo(() => {
    const list: any[] = userDefinedVariables.map((value) => ({
      label: `\${${value.label}}`,
      code: value.code,
    }));
    const { file_search } = appBasicConfig?.config || {};
    if (file_search?.enable_search) {
      list.push({
        label: '${documents}',
        code: 'documents',
      });
    }
    return list;
  }, [appBasicConfig, userDefinedVariables]);

  const getEditorValue = () => {
    const ssml = editorRef.current?.getSSML();
    promptEventBus.setValue('getEditorValue', ssml);
  };

  const setEditorValue = (val: string) => {
    editorRef.current?.setEditorCon(val);
  };

  const onUserDefinedVariablesChange = useCallback(
    (userDefinedVariables: { label: string; code: string }[] = []) => {
      setUserDefinedVariables(userDefinedVariables);
    },
    [setUserDefinedVariables],
  );

  useMount(() => {
    setEditorValue(prompt);
    promptEventBus.on('getEditorValue', getEditorValue);
    promptEventBus.on('setEditorValue', setEditorValue);
    promptEventBus.on(
      'userDefinedVariablesChange',
      onUserDefinedVariablesChange,
    );
  });

  useUnmount(() => {
    promptEventBus.removeListener('getEditorValue', getEditorValue);
    promptEventBus.removeListener('setEditorValue', setEditorValue);
    promptEventBus.removeListener(
      'userDefinedVariablesChange',
      onUserDefinedVariablesChange,
    );
  });

  useEffect(() => {
    promptEventBus.setValue('getVariables', variables);
  }, [variables]);

  const handleChangePrompt = (val: string) => {
    if (promptCache.current === val) return;
    promptCache.current = val;
    props.changePrompt(val);
  };

  useEffect(() => {
    // Update the local highlighted variables list according to the changes of prompt_variables in the backend configuration
    setUserDefinedVariables(
      prompt_variables?.map((variable: any) => ({
        label: variable.name,
        code: variable.name,
      })) || [],
    );
  }, [prompt_variables]);

  return (
    <div
      className={cls(
        props.className,
        styles.formItem,
        styles[props.size || ''],
      )}
    >
      <Flex className={cls(styles.label, 'mb-[6px]')} justify="space-between">
        <div
          className="text-[13px] font-medium leading-[20px]"
          style={{ color: 'var(--ag-ant-color-text)' }}
        >
          {$i18n.get({
            id: 'main.pages.MCP.Detail.promptWords',
            dm: '提示词',
          })}
        </div>
      </Flex>
      <AssistantPromptEditor
        prompt={props.prompt}
        setPrompt={handleChangePrompt}
        ref={editorRef}
        variables={variables}
        maxTokenContext={props.maxTokenContext}
        bindId="main"
      />

      <div
        className={'text-[12px] font-normal leading-[24px] mt-[4px] mb-[24px]'}
        style={{
          color: 'var(--ag-ant-color-text-tertiary)',
        }}
      >
        {$i18n.get({
          id: 'main.pages.App.AssistantAppEdit.components.AssistantPromptEditor.index.variablesOptionsComeFromBelowVariableConfiguration',
          dm: '提示词中变量的选项来自下方“变量配置”，如需新增请在下方操作',
        })}
      </div>
      <Variables
        variables={variables}
        userDefinedVariables={userDefinedVariables}
        setUserDefinedVariables={setUserDefinedVariables}
      ></Variables>
    </div>
  );
}
