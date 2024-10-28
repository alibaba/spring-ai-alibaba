import Mock from "mockjs";

Mock.mock("/mock/destinationRule/search", "get", () => {
  const total = Mock.mock("@integer(8, 1000)");
  const list = [];
  for (let i = 0; i < total; i++) {
    list.push({
      ruleName: "app_" + Mock.mock("@string(2,10)"),
      createTime: Mock.mock("@datetime"),
    });
  }
  return {
    code: 200,
    message: "success",
    data: Mock.mock({
      total: total,
      curPage: 1,
      pageSize: 10,
      data: list,
    }),
  };
});
