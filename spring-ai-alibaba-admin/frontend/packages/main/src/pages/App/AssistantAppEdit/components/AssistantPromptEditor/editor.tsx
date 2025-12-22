import $i18n from '@/i18n';
import { Dropdown, IconFont, SlateEditor, Tag } from '@spark-ai/design';
import { EditorRefProps } from '@spark-ai/design/dist/components/commonComponents/SlateEditor';
import { useMount, useSetState, useUnmount } from 'ahooks';
import { ConfigProvider, Flex, message } from 'antd';
import { default as classNames, default as cls } from 'classnames';
import { debounce } from 'lodash-es';
import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useLayoutEffect,
  useRef,
} from 'react';
import { promptEventBus } from './eventBus';
import styles from './index.module.less';

interface IProps {
  variables: Array<{ label: string; code: string }>;
  prompt: string;
  setPrompt: (val: string) => void;
  onBlur?: () => void;
  maxTokenContext: number;
  bindId: 'main' | 'optimize';
}
export const AssistantPromptEditor = forwardRef((props: IProps, ref: any) => {
  const { componentDisabled } = ConfigProvider.useConfig();
  const { variables, bindId } = props;
  const [state, setState] = useSetState({
    expand: false,
    items: [] as {
      key: string;
      label: string;
      disabled: boolean;
      onClick: () => void;
    }[],
    visibleCount: 0,
  });
  const editorRef = useRef<EditorRefProps>(null);
  const headerRef = useRef<null | HTMLDivElement>(null);
  const addVarTag = useCallback((code: string) => {
    if (!editorRef.current) return;
    if (`\$\{${code}\}`.length > props.maxTokenContext) {
      message.warning(
        $i18n.get(
          {
            id: 'main.pages.App.AssistantAppEdit.components.AssistantPromptEditor.editor.maxInputCharacters',
            dm: '最多可输入{var1}字',
          },
          { var1: props.maxTokenContext },
        ),
      );
      return;
    }
    editorRef.current?._insertNodes({
      type: 'var',
      label: `label`,
      code,
      children: [{ text: '' }],
    });
  }, []);

  const setEditorCon = (val: string) => {
    editorRef.current?._setEditorContentByStr(val);
    props.setPrompt(val);
  };

  useImperativeHandle(ref, () => ({
    setEditorCon,
  }));

  // calculate the visible tags count
  const calculateVisibleTags = useCallback(
    (header: HTMLElement, children: Element[]) => {
      const extraWidth = 32; // width of Dropdown

      // the first element is the description text, thus there's no need to hide tags
      if (children.length < 2) return 0;

      let totalWidth = (children[0] as HTMLElement).offsetWidth; // width of description text
      let visibleCount = 0;

      // calculate how many tags can be contained in the container
      for (let i = 1; i < children.length; i++) {
        // skip the Dropdown element
        if (
          (children[i] as HTMLElement).classList.contains(
            'dropdown-menu-trigger',
          )
        )
          continue;

        const tagStyle =
          (children[i] as any).currentStyle ||
          window.getComputedStyle(children[i] as HTMLElement);
        const tagWidth =
          (children[i] as HTMLElement).offsetWidth +
          parseFloat(tagStyle.marginRight);
        // add the width of tag and Dropdown, check if it exceeds the container's width
        if (totalWidth + tagWidth + extraWidth > header.offsetWidth) {
          break;
        }

        totalWidth += tagWidth;
        visibleCount++;
      }

      return Math.min(visibleCount, variables.length);
    },
    [variables],
  );

  // check if the variable has been used or not
  const isVariableUsed = useCallback(
    (variableLabel: string) => {
      if (!props.prompt) return false;
      return props.prompt.includes(variableLabel);
    },
    [props.prompt],
  );

  const calcHeaderEllipsis = useCallback(() => {
    if (!headerRef.current) {
      // if the page is not rendered, retry
      setTimeout(() => {
        calcHeaderEllipsis();
      }, 100);
      return;
    }
    /**Calculate whether to show ellipsis based on header width and variables' width */
    const header = headerRef.current;
    const children = Array.from(header.children);
    // calculate the visible tags count
    const newVisibleCount = calculateVisibleTags(header, children);

    // Only update the state when the visible count actually changes

    // Check if there is overflow
    if (newVisibleCount < variables.length) {
      // There is overflow, directly create dropdown menu items
      const menuItems = variables.slice(newVisibleCount).map((item: any) => ({
        key: item.code,
        label: item.label,
        disabled: isVariableUsed(item.label),
        onClick: () => {
          addVarTag(item.code);
        },
      }));

      setState({
        items: menuItems,
        visibleCount: newVisibleCount,
      });
    } else {
      // No overflow
      setState({
        items: [],
        visibleCount: variables.length,
      });
    }
  }, [variables, isVariableUsed, addVarTag, calculateVisibleTags]);

  // Use debounce function to wrap handleResizeChange
  const debouncedHandleResizeChange = useCallback(
    debounce(calcHeaderEllipsis, 50),
    [calcHeaderEllipsis],
  );

  useEffect(() => {
    if (state.items.length === 0) return; // No items, no need to update

    setState((prevState) => ({
      items: prevState.items.map((item) => {
        return {
          ...item,
          disabled: isVariableUsed(item.label),
        };
      }),
    }));
  }, [props.prompt]);

  useLayoutEffect(() => {
    const resizeObserver = new ResizeObserver(debouncedHandleResizeChange);

    // Start observing the container
    if (headerRef.current) {
      resizeObserver.observe(headerRef.current);
    }

    return () => {
      if (headerRef.current) {
        resizeObserver.unobserve(headerRef.current);
      }
    };
  }, [debouncedHandleResizeChange]);

  useMount(() => {
    if (bindId === 'main') {
      promptEventBus.on('insertPromptFragment', (template) => {
        editorRef.current?._insertFragment?.(template, true);
      });
      promptEventBus.on('setEditorCon', setEditorCon); // Set editor content, generally used for initialization or version switching
    }
  });

  useUnmount(() => {
    if (bindId === 'main') {
      promptEventBus.removeAllListeners();
    }
  });

  // Helper function for rendering tags
  const renderTag = (
    item: { label: string; code: string },
    isHidden: boolean,
  ) => {
    const disabled = isVariableUsed(item.label) || componentDisabled;
    return (
      <Tag
        color={disabled ? 'mauve' : 'purple'}
        key={isHidden ? `hidden-${item.code}` : item.code}
        onClick={() => {
          if (disabled || isHidden) return;
          addVarTag(item.code);
        }}
        className={cls(styles.tag, {
          [styles.disabled]: disabled,
        })}
        style={{
          visibility: isHidden ? 'hidden' : 'visible',
          position: isHidden ? 'absolute' : 'static',
          pointerEvents: isHidden ? 'none' : 'auto',
        }}
      >
        {item.label}
      </Tag>
    );
  };

  useEffect(() => {
    calcHeaderEllipsis();
  }, [variables]);

  return (
    <Flex
      className={cls(styles.promptEditor, {
        [styles.expanded]: state.expand,
        [styles.noHead]: !variables.length,
      })}
      vertical
    >
      {!!variables.length && (
        <div className={styles.header} ref={headerRef}>
          <span className={styles.desc}>
            {$i18n.get(
              {
                id: 'main.pages.App.AssistantAppEdit.components.AssistantPromptEditor.editor.introduceVariables',
                dm: '可引入{var1}项变量（可通过点击添加）：',
              },
              { var1: variables.length },
            )}
          </span>

          {/* Render visible tags */}
          {variables
            .slice(0, state.visibleCount)
            .map((item) => renderTag(item, false))}

          {/* Display dropdown menu */}
          {state.items.length > 0 && (
            <Dropdown
              menu={{ items: state.items }}
              className="dropdown-menu-trigger"
            >
              <Tag color="mauve" className="px-[3px]">
                <IconFont type="spark-more-line" size={'small'}></IconFont>
              </Tag>
            </Dropdown>
          )}

          {/* Hide overflow tags */}
          {variables
            .slice(state.visibleCount)
            .map((item) => renderTag(item, true))}
        </div>
      )}
      <div className="overflow-y-auto m-[12px_0]">
        <SlateEditor
          disabled={componentDisabled}
          ref={editorRef}
          value={props.prompt}
          onChange={(val) => {
            props.setPrompt(val || '');
          }}
          wordLimit={props.maxTokenContext}
          renderVarLabel={(code) => {
            return `$\{${code}\}`;
          }}
          placeholder={$i18n.get({
            id: 'work-station-app.pages.App.AssistantAppEdit.components.AssistantPromptEditor.editor.writeSystemPromptIncludingRoleSettingTaskObjectiveAbilityAndReplyRequirements',
            dm: '在这里编写系统提示词，包括角色设定、任务目标、具备的能力及回复的要求与限制等，好的提示词会直接影响智能体效果',
          })}
        ></SlateEditor>
      </div>
      <div
        className={classNames('text-[12px] leading-[20px] w-full text-right')}
        style={{
          color: 'var(--ag-ant-color-text-quaternary)',
        }}
      >
        {props.prompt?.length}&nbsp;/&nbsp;{props.maxTokenContext}
      </div>
    </Flex>
  );
});
