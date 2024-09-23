import 'Frontend/generated/jar-resources/copilot/copilot.js';
// @ts-ignore
if (import.meta.hot) {
  // @ts-ignore
  import.meta.hot.on('vite:afterUpdate', () => {
    const eventbus = (window as any).Vaadin.copilot.eventbus;
    if (eventbus) {
      eventbus.emit('vite-after-update',{});
    }
  });
}

import '@vaadin/vertical-layout/theme/lumo/vaadin-vertical-layout.js';
import '@vaadin/horizontal-layout/theme/lumo/vaadin-horizontal-layout.js';
import '@vaadin/context-menu/theme/lumo/vaadin-context-menu.js';
import '@vaadin/checkbox/theme/lumo/vaadin-checkbox.js';
import '@vaadin/text-field/theme/lumo/vaadin-text-field.js';
import '@vaadin/text-area/theme/lumo/vaadin-text-area.js';
import '@vaadin/menu-bar/theme/lumo/vaadin-menu-bar.js';
import '@vaadin/grid/theme/lumo/vaadin-grid.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-tree-column.js';
import '@vaadin/details/theme/lumo/vaadin-details.js';
import '@vaadin/select/theme/lumo/vaadin-select.js';
import '@vaadin/overlay/theme/lumo/vaadin-overlay.js';
import '@vaadin/list-box/theme/lumo/vaadin-list-box.js';
import '@vaadin/combo-box/theme/lumo/vaadin-combo-box.js';
import '@vaadin/item/theme/lumo/vaadin-item.js';
import '@vaadin/dialog/theme/lumo/vaadin-dialog.js';
import '@vaadin/multi-select-combo-box/theme/lumo/vaadin-multi-select-combo-box.js';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/icon/vaadin-icon.js';
import client from 'Frontend/generated/connect-client.default.js';
let generatedId = 0;
const copilotMiddleware: any = async function(
  context: any,
  next: any
) {
    generatedId++;
    const requestData = { endpoint: context.endpoint, method: context.method, params: context.params, id: generatedId };
    (window as any).Vaadin.copilot.eventbus.emit('endpoint-request', requestData);

    const response: Response = await next(context);
    const text = await response.clone().text();
    const responseData = { text, id: generatedId} ;
    (window as any).Vaadin.copilot.eventbus.emit('endpoint-response', responseData);

    return response;
};

client.middlewares = [...client.middlewares, copilotMiddleware];

import './vaadin-featureflags.js';

import './index';

import './vaadin-react.js';
import 'Frontend/generated/jar-resources/vaadin-dev-tools/vaadin-dev-tools.js';

import { Outlet } from 'react-router-dom';
(window as any).Vaadin ??= {};
(window as any).Vaadin.copilot ??= {};
(window as any).Vaadin.copilot._ref ??= {};
(window as any).Vaadin.copilot._ref.Outlet = Outlet;
