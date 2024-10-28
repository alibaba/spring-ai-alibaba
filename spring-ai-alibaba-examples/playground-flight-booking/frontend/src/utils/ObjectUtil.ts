// todo with no exception handle
export function deepCopy(org: {} | []) {
  return JSON.parse(JSON.stringify(org));
}
