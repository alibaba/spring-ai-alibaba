// @ts-nocheck
export function areIndexStatusesEqual(data) {
  const firstStatus = data[0].index_status;
  return data.every((item) => item.index_status === firstStatus);
}
