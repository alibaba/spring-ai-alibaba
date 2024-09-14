import { I as A, b as p, x as i, F as D, T as u, M as $, c as P, t as k, J as j, V as E, K as S } from "./copilot-BcASoA3D.js";
import { r as v, i as J } from "./icons-C4lgWZRy.js";
import { B as R } from "./base-panel-B5Bf009A.js";
import { a as H } from "./copilot-notification-jGQjkdF7.js";
import { a as V } from "./_commonjsHelpers-Dn0DSpot.js";
const _ = "copilot-info-panel{--dev-tools-red-color: red;--dev-tools-grey-color: gray;--dev-tools-green-color: green;position:relative}copilot-info-panel div.info-tray{display:flex;flex-direction:column;gap:10px}copilot-info-panel dl{display:grid;grid-template-columns:auto auto;gap:0;margin:var(--space-100) var(--space-50);font:var(--font-xsmall)}copilot-info-panel dl>dt,copilot-info-panel dl>dd{padding:3px 10px;margin:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}copilot-info-panel dd.live-reload-status>span{overflow:hidden;text-overflow:ellipsis;display:block;color:var(--status-color)}copilot-info-panel dd span.hidden{display:none}copilot-info-panel dd span.true{color:var(--dev-tools-green-color);font-size:large}copilot-info-panel dd span.false{color:var(--dev-tools-red-color);font-size:large}copilot-info-panel li{list-style:none}";
var F = function() {
  var e = document.getSelection();
  if (!e.rangeCount)
    return function() {
    };
  for (var t = document.activeElement, a = [], s = 0; s < e.rangeCount; s++)
    a.push(e.getRangeAt(s));
  switch (t.tagName.toUpperCase()) {
    case "INPUT":
    case "TEXTAREA":
      t.blur();
      break;
    default:
      t = null;
      break;
  }
  return e.removeAllRanges(), function() {
    e.type === "Caret" && e.removeAllRanges(), e.rangeCount || a.forEach(function(r) {
      e.addRange(r);
    }), t && t.focus();
  };
}, T = F, y = {
  "text/plain": "Text",
  "text/html": "Url",
  default: "Text"
}, U = "Copy to clipboard: #{key}, Enter";
function O(e) {
  var t = (/mac os x/i.test(navigator.userAgent) ? "⌘" : "Ctrl") + "+C";
  return e.replace(/#{\s*key\s*}/g, t);
}
function B(e, t) {
  var a, s, r, o, l, n, h = !1;
  t || (t = {}), a = t.debug || !1;
  try {
    r = T(), o = document.createRange(), l = document.getSelection(), n = document.createElement("span"), n.textContent = e, n.ariaHidden = "true", n.style.all = "unset", n.style.position = "fixed", n.style.top = 0, n.style.clip = "rect(0, 0, 0, 0)", n.style.whiteSpace = "pre", n.style.webkitUserSelect = "text", n.style.MozUserSelect = "text", n.style.msUserSelect = "text", n.style.userSelect = "text", n.addEventListener("copy", function(c) {
      if (c.stopPropagation(), t.format)
        if (c.preventDefault(), typeof c.clipboardData > "u") {
          a && console.warn("unable to use e.clipboardData"), a && console.warn("trying IE specific stuff"), window.clipboardData.clearData();
          var m = y[t.format] || y.default;
          window.clipboardData.setData(m, e);
        } else
          c.clipboardData.clearData(), c.clipboardData.setData(t.format, e);
      t.onCopy && (c.preventDefault(), t.onCopy(c.clipboardData));
    }), document.body.appendChild(n), o.selectNodeContents(n), l.addRange(o);
    var x = document.execCommand("copy");
    if (!x)
      throw new Error("copy command was unsuccessful");
    h = !0;
  } catch (c) {
    a && console.error("unable to copy using execCommand: ", c), a && console.warn("trying IE specific stuff");
    try {
      window.clipboardData.setData(t.format || "text", e), t.onCopy && t.onCopy(window.clipboardData), h = !0;
    } catch (m) {
      a && console.error("unable to copy using clipboardData: ", m), a && console.error("falling back to prompt"), s = O("message" in t ? t.message : U), window.prompt(s, e);
    }
  } finally {
    l && (typeof l.removeRange == "function" ? l.removeRange(o) : l.removeAllRanges()), n && document.body.removeChild(n), r();
  }
  return h;
}
var N = B;
const L = /* @__PURE__ */ V(N);
var M = Object.defineProperty, W = Object.getOwnPropertyDescriptor, g = (e, t, a, s) => {
  for (var r = s > 1 ? void 0 : s ? W(t, a) : t, o = e.length - 1, l; o >= 0; o--)
    (l = e[o]) && (r = (s ? l(t, a, r) : l(r)) || r);
  return s && r && M(t, a, r), r;
};
const b = i`<a
  href="${j}"
  target="_blank"
  @click="${() => C("idea")}"
  title="Get IntelliJ plugin"
  >Get IntelliJ plugin</a
>`, w = i`<a
  href="${E}"
  target="_blank"
  @click="${() => C("vscode")}"
  title="Get VS Code plugin"
  >Get VS Code plugin</a
>`;
function C(e) {
  return S("get-plugin", e), !1;
}
let f = class extends R {
  constructor() {
    super(...arguments), this.serverInfo = [], this.clientInfo = [{ name: "Browser", version: navigator.userAgent }], this.handleServerInfoEvent = (e) => {
      const t = JSON.parse(e.data.info);
      this.serverInfo = t.versions, this.jdkInfo = t.jdkInfo, this.updateIdePluginInfo(), A().then((a) => {
        a && (this.clientInfo.unshift({ name: "Vaadin Employee", version: "true", more: void 0 }), this.requestUpdate("clientInfo"));
      });
    };
  }
  connectedCallback() {
    super.connectedCallback(), this.onCommand("copilot-info", this.handleServerInfoEvent), this.onEventBus("system-info-with-callback", (e) => {
      e.detail.callback(this.getInfoForClipboard(e.detail.notify));
    }), this.reaction(
      () => p.idePluginState,
      () => {
        this.updateIdePluginInfo(), this.requestUpdate("serverInfo");
      }
    );
  }
  updateIdePluginInfo() {
    const e = this.getIndex("Copilot IDE Plugin");
    let t = "false", a;
    p.idePluginState?.active ? t = `${p.idePluginState.version}-${p.idePluginState.ide}` : p.idePluginState?.ide === "vscode" ? a = w : p.idePluginState?.ide === "idea" ? a = b : a = i`${b} or ${w}`, this.serverInfo[e].version = t, this.serverInfo[e].more = a;
  }
  getIndex(e) {
    return this.serverInfo.findIndex((t) => t.name === e);
  }
  render() {
    return i`<style>
        ${_}
      </style>
      <div class="info-tray">
        <dl>
          ${[...this.serverInfo, ...this.clientInfo].map(
      (e) => i`
              <dt>${e.name}</dt>
              <dd title="${e.version}" style="${e.name === "Java Hotswap" ? "white-space: normal" : ""}">
                ${this.renderVersion(e)} ${e.more}
              </dd>
            `
    )}
        </dl>
      </div>`;
  }
  renderVersion(e) {
    return e.name === "Java Hotswap" ? this.renderJavaHotswap() : this.renderValue(e.version);
  }
  renderValue(e) {
    return e === "false" ? d(!1) : e === "true" ? d(!0) : e;
  }
  getInfoForClipboard(e) {
    const t = this.renderRoot.querySelectorAll(".info-tray dt"), r = Array.from(t).map((o) => ({
      key: o.textContent.trim(),
      value: o.nextElementSibling.textContent.trim()
    })).filter((o) => o.key !== "Live reload").filter((o) => !o.key.startsWith("Vaadin Emplo")).map((o) => {
      const { key: l } = o;
      let { value: n } = o;
      return l === "Copilot IDE Plugin" && !p.idePluginState?.active ? n = "false" : l === "Java Hotswap" && (n = String(n.includes("JRebel is in use") || n.includes("HotswapAgent is in use"))), `${l}: ${n}`;
    }).join(`
`);
    return e && H({
      type: D.INFORMATION,
      message: "Environment information copied to clipboard",
      dismissId: "versionInfoCopied"
    }), r.trim();
  }
  renderJavaHotswap() {
    if (!this.jdkInfo)
      return u;
    const e = this.jdkInfo.extendedClassDefCapable && this.jdkInfo.runningWithExtendClassDef && this.jdkInfo.hotswapAgentFound && this.jdkInfo.runningWitHotswap && this.jdkInfo.hotswapAgentPluginsFound, t = this.jdkInfo.jrebel;
    return !this.jdkInfo.extendedClassDefCapable && !t ? i`<details>
        <summary>${d(!1)} No Hotswap solution in use</summary>
        <p>To enable hotswap for Java, you can either use HotswapAgent or JRebel.</p>
        <p>HotswapAgent is an open source project that utilizes the JetBrains Runtime (JDK).</p>
        <ul>
          <li>If you are running IntelliJ, edit the launch configuration to use the bundled JDK.</li>
          <li>
            Otherwise, download it from
            <a target="_blank" href="https://github.com/JetBrains/JetBrainsRuntime/releases"
              >the JetBrains release page</a
            >
            to get started.
          </li>
        </ul>
        <p>
          JRebel is a commercial solution available from
          <a target="_blank" href="https://www.jrebel.com/">jrebel.com</a>
        </p>
      </details>` : t ? i`${d(!0)} JRebel is in use` : e ? i`${d(!0)} HotswapAgent is in use` : i`<details>
      <summary>${d(!1)} HotswapAgent is partially enabled</summary>
      <ul style="margin:0;padding:0">
        <li>${d(this.jdkInfo.extendedClassDefCapable)} JDK supports hotswapping</li>
        <li>
          ${d(this.jdkInfo.runningWithExtendClassDef)} JDK hotswapping
          enabled${this.jdkInfo.runningWithExtendClassDef ? u : i`<br />Add the <code>-XX:+AllowEnhancedClassRedefinition</code> JVM argument when launching the
                application`}
        </li>
        <li>
          ${d(this.jdkInfo.hotswapAgentFound)} HotswapAgent
          installed${this.jdkInfo.hotswapAgentFound ? u : i`<br /><a target="_blank" href="https://github.com/HotswapProjects/HotswapAgent/releases"
                  >Download the latest HotswapAgent</a
                >
                and place it in <code>${this.jdkInfo.hotswapAgentLocation}</code>`}
        </li>
        <li>
          ${d(this.jdkInfo.runningWitHotswap)} HotswapAgent configured
          ${this.jdkInfo.runningWitHotswap ? u : i`<br />Add the <code>-XX:HotswapAgent=fatjar</code> JVM argument when launching the application`}
        </li>
        <li>
          ${d(this.jdkInfo.hotswapAgentPluginsFound)} Vaadin HotswapAgent plugin available
          ${this.jdkInfo.hotswapAgentPluginsFound ? u : i`<div>
                Add src/main/resources/hotswap-agent.properties containing
                <!-- prettier-ignore -->
                <code class="codeblock"><copilot-copy></copilot-copy>pluginPackages=com.vaadin.hilla.devmode.hotswapagent
disabledPlugins=vaadin # The Vaadin/Flow plugin causes extra reloads if enabled</code>
                and restart the application
              </div>`}
        </li>
      </ul>
    </details> `;
  }
};
g([
  v()
], f.prototype, "serverInfo", 2);
g([
  v()
], f.prototype, "clientInfo", 2);
g([
  v()
], f.prototype, "jdkInfo", 2);
f = g([
  k("copilot-info-panel")
], f);
let I = class extends $ {
  createRenderRoot() {
    return this;
  }
  connectedCallback() {
    super.connectedCallback(), this.style.display = "flex";
  }
  render() {
    return i`<button title="Copy to clipboard" aria-label="Copy to clipboard" theme="icon tertiary">
      <span
        @click=${() => {
      P.emit("system-info-with-callback", {
        callback: L,
        notify: !0
      });
    }}
        >${J.copy}</span
      >
    </button>`;
  }
};
I = g([
  k("copilot-info-actions")
], I);
const G = {
  header: "Info",
  expanded: !0,
  draggable: !0,
  panelOrder: 15,
  panel: "right",
  floating: !1,
  tag: "copilot-info-panel",
  actionsTag: "copilot-info-actions"
}, K = {
  init(e) {
    e.addPanel(G);
  }
};
window.Vaadin.copilot.plugins.push(K);
function d(e) {
  return e ? i`<span class="true">☑</span>` : i`<span class="false">☒</span>`;
}
export {
  I as Actions,
  f as CopilotInfoPanel
};
