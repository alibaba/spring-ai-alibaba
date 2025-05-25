import { NODE_TITLE, NODE_TYPE } from '../Common/manageNodes';

const StartNode = () => {
  return <div>{NODE_TITLE[NODE_TYPE.START]}</div>;
};

export default StartNode;
