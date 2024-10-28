import type { TableColumnsType } from "ant-design-vue";
import { reactive } from "vue";

export type DICT_TYPE = "SELECT" | "BUTTON" | "RADIO";

export class SearchDomain {
  // form of search
  noPaged?: boolean;
  queryForm: any;
  params: [
    {
      label: string;
      param: string;
      defaultValue: string;
      style?: any;
      dict: [
        {
          label: string;
          value: string;
        },
      ];
      dictType: DICT_TYPE;
    },
  ];
  searchApi: Function;
  result: any;
  handleResult?: Function;
  tableStyle: any;
  table: {
    loading?: boolean;
    columns: (TableColumnsType & { __hide: boolean }) | any;
  } = { columns: [] };
  paged = {
    curPage: 1,
    pageOffset: "0",
    total: 0,
    pageSize: 10,
  };

  constructor(
    query: any,
    searchApi: any,
    columns: TableColumnsType | any,
    paged?: any | undefined,
    noPaged?: boolean,
    handleResult?: Function,
  ) {
    this.params = query;
    this.noPaged = noPaged;
    this.queryForm = reactive({});
    this.table.columns = columns;
    query.forEach((c: any) => {
      if (c.defaultValue) {
        this.queryForm[c.param] = c.defaultValue;
      }
    });
    if (paged) {
      this.paged = { ...this.paged, ...paged };
    }
    this.searchApi = searchApi;
    handleResult && this.onSearch(handleResult);
  }

  async onSearch(handleResult: Function) {
    this.table.loading = true;
    setTimeout(() => {
      this.table.loading = false;
    }, 5000);
    const queryParams = {
      ...this.queryForm,
      ...(this.noPaged
        ? {}
        : {
            pageSize: this.paged.pageSize,
            pageOffset: (this.paged.curPage - 1) * this.paged.pageSize,
          }),
    };

    try {
      const {
        data: { list, pageInfo },
      } = await this.searchApi(queryParams);
      this.result = handleResult ? handleResult(list) : list;

      if (!this.noPaged) {
        this.paged.total = pageInfo?.Total || 0;
      }
    } catch (error) {
      console.error("Error fetching data:", error);
    }
    this.table.loading = false;
  }
}

export function sortString(a: any, b: any) {
  if (!isNaN(a - b)) {
    return a - b;
  }
  return a.localeCompare(b);
}
