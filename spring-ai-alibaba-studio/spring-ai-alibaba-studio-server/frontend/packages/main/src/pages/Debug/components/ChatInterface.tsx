import React from 'react';
import { useConfigContext } from '../contexts/ConfigContext';
import Sidebar from './Sidebar';
import MessageArea from './MessageArea';
import DebugPanel from './DebugPanel';
import styles from '../index.module.less';

const ChatInterface: React.FC = () => {
  const { config } = useConfigContext();

  return (
    <div className={styles.chatInterface}>
      <Sidebar />
      <div className={styles.mainContent}>
        <MessageArea />
      </div>
      {config.showDebugInfo && <DebugPanel />}
    </div>
  );
};

export default ChatInterface;
