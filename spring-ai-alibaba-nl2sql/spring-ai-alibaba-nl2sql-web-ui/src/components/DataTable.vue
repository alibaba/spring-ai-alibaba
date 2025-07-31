<!--
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<template>
  <div class="data-table-container">
    <!-- 表格工具栏 -->
    <div v-if="showToolbar" class="table-toolbar">
      <div class="toolbar-left">
        <slot name="toolbar-left">
          <div v-if="selectedRows.length > 0" class="batch-actions">
            <span class="selected-count">已选择 {{ selectedRows.length }} 项</span>
            <slot name="batch-actions" :selectedRows="selectedRows">
              <button class="btn btn-sm btn-outline" @click="clearSelection">
                取消选择
              </button>
            </slot>
          </div>
        </slot>
      </div>
      <div class="toolbar-right">
        <slot name="toolbar-right">
          <button v-if="refreshable" class="btn btn-sm btn-outline" @click="handleRefresh">
            <i class="bi bi-arrow-clockwise"></i>
            刷新
          </button>
        </slot>
      </div>
    </div>

    <!-- 表格容器 -->
    <div class="table-wrapper" :class="{ loading: loading }">
      <table class="data-table" :class="tableClass">
        <!-- 表头 -->
        <thead>
          <tr>
            <th v-if="selectable" class="selection-column">
              <input
                type="checkbox"
                :checked="isAllSelected"
                :indeterminate="isPartialSelected"
                @change="toggleSelectAll"
              />
            </th>
            <th
              v-for="column in columns"
              :key="column.key"
              :class="[
                column.className,
                { sortable: column.sortable, sorted: sortColumn === column.key }
              ]"
              :style="{ width: column.width, minWidth: column.minWidth }"
              @click="column.sortable && handleSort(column.key)"
            >
              <div class="th-content">
                <span>{{ column.title }}</span>
                <div v-if="column.sortable" class="sort-icons">
                  <i
                    class="bi bi-caret-up-fill"
                    :class="{ active: sortColumn === column.key && sortOrder === 'asc' }"
                  ></i>
                  <i
                    class="bi bi-caret-down-fill"
                    :class="{ active: sortColumn === column.key && sortOrder === 'desc' }"
                  ></i>
                </div>
              </div>
            </th>
          </tr>
        </thead>

        <!-- 表体 -->
        <tbody>
          <tr v-if="loading" class="loading-row">
            <td :colspan="totalColumns" class="loading-cell">
              <div class="loading-content">
                <div class="spinner"></div>
                <span>加载中...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="data.length === 0" class="empty-row">
            <td :colspan="totalColumns" class="empty-cell">
              <div class="empty-content">
                <slot name="empty">
                  <i class="bi bi-inbox"></i>
                  <span>{{ emptyText }}</span>
                </slot>
              </div>
            </td>
          </tr>
          <tr
            v-else
            v-for="(row, index) in data"
            :key="getRowKey(row, index)"
            class="data-row"
            :class="[
              { selected: isRowSelected(row), hover: hoverable },
              getRowClassName(row, index)
            ]"
            @click="handleRowClick(row, index)"
          >
            <td v-if="selectable" class="selection-column">
              <input
                type="checkbox"
                :checked="isRowSelected(row)"
                @change="toggleRowSelection(row)"
                @click.stop
              />
            </td>
            <td
              v-for="column in columns"
              :key="column.key"
              :class="column.className"
              :style="{ width: column.width, minWidth: column.minWidth }"
            >
              <div class="cell-content">
                <slot
                  :name="column.key"
                  :row="row"
                  :column="column"
                  :index="index"
                  :value="getCellValue(row, column.key)"
                >
                  {{ formatCellValue(getCellValue(row, column.key), column) }}
                </slot>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页器 -->
    <div v-if="pagination && data.length > 0" class="table-pagination">
      <div class="pagination-info">
        显示第 {{ (pagination.current - 1) * pagination.pageSize + 1 }} 到 
        {{ Math.min(pagination.current * pagination.pageSize, pagination.total) }} 条，
        共 {{ pagination.total }} 条
      </div>
      <div class="pagination-controls">
        <button
          class="btn btn-sm btn-outline"
          :disabled="pagination.current <= 1"
          @click="handlePageChange(pagination.current - 1)"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ pagination.current }} / {{ Math.ceil(pagination.total / pagination.pageSize) }} 页
        </span>
        <button
          class="btn btn-sm btn-outline"
          :disabled="pagination.current >= Math.ceil(pagination.total / pagination.pageSize)"
          @click="handlePageChange(pagination.current + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'

