import $i18n from '@/i18n';
import { IValueType } from '@/types/work-flow';
import { javascript } from '@codemirror/lang-javascript';
import { Diagnostic, linter } from '@codemirror/lint';
import { EditorView } from '@codemirror/view';
import { getCommonConfig, Modal } from '@spark-ai/design';
import { vscodeDark, vscodeLight } from '@uiw/codemirror-theme-vscode';
import ReactCodeMirror from '@uiw/react-codemirror';
import classNames from 'classnames';
import React, { useMemo, useState } from 'react';
import CustomIcon from '../CustomIcon';
import './index.less';

interface CodeInputProps {
  value?: string;
  onChange?: (value: string) => void;
  type: IValueType;
  height?: string;
  className?: string;
  isCompact?: boolean;
  disabled?: boolean;
}

/**
 * Get more precise JavaScript value type
 * @param value The value to check type for
 * @returns Type string
 */
const getDetailedType = (value: any): string => {
  if (value === null) return 'null';
  if (value === undefined) return 'undefined';

  if (Array.isArray(value)) {
    if (value.length === 0)
      return $i18n.get({
        id: 'spark-flow.components.CodeInput.index.arrayEmpty',
        dm: 'Array(空数组)',
      });
    const elementTypes = new Set(value.map((item) => getDetailedType(item)));
    if (elementTypes.size === 1) {
      return `Array<${Array.from(elementTypes)[0]}>`;
    } else {
      return $i18n.get(
        {
          id: 'spark-flow.components.CodeInput.index.arrayMixed',
          dm: 'Array<混合类型>',
        },
        {},
      );
    }
  }

  if (typeof value === 'object') {
    if (value instanceof Date) return 'Date';
    if (value instanceof RegExp) return 'RegExp';
    if (value instanceof Map) return 'Map';
    if (value instanceof Set) return 'Set';
    if (
      value.name &&
      value.type &&
      typeof value.name === 'string' &&
      typeof value.type === 'string'
    ) {
      return $i18n.get({
        id: 'spark-flow.components.CodeInput.index.fileObject',
        dm: 'File样式对象',
      });
    }
    return 'Object';
  }

  if (typeof value === 'number') {
    if (isNaN(value)) return 'NaN';
    if (!isFinite(value)) return value > 0 ? 'Infinity' : '-Infinity';
  }

  return typeof value;
};

/**
 * Validate if JSON value matches the specified type
 * @param value JSON string
 * @param type Expected type
 * @returns Array of error messages, empty array if validation passes
 */
const validateValueByType = (value: string, type: IValueType): string[] => {
  const errors: string[] = [];
  try {
    const parsedValue = JSON.parse(value);
    if (type.startsWith('Array<')) {
      if (!Array.isArray(parsedValue)) {
        errors.push(
          $i18n.get(
            {
              id: 'spark-flow.components.CodeInput.index.expectArrayGotVar1',
              dm: '期望类型为数组，但得到了{var1}',
            },
            { var1: getDetailedType(parsedValue) },
          ),
        );
        return errors;
      }

      const elementType = type.substring(6, type.length - 1);
      parsedValue.forEach((item, index) => {
        if (elementType === 'Object') {
          if (
            typeof item !== 'object' ||
            item === null ||
            Array.isArray(item)
          ) {
            errors.push(
              $i18n.get(
                {
                  id: 'spark-flow.components.CodeInput.index.arrayItemNotObjectVar1Var2',
                  dm: '数组中第{var1}项不是对象类型，而是{var2}',
                },
                { var1: index + 1, var2: getDetailedType(item) },
              ),
            );
          }
        } else if (elementType === 'String') {
          if (typeof item !== 'string') {
            errors.push(
              $i18n.get(
                {
                  id: 'spark-flow.components.CodeInput.index.arrayItemNotStringVar1Var2',
                  dm: '数组中第{var1}项不是字符串类型，而是{var2}',
                },
                { var1: index + 1, var2: getDetailedType(item) },
              ),
            );
          }
        } else if (elementType === 'Number') {
          if (typeof item !== 'number' || isNaN(item)) {
            errors.push(
              $i18n.get(
                {
                  id: 'spark-flow.components.CodeInput.index.arrayItemNotNumberVar1Var2',
                  dm: '数组中第{var1}项不是数字类型，而是{var2}',
                },
                { var1: index + 1, var2: getDetailedType(item) },
              ),
            );
          }
        } else if (elementType === 'Boolean') {
          if (typeof item !== 'boolean') {
            errors.push(
              $i18n.get(
                {
                  id: 'spark-flow.components.CodeInput.index.arrayItemNotBooleanVar1Var2',
                  dm: '数组中第{var1}项不是布尔类型，而是{var2}',
                },
                { var1: index + 1, var2: getDetailedType(item) },
              ),
            );
          }
        } else if (elementType === 'File') {
          if (typeof item !== 'object' || !item.name || !item.type) {
            errors.push(
              $i18n.get(
                {
                  id: 'spark-flow.components.CodeInput.index.arrayItemNotFileVar1Var2',
                  dm: '数组中第{var1}项不是文件类型，而是{var2}',
                },
                { var1: index + 1, var2: getDetailedType(item) },
              ),
            );
          }
        }
      });
    } else if (type === 'Object') {
      if (
        typeof parsedValue !== 'object' ||
        parsedValue === null ||
        Array.isArray(parsedValue)
      ) {
        errors.push(
          $i18n.get(
            {
              id: 'spark-flow.components.CodeInput.index.expectObjectGotVar1',
              dm: '期望类型为对象，但得到了{var1}',
            },
            { var1: getDetailedType(parsedValue) },
          ),
        );
      }
    } else if (type === 'String') {
      if (typeof parsedValue !== 'string') {
        errors.push(
          $i18n.get(
            {
              id: 'spark-flow.components.CodeInput.index.expectStringGotVar1',
              dm: '期望类型为字符串，但得到了{var1}',
            },
            { var1: getDetailedType(parsedValue) },
          ),
        );
      }
    } else if (type === 'Number') {
      if (typeof parsedValue !== 'number' || isNaN(parsedValue)) {
        errors.push(
          $i18n.get(
            {
              id: 'spark-flow.components.CodeInput.index.expectNumberGotVar1',
              dm: '期望类型为数字，但得到了{var1}',
            },
            { var1: getDetailedType(parsedValue) },
          ),
        );
      }
    } else if (type === 'Boolean') {
      if (typeof parsedValue !== 'boolean') {
        errors.push(
          $i18n.get(
            {
              id: 'spark-flow.components.CodeInput.index.expectBooleanGotVar1',
              dm: '期望类型为布尔值，但得到了{var1}',
            },
            { var1: getDetailedType(parsedValue) },
          ),
        );
      }
    } else if (type === 'File') {
      if (
        typeof parsedValue !== 'object' ||
        !parsedValue.name ||
        !parsedValue.type
      ) {
        errors.push(
          $i18n.get(
            {
              id: 'spark-flow.components.CodeInput.index.expectFileGotVar1',
              dm: '期望类型为文件，但得到了{var1}',
            },
            { var1: getDetailedType(parsedValue) },
          ),
        );
      }
    }
  } catch (error: any) {
    errors.push(
      $i18n.get(
        {
          id: 'spark-flow.components.CodeInput.index.jsonParseErrorVar1',
          dm: 'JSON解析错误:{var1}',
        },
        { var1: error.message },
      ),
    );
  }
  return errors;
};

const CodeEdit = ({
  value,
  height,
  onChange,
  type,
  showGutter = false,
  disabled,
}: {
  value?: string;
  height?: string;
  onChange: (value: string) => void;
  type: IValueType;
  showGutter?: boolean;
  disabled?: boolean;
}) => {
  const typeLinter = useMemo(() => {
    return linter((view: EditorView) => {
      const text = view.state.doc.toString();
      const diagnostics: Diagnostic[] = [];

      try {
        JSON.parse(text);
        const typeErrors = validateValueByType(text, type);
        if (typeErrors.length > 0) {
          typeErrors.forEach((error) => {
            diagnostics.push({
              from: 0,
              to: text.length,
              severity: 'error',
              message: error,
              actions: [],
            });
          });
        }
      } catch (error: any) {
        diagnostics.push({
          from: 0,
          to: text.length,
          severity: 'error',
          message: $i18n.get(
            {
              id: 'spark-flow.components.CodeInput.index.jsonParseErrorVar1',
              dm: 'JSON解析错误:{var1}',
            },
            { var1: error.message },
          ),
          actions: [],
        });
      }

      return diagnostics;
    });
  }, [type]);

  return (
    <ReactCodeMirror
      value={value}
      height={height}
      onChange={onChange}
      readOnly={disabled}
      theme={getCommonConfig().isDarkMode ? vscodeDark : vscodeLight}
      basicSetup={{
        lineNumbers: showGutter,
        foldGutter: showGutter,
      }}
      extensions={[javascript({ typescript: false, jsx: false }), typeLinter]}
    />
  );
};

export const CodeInputModal = ({
  value,
  onOk,
  type,
  onClose,
  disabled,
}: {
  value?: string;
  onOk: (value?: string) => void;
  type: IValueType;
  onClose: () => void;
  disabled?: boolean;
}) => {
  const [codeVal, setCodeVal] = useState(value);

  const handleOk = () => {
    onOk(codeVal);
  };

  return (
    <Modal
      onCancel={onClose}
      title={$i18n.get({
        id: 'spark-flow.components.CodeInput.index.jsonEdit',
        dm: 'JSON编辑',
      })}
      open
      width={960}
      onOk={handleOk}
    >
      <div className="spark-flow-code-input-modal-content spark-flow-code-input">
        <CodeEdit
          showGutter
          height={'60vh'}
          onChange={setCodeVal}
          value={codeVal}
          type={type}
          disabled={disabled}
        />
      </div>
    </Modal>
  );
};

const CodeInput: React.FC<CodeInputProps> = ({
  value,
  onChange,
  type,
  height = '200px',
  className,
  isCompact,
  disabled,
}) => {
  const [open, setOpen] = useState(false);

  const handleChange = (value: string) => {
    onChange?.(value);
  };

  const handleOk = (value?: string) => {
    onChange?.(value as string);
    setOpen(false);
  };

  return (
    <>
      <div
        className={classNames('spark-flow-code-input', className, {
          'spark-flow-code-input-compact': isCompact,
          'spark-flow-code-input-disabled': disabled,
        })}
      >
        <CodeEdit
          value={value}
          height={height}
          onChange={handleChange}
          type={type}
          disabled={disabled}
        />

        <CustomIcon
          className="spark-flow-code-input-expand-icon"
          isCursorPointer
          type="spark-enlarge-line"
          onClick={() => setOpen(true)}
        />
      </div>
      {open && (
        <CodeInputModal
          value={value}
          onOk={handleOk}
          type={type}
          onClose={() => setOpen(false)}
          disabled={disabled}
        />
      )}
    </>
  );
};

export default CodeInput;
