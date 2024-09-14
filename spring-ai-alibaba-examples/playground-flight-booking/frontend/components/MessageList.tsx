import React, { useRef, useEffect } from 'react';
import Message, {MessageItem} from './Message';

interface MessageListProps {
  messages: MessageItem[];
  className?: string;
}

export default function MessageList({ messages, className }: MessageListProps) {
  const endOfMessagesRef = useRef<HTMLDivElement>(null);

  // Automatically scroll down whenever the messages change
  useEffect(() => {
    if (endOfMessagesRef.current) {
      endOfMessagesRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  return (
    <div className={className}>
      {messages.map((msg, index) => (
        <Message key={index} message={msg} />
      ))}
      <div ref={endOfMessagesRef} />
    </div>
  );
}