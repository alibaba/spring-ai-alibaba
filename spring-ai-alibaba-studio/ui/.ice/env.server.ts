// Define process.env in top make it possible to use ICE_CORE_* in @ice/runtime, esbuild define options doesn't have the ability
// The runtime value such as __process.env.ICE_CORE_*__ will be replaced by esbuild define, so the value is real-time

process.env.ICE_CORE_MODE = __process.env.ICE_CORE_MODE__;
process.env.ICE_CORE_ROUTER = __process.env.ICE_CORE_ROUTER__;
process.env.ICE_CORE_ERROR_BOUNDARY = __process.env.ICE_CORE_ERROR_BOUNDARY__;
process.env.ICE_CORE_INITIAL_DATA = __process.env.ICE_CORE_INITIAL_DATA__;
process.env.ICE_CORE_DEV_PORT = __process.env.ICE_CORE_DEV_PORT__;
process.env.ICE_CORE_REMOVE_ROUTES_CONFIG = __process.env.ICE_CORE_REMOVE_ROUTES_CONFIG__;
process.env.ICE_CORE_REMOVE_DATA_LOADER = __process.env.ICE_CORE_REMOVE_DATA_LOADER__;