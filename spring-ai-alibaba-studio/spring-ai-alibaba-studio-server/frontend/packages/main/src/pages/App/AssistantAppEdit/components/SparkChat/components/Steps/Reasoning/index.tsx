import { Markdown } from '@spark-ai/chat';

export default (props: { reasoning: string }) => {
  return <Markdown content={props.reasoning || ''} baseFontSize={12} />;
};
