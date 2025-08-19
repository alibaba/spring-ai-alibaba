import $i18n from '@/i18n';
import { baseURL } from '@/request/request';
import { IAppType } from '@/services/appComponent';
import {
  Button,
  CodeBlock,
  CollapsePanel,
  Drawer,
  IconFont,
  message,
} from '@spark-ai/design';
import copy from 'copy-to-clipboard';
import { useMemo, useState } from 'react';

export default function (props: { appId: string; type: IAppType }) {
  const [open, setOpen] = useState(false);

  const title = $i18n.get({
    id: 'main.pages.App.components.ChannelConfig.index.viewApi',
    dm: '查看API',
  });

  const codeText = useMemo(() => {
    return props.type === IAppType.WORKFLOW
      ? `curl -X 'POST' \\
'${baseURL.get()}/api/v1/apps/workflow/completions' \\
-H 'accept: */*' \\
-H 'Authorization: Bearer sk-xxxxxxxxxxxxxxxxxx' \\
-H 'Content-Type: application/json' \\
-d '{
  "app_id": "${props.appId}",
  "inputParams": [
    {
      "key": "query",
      "type": "String",
      "desc": "user questions",
      "required": false,
      "source": "sys",
      "value": "What is Spring AI Alibaba?"
    }
  ],
  "stream": true
}'`
      : `curl -X 'POST' \\
'${baseURL.get()}/api/v1/apps/chat/completions' \\
-H 'accept: */*' \\
-H 'Authorization: Bearer sk-xxxxxxxxxxxxxxxxxx' \\
-H 'Content-Type: application/json' \\
-d '{
  "app_id": "${props.appId}",
  "messages": [
    {
      "role": "user",
      "content": "What is Spring AI Alibaba?",
      "content_type": "text"
    }
  ],
  "stream": true
}'`;
  }, [props.appId, props.type]);

  return (
    <>
      <Button
        onClick={() => setOpen(true)}
        type="text"
        size="small"
        className="self-center"
        icon={<IconFont type="spark-fileCode-line" />}
      >
        {title}
      </Button>
      <Drawer
        open={open}
        onClose={() => setOpen(false)}
        width={640}
        title={title}
      >
        <CollapsePanel
          title="curl"
          expandOnPanelClick={false}
          extra={
            <IconFont
              type="spark-copy-line"
              size="small"
              onClick={() => {
                copy(codeText);
                message.success(
                  $i18n.get({
                    id: 'main.pages.App.index.copySuccess',
                    dm: '复制成功',
                  }),
                );
              }}
            />
          }
        >
          <CodeBlock language={[]} value={codeText} readOnly />
        </CollapsePanel>
      </Drawer>
    </>
  );
}
