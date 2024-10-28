import { notification } from "ant-design-vue";

/**
 * get function  invoke stack
 * @param more Offset relative to the default stack number
 */
function getCurrentFunctionLocation(more = 0): string {
  try {
    throw new Error();
  } catch (error: any) {
    // error.stack
    const stackLines = error.stack.split("\n");
    // The forth line typically contains information about the calling location.
    const offset = 2 + more;
    if (offset >= 0 && stackLines.length >= offset) {
      return stackLines[offset].trim();
    }
    return "wrong location";
  }
}

/**
 * custom notification about to-do fun
 * for developer
 * @param todoDetail
 */
const todo = (todoDetail: string) => {
  notification.warn({
    message: `TODO: ${todoDetail} =>: ${devTool.getCurrentFunctionLocation(1)}`,
  });
};

const mockUrl = (raw: string) => {
  return RegExp(raw + ".*");
};

const devTool = {
  getCurrentFunctionLocation,
  todo,
  mockUrl,
};

export default devTool;
