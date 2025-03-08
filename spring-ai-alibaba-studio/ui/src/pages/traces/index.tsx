/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useEffect, useState } from 'react';
import { Card, Table, Tag } from 'antd';
import type { TableProps } from 'antd';
import { traceDetailList } from '@/mock/tracemock';
import { createStyles } from 'antd-style';
import TraceDetailComp from '@/components/trace_detail_comp';
import { convertToTraceInfo } from '@/utils/trace_util';
import traceClient from '@/services/trace_clients';
import { TraceInfo } from '@/types/traces';

const useStyle = createStyles(({ css, token }) => {
  // @ts-ignore
  const { antCls } = token;
  return {
    customTable: css`
      ${antCls}-table {
        ${antCls}-table-container {
          ${antCls}-table-body,
          ${antCls}-table-content {
            scrollbar-width: thin;
            scrollbar-color: #eaeaea transparent;
            scrollbar-gutter: stable;
          }
        }
      }
    `,
  };
});

export default function History() {
  const [data, setData] = useState<TraceInfo[]>([]);
  const [openTraceDetail, setOpenTraceDetail] = useState(false);
  const [traceDetail, setTraceDetail] = useState({} as any);
  const columns: TableProps<TraceInfo>['columns'] = [
    {
      title: 'id',
      dataIndex: 'id',
      key: 'id',
      fixed: 'left',
      width: 100,
      ellipsis: true,
      render: (text, record) => (
        <div style={{ width: 100 }}>
          <a
            onClick={() => {
              setTraceDetail(record);
              setOpenTraceDetail(true);
            }}
            style={{
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
          >
            {text}
          </a>
          ...
        </div>
      ),
    },
    {
      title: 'timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
      ellipsis: true,
      // sorter: (a, b) => a.timestamp - b.timestamp,
      width: 100,
    },
    {
      title: 'name',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'model',
      dataIndex: 'model',
      filters: [
        {
          text: 'gpt系列模型',
          value: 'gpt',
        },
        {
          text: 'Claude系列',
          value: 'claude',
        },
      ],
      key: 'model',
      onFilter: (value, record) => record.model.startsWith(value as string),
    },
    {
      title: 'latency',
      key: 'latencyMilliseconds',
      dataIndex: 'latencyMilliseconds',
      render: (_, { latencyMilliseconds }) => (
        <div>{latencyMilliseconds} ms</div>
      ),
    },
    {
      title: 'usageDetails',
      dataIndex: 'usageDetails',
      key: 'usageDetails',
      render: (_, { usageDetails }) => (
        <span>{`${usageDetails?.input} → ${usageDetails?.output} (∑ ${usageDetails?.total})`}</span>
      ),
    },
    {
      title: 'Total Cost',
      dataIndex: 'costDetails',
      key: 'costDetails',
      render: (_, { costDetails }) => <span>{`${costDetails?.total}$`}</span>,
    },
    {
      title: 'tags',
      key: 'tags',
      dataIndex: 'tags',
      render: (_, { tags }) => (
        <>
          {tags.map((tag) => {
            let color = tag.length > 5 ? 'geekblue' : 'green';
            if (tag === 'loser') {
              color = 'volcano';
            }
            return (
              <Tag color={color} key={tag}>
                {tag.toUpperCase()}
              </Tag>
            );
          })}
        </>
      ),
    },
    {
      title: 'calculatedTotalCost',
      key: 'calculatedTotalCost',
      dataIndex: 'calculatedTotalCost',
    },
    {
      title: 'calculatedInputCost',
      key: 'calculatedInputCost',
      dataIndex: 'calculatedInputCost',
    },
    {
      title: 'calculatedOutputCost',
      key: 'calculatedOutputCost',
      dataIndex: 'calculatedOutputCost',
    },
    {
      title: 'Action',
      key: 'operation',
      fixed: 'right',
      width: 100,
      render: () => <a>查看详情</a>,
    },
  ];

  const { styles } = useStyle();
  const handleGetTraceData = async () => {
    const traceList = await traceClient.getTraceDetailClient();
    console.log(traceList);
    setData(
      traceList
        .map((trace) => {
          const traceInfo = convertToTraceInfo(trace);
          console.log(traceInfo);
          return traceInfo;
        })
        .filter((trace) => trace !== null),
    );
    // const temp = tableList.result.data.json.traces;
    // temp.forEach((trace) => {
    //   // @ts-ignore
    //   trace.model = Math.random() > 0.5 ? 'gpt-4o' : 'claude-3-5-sonnet';
    // });
    // @ts-ignore
    // setData(traceDetailList.map((trace) => { const traceInfo = convertToTraceInfo(trace); console.log(traceInfo); return traceInfo; }).filter(trace => trace !== null));
  };

  useEffect(() => {
    handleGetTraceData();
  }, []);
  return (
    <div>
      <Card title={'Traces'}>
        <Table
          className={styles.customTable}
          columns={columns}
          dataSource={data}
          scroll={{ x: 'max-content' }}
          rowKey={(recode) => recode.id}
        />
        <TraceDetailComp
          record={traceDetail}
          open={openTraceDetail}
          setOpen={setOpenTraceDetail}
        />
      </Card>
    </div>
  );
}