export default {
  name: 'DataTable',
  props: {
    data: {
      type: Array,
      default: () => []
    },
    columns: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    selectable: {
      type: Boolean,
      default: false
    },
    hoverable: {
      type: Boolean,
      default: true
    },
    showToolbar: {
      type: Boolean,
      default: true
    },
    refreshable: {
      type: Boolean,
      default: false
    },
    emptyText: {
      type: String,
      default: '暂无数据'
    },
    rowKey: {
      type: [String, Function],
      default: 'id'
    },
    rowClassName: {
      type: [String, Function],
      default: ''
    },
    tableClass: {
      type: String,
      default: ''
    },
    pagination: {
      type: Object,
      default: null
    }
  },
  emits: [
    'row-click',
    'selection-change',
    'sort-change',
    'page-change',
    'refresh'
  ],
  setup(props, { emit }) {
    const selectedRows = ref([])
    const sortColumn = ref('')
    const sortOrder = ref('asc')

    const totalColumns = computed(() => {
      return props.columns.length + (props.selectable ? 1 : 0)
    })

    const isAllSelected = computed(() => {
      return props.data.length > 0 && selectedRows.value.length === props.data.length
    })

    const isPartialSelected = computed(() => {
      return selectedRows.value.length > 0 && selectedRows.value.length < props.data.length
    })

    const getRowKey = (row, index) => {
      if (typeof props.rowKey === 'function') {
        return props.rowKey(row, index)
      }
      return row[props.rowKey] || index
    }

    const getRowClassName = (row, index) => {
      if (typeof props.rowClassName === 'function') {
        return props.rowClassName(row, index)
      }
      return props.rowClassName
    }

    const getCellValue = (row, key) => {
      return key.split('.').reduce((obj, k) => obj?.[k], row)
    }

    const formatCellValue = (value, column) => {
      if (column.formatter && typeof column.formatter === 'function') {
        return column.formatter(value)
      }
      if (value === null || value === undefined) {
        return '-'
      }
      return value
    }

    const isRowSelected = (row) => {
      const rowKey = getRowKey(row)
      return selectedRows.value.some(selectedRow => getRowKey(selectedRow) === rowKey)
    }

    const toggleRowSelection = (row) => {
      const rowKey = getRowKey(row)
      const index = selectedRows.value.findIndex(selectedRow => getRowKey(selectedRow) === rowKey)
      
      if (index > -1) {
        selectedRows.value.splice(index, 1)
      } else {
        selectedRows.value.push(row)
      }
      
      emit('selection-change', selectedRows.value)
    }

    const toggleSelectAll = () => {
      if (isAllSelected.value) {
        selectedRows.value = []
      } else {
        selectedRows.value = [...props.data]
      }
      emit('selection-change', selectedRows.value)
    }

    const clearSelection = () => {
      selectedRows.value = []
      emit('selection-change', selectedRows.value)
    }

    const handleRowClick = (row, index) => {
      emit('row-click', row, index)
    }

    const handleSort = (column) => {
      if (sortColumn.value === column) {
        sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
      } else {
        sortColumn.value = column
        sortOrder.value = 'asc'
      }
      
      emit('sort-change', {
        column: sortColumn.value,
        order: sortOrder.value
      })
    }

    const handlePageChange = (page) => {
      emit('page-change', page)
    }

    const handleRefresh = () => {
      emit('refresh')
    }

    // 监听数据变化，清除无效的选中项
    watch(() => props.data, (newData) => {
      if (selectedRows.value.length > 0) {
        const validSelectedRows = selectedRows.value.filter(selectedRow => {
          const selectedRowKey = getRowKey(selectedRow)
          return newData.some(row => getRowKey(row) === selectedRowKey)
        })
        
        if (validSelectedRows.length !== selectedRows.value.length) {
          selectedRows.value = validSelectedRows
          emit('selection-change', selectedRows.value)
        }
      }
    }, { deep: true })

    return {
      selectedRows,
      sortColumn,
      sortOrder,
      totalColumns,
      isAllSelected,
      isPartialSelected,
      getRowKey,
      getRowClassName,
      getCellValue,
      formatCellValue,
      isRowSelected,
      toggleRowSelection,
      toggleSelectAll,
      clearSelection,
      handleRowClick,
      handleSort,
      handlePageChange,
      handleRefresh
    }
  }
}
</script>

<style scoped>
.data-table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 6px;
}

.selected-count {
  color: #1890ff;
  font-weight: 500;
  font-size: 14px;
}

.table-wrapper {
  position: relative;
  overflow-x: auto;
}

.table-wrapper.loading {
  pointer-events: none;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  vertical-align: middle;
}

.data-table th {
  background: #fafafa;
  font-weight: 500;
  color: #262626;
  position: sticky;
  top: 0;
  z-index: 10;
}

.data-table th.sortable {
  cursor: pointer;
  user-select: none;
}

.data-table th.sortable:hover {
  background: #f0f0f0;
}

.th-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sort-icons {
  display: flex;
  flex-direction: column;
  margin-left: 8px;
  opacity: 0.3;
  transition: opacity 0.2s;
}

.data-table th.sortable:hover .sort-icons,
.data-table th.sorted .sort-icons {
  opacity: 1;
}

.sort-icons i {
  font-size: 10px;
  line-height: 1;
  color: #8c8c8c;
}

.sort-icons i.active {
  color: #1890ff;
}

.selection-column {
  width: 48px;
  text-align: center;
}

.selection-column input[type="checkbox"] {
  cursor: pointer;
}

.data-row {
  transition: background-color 0.2s;
}

.data-row.hover:hover {
  background: #fafafa;
}

.data-row.selected {
  background: #e6f7ff;
}

.data-row:hover {
  cursor: pointer;
}

.cell-content {
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.loading-row,
.empty-row {
  height: 200px;
}

.loading-cell,
.empty-cell {
  text-align: center;
  vertical-align: middle;
}

.loading-content,
.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #8c8c8c;
}

.loading-content .spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #f0f0f0;
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.empty-content i {
  font-size: 48px;
  color: #d9d9d9;
}

.table-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.pagination-info {
  color: #8c8c8c;
  font-size: 14px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-info {
  color: #262626;
  font-size: 14px;
  font-weight: 500;
}

.btn {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
  background: none;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

.btn-outline {
  border-color: #d9d9d9;
  color: #262626;
}

.btn-outline:hover:not(:disabled) {
  border-color: #1890ff;
  color: #1890ff;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .table-toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .toolbar-left,
  .toolbar-right {
    justify-content: center;
  }
  
  .table-pagination {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
  
  .data-table th,
  .data-table td {
    padding: 8px 12px;
    font-size: 13px;
  }
}
</style>
