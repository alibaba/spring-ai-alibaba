export type BaseNodeProps = {
  id: string; // 节点 ID
  data: {
    label: string;
    icon?: string;
    iconBg?: string;
    background?: string;
    isStartNode?: boolean;
    isEndNode?: boolean;
  };
  isConnectable?: boolean;
};
