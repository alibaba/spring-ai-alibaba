import Mock from "mockjs";

Mock.mock("/mock/metrics/metadata", "get", {
  code: 200,
  message: "成功",
  data: {
    versions: ["dubbo-golang-3.0.4"],
    protocols: ["tri"],
    rules: [
      "DemoService:1.0.0:test.configurators",
      "DemoService4:bb:aa.configurators",
    ],
    configCenter: "127.0.0.1:2181",
    registry: "127.0.0.1:2181",
    metadataCenter: "127.0.0.1:2181",
    // make sure the X-Frame-Options is forbidden
    grafana: `http://${window.location.host}/admin/home`,
    prometheus: "127.0.0.1:9090",
  },
});
