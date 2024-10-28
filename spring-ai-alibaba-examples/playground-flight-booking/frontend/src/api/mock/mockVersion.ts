import Mock from "mockjs";
Mock.mock("/mock/version", "get", {
  code: 200,
  message: "成功",
  data: {
    gitVersion: "dubbo-admin-",
    gitCommit: "$Format:%H$",
    gitTreeState: "",
    buildDate: "1970-01-01T00:00:00Z",
    goVersion: "go1.20.4",
    compiler: "gc",
    platform: "darwin/arm64",
  },
});
