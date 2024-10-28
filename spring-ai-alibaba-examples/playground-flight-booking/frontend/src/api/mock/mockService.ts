import Mock from "mockjs";
import devTool from "@/utils/DevToolUtil";

Mock.mock(devTool.mockUrl("/mock/service/search"), "get", {
  code: 200,
  message: "success",
  data: {
    total: 8,
    curPage: 1,
    pageSize: 5,
    data: [
      {
        serviceName: "org.apache.dubbo.samples.UserService",
        versionGroup: [
          {
            version: "1.0.0",
            group: "group1",
          },
          {
            version: "1.0.0",
            group: null,
          },
          {
            version: null,
            group: "group1",
          },
          {
            version: null,
            group: null,
          },
        ],
        avgQPS: 6,
        avgRT: "194ms",
        requestTotal: 200,
      },
      {
        serviceName: "org.apache.dubbo.samples.OrderService",
        versionGroup: [
          {
            version: "1.0.0",
            group: "group1",
          },
          {
            version: "1.0.0",
            group: null,
          },
          {
            version: null,
            group: "group1",
          },
          {
            version: null,
            group: null,
          },
        ],
        avgQPS: 13,
        avgRT: "189ms",
        requestTotal: 164,
      },
      {
        serviceName: "org.apache.dubbo.samples.DetailService",
        versionGroup: [
          {
            version: "1.0.0",
            group: "group1",
          },
          {
            version: "1.0.0",
            group: null,
          },
          {
            version: null,
            group: "group1",
          },
          {
            version: null,
            group: null,
          },
        ],
        avgQPS: 0.5,
        avgRT: "268ms",
        requestTotal: 1324,
      },
      {
        serviceName: "org.apache.dubbo.samples.PayService",
        versionGroup: [
          {
            version: "1.0.0",
            group: "group1",
          },
          {
            version: "1.0.0",
            group: null,
          },
          {
            version: null,
            group: "group1",
          },
          {
            version: null,
            group: null,
          },
        ],
        avgQPS: 9,
        avgRT: "346ms",
        requestTotal: 189,
      },
      {
        serviceName: "org.apache.dubbo.samples.CommentService",
        versionGroup: [
          {
            version: "1.0.0",
            group: "group1",
          },
          {
            version: "1.0.0",
            group: null,
          },
          {
            version: null,
            group: "group1",
          },
          {
            version: null,
            group: null,
          },
        ],
        avgQPS: 8,
        avgRT: "936ms",
        requestTotal: 200,
      },
      {
        serviceName: "org.apache.dubbo.samples.RepayService",
        versionGroup: [
          {
            version: "1.0.0",
            group: "group1",
          },
          {
            version: "1.0.0",
            group: null,
          },
          {
            version: null,
            group: "group1",
          },
          {
            version: null,
            group: null,
          },
        ],
        avgQPS: 17,
        avgRT: "240ms",
        requestTotal: 146,
      },
      {
        serviceName: "org.apche.dubbo.samples.TransportService",
        versionGroup: [
          {
            version: "1.0.0",
            group: "group1",
          },
          {
            version: "1.0.0",
            group: null,
          },
          {
            version: null,
            group: "group1",
          },
          {
            version: null,
            group: null,
          },
        ],
        avgQPS: 43,
        avgRT: "89ms",
        requestTotal: 367,
      },
      {
        serviceName: "org.apche.dubbo.samples.DistributionService",
        versionGroup: [
          {
            version: "1.0.0",
            group: "group1",
          },
          {
            version: "1.0.0",
            group: null,
          },
          {
            version: null,
            group: "group1",
          },
          {
            version: null,
            group: null,
          },
        ],
        avgQPS: 4,
        avgRT: "78ms",
        requestTotal: 145,
      },
    ],
  },
});
