import Markdown from "react-markdown";

export interface MessageItem {
  role: 'user' | 'assistant';
  content: string;
}

interface MessageProps {
  message: MessageItem;
}

export default function Message({message}: MessageProps) {
  return (
    <div className="mb-l">
      <div className="font-bold">{message.role === 'user' ? '🧑‍💻 You' : '🤖 Assistant'}</div>
      <div>
        <Markdown>
          {message.content}
        </Markdown>
      </div>
    </div>
  )
};