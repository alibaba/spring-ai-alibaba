import { useState } from "react";
import $i18n from '@/i18n';

const usePagination = () => {

  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ['10', '20', '50', '100'],
    showTotal: (total: number, range: number[]) => {
      return $i18n.get(
        {
          id: 'legacy.pagination.showTotal',
          dm: '第 {start}-{end} 条，共 {total} 条',
        },
        { start: range[0], end: range[1], total },
      );
    },
  });

  const onChange = (page: number, pageSize: number) => {
    setPagination({
      ...pagination,
      current: page,
      pageSize: pageSize,
    });
  };

  const onShowSizeChange = (page: number, pageSize: number) => {
    setPagination({
      ...pagination,
      current: page,
      pageSize: pageSize,
    });
  };

  return {
    setPagination,
    pagination,
    onPaginationChange: onChange,
    onShowSizeChange,
  }

};

export default usePagination;
