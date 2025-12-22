import { useState } from "react";

const usePagination = () => {

  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ['10', '20', '50', '100'],
    showTotal: (total: number, range: number[]) => {
      return `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
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