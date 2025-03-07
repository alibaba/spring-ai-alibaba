import ExpandNodeToolBar from '@/pages/Graph/Design/types/ExpandNodeToolBar';
import { graphState } from '@/store/GraphState';
import { Icon } from '@iconify/react';
import { useProxy } from '@umijs/max';
import { Handle, Position } from '@xyflow/react';
import { Flex, Tag } from 'antd';
import React, { memo } from 'react';
import './base.less';

export type CaseType = {
  id: string;
  case_id: string;
  conditions: {
    comparisonOperator: string;
    value: string;
    varType: string;
    variableSelector: [any, string];
  }[];
  logical_operator?: string;
};

interface Props {
  data: any;
}

const ToolbarNode: React.FC<Props> = ({ data }) => {
  const graphStore = useProxy(graphState);
  const onClick = () => {
    graphStore.formDrawer.isOpen = true;
  };

  let cases: CaseType[] = data.cases;
  if (!cases || cases.length === 0) {
    cases = [
      {
        id: 'true',
        case_id: 'true',
        conditions: [],
      },
    ];
  }
  if (cases.length === 1) {
    cases = [
      ...cases,
      {
        id: 'false',
        case_id: 'false',
        conditions: [],
      },
    ];
  }

  return (
    <div onClick={onClick}>
      <ExpandNodeToolBar></ExpandNodeToolBar>
      <Handle
        type="target"
        position={Position.Left}
        className={'graph-node__handle'}
      ></Handle>
      <Flex vertical={true} className="cust-node-wrapper">
        <Flex>
          <div className="node-type">
            <Icon className="type-icon" icon="jam:branch-f"></Icon>Branch Node
          </div>
        </Flex>
        <Tag
          style={{
            display: 'block',
            marginRight: '0',
            border: 'none',
            color: 'var(--ant-color-bg-solid-hover)',
          }}
        >
          {data.label}
        </Tag>
        <Flex
          vertical={true}
          style={{
            width: '100%',
          }}
        >
          {cases &&
            cases.map((x: CaseType, idx: number) => {
              let title = '';
              if (idx === 0) {
                title = 'IF';
              } else if (idx === cases.length - 1) {
                title = 'ELSE';
              } else {
                title = 'ELSE-IF';
              }
              return (
                <>
                  <div
                    style={{
                      position: 'relative',
                      width: '100%',
                      fontSize: '16px',
                      marginBottom: '10px',
                    }}
                  >
                    <Flex
                      vertical={true}
                      style={{
                        width: '100%',
                        alignItems: 'flex-end',
                      }}
                      gap={5}
                    >
                      <div>{title}</div>
                      {x.conditions.map((c) => {
                        return (
                          <>
                            <Tag
                              style={{
                                marginRight: '0',
                                border: 'none',
                                color: 'var(--ant-color-bg-solid-hover)',
                              }}
                            >
                              <b
                                style={{
                                  color: 'var(--ant-color-primary)',
                                }}
                              >
                                {c.variableSelector[1]}
                              </b>{' '}
                              {c.comparisonOperator} {c.value}
                            </Tag>
                          </>
                        );
                      })}
                    </Flex>
                    <Handle
                      className={'graph-node__handle'}
                      type="source"
                      id={x.id}
                      style={{
                        top: '10px',
                        right: '-12px',
                        position: 'absolute',
                      }}
                      position={Position.Right}
                    />
                  </div>
                </>
              );
            })}
        </Flex>
      </Flex>
    </div>
  );
};

export default memo(ToolbarNode);
