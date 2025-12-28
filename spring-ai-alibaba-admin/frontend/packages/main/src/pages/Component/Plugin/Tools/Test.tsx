import $i18n from '@/i18n';
import { testTool } from '@/services/plugin';
import { Button, CodeBlock, Drawer, IconFont } from '@spark-ai/design';
import { Collapse, CollapseProps, Flex } from 'antd';
import { useEffect, useState } from 'react';
import { InputParamItem } from '../components/InputParamsConfig';
import styles from './index.module.less';
export default function (props: {
  toolId: string;
  pluginId: string;
  inputParams: InputParamItem[];
}) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState<string>('');
  const [finalInput, setFinalInput] = useState<string>('');
  const [result, setResult] = useState('');

  useEffect(() => {
    if (open) {
      try {
        const _input = props.inputParams.reduce((acc, cur) => {
          return {
            ...acc,
            [cur.key as string]: '',
          };
        }, {});
        setInput(JSON.stringify(_input));
      } catch (error) {}
    } else {
      setInput('');
    }
  }, [open]);

  const items: CollapseProps['items'] = [
    {
      key: '1',
      label: $i18n.get({
        id: 'main.pages.Component.Plugin.Tools.Test.input',
        dm: '入参',
      }),
      classNames: { header: styles.collapse },
      children: (
        <Flex gap={20} vertical>
          <CodeBlock
            // @ts-ignore
            onChange={(value: string) => {
              setFinalInput(value);
            }}
            value={input}
            language="json"
          />
          <Button
            type="primary"
            className="self-start mb-2"
            onClick={() => {
              const _input = JSON.parse(finalInput);
              testTool(props.pluginId, props.toolId, _input).then((res) => {
                setResult(JSON.stringify(res));
              });
            }}
          >
            {$i18n.get({
              id: 'main.pages.Component.Plugin.Tools.Test.startRunning',
              dm: '开始运行',
            })}
          </Button>
        </Flex>
      ),
    },
    {
      key: '2',
      label: $i18n.get({
        id: 'main.pages.Component.Plugin.Tools.Test.output',
        dm: '出参',
      }),
      classNames: { header: styles.collapse },
      children: <CodeBlock value={result} language="json" />,
    },
  ];

  return (
    <>
      <Button iconType="spark-circlePlay-line" onClick={() => setOpen(true)}>
        {$i18n.get({
          id: 'main.pages.Component.Plugin.Tools.Test.testTool',
          dm: '测试工具',
        })}
      </Button>
      <Drawer
        title={$i18n.get({
          id: 'main.pages.Component.Plugin.Tools.Test.testTool',
          dm: '测试工具',
        })}
        width={480}
        open={open}
        onClose={() => setOpen(false)}
      >
        <div>
          <Collapse
            items={items}
            defaultActiveKey={['1', '2']}
            size="small"
            bordered={false}
            expandIcon={({ isActive }) => (
              <IconFont type={isActive ? 'spark-down-line' : 'spark-up-line'} />
            )}
          />
        </div>
      </Drawer>
    </>
  );
}
