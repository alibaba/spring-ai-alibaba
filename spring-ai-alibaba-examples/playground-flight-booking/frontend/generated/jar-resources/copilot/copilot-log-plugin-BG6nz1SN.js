import { E as d, e as E, F as c, x as l, G as k, h as R, H as C, M as D, c as M, m as T, t as w } from "./copilot-BcASoA3D.js";
import { i as n, r as x } from "./icons-C4lgWZRy.js";
import { B as S } from "./base-panel-B5Bf009A.js";
const L = "copilot-log-panel{padding:var(--space-100);font:var(--font-xsmall);display:flex;flex-direction:column;gap:var(--space-50);overflow-y:auto}copilot-log-panel .row{display:flex;align-items:flex-start;padding:var(--space-50) var(--space-100);border-radius:var(--radius-2);gap:var(--space-100)}copilot-log-panel .row.information{background-color:var(--blue-50)}copilot-log-panel .row.warning{background-color:var(--yellow-50)}copilot-log-panel .row.error{background-color:var(--red-50)}copilot-log-panel .type{margin-top:var(--space-25)}copilot-log-panel .type.error{color:var(--red)}copilot-log-panel .type.warning{color:var(--yellow)}copilot-log-panel .type.info{color:var(--color)}copilot-log-panel .message{display:flex;flex-direction:column;flex-grow:1;gap:var(--space-25);overflow:hidden}copilot-log-panel .message>*{white-space:nowrap}copilot-log-panel .firstrow{display:flex;align-items:baseline;gap:.5em;flex-direction:column}copilot-log-panel .firstrowmessage{width:100%}copilot-log-panel button{padding:0;border:0;background:transparent}copilot-log-panel svg{height:12px;width:12px}copilot-log-panel .secondrow,copilot-log-panel .timestamp{font-size:var(--font-size-0);line-height:var(--line-height-1)}copilot-log-panel .expand span{height:12px;width:12px}";
var b = Object.defineProperty, _ = Object.getOwnPropertyDescriptor, h = (e, t, a, o) => {
  for (var s = o > 1 ? void 0 : o ? _(t, a) : t, p = e.length - 1, i; p >= 0; p--)
    (i = e[p]) && (s = (o ? i(t, a, s) : i(s)) || s);
  return o && s && b(t, a, s), s;
};
class I {
  constructor() {
    this.showTimestamps = !1, T(this);
  }
  toggleShowTimestamps() {
    this.showTimestamps = !this.showTimestamps;
  }
}
const g = new I();
let r = class extends S {
  constructor() {
    super(), this.unreadErrors = !1, this.messages = [], this.nextMessageId = 1, this.transitionDuration = 0, this.catchErrors();
  }
  connectedCallback() {
    super.connectedCallback(), this.onCommand("log", (e) => {
      this.handleLogEventData({ type: e.data.type, message: e.data.message });
    }), this.onEventBus("log", (e) => this.handleLogEvent(e)), this.onEventBus("update-log", (e) => this.updateLog(e.detail)), this.onEventBus("notification-dismissed", (e) => this.handleNotification(e)), this.onEventBus("clear-log", () => this.clear()), this.transitionDuration = parseInt(
      window.getComputedStyle(this).getPropertyValue("--dev-tools-transition-duration"),
      10
    );
  }
  clear() {
    this.messages = [];
  }
  handleNotification(e) {
    this.log(e.detail.type, e.detail.message, !0, e.detail.details, e.detail.link, void 0);
  }
  handleLogEvent(e) {
    this.handleLogEventData(e.detail);
  }
  handleLogEventData(e) {
    this.log(
      e.type,
      e.message,
      !!e.internal,
      e.details,
      e.link,
      d(e.expandedMessage),
      d(e.expandedDetails),
      e.id
    );
  }
  activate() {
    this.unreadErrors = !1, this.updateComplete.then(() => {
      const e = this.renderRoot.querySelector(".message:last-child");
      e && e.scrollIntoView();
    });
  }
  format(e) {
    return e.message ? e.message.toString() : e.toString();
  }
  catchErrors() {
    const e = window.Vaadin.ConsoleErrors;
    window.Vaadin.ConsoleErrors = {
      push: (t) => {
        E.attentionRequiredPanelTag = v.tag, t[0].type !== void 0 && t[0].message !== void 0 ? this.log(t[0].type, t[0].message, !!t[0].internal, t[0].details, t[0].link) : this.log(c.ERROR, t.map((a) => this.format(a)).join(" "), !1), e.push(t);
      }
    };
  }
  render() {
    return l`<style>
        ${L}
      </style>
      ${this.messages.map((e) => this.renderMessage(e))} `;
  }
  renderMessage(e) {
    let t, a, o;
    return e.type === c.ERROR ? (t = "error", o = n.exclamationMark, a = "Error") : e.type === c.WARNING ? (t = "warning", o = n.warning, a = "Warning") : (t = "info", o = n.info, a = "Info"), e.internal && (t += " internal"), l`
      <div class="row ${e.type} ${e.details || e.link ? "has-details" : ""}">
        <span class="type ${t}" title="${a}">${o}</span>
        <div class="message" @click=${() => this.toggleExpanded(e)}>
          <span class="firstrow">
            <span class="timestamp" ?hidden=${!g.showTimestamps}>${A(e.timestamp)}</span>
            <span class="firstrowmessage"
              >${e.expanded && e.expandedMessage ? e.expandedMessage : e.message}
            </span>
          </span>
          ${e.expanded ? l` <span class="secondrow">${e.expandedDetails}</span>` : l`<span class="secondrow" ?hidden="${!e.details && !e.link}"
                >${d(e.details)}
                ${e.link ? l`<a class="ahreflike" href="${e.link}" target="_blank">Learn more</a>` : ""}</span
              >`}
        </div>
        <button
          aria-label="Expand details"
          theme="icon tertiary"
          class="expand"
          @click=${() => this.toggleExpanded(e)}
          ?hidden=${!e.expandedDetails}>
          <span>${e.expanded ? n.chevronDown : n.chevronRight}</span>
        </button>
      </div>
    `;
  }
  log(e, t, a, o, s, p, i, y) {
    const $ = this.nextMessageId;
    this.nextMessageId += 1;
    const u = k(t, 200);
    for (u !== t && !i && (i = t), this.messages.push({
      id: $,
      type: e,
      message: u,
      details: o,
      link: s,
      dontShowAgain: !1,
      deleted: !1,
      expanded: !1,
      expandedMessage: p,
      expandedDetails: i,
      timestamp: /* @__PURE__ */ new Date(),
      internal: a,
      userId: y
    }); this.messages.length > r.MAX_LOG_ROWS; )
      this.messages.shift();
    this.requestUpdate(), this.updateComplete.then(() => {
      const m = this.renderRoot.querySelector(".message:last-child");
      m ? (setTimeout(() => m.scrollIntoView({ behavior: "smooth" }), this.transitionDuration), this.unreadErrors = !1) : e === c.ERROR && (this.unreadErrors = !0);
    });
  }
  updateLog(e) {
    const t = this.messages.find((a) => a.userId === e.id);
    if (!t) {
      R(`Unable to find message with id ${e.id}`);
      return;
    }
    Object.assign(t, e), C(t.expandedDetails) && (t.expandedDetails = d(t.expandedDetails)), this.requestUpdate();
  }
  toggleExpanded(e) {
    e.expandedDetails && (e.expanded = !e.expanded, this.requestUpdate());
  }
};
r.MAX_LOG_ROWS = 1e3;
h([
  x()
], r.prototype, "unreadErrors", 2);
h([
  x()
], r.prototype, "messages", 2);
r = h([
  w("copilot-log-panel")
], r);
let f = class extends D {
  createRenderRoot() {
    return this;
  }
  connectedCallback() {
    super.connectedCallback(), this.style.display = "flex";
  }
  render() {
    return l`
      <button title="Clear log" aria-label="Clear log" theme="icon tertiary">
        <span
          @click=${() => {
      M.emit("clear-log", {});
    }}
          >${n.trash}</span
        >
      </button>
      <button title="Toggle timestamps" aria-label="Toggle timestamps" theme="icon tertiary">
        <span
          class="${g.showTimestamps ? "on" : "off"}"
          @click=${() => {
      g.toggleShowTimestamps();
    }}
          >${n.clock}</span
        >
      </button>
    `;
  }
};
f = h([
  w("copilot-log-panel-actions")
], f);
const v = {
  header: "Log",
  expanded: !0,
  draggable: !0,
  panelOrder: 0,
  panel: "bottom",
  floating: !1,
  tag: "copilot-log-panel",
  actionsTag: "copilot-log-panel-actions"
}, P = {
  init(e) {
    e.addPanel(v);
  }
};
window.Vaadin.copilot.plugins.push(P);
const B = { hour: "numeric", minute: "numeric", second: "numeric", fractionalSecondDigits: 3 }, q = new Intl.DateTimeFormat(navigator.language, B);
function A(e) {
  return q.format(e);
}
export {
  f as Actions,
  r as CopilotLogPanel
};
