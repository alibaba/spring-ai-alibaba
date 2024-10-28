import Mock from "mockjs";

Mock.mock(/\/search\?searchType=\w+&keywords=\w*/, "get", {
  code: 200,
  message: "成功",
  data: {
    find: true,
    candidates: ["test1", "test2", "tset3"],
  },
});
