import { EndpointRequestInit as EndpointRequestInit_1 } from "@vaadin/hilla-frontend";
import type BookingDetails_1 from "./ai/spring/demo/ai/playground/services/BookingTools/BookingDetails.js";
import client_1 from "./connect-client.default.js";
async function getBookings_1(init?: EndpointRequestInit_1): Promise<Array<BookingDetails_1>> { return client_1.call("BookingService", "getBookings", {}, init); }
export { getBookings_1 as getBookings };
