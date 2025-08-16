import { useStore } from '@/flow/context';
import $i18n from '@/i18n';
import { SlateEditor } from '@spark-ai/design';
import { EditorRefProps } from '@spark-ai/design/dist/components/commonComponents/SlateEditor';
import { useSetState } from 'ahooks';
import { message, Typography } from 'antd';
import classNames from 'classnames';
import React, {
  ForwardedRef,
  forwardRef,
  memo,
  useCallback,
  useImperativeHandle,
  useRef,
} from 'react';
import CustomIcon from '../CustomIcon';
import VariableTreeSelect, { IVarTreeItem } from '../VariableTreeSelect';
import './index.less';

const defaultPosition = { x: 0, y: 0 };

const getSelectionPosition = (ele?: null | HTMLDivElement) => {
  let selection = window.getSelection();
  if (ele && selection && selection.rangeCount > 0) {
    try {
      let range = selection.getRangeAt(0);
      let rect = range.getClientRects()[0];
      let inputRect = ele.getBoundingClientRect();
      let x = rect.left + rect.width - inputRect.left;
      let y = rect.top + rect.height - inputRect.top;
      return { x, y };
    } catch (err) {
      return defaultPosition;
    }
  } else {
    return defaultPosition;
  }
};

export interface IVariableInputReferProps {
  setEditorValue: (value: string) => void;
}

export default memo(
  forwardRef(
    (
      {
        variableList,
        value,
        onChange,
        maxLength = 100000,
        disabled,
      }: {
        variableList: IVarTreeItem[];
        value?: string;
        maxLength?: number;
        onChange?: (value?: string) => void;
        disabled?: boolean;
      },
      ref: ForwardedRef<IVariableInputReferProps>,
    ) => {
      const nodeSchemaMap = useStore((state) => state.nodeSchemaMap);
      const editorCon = useRef<HTMLDivElement>(null);
      const editorRef = useRef<EditorRefProps>(null);
      const [state, setState] = useSetState({
        showParams: false,
        position: defaultPosition,
      });

      const checkSlash = useCallback((e: any) => {
        switch (e.keyCode) {
          case 8:
          case 27:
            setState({ showParams: false });
            break;
          case 191:
          case 111:
            if (!e.shiftKey) {
              setState({
                showParams: true,
                position: getSelectionPosition(editorCon.current),
              });
            }
            break;
        }
      }, []);

      const addVarTag = (name: string) => {
        if (!editorRef.current) return;
        const text = editorRef.current.getEditorValue() || '';
        if (`${text}${name}`.length > maxLength) {
          message.warning(
            $i18n.get(
              {
                id: 'spark-flow.components.VarInputTextArea.index.maxInputChars',
                dm: '最多可输入{var1}字',
              },
              { var1: maxLength },
            ),
          );
          return;
        }
        const nameMatch = name.match(/\${([^}]+)}/);
        const code = nameMatch ? nameMatch[1] : name;

        editorRef.current?._insertNodes(
          {
            type: 'var',
            label: name,
            code,
            children: [{ text: '' }],
          },
          { deletePrefix: true },
        );
      };

      const renderVarLabel = useCallback(
        (code: string) => {
          const finalValue = code.replace(/[\[\]]/g, '');
          const list = finalValue.split('.');
          if (!list.length)
            return (
              <>
                {$i18n.get({
                  id: 'spark-flow.components.VarInputTextArea.index.invalidVariable',
                  dm: '无效变量',
                })}
              </>
            );
          const nodeId = list[0];
          let nodeLabel = '';
          let nodeType = '';
          const variableKey = list[list.length - 1];

          if (['sys', 'conversation'].includes(nodeId)) {
            nodeLabel =
              nodeId === 'sys'
                ? $i18n.get({
                    id: 'spark-flow.components.VarInputTextArea.index.builtinVariable',
                    dm: '内置变量',
                  })
                : $i18n.get({
                    id: 'spark-flow.components.VarInputTextArea.index.conversationVariable',
                    dm: '会话变量',
                  });
            nodeType = nodeId;
          } else {
            const targetNode = variableList?.find(
              (node) => node.nodeId === nodeId,
            );
            if (!targetNode)
              return (
                <>
                  {$i18n.get({
                    id: 'spark-flow.components.VarInputTextArea.index.invalidVariable',
                    dm: '无效变量',
                  })}
                </>
              );
            nodeLabel = targetNode.label;
            nodeType = targetNode.nodeType;
          }

          return (
            <span className="inline-flex gap-[2px] spark-flow-var-input-var-tag">
              <CustomIcon
                size="small"
                className="spark-flow-var-input-var-tag-icon"
                type={nodeSchemaMap[nodeType]?.iconType}
              />

              <Typography.Text
                ellipsis={{ tooltip: true }}
                className="spark-flow-var-input-var-tag-label"
              >
                {nodeLabel}
              </Typography.Text>
              <Typography.Text
                ellipsis={{ tooltip: variableKey }}
                className="spark-flow-var-input-var-tag-key"
              >
                {`/${variableKey}`}
              </Typography.Text>
            </span>
          );
        },
        [variableList, nodeSchemaMap],
      );

      useImperativeHandle(ref, () => ({
        setEditorValue: (value: string) => {
          editorRef.current?._setEditorContentByStr(value);
        },
      }));

      return (
        <div
          ref={editorCon}
          className={classNames('spark-flow-var-input-text-area relative', {
            ['spark-flow-var-input-text-area-disabled']: disabled,
          })}
        >
          <SlateEditor
            ref={editorRef}
            renderVarLabel={renderVarLabel}
            value={value}
            onChange={onChange}
            disabled={disabled}
            onKeyDown={disabled ? undefined : checkSlash}
            wordLimit={maxLength}
          />

          {state.showParams && (
            <VariableTreeSelect
              onChange={(val) => addVarTag(val.value)}
              defaultOpen
              onClose={() => setState({ showParams: false })}
              options={variableList}
            >
              <div
                className="absolute"
                style={{
                  left: state.position.x,
                  top: state.position.y,
                  width: 200,
                }}
              ></div>
            </VariableTreeSelect>
          )}
          <div className="spark-flow-var-input-text-area-footer flex-justify-between">
            <span>
              {$i18n.get({
                id: 'spark-flow.components.VarInputTextArea.index.insertVariableWithSlash',
                dm: '输入"/"插入变量',
              })}
            </span>
            <span></span>
          </div>
        </div>
      );
    },
  ),
);
