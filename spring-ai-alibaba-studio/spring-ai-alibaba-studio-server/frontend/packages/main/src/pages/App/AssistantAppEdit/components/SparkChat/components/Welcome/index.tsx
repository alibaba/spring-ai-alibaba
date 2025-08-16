import $i18n from '@/i18n';
import { ChatAnywhereRef } from '@spark-ai/chat';
import { IconFont } from '@spark-ai/design';
import { ConfigProvider, Flex } from 'antd';
import classNames from 'classnames';
import './index.less';
interface IProps {
  data: {
    prologue?: string;
    suggested_questions?: string[];
    name?: string;
    icon?: string;
    modalType?: 'textDialog' | 'textGenerate' | 'audio' | 'video';
  };
  sparkChatRef?: React.MutableRefObject<ChatAnywhereRef | undefined>;
  title?: string;
}

const CornerMark = ({
  modalType,
  diameter = 28,
  offset = -8,
}: {
  modalType: IProps['data']['modalType'];
  diameter?: number;
  offset?: number;
}) => {
  let iconType = 'spark-text-line';
  if (modalType === 'textDialog') {
    iconType = 'spark-text-line';
  }
  if (modalType === 'textGenerate') {
    iconType = 'spark-aiEdit-line';
  }
  if (modalType === 'audio') {
    iconType = 'spark-dial-line';
  }
  if (modalType === 'video') {
    iconType = 'spark-video-line';
  }
  return (
    <div
      className="absolute rounded-[14px] flex justify-center items-center border-2 border-white border-solid"
      style={{
        width: `${diameter}px`,
        height: `${diameter}px`,
        bottom: `${offset}px`,
        right: `${offset}px`,
        backgroundColor: 'var(--ag-ant-color-text-base)',
      }}
    >
      <IconFont
        type={iconType}
        style={{ color: 'var(--ag-ant-color-bg-base)' }}
        size="small"
      />
    </div>
  );
};

const DefaultAvatar = ({
  modalType,
}: {
  modalType: IProps['data']['modalType'];
}) => {
  return (
    <div className="relative w-[64px] h-[64px]">
      <img className="w-full h-full" src="/images/defaultAvatar.png" alt="" />
      <CornerMark modalType={modalType} diameter={24} offset={-4} />
    </div>
  );
};

export default function Welcome(props: IProps) {
  const { data } = props;
  const modalType = data.modalType;
  const { componentDisabled } = ConfigProvider.useConfig();
  if (!data.suggested_questions?.length && !data.prologue) {
    return (
      <Flex
        justify="center"
        align="center"
        style={{ marginTop: '20vh' }}
        vertical
      >
        <DefaultAvatar modalType={modalType} />
        <div
          className="text-[16px] text-center font-semibold mt-[24px]"
          style={{ color: 'var(--ag-ant-tooltip-bg)' }}
        >
          {props.title ||
            $i18n.get({
              id: 'main.pages.App.AssistantAppEdit.components.SparkChat.components.Welcome.index.inputQuestion',
              dm: '输入问题进行测试体验',
            })}
        </div>
      </Flex>
    );
  }
  return (
    <Flex
      justify="center"
      align="center"
      style={{
        marginTop: '20vh',
      }}
    >
      <Flex className="w-[400px]" vertical gap={12} align="center">
        {data.icon ? (
          <div className="relative w-[76px] h-[76px]">
            <img className="w-full h-full" src={data.icon} alt="" />
            <CornerMark modalType={modalType} />
          </div>
        ) : (
          <DefaultAvatar modalType={modalType} />
        )}
        <div
          className="text-[16px] text-center font-medium"
          style={{ color: 'var(--ag-ant-color-text)' }}
        >
          {data.name}
        </div>
        <div
          className="text-[14px]"
          style={{ color: 'var(--ag-ant-color-text-secondary)' }}
        >
          {data.prologue}
        </div>
        {(data.suggested_questions || []).map((query) => {
          return (
            <Flex
              key={query}
              className={classNames(
                'text-[14px] cursor-pointer px-[16px] py-[10px] rounded-[6px] w-full suggested-questions-item-wrapper',
                { 'cursor-not-allowed': componentDisabled },
              )}
              onClick={() => {
                if (componentDisabled) return;
                // @ts-ignore
                props.sparkChatRef?.current?.onInput.onSubmit({
                  query: query,
                });
              }}
              gap={12}
              justify="space-between"
              align="center"
            >
              <Flex gap={12} className="break-all">
                <img
                  src="/images/suggestQuestion.png"
                  className="w-[20px] h-[20px]"
                  alt=""
                />

                {query}
              </Flex>
              <div className="flex items-center justify-center w-[16px] h-[16px] rounded-[4px] suggested_questions-item-icon-wrapper">
                <IconFont type="spark-rightArrow-line" size="small" />
              </div>
            </Flex>
          );
        })}
      </Flex>
    </Flex>
  );
}
