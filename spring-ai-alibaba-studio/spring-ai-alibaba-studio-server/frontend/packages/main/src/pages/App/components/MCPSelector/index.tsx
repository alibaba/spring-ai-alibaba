import $i18n from '@/i18n';
import PureLayout from '@/layouts/Pure';
import { CreateMcpBtn } from '@/pages/MCP/Manage';
import { listMcpServers } from '@/services/mcp';
import {
  IListMcpServersParams,
  IMcpServer,
  IMCPTool,
  MCP_MAX_LIMIT,
  McpStatus,
} from '@/types/mcp';
import {
  Button,
  Drawer,
  Empty,
  IconFont,
  Input,
  message,
  Modal,
  Pagination,
} from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Flex, Spin } from 'antd';
import { debounce } from 'lodash-es';
import { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import MCPServerListItem from './MCPServerListItem';

export interface IMCPServerSelectorProps {
  onMcpServersChange?: (val: IMcpServer[]) => void;
  selectedServers?: IMcpServer[];
  onSelectTool?: (item: IMCPTool, server: IMcpServer) => void;
  selectedTool?: IMCPTool;
  mode: 'server' | 'tool';
}

const MCPServerSelector = (props: IMCPServerSelectorProps) => {
  const [filterParams, setFilterParams] = useSetState<IListMcpServersParams>({
    need_tools: true,
    status: McpStatus.ENABLED,
    current: 1,
    size: 10,
    name: '',
  });
  const [total, setTotal] = useState(0);
  const [list, setList] = useState<IMcpServer[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchList = () => {
    setLoading(true);
    listMcpServers(filterParams)
      .then((res) => {
        setList(res.data.records);
        setTotal(res.data.total);
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchList();
  }, [filterParams]);

  const onInputChange = debounce((e) => {
    setFilterParams({
      current: 1,
      name: e.target.value,
    });
  }, 500);

  return (
    <>
      <Flex justify="space-between" className="mb-[16px]">
        <Input
          onChange={onInputChange}
          prefix={<IconFont type="spark-search-line" />}
          placeholder={$i18n.get({
            id: 'main.pages.App.components.MCPSelector.index.inputHere',
            dm: '在此输入',
          })}
          allowClear
          style={{ width: 220 }}
        ></Input>
        <CreateMcpBtn isOpenNew buttonProps={{ type: 'default' }} />
      </Flex>
      {loading ? (
        <Spin className="w-full h-full"></Spin>
      ) : (
        <Flex vertical gap={16}>
          {list.length ? (
            <>
              {list.map((item) => (
                <MCPServerListItem
                  item={item}
                  selectedServers={props.selectedServers}
                  key={item.server_code}
                  onSelectServer={(item) => {
                    if (
                      props.selectedServers &&
                      props.selectedServers.length >= MCP_MAX_LIMIT
                    ) {
                      message.warning(
                        $i18n.get({
                          id: 'main.pages.App.components.MCPSelector.index.reachedMaxLimit',
                          dm: '已达到最大数量限制',
                        }),
                      );
                      return;
                    }
                    props.onMcpServersChange?.([
                      ...(props.selectedServers || []),
                      item,
                    ]);
                  }}
                  onRemoveServer={(serverCode) => {
                    props.onMcpServersChange?.(
                      (props.selectedServers || [])?.filter(
                        (item) => item.server_code !== serverCode,
                      ),
                    );
                  }}
                  selectMode={props.mode}
                  onSelectTool={(tool, server) => {
                    props.onSelectTool?.(tool, server);
                  }}
                  selectedTool={props.selectedTool}
                  fetchList={fetchList}
                />
              ))}
              <Pagination
                pageSize={filterParams.size}
                current={filterParams.current}
                total={total}
                hideOnSinglePage
                hideTips
                onChange={(page, pageSize) => {
                  setFilterParams({ current: page, size: pageSize });
                }}
                pageSizeOptions={[10, 20]}
              />
            </>
          ) : (
            <Flex className="h-full" align="center" justify="center">
              <Empty
                title={
                  filterParams.name?.length
                    ? $i18n.get({
                        id: 'main.pages.App.components.MCPSelector.index.noSearchResult',
                        dm: '暂无搜索结果',
                      })
                    : $i18n.get({
                        id: 'main.pages.App.components.MCPSelector.index.noCustomMcpService',
                        dm: '暂无自定义MCP服务',
                      })
                }
                description={
                  !filterParams.name?.length && (
                    <CreateMcpBtn
                      isOpenNew
                      text={$i18n.get({
                        id: 'main.pages.App.components.MCPSelector.index.goCreate',
                        dm: '去创建',
                      })}
                    />
                  )
                }
              ></Empty>
            </Flex>
          )}
        </Flex>
      )}
    </>
  );
};

export interface IMCPSelectorDrawerProps {
  onOk: (val: IMcpServer[]) => void;
  onClose: () => void;
  selectedServers?: IMcpServer[];
}
export const MCPServerSelectDrawer = (props: IMCPSelectorDrawerProps) => {
  const [cacheSelcted, setCacheSelected] = useState<IMcpServer[]>([
    ...(props.selectedServers || []),
  ]);
  return (
    <Drawer
      title={$i18n.get({
        id: 'main.pages.App.components.MCPSelector.index.selectMcpService',
        dm: '选择MCP服务',
      })}
      open
      width={640}
      onClose={props.onClose}
      footer={
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
          className="w-full"
        >
          <div
            style={{
              color: 'var(--ag-ant-color-text-tertiary)',
              fontSize: '14px',
              fontWeight: 'normal',
              lineHeight: '24px',
            }}
          >
            {!!cacheSelcted.length &&
              $i18n.get(
                {
                  id: 'main.pages.App.components.MCPSelector.index.addedMcpVar1Var2',
                  dm: '已添加MCP{var1}/{var2}',
                },
                { var1: cacheSelcted.length, var2: MCP_MAX_LIMIT },
              )}
          </div>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              gap: 12,
            }}
          >
            <Button
              type="default"
              onClick={() => {
                props.onClose();
              }}
            >
              {$i18n.get({
                id: 'main.pages.App.components.MCPSelector.index.cancel',
                dm: '取消',
              })}
            </Button>
            <Button
              type="primary"
              onClick={() => {
                props.onOk(cacheSelcted);
                props.onClose();
                message.success(
                  $i18n.get({
                    id: 'main.pages.App.components.MCPSelector.index.addSuccess',
                    dm: '添加成功！',
                  }),
                );
              }}
            >
              {$i18n.get({
                id: 'main.pages.Setting.ModelService.components.ModelServiceProviderModal.index.confirm',
                dm: '确认',
              })}
            </Button>
          </div>
        </div>
      }
    >
      <MCPServerSelector
        mode="server"
        onMcpServersChange={setCacheSelected}
        selectedServers={cacheSelcted}
      ></MCPServerSelector>
    </Drawer>
  );
};

export interface IMCPToolSelectModalProps {
  onOk: (val: IMCPTool, server: IMcpServer) => void;
  onClose: () => void;
  selectedMcpTool?: IMCPTool;
}

export const MCPToolSelectModal = (props: IMCPToolSelectModalProps) => {
  const [value, setValue] = useState<IMCPTool | undefined>(
    props.selectedMcpTool,
  );
  const [server, setServer] = useState<IMcpServer | undefined>(undefined);
  return (
    <Modal
      width={740}
      title={$i18n.get({
        id: 'main.pages.App.components.MCPSelector.index.11',
        dm: '选择MCP工具',
      })}
      open
      onCancel={props.onClose}
      bodyProps={{ style: { padding: 0 } }}
      footer={
        <div className="justify-end flex gap-[8px]">
          <Button onClick={props.onClose}>
            {$i18n.get({
              id: 'main.pages.App.components.MCPSelector.index.cancel',
              dm: '取消',
            })}
          </Button>
          <Button
            type="primary"
            onClick={() => {
              if (!value || !server) {
                message.warning(
                  $i18n.get({
                    id: 'main.pages.App.components.MCPSelector.index.selectMcpTool',
                    dm: '请选择MCP工具',
                  }),
                );
                return;
              }
              props.onOk(value, server);
              props.onClose();
            }}
          >
            {$i18n.get({
              id: 'main.pages.App.components.MCPSelector.index.confirm',
              dm: '确定',
            })}
          </Button>
        </div>
      }
    >
      <div className="p-[16px_24px] max-h-[400px] overflow-y-auto">
        <MCPServerSelector
          mode="tool"
          onSelectTool={(tool, server) => {
            setValue(tool);
            setServer(server);
          }}
          selectedTool={value}
        ></MCPServerSelector>
      </div>
    </Modal>
  );
};

export const MCPToolSelectModalFuncs = {
  show: (options: {
    onOk?: (tool: IMCPTool, server: IMcpServer) => void;
    onCancel?: () => void;
    selectedMcpTool?: IMCPTool;
  }) => {
    const div = document.createElement('div');
    document.body.appendChild(div);
    const root = createRoot(div);

    const handleClose = () => {
      root.unmount();
      div.remove();
      options.onCancel?.();
    };

    const handleOk = (tool: IMCPTool, server: IMcpServer) => {
      options.onOk?.(tool, server);
      handleClose();
    };

    root.render(
      <PureLayout>
        <MCPToolSelectModal
          selectedMcpTool={options.selectedMcpTool}
          onClose={handleClose}
          onOk={handleOk}
        />
      </PureLayout>,
    );
  },
};
