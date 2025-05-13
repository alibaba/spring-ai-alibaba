import React, { useState, useEffect } from "react";
import ReactDOM from "react-dom";
import Message from "./Message";
import "./index.css";

// 创建一个容器用于挂载消息
let messageContainer;

// 全局消息管理器
let messageId = 0; // 用于生成唯一 ID
const messageManager = {
  messages: [],

  addMessage({ type, content, duration }) {
    const id = messageId++;
    const newMessage = { id, type, content, duration };
    this.messages.push(newMessage);
    this.renderMessages();

    // 返回一个关闭函数
    return () => {
      this.removeMessage(id);
    };
  },

  removeMessage(id) {
    this.messages = this.messages.filter((msg) => msg.id !== id);
    this.renderMessages();
  },

  renderMessages() {
    ReactDOM.render(
      <div className="message-container">
        {this.messages.map((msg) => (
          <Message
            key={msg.id}
            type={msg.type}
            content={msg.content}
            duration={msg.duration}
            onClose={() => this.removeMessage(msg.id)}
          />
        ))}
      </div>,
      messageContainer
    );
  },
};

// 提供全局 API
export const showMessage = (type, content, duration = 3000) => {
  // useEffect(() => {
    messageContainer = document.createElement("div");
    document.body.appendChild(messageContainer);
  // }, []);
  return messageManager.addMessage({ type, content, duration });
};

export const success = (content, duration) => {
  showMessage("success", content, duration);
};

export const error = (content, duration) =>
  showMessage("error", content, duration);
export const warning = (content, duration) =>
  showMessage("warning", content, duration);
export const info = (content, duration) =>
  showMessage("info", content, duration);
