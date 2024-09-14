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
import '@vaadin/context-menu/theme/lumo/vaadin-context-menu.js';
import '@vaadin/text-field/theme/lumo/vaadin-text-field.js';
import '@vaadin/text-area/theme/lumo/vaadin-text-area.js';
import '@vaadin/menu-bar/theme/lumo/vaadin-menu-bar.js';
import '@vaadin/grid/theme/lumo/vaadin-grid.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-tree-column.js';
import '@vaadin/details/theme/lumo/vaadin-details.js';
import '@vaadin/select/theme/lumo/vaadin-select.js';
import '@vaadin/overlay/theme/lumo/vaadin-overlay.js';
import '@vaadin/list-box/theme/lumo/vaadin-list-box.js';
import '@vaadin/item/theme/lumo/vaadin-item.js';
import '@vaadin/dialog/theme/lumo/vaadin-dialog.js';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/icon/vaadin-icon.js';
import './vaadin-featureflags.js';

import './index';

import 'Frontend/generated/jar-resources/vaadin-dev-tools/vaadin-dev-tools.js';
