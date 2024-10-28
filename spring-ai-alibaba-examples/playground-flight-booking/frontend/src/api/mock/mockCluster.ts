import Mock from "mockjs";
Mock.mock("/mock/metrics/cluster", "get", {
  code: 200,
  message: "成功",
  data: {
    all: Mock.mock("@integer(100, 500)"),
    application: Mock.mock("@integer(80, 200)"),
    consumers: Mock.mock("@integer(80, 200)"),
    providers: Mock.mock("@integer(80, 200)"),
    services: Mock.mock("@integer(80, 200)"),
  },
});
