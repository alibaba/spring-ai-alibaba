import request from "@/base/http/request";

export const getBookings = (params: any): Promise<any> => {
  return request({
    url: "/bookings",
    method: "get",
    params,
  });
};
