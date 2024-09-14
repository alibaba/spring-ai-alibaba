import { x as p, F as c, t as g } from "./copilot-BcASoA3D.js";
import { i as u, r as f } from "./icons-C4lgWZRy.js";
import { B as h } from "./base-panel-B5Bf009A.js";
import { a as m } from "./copilot-notification-jGQjkdF7.js";
const v = "copilot-features-panel{padding:var(--space-100);font:var(--font-xsmall);display:grid;grid-template-columns:auto 1fr;gap:var(--space-50);height:auto}copilot-features-panel a{display:flex;align-items:center;gap:var(--space-50);white-space:nowrap}copilot-features-panel a svg{height:12px;width:12px;min-height:12px;min-width:12px}";
var b = Object.defineProperty, F = Object.getOwnPropertyDescriptor, d = (e, t, a, s) => {
  for (var r = s > 1 ? void 0 : s ? F(t, a) : t, o = e.length - 1, l; o >= 0; o--)
    (l = e[o]) && (r = (s ? l(t, a, r) : l(r)) || r);
  return s && r && b(t, a, r), r;
};
const n = window.Vaadin.devTools;
let i = class extends h {
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
            >learn more ${u.linkExternal}</a
          >
        `
    )}`;
  }
  toggleFeatureFlag(e, t) {
    const a = e.target.checked;
    n.frontendConnection ? (n.frontendConnection.send("setFeature", { featureId: t.id, enabled: a }), m({
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
const $ = {
  header: "Features",
  expanded: !0,
  draggable: !0,
  panelOrder: 20,
  panel: "right",
  floating: !1,
  tag: "copilot-features-panel"
}, w = {
  init(e) {
    e.addPanel($);
  }
};
window.Vaadin.copilot.plugins.push(w);
export {
  i as CopilotFeaturesPanel
};
