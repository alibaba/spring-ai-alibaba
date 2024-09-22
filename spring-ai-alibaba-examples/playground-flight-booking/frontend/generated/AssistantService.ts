import { Subscription as Subscription_1 } from "@vaadin/hilla-frontend";
import client_1 from "./connect-client.default.js";
function chat_1(chatId: string, userMessage: string): Subscription_1<string> { return client_1.subscribe("AssistantService", "chat", { chatId, userMessage }); }
export { chat_1 as chat };
