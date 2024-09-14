import { x as p, H as c, t as g } from "./copilot-C5kdwofL.js";
import { r as f } from "./state-DIGAkjaT.js";
import { B as u } from "./base-panel-Bo-7csvK.js";
import { showNotification as h } from "./copilot-notification-COJP6ks5.js";
import { i as m } from "./icons-BFatMQTq.js";
const v = "copilot-features-panel{padding:var(--space-100);font:var(--font-xsmall);display:grid;grid-template-columns:auto 1fr;gap:var(--space-50);height:auto}copilot-features-panel a{display:flex;align-items:center;gap:var(--space-50);white-space:nowrap}copilot-features-panel a svg{height:12px;width:12px;min-height:12px;min-width:12px}";
var b = Object.defineProperty, F = Object.getOwnPropertyDescriptor, d = (e, t, a, r) => {
  for (var o = r > 1 ? void 0 : r ? F(t, a) : t, s = e.length - 1, l; s >= 0; s--)
    (l = e[s]) && (o = (r ? l(t, a, o) : l(o)) || o);
  return r && o && b(t, a, o), o;
};
const n = window.Vaadin.devTools;
let i = class extends u {
  constructor() {
    super(...arguments), this.features = [], this.handleFeatureFlags = (e) => {
      this.features = e.data.features;
    };
  }
  connectedCallback() {
    super.connectedCallback(), this.onCommand("featureFlags", this.handleFeatureFlags);
  }
  render() {
    return p` <style>
        ${v}
      </style>
      ${this.features.map(
      (e) => p`
          <copilot-toggle-button
            .title="${e.title}"
            ?checked=${e.enabled}
            @on-change=${(t) => this.toggleFeatureFlag(t, e)}>
          </copilot-toggle-button>
          <a class="ahreflike" href="${e.moreInfoLink}" title="Learn more" target="_blank"
            >learn more ${m.linkExternal}</a
          >
        `
    )}`;
  }
  toggleFeatureFlag(e, t) {
    const a = e.target.checked;
    n.frontendConnection ? (n.frontendConnection.send("setFeature", { featureId: t.id, enabled: a }), h({
      type: c.INFORMATION,
      message: `“${t.title}” ${a ? "enabled" : "disabled"}`,
      details: t.requiresServerRestart ? "This feature requires a server restart" : void 0,
      dismissId: `feature${t.id}${a ? "Enabled" : "Disabled"}`
    })) : n.log("error", `Unable to toggle feature ${t.title}: No server connection available`);
  }
};
d([
  f()
], i.prototype, "features", 2);
i = d([
  g("copilot-features-panel")
], i);
const w = {
  header: "Features",
  expanded: !0,
  panelOrder: 20,
  panel: "right",
  floating: !1,
  tag: "copilot-features-panel",
  helpUrl: "https://vaadin.com/docs/latest/flow/configuration/feature-flags"
}, $ = {
  init(e) {
    e.addPanel(w);
  }
};
window.Vaadin.copilot.plugins.push($);
export {
  i as CopilotFeaturesPanel
};
