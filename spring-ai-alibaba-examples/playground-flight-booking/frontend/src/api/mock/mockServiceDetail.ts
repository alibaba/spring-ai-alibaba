import Mock from "mockjs";

Mock.mock("/mock/service/detail", "get", {
  code: 200,
  message: "success",
  data: {
    total: 8,
    curPage: 1,
    pageSize: 1,
    data: {
      serviceName: "org.apache.dubbo.samples.UserService",
      versionGroup: ["version=v1", "version=2.0,group=group1"],
      protocol: "triple",
      delay: "3000ms",
      timeOut: "3000ms",
      retry: 3,
      requestTotal: 1384,
      avgRT: "96ms",
      avgQPS: 12,
      obsolete: false,
    },
  },
});
