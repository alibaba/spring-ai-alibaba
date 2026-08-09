import { DeleteOutlined } from '@ant-design/icons';
import { Row, Col, Card, Button, Popconfirm, Typography, Tooltip, Switch } from 'antd';

const { Text } = Typography;

const FunctionList = ({
  functions = [], onClick, onDelete, size = 'middle', enable = true,
  onEnableChange
}) => {
  if (!functions || functions.length === 0) {
    return (
      null
    );
  }

  return (
    <div>
      <Text strong className='mb-1 block' style={{ fontSize: size === 'small' ? '12px' : '14px' }}>
        Function Configuration
        <Switch
          defaultChecked
          checkedChildren="Enabled"
          className='ml-2' size="small" onChange={(checked) => onEnableChange(checked)}
        />
      </Text>
      <Row gutter={12}>
        {
          functions.map(fn => (
            <Col key={fn.toolDefinition.name} span={6}>
              <Card
                size='small' title={fn.toolDefinition.name}
                className='cursor-pointer'
                onClick={() => onClick(fn)}
                extra={
                  <Popconfirm
                    title="Delete this function?"
                    onConfirm={(e) => {
                      e?.stopPropagation();
                      onDelete(fn);
                    }}
                    onCancel={(e) => e?.stopPropagation()}
                    okText="OK"
                    cancelText="Cancel"
                  >
                    <Button
                      type="text"
                      danger
                      size="small"
                      icon={<DeleteOutlined />}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </Popconfirm>
                }
              >
                <Tooltip title={fn.toolDefinition.description}>
                  <div className='text-xs text-gray-500 mt-1 truncate max-w-full text-ellipsis overflow-hidden whitespace-nowrap'>
                    {fn.toolDefinition.description}
                  </div>
                </Tooltip>
              </Card>
            </Col>
          ))
        }
      </Row>
    </div>
  );
};

export default FunctionList;