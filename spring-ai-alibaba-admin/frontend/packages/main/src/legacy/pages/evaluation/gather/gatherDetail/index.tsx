import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { 
  Card, 
  Button, 
  Descriptions, 
  Table, 
  Tag, 
  Spin, 
  message,
  Tabs,
  Alert,
  Modal,
  Form,
  Typography,
  Input,
  Tooltip
} from 'antd';
import { 
  ArrowLeftOutlined, 
  EditOutlined, 
  DeleteOutlined,
  EyeOutlined,
  DownloadOutlined,
  PlusOutlined,
  SaveOutlined,
  CloseOutlined,
  CheckOutlined
} from '@ant-design/icons';
import API from '../../../../services';
import usePagination from '../../../../hooks/usePagination';
import './index.css';

const { TabPane } = Tabs;
const { Title, Text } = Typography;

// 格式化时间显示
const formatDateTime = (dateTimeString: string) => {
    if (!dateTimeString) return '-';
    try {
        const date = new Date(dateTimeString);
        return date.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    } catch {
        return dateTimeString;
    }
};

// 评测集详情接口
interface DatasetDetail {
  id: number;
  name: string;
  description: string;
  columnsConfig: Array<{
    name: string;
    dataType: string;
    displayFormat: string;
    description?: string;
    required: boolean;
  }>;
  dataCount: number;
  createTime: string;
  updateTime: string;
  latestVersionId: number;
  versions: any;
  experiments: any;
}

