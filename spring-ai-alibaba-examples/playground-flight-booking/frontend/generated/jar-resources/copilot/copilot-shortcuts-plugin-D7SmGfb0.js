import { t as u, x as d, D as g, u as e } from "./copilot-C5kdwofL.js";
import { B as h } from "./base-panel-Bo-7csvK.js";
import { i as l } from "./icons-BFatMQTq.js";
const f = "copilot-shortcuts-panel{font:var(--font-xsmall);padding:var(--space-200);display:flex;flex-direction:column;gap:var(--space-50)}copilot-shortcuts-panel h3{font:var(--font-xsmall-strong);margin:0;padding:0}copilot-shortcuts-panel h3:not(:first-of-type){margin-top:var(--space-200)}copilot-shortcuts-panel ul{list-style:none;margin:0;padding:0 var(--space-50);display:flex;flex-direction:column}copilot-shortcuts-panel ul li{display:flex;align-items:center;gap:var(--space-150);padding:var(--space-75) 0}copilot-shortcuts-panel ul li:not(:last-of-type){border-bottom:1px dashed var(--border-color)}copilot-shortcuts-panel ul li svg{height:16px;width:16px}copilot-shortcuts-panel ul li .kbds{flex:1;text-align:right}copilot-shortcuts-panel kbd{display:inline-block;border-radius:var(--radius-1);border:1px solid var(--border-color);min-width:1em;min-height:1em;text-align:center;margin:0 .1em;padding:.25em;box-sizing:border-box;font-size:var(--font-size-1);font-family:var(--font-family);line-height:1}";
var m = Object.defineProperty, $ = Object.getOwnPropertyDescriptor, b = (i, a, n, s) => {
  for (var o = s > 1 ? void 0 : s ? $(a, n) : a, r = i.length - 1, p; r >= 0; r--)
    (p = i[r]) && (o = (s ? p(a, n, o) : p(o)) || o);
  return s && o && m(a, n, o), o;
};
let c = class extends h {
  render() {
    return d`<style>
        ${f}
      </style>
      <h3>Global</h3>
      <ul>
        <li>${l.vaadinLogo} Copilot ${t(e.toggleCopilot)}</li>
        <li>${l.terminal} Command window ${t(e.toggleCommandWindow)}</li>
        <li>${l.undo} Undo ${t(e.undo)}</li>
        <li>${l.redo} Redo ${t(e.redo)}</li>
      </ul>
      <h3>Selected component</h3>
      <ul>
        <li>${l.code} Go to source ${t(e.goToSource)}</li>
        <li>${l.copy} Copy ${t(e.copy)}</li>
        <li>${l.paste} Paste ${t(e.paste)}</li>
        <li>${l.duplicate} Duplicate ${t(e.duplicate)}</li>
        <li>${l.userUp} Select parent ${t(e.selectParent)}</li>
        <li>${l.userLeft} Select previous sibling ${t(e.selectPreviousSibling)}</li>
        <li>${l.userRight} Select first child / next sibling ${t(e.selectNextSibling)}</li>
        <li>${l.trash} Delete ${t(e.delete)}</li>
      </ul>`;
  }
};
c = b([
  u("copilot-shortcuts-panel")
], c);
function t(i) {
  return d`<span class="kbds">${g(i)}</span>`;
}
const v = {
  header: "Keyboard Shortcuts",
  expanded: !0,
  expandable: !1,
  panelOrder: 0,
  floating: !1,
  tag: "copilot-shortcuts-panel",
  width: 400,
  height: 475,
  floatingPosition: {
    top: 50,
    left: 50
  }
}, x = {
  init(i) {
    i.addPanel(v);
  }
};
window.Vaadin.copilot.plugins.push(x);