// 数据项接口
interface DataItem {
  id: number;
  datasetId: number;
  dataContent: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

const GatherDetail: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<DatasetDetail | null>(null);
  const [dataItems, setDataItems] = useState<any[]>([]);
  const [dataLoading, setDataLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('data');
  const [isEditing, setIsEditing] = useState(false); // 编辑模式状态
  const [editForm] = Form.useForm(); // 编辑表单
  const [saving, setSaving] = useState(false); // 保存状态
  // 添加选中状态管理
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  // 添加版本数据和关联实验数据状态
  const [versions, setVersions] = useState<any[]>([]);
  const [versionsLoading, setVersionsLoading] = useState(false);
  const [experiments, setExperiments] = useState<any[]>([]);
  const [experimentsLoading, setExperimentsLoading] = useState(false);
  // 添加数据弹窗相关状态
  const [addDataModalVisible, setAddDataModalVisible] = useState(false);
  const [addDataForm] = Form.useForm();
  const [addingData, setAddingData] = useState(false);
  // 添加行内编辑相关状态
  const [editingRowId, setEditingRowId] = useState<number | null>(null);
  const [editingData, setEditingData] = useState<Record<string, any>>({});
  const [updatingData, setUpdatingData] = useState(false);
  // 添加待提交数据状态
  const [pendingDataItems, setPendingDataItems] = useState<any[]>([]);
  
  // 为三个Tab分别创建独立的分页状态
  const { pagination: dataPagination, setPagination: setDataPagination, onPaginationChange: onDataPaginationChange, onShowSizeChange: onDataShowSizeChange } = usePagination();
  const { pagination: versionsPagination, setPagination: setVersionsPagination, onPaginationChange: onVersionsPaginationChange, onShowSizeChange: onVersionsShowSizeChange } = usePagination();
  const { pagination: experimentsPagination, setPagination: setExperimentsPagination, onPaginationChange: onExperimentsPaginationChange, onShowSizeChange: onExperimentsShowSizeChange } = usePagination();

  // 动态生成表格列
  const generateTableColumns = () => {
    const dynamicColumns: any[] = [];
    
    // 根据columnsConfig生成动态列
    detail?.columnsConfig?.forEach((column, index) => {
      dynamicColumns.push({
        title: column.name,
        dataIndex: column.name,
        ellipsis: true,
        render: (text: string, record: any) => {
          if (editingRowId === record.id) {
            return (
              <Input.TextArea
                value={editingData[column.name] || ''}
                onChange={(e) => handleEditDataChange(column.name, e.target.value)}
                rows={2}
                maxLength={1000}
                placeholder={`输入${column.description || column.name}`}
              />
            );
          }
          return <Tooltip placement="topLeft" title={text}>
            <span>{text}</span>
          </Tooltip>
        }
      });
    });
    
    // 添加固定列：创建时间、更新时间、操作
    dynamicColumns.push(
      { 
        title: '创建时间', 
        dataIndex: 'createTime',
        render: (text: string) => formatDateTime(text)
      },
      { 
        title: '更新时间', 
        dataIndex: 'updateTime',
        render: (text: string) => formatDateTime(text)
      },
      {
        title: '操作',
        width: '10%',
        render: (_: any, record: any) => {
          if (editingRowId === record.id) {
            return (
              <div className="flex space-x-1">
                <Button 
                  type="text" 
                  icon={<CloseOutlined />} 
                  size="small"
                  title="取消"
                  onClick={handleCancelEdit}
                  disabled={updatingData}
                />
                <Button 
                  type="text" 
                  icon={<CheckOutlined />} 
                  size="small"
                  title="确认"
                  loading={updatingData}
                  onClick={() => handleConfirmEdit(record)}
                  style={{ color: '#52c41a' }}
                />
              </div>
            );
          }
          return (
            <div className="flex space-x-1">
              <Button 
                type="text" 
                icon={<EditOutlined />} 
                size="small"
                title="编辑"
                onClick={() => handleEditRow(record)}
              />
              <Button 
                type="text" 
                icon={<DeleteOutlined />} 
                size="small"
                danger
                title="删除"
                onClick={() => handleDeleteRow(record)}
              />
            </div>
          );
        }
      }
    );
    
    return dynamicColumns;
  };

  // 选择处理
  const onSelectChange = (newSelectedRowKeys: React.Key[]) => {
    setSelectedRowKeys(newSelectedRowKeys);
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: onSelectChange,
  };

  // 添加数据
  const handleAddData = () => {
    setAddDataModalVisible(true);
    addDataForm.resetFields();
  };

  // 处理数据添加提交
  const handleAddDataSubmit = async () => {
    try {
      setAddingData(true);
      const values = await addDataForm.validateFields();
      
      // 根据columnsConfig动态构造dataContent对象
      const dataContent: Record<string, any> = {};
      
      // 遍历columnsConfig中的每个字段，将表单值添加到dataContent中
      detail?.columnsConfig?.forEach(column => {
        if (values[column.name] !== undefined) {
          dataContent[column.name] = values[column.name];
        }
      });
      
      // 添加备注字段（如果有的话）
      if (values.remark) {
        dataContent.remark = values.remark;
      }
      
      // 创建临时数据项（不调用API，而是添加到本地列表）
      const newItem = {
        id: `pending_${Date.now()}`, // 临时ID，使用特殊前缀标识
        ...dataContent,
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString()
      };
      
      // 添加到待提交数据列表
      setPendingDataItems(prev => [...prev, newItem]);
      
      // 同时添加到显示列表中
      setDataItems(prev => [...prev, newItem]);
      
      // 更新分页信息 - 增加总数
      setDataPagination(prev => ({
        ...prev,
        total: prev.total + 1
      }));
      
      message.success('数据已添加到待提交列表，请提交新版本以保存更改');
      setAddDataModalVisible(false);
      addDataForm.resetFields();
    } catch (error) {
      console.error('数据添加失败:', error);
      message.error('数据添加失败，请重试');
    } finally {
      setAddingData(false);
    }
  };

  // 取消添加数据
  const handleAddDataCancel = () => {
    setAddDataModalVisible(false);
    addDataForm.resetFields();
  };

  // 开始编辑行
  const handleEditRow = (record: any) => {
    setEditingRowId(record.id);
    
    // 初始化编辑数据，包含所有columnsConfig字段
    const editData: Record<string, any> = {};
    detail?.columnsConfig?.forEach(column => {
      editData[column.name] = record[column.name] || '';
    });
    
    setEditingData(editData);
  };

  // 取消编辑
  const handleCancelEdit = () => {
    setEditingRowId(null);
    setEditingData({});
  };

  // 确认编辑
  const handleConfirmEdit = async (record: any) => {
    try {
      setUpdatingData(true);
      
      // 构造更新的dataContent，包含所有columnsConfig字段
      const dataContent: Record<string, any> = {};
      detail?.columnsConfig?.forEach(column => {
        dataContent[column.name] = editingData[column.name] || '';
      });
      
      // 调用API更新数据
      const response = await API.updateDatasetDataItem({
        id: record.id,
        dataContent: JSON.stringify(dataContent),
      });
      
      if (response.code === 200) {
        message.success('数据更新成功');
        setEditingRowId(null);
        setEditingData({});
        // 重新获取数据列表
        fetchDataItems();
      } else {
        throw new Error(response.message || '数据更新失败');
      }
    } catch (error) {
      console.error('数据更新失败:', error);
      message.error('数据更新失败，请重试');
    } finally {
      setUpdatingData(false);
    }
  };

  // 处理编辑数据变化
  const handleEditDataChange = (field: string, value: string) => {
    setEditingData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // 删除单行数据（前端删除）
  const handleDeleteRow = (record: any) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除这条数据吗？此操作仅在前端生效，需要点击"提交新版本"才会保存到后端。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => {
        // 检查是否是待提交的数据
        const isPendingData = pendingDataItems.some(item => item.id === record.id);
        
        if (isPendingData) {
          // 如果是待提交的数据，从待提交列表中移除
          setPendingDataItems(prev => prev.filter(item => item.id !== record.id));
        }
        
        // 从前端列表中移除该数据
        setDataItems(prev => prev.filter(item => item.id !== record.id));
        
        // 如果该数据在选中列表中，也要移除
        setSelectedRowKeys(prev => prev.filter(key => key !== record.id));
        
        // 更新分页信息 - 减少总数
        setDataPagination(prev => ({
          ...prev,
          total: prev.total - 1
        }));
        
        message.success(isPendingData ? '待提交数据已删除' : '数据已删除（前端）');
      }
    });
  };

  // 批量删除
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的数据');
      return;
    }
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除选中的 ${selectedRowKeys.length} 条数据吗？此操作仅在前端生效，需要点击"提交新版本"才会保存到后端。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => {
        // 从前端列表中移除选中的数据
        const deletedCount = selectedRowKeys.length;
        setDataItems(prev => prev.filter(item => !selectedRowKeys.includes(item.id)));
        
        // 从待提交列表中移除选中的数据（如果有的话）
        setPendingDataItems(prev => prev.filter(item => !selectedRowKeys.includes(item.id)));
        
        // 更新分页信息 - 减少总数
        setDataPagination(prev => ({
          ...prev,
          total: prev.total - deletedCount
        }));
        
        message.success(`已删除 ${deletedCount} 条数据（前端）`);
        setSelectedRowKeys([]);
      }
    });
  };

  // 提交新版本
  const handleSubmitVersion = async () => {
    Modal.confirm({
      title: '提交新版本',
      content: (
        <div>
          <p>确定要基于当前数据提交新版本吗？</p>
          <div className="text-sm text-gray-500 mt-3 p-3 bg-gray-50 rounded">
            <div>评测集：{detail?.name}</div>
            <div>数据量：{dataItems.length} 条</div>
            <div>列配置：{detail?.columnsConfig?.length || 0} 个列</div>
            {pendingDataItems.length > 0 && (
              <div className="mt-2 text-orange-600">
                注意：将同时提交 {pendingDataItems.length} 条新增数据
              </div>
            )}
          </div>
        </div>
      ),
      okText: '提交',
      cancelText: '取消',
      onOk: async () => {
        try {
          // 收集当前页面上已存在的数据项ID（不包括待提交的数据）
          const existingDatasetItemIds = dataItems
            .filter(item => !pendingDataItems.some(pending => pending.id === item.id))
            .map(item => item.id);
          
          // 如果有待提交的数据，先批量提交这些数据
          let allDatasetItemIds = [...existingDatasetItemIds];
          if (pendingDataItems.length > 0) {
            // 批量提交待提交的数据
            const dataContentArray = pendingDataItems.map(item => {
              const dataContent: Record<string, any> = {};
              detail?.columnsConfig?.forEach(column => {
                if (item[column.name] !== undefined) {
                  dataContent[column.name] = item[column.name];
                }
              });
              if (item.remark) {
                dataContent.remark = item.remark;
              }
              return JSON.stringify(dataContent);
            });
            
            const response: any = await API.createDatasetDataItem({
              datasetId: Number(id),
              dataContent: dataContentArray, // 现在传递数组
              columnsConfig: detail?.columnsConfig || []
            });
            
            if (response.code !== 200) {
              throw new Error(response.message || '数据提交失败');
            }
            
            // 获取新添加的数据项ID
            if (response.data) {
              // 如果是批量提交，response.data应该是一个数组
              if (Array.isArray(response.data)) {
                const newIds = response.data.map((item: any) => item.id);
                allDatasetItemIds = [...allDatasetItemIds, ...newIds];
              } else if (response.data.id) {
                // 如果是单个提交，response.data应该是一个对象
                allDatasetItemIds = [...allDatasetItemIds, response.data.id];
              }
            }
            
            message.success(`成功提交 ${pendingDataItems.length} 条新增数据`);
            // 清空待提交数据列表
            setPendingDataItems([]);
          }
          
          // 调用API提交版本，使用所有数据项ID（包括新添加的）
          const versionResponse = await API.createDatasetVersion({
            datasetId: Number(id),
            description: `基于当前数据创建的新版本 - ${new Date().toLocaleString()}`,
            columnsConfig: detail?.columnsConfig || [],
            datasetItems: allDatasetItemIds, // 使用所有数据项ID
            status: 'draft'
          });
          
          console.log('创建新版本API返回数据:', versionResponse);
          
          if (versionResponse.code === 200) {
            message.success('新版本提交成功');
            
            // 获取新版本ID
            const newVersionId = versionResponse.data?.id;
            console.log('新创建的版本ID:', newVersionId);
            
            // 重新获取评测集详情以更新页面信息
            await fetchDatasetDetail();
            
            // 使用新版本ID获取数据项
            if (newVersionId) {
              fetchDataItemsWithVersionId(newVersionId);
            } else {
              // 如果没有获取到新版本ID，则使用常规方式刷新数据
              fetchDataItems(dataPagination.current, dataPagination.pageSize);
            }
            
            // 重新获取版本信息
            fetchVersions();
          } else {
            throw new Error(versionResponse.message || '版本提交失败');
          }
        } catch (error) {
          const errMsg = error instanceof Error ? error.message : String(error);
          message.error(`提交失败: ${errMsg || '请重试'}`);
          // 如果是数据提交失败，不要继续执行版本提交
          return Promise.reject(error);
        }
      }
    });
  };

  // 获取评测集详情
  const fetchDatasetDetail = async () => {
    try {
      setLoading(true);
      
      const response = await API.getDataset({ datasetId: Number(id) });
      if (response.code === 200) {
        const apiData = response.data as any; // 使用any类型绕过类型检查
        // 转换API数据为组件所需的类型
        const detailData: DatasetDetail = {
          id: apiData.id,
          name: apiData.name,
          description: apiData.description,
          columnsConfig: [],
          dataCount: apiData.dataCount,
          createTime: apiData.createTime,
          updateTime: apiData.updateTime,
          latestVersionId: apiData.latestVersionId,
          versions: apiData.versions,
          experiments: apiData.experiments
        };
        
        // 解析columnsConfig字符串为对象
        try {
          detailData.columnsConfig = JSON.parse(apiData.columnsConfig || '[]');
        } catch {
          detailData.columnsConfig = [];
        }
        
        setDetail(detailData);
      } else {
        throw new Error(response.message || '获取详情失败');
      }
    } catch (error) {
      console.error('获取评测集详情失败:', error);
      // 发生错误时设置为空状态
      setDetail(null);
      setLoading(false);
    } finally {
      setLoading(false);
    }
  };

  // 修改获取数据项列表函数，支持分页
  const fetchDataItems = async (pageNumber: number = 1, pageSize: number = 10) => {
    try {
      setDataLoading(true);
      
      console.log('调用getDatasetDataItems接口，参数:', { 
        datasetVersionId: detail?.latestVersionId,
        pageNumber: pageNumber,
        pageSize: pageSize
      });
      
      const response = await API.getDatasetDataItems({ 
        datasetVersionId: detail?.latestVersionId,
        pageNumber: pageNumber,
        pageSize: pageSize
      });
      
      console.log('getDatasetDataItems接口返回数据:', response);
      
      if (response.code === 200 && response.data) {
        const dataResponse = response.data;
        
        console.log('解析后的dataResponse:', dataResponse);
        
        // 使用pageItems字段
        if (dataResponse.pageItems && Array.isArray(dataResponse.pageItems)) {
          console.log('获取到pageItems数组:', dataResponse.pageItems);
          
          // 转换API数据为组件需要的格式
          const transformedData = dataResponse.pageItems.map((item: any) => {
            try {
              console.log('处理数据项:', item);
              const parsedContent = JSON.parse(item.dataContent);
              console.log('解析后的dataContent:', parsedContent);
              
              // 根据columnsConfig动态构造数据对象
              const dataObject: any = {
                id: item.id,
                createTime: item.createTime,
                updateTime: item.updateTime
              };
              
              // 添加动态字段
              detail?.columnsConfig?.forEach(column => {
                dataObject[column.name] = parsedContent[column.name] || '';
              });
              
              console.log('转换后的数据对象:', dataObject);
              return dataObject;
            } catch (e) {
              console.error('解析dataContent失败:', e, item.dataContent);
              // 如果解析失败，返回基本数据结构
              const dataObject: any = {
                id: item.id,
                createTime: item.createTime,
                updateTime: item.updateTime
              };
              
              // 为所有配置字段设置默认值
              detail?.columnsConfig?.forEach(column => {
                dataObject[column.name] = '';
              });
              
              return dataObject;
            }
          });
          
          console.log('最终转换后的数据项数组:', transformedData);
          setDataItems(transformedData);
          
          // 更新分页信息
          setDataPagination(prev => ({
            ...prev,
            current: dataResponse.pageNumber || pageNumber,
            pageSize: dataResponse.pageSize || pageSize,
            total: dataResponse.totalCount || 0
          }));
        } else {
          console.warn('API返回数据中pageItems字段不是数组或不存在:', dataResponse);
          setDataItems([]);
          setDataPagination(prev => ({
            ...prev,
            current: 1,
            total: 0
          }));
        }
      } else {
        console.error('API返回错误:', response.code, response.message);
        throw new Error(response.message || '获取数据项失败');
      }
    } catch (error) {
      console.error('获取数据项失败:', error);
      // 发生错误时设置为空数组
      setDataItems([]);
      setDataPagination(prev => ({
        ...prev,
        current: 1,
        total: 0
      }));
    } finally {
      setDataLoading(false);
    }
  };

  // 使用指定版本ID获取数据项列表
  const fetchDataItemsWithVersionId = async (versionId: number) => {
    try {
      setDataLoading(true);
      
      console.log('使用新版本ID调用getDatasetDataItems接口，参数:', { 
        datasetVersionId: versionId,
        pageNumber: 1,
        pageSize: 10
      });
      
      const response = await API.getDatasetDataItems({ 
        datasetVersionId: versionId,
        pageNumber: 1,
        pageSize: 10
      });
      
      console.log('新版本getDatasetDataItems接口返回数据:', response);
      
      if (response.code === 200 && response.data) {
        const dataResponse = response.data;
        
        console.log('新版本解析后的dataResponse:', dataResponse);
        
        // 使用pageItems字段
        if (dataResponse.pageItems && Array.isArray(dataResponse.pageItems)) {
          console.log('新版本获取到pageItems数组:', dataResponse.pageItems);
          
          // 转换API数据为组件需要的格式
          const transformedData = dataResponse.pageItems.map((item: any) => {
            try {
              console.log('处理数据项:', item);
              const parsedContent = JSON.parse(item.dataContent);
              console.log('解析后的dataContent:', parsedContent);
              
              // 根据columnsConfig动态构造数据对象
              const dataObject: any = {
                id: item.id,
                createTime: item.createTime,
                updateTime: item.updateTime
              };
              
              // 添加动态字段
              detail?.columnsConfig?.forEach(column => {
                dataObject[column.name] = parsedContent[column.name] || '';
              });
              
              console.log('转换后的数据对象:', dataObject);
              return dataObject;
            } catch (e) {
              console.error('解析dataContent失败:', e, item.dataContent);
              // 如果解析失败，返回基本数据结构
              const dataObject: any = {
                id: item.id,
                createTime: item.createTime,
                updateTime: item.updateTime
              };
              
              // 为所有配置字段设置默认值
              detail?.columnsConfig?.forEach(column => {
                dataObject[column.name] = '';
              });
              
              return dataObject;
            }
          });
          
          console.log('新版本最终转换后的数据项数组:', transformedData);
          setDataItems(transformedData);
        } else {
          console.warn('新版本API返回数据中pageItems字段不是数组或不存在:', dataResponse);
          setDataItems([]);
        }
      } else {
        console.error('新版本API返回错误:', response.code, response.message);
        throw new Error(response.message || '获取数据项失败');
      }
    } catch (error) {
      console.error('使用新版本ID获取数据项失败:', error);
      // 发生错误时设置为空数组
      setDataItems([]);
    } finally {
      setDataLoading(false);
    }
  };

  // 修改获取版本数据列表函数，支持分页
  const fetchVersions = async (pageNumber: number = 1, pageSize: number = 10) => {
    try {
      setVersionsLoading(true);
      
      const response = await API.getDatasetVersions({
        datasetId: Number(id),
        pageNumber: pageNumber,
        pageSize: pageSize
      });
      
      if (response.code === 200 && response.data) {
        const dataResponse = response.data;
        const versionsData = dataResponse.pageItems || [];
        setVersions(versionsData);
        
        // 更新分页信息
        setVersionsPagination(prev => ({
          ...prev,
          current: dataResponse.pageNumber || pageNumber,
          pageSize: dataResponse.pageSize || pageSize,
          total: dataResponse.totalCount || 0
        }));
      } else {
        throw new Error(response.message || '获取版本数据失败');
      }
    } catch (error) {
      console.error('获取版本数据失败:', error);
      // 发生错误时设置为空数组
      setVersions([]);
      setVersionsPagination(prev => ({
        ...prev,
        current: 1,
        total: 0
      }));
    } finally {
      setVersionsLoading(false);
    }
  };

  // 修改获取关联实验列表函数，支持分页
  const fetchExperiments = async (pageNumber: number = 1, pageSize: number = 10) => {
    try {
      setExperimentsLoading(true);
      
      const response = await API.getDatasetExperiments({
        datasetId: Number(id),
        pageNumber: pageNumber,
        pageSize: pageSize
      });
      
      if (response.code === 200 && response.data) {
        const dataResponse = response.data;
        const experimentsData = dataResponse.pageItems || [];
        // 转换为组件需要的格式
        const transformedExperiments = experimentsData.map((exp: any) => ({
          id: exp.id,
          name: exp.name,
          version: exp.datasetVersion,
          status: exp.status,
          createTime: exp.createTime
        }));
        setExperiments(transformedExperiments);
        
        // 更新分页信息
        setExperimentsPagination(prev => ({
          ...prev,
          current: dataResponse.pageNumber || pageNumber,
          pageSize: dataResponse.pageSize || pageSize,
          total: dataResponse.totalCount || 0
        }));
      } else {
        throw new Error(response.message || '获取关联实验失败');
      }
    } catch (error) {
      console.error('获取关联实验失败:', error);
      // 发生错误时设置为空数组
      setExperiments([]);
      setExperimentsPagination(prev => ({
        ...prev,
        current: 1,
        total: 0
      }));
    } finally {
      setExperimentsLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      fetchDatasetDetail();
      // 页面加载时直接获取关联实验数据
      fetchExperiments(experimentsPagination.current, experimentsPagination.pageSize);
    }
  }, [id]);

  useEffect(() => {
    if (detail) {
      // 详情加载完成后，立即获取版本数据
      fetchVersions(versionsPagination.current, versionsPagination.pageSize);
      // 如果当前在数据管理tab，也获取数据项
      if (activeTab === 'data') {
        fetchDataItems(dataPagination.current, dataPagination.pageSize);
      }
    }
  }, [detail]);

  useEffect(() => {
    // 只有在切换到数据管理tab时才获取数据项
    if (detail && activeTab === 'data') {
      fetchDataItems(dataPagination.current, dataPagination.pageSize);
    }
    // 切换到版本记录tab时获取版本数据
    else if (detail && activeTab === 'version') {
      fetchVersions(versionsPagination.current, versionsPagination.pageSize);
    }
    // 切换到关联实验tab时获取实验数据
    else if (detail && activeTab === 'experiment') {
      // 直接获取关联实验数据
      fetchExperiments(experimentsPagination.current, experimentsPagination.pageSize);
    }
  }, [activeTab]);

  // 返回列表页面
  const handleGoBack = () => {
    navigate('/evaluation-gather');
  };

  // 进入编辑模式
  const handleEdit = () => {
    setIsEditing(true);
    // 设置表单初始值
    editForm.setFieldsValue({
      name: detail?.name,
      description: detail?.description
    });
  };

  // 保存编辑
  const handleSave = async () => {
    try {
      setSaving(true);
      const values = await editForm.validateFields();
      
      // 调用API保存修改
      const response = await API.updateDataset({
        datasetId: Number(id),
        name: values.name,
        description: values.description,
        columnsConfig: detail?.columnsConfig || []
      });
      
      if (response.code === 200) {
        message.success('保存成功');
        // 更新本地数据
        setDetail(prev => prev ? {
          ...prev,
          name: values.name,
          description: values.description
        } : null);
        setIsEditing(false);
      } else {
        throw new Error(response.message || '保存失败');
      }
    } catch (error) {
      console.error('保存失败:', error);
      message.error('保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  // 取消编辑
  const handleCancel = () => {
    setIsEditing(false);
    editForm.resetFields();
  };

  // 删除评测集
  const handleDelete = async () => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除评测集「${detail?.name}」吗？此操作不可恢复。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await API.deleteDataset({ datasetId: Number(id) });
          message.success('评测集已删除');
          navigate('/evaluation-gather');
        } catch (error) {
          message.error('删除失败，请重试');
          console.error('删除评测集失败:', error);
        }
      }
    });
  };

  // 渲染数据类型标签
  const renderDataTypeTag = (dataType: string) => {
    const colorMap: Record<string, string> = {
      'String': 'blue',
      'Number': 'green',
      'Boolean': 'orange',
      'Array': 'purple',
      'Object': 'red'
    };
    return <Tag color={colorMap[dataType] || 'default'}>{dataType}</Tag>;
  };



  if (loading) {
    return (
      <div className="gather-detail-page">
        <div className="loading-container" style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          width: '100%',
          position: 'fixed',
          top: 0,
          left: 0,
          zIndex: 1000,
          backgroundColor: 'white'
        }}>
          <Spin size="large" />
        </div>
      </div>
    );
  }

  if (!detail && !loading) {
    return (
      <div className="gather-detail-page">
        <div className="error-container" style={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          width: '100%'
        }}>
          <h3>评测集详情不存在</h3>
          <Button onClick={handleGoBack}>返回列表</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="gather-detail-page p-8 fade-in">
      {/* 页面头部 */}
      <div className="flex mb-6">
          <Button 
            type="text" 
            icon={<ArrowLeftOutlined />} 
            onClick={handleGoBack}
            size="large"
          />
          <Title level={2} className="m-0">{detail?.name}</Title>
      </div>

      {/* 评测集信息展示区域 */}
      {detail && (
        <Card 
          title="评测集信息" 
          extra={
            !isEditing ? (
              <Button 
                type="primary"
                icon={<EditOutlined />}
                onClick={handleEdit}
              >
                编辑
              </Button>
            ) : (
              <div className="flex gap-2">
                <Button 
                  onClick={handleCancel}
                  icon={<CloseOutlined />}
                >
                  取消
                </Button>
                <Button 
                  type="primary"
                  loading={saving}
                  icon={<SaveOutlined />}
                  onClick={handleSave}
                >
                  保存
                </Button>
              </div>
            )
          }
          className="mb-6"
        >
          {!isEditing ? (
            // 展示模式
            <>
              <Descriptions column={2} labelStyle={{ fontWeight: 500 }}>
                <Descriptions.Item label="名称">{detail.name || '-'}</Descriptions.Item>
                {/* <Descriptions.Item label="创建人">{detail.creator}</Descriptions.Item> */}
                <Descriptions.Item label="描述">{detail.description || '-'}</Descriptions.Item>
                <Descriptions.Item label="数据量">{detail.dataCount || 0} 条</Descriptions.Item>
                <Descriptions.Item label="创建时间">{formatDateTime(detail.createTime)}</Descriptions.Item>
                <Descriptions.Item label="更新时间">{formatDateTime(detail.updateTime)}</Descriptions.Item>
              </Descriptions>
            </>
          ) : (
            // 编辑模式
            <Form
              form={editForm}
              layout="vertical"
              className="edit-form"
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                <Form.Item
                  label="评测集名称"
                  name="name"
                  rules={[
                    { required: true, message: '请输入评测集名称' },
                    { max: 100, message: '名称不能超过100个字符' }
                  ]}
                >
                  <Input placeholder="请输入评测集名称" />
                </Form.Item>
              </div>
              
              <Form.Item
                label="描述"
                name="description"
                rules={[
                  { max: 500, message: '描述不能超过500个字符' }
                ]}
              >
                <Input.TextArea 
                  rows={4} 
                  placeholder="请输入评测集描述"
                  showCount
                  maxLength={500}
                />
              </Form.Item>
            </Form>
          )}
        </Card>
      )}

      {/* Tab导航区域 */}
      <Card>
        <Tabs 
          activeKey={activeTab} 
          onChange={setActiveTab}
          className="mb-6"
        >
          <TabPane tab="数据管理" key="data">
            {/* 操作按钮区域 */}
            <div className="mb-4 flex gap-4 justify-between items-center" style={{flexWrap: 'wrap'}}>
              <Button 
                type="primary" 
                icon={<PlusOutlined />}
                onClick={handleAddData}
              >
                添加数据
              </Button>
              <Button 
                onClick={handleBatchDelete}
                disabled={selectedRowKeys.length === 0}
              >
                批量删除
              </Button>
              <div style={{flex: 1}}></div>
              <Button 
                type="primary"
                onClick={handleSubmitVersion}
              >
                提交新版本
              </Button>
            </div>

            {/* 数据表格 */}
            <Table
                rowSelection={rowSelection}
                dataSource={dataItems}
                rowKey="id"
                loading={dataLoading}
                columns={generateTableColumns()}
                pagination={{
                  current: dataPagination.current,
                  pageSize: dataPagination.pageSize,
                  total: dataPagination.total,
                  showTotal: dataPagination.showTotal,
                  showSizeChanger: dataPagination.showSizeChanger,
                  showQuickJumper: dataPagination.showQuickJumper,
                  pageSizeOptions: dataPagination.pageSizeOptions,
                  onChange: (page, pageSize) => {
                    onDataPaginationChange(page, pageSize);
                    fetchDataItems(page, pageSize || 10);
                  },
                  onShowSizeChange: (page, pageSize) => {
                    onDataShowSizeChange(page, pageSize);
                    fetchDataItems(page, pageSize);
                  }
                }}
              />
          </TabPane>

          <TabPane tab="版本记录" key="version">
            <Table
                dataSource={versions}
                rowKey="id"
                loading={versionsLoading}
                columns={[
                  { 
                    title: '版本号', 
                    dataIndex: 'version',
                    width: '15%',
                    render: (version: string) => (
                      <Tag color="blue">{version}</Tag>
                    )
                  },
                  { 
                    title: '描述', 
                    dataIndex: 'description',
                    width: '35%'
                  },
                  { 
                    title: '数据量', 
                    dataIndex: 'dataCount',
                    width: '15%',
                    render: (count: number) => `${count} 条`
                  },
                  // { 
                  //   title: '创建人', 
                  //   dataIndex: 'creator',
                  //   width: '15%'
                  // },
                  { 
                    title: '创建时间', 
                    dataIndex: 'createTime',
                    width: '20%',
                    render: (text: string) => formatDateTime(text)
                  }
                ]}
                pagination={{
                  current: versionsPagination.current,
                  pageSize: versionsPagination.pageSize,
                  total: versionsPagination.total,
                  showTotal: versionsPagination.showTotal,
                  showSizeChanger: versionsPagination.showSizeChanger,
                  showQuickJumper: versionsPagination.showQuickJumper,
                  pageSizeOptions: versionsPagination.pageSizeOptions,
                  onChange: (page, pageSize) => {
                    onVersionsPaginationChange(page, pageSize);
                    fetchVersions(page, pageSize || 10);
                  },
                  onShowSizeChange: (page, pageSize) => {
                    onVersionsShowSizeChange(page, pageSize);
                    fetchVersions(page, pageSize);
                  }
                }}
              />
              
              {!versionsLoading && versions.length === 0 && (
                <div className="text-center py-8 text-gray-500">
                  <div className="text-lg mb-2">暂无版本记录</div>
                  <div>创建新版本后将在此显示</div>
                </div>
              )}
          </TabPane>

          <TabPane tab="关联实验" key="experiment">
            <Table
                dataSource={experiments}
                rowKey="id"
                loading={experimentsLoading}
                columns={[
                  { 
                    title: '版本号', 
                    dataIndex: 'version',
                    width: '15%',
                    render: (version: string) => (
                      <Tag color="cyan">{version}</Tag>
                    )
                  },
                  { 
                    title: '实验名称', 
                    dataIndex: 'name',
                    width: '40%',
                    render: (name: string, record: any) => (
                      <span 
                        className="text-blue-600 cursor-pointer hover:text-blue-800 hover:underline font-medium"
                        onClick={() => {
                          // 跳转到实验详情页面
                          navigate(`/evaluation-experiment/detail/${record.id}`);
                        }}
                      >
                        {name}
                      </span>
                    )
                  },
                  { 
                    title: '状态', 
                    dataIndex: 'status',
                    width: '15%',
                    render: (status: string) => {
                      const statusConfig = {
                        'RUNNING': { color: 'processing', text: '运行中' },
                        'COMPLETED': { color: 'success', text: '已完成' },
                        'FAILED': { color: 'error', text: '已停止' },
                        'WAITING': { color: 'default', text: '等待中' },
                        '运行中': { color: 'processing', text: '运行中' },
                        '已完成': { color: 'success', text: '已完成' },
                        '已停止': { color: 'error', text: '已停止' },
                        '等待中': { color: 'default', text: '等待中' }
                      };
                      const config = statusConfig[status as keyof typeof statusConfig] || statusConfig['等待中'];
                      return <Tag color={config.color}>{config.text}</Tag>;
                    }
                  },
                  { 
                    title: '创建时间', 
                    dataIndex: 'createTime',
                    width: '30%',
                    render: (text: string) => formatDateTime(text)
                  }
                ]}
                pagination={{
                  current: experimentsPagination.current,
                  pageSize: experimentsPagination.pageSize,
                  total: experimentsPagination.total,
                  showTotal: experimentsPagination.showTotal,
                  showSizeChanger: experimentsPagination.showSizeChanger,
                  showQuickJumper: experimentsPagination.showQuickJumper,
                  pageSizeOptions: experimentsPagination.pageSizeOptions,
                  onChange: (page, pageSize) => {
                    onExperimentsPaginationChange(page, pageSize);
                    fetchExperiments(page, pageSize || 10);
                  },
                  onShowSizeChange: (page, pageSize) => {
                    onExperimentsShowSizeChange(page, pageSize);
                    fetchExperiments(page, pageSize);
                  }
                }}
              />
              
              {!experimentsLoading && experiments.length === 0 && (
                <div className="text-center py-8 text-gray-500">
                  <div className="text-lg mb-2">暂无关联实验</div>
                  <div>创建实验后将在此显示</div>
                </div>
              )}
          </TabPane>
        </Tabs>
      </Card>

      {/* 添加数据弹窗 */}
      <Modal
        title="添加数据"
        open={addDataModalVisible}
        onCancel={handleAddDataCancel}
        footer={[
          <Button key="cancel" onClick={handleAddDataCancel}>
            取消
          </Button>,
          <Button 
            key="submit" 
            type="primary" 
            loading={addingData}
            onClick={handleAddDataSubmit}
          >
            确定
          </Button>,
        ]}
        width={700}
        style={{
          maxHeight: '90vh',
          top: 20
        }}
        bodyStyle={{
          maxHeight: 'calc(90vh - 110px)',
          overflowY: 'auto',
          padding: '20px 24px'
        }}
        destroyOnClose
      >
        <div className="add-data-form-container">
          <Form
            form={addDataForm}
            layout="vertical"
          >
            {/* 根据columnsConfig动态生成表单字段 */}
            {detail?.columnsConfig?.map((column, index) => (
              <Form.Item
                key={column.name}
                label={column.name}
                name={column.name}
                rules={[
                  ...(column.required ? [{ required: true, message: `请输入${column.description || column.name}内容` }] : []),
                  { max: 1000, message: `${column.description || column.name}内容不能超过1000个字符` }
                ]}
              >
                <Input.TextArea 
                  rows={4} 
                  placeholder={`输入${column.description || column.name}内容`}
                  showCount
                  maxLength={1000}
                  style={{ resize: 'none' }}
                />
              </Form.Item>
            ))}
            
            <Form.Item
              label="备注（可选）"
              name="remark"
              rules={[
                { max: 200, message: '备注不能超过200个字符' }
              ]}
            >
              <Input.TextArea 
                rows={2} 
                placeholder="请输入备注信息（可选）"
                showCount
                maxLength={200}
              />
            </Form.Item>
          </Form>
        </div>
      </Modal>
    </div>
  );
};

export default GatherDetail;