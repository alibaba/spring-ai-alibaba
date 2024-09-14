import { P as h } from "./copilot-C5kdwofL.js";
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const i = (t, e, a) => (a.configurable = !0, a.enumerable = !0, Reflect.decorate && typeof e != "object" && Object.defineProperty(t, e, a), a);
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
function p(t, e) {
  return (a, r, d) => {
    const l = (o) => o.renderRoot?.querySelector(t) ?? null;
    if (e) {
      const { get: o, set: s } = typeof r == "object" ? a : d ?? (() => {
        const n = Symbol();
        return { get() {
          return this[n];
        }, set(m) {
          this[n] = m;
        } };
      })();
      return i(a, r, { get() {
        let n = o.call(this);
        return n === void 0 && (n = l(this), (n !== null || this.hasUpdated) && s.call(this, n)), n;
      } });
    }
    return i(a, r, { get() {
      return l(this);
    } });
  };
}
function b(t) {
  t.querySelectorAll(
    "vaadin-context-menu, vaadin-menu-bar, vaadin-menu-bar-submenu, vaadin-select, vaadin-combo-box, vaadin-tooltip, vaadin-dialog, vaadin-multi-select-combo-box"
  ).forEach((e) => {
    e?.$?.comboBox && (e = e.$.comboBox);
    let a = e.shadowRoot?.querySelector(
      `${e.localName}-overlay, ${e.localName}-submenu, vaadin-menu-bar-overlay`
    );
    a?.localName === "vaadin-menu-bar-submenu" && (a = a.shadowRoot.querySelector("vaadin-menu-bar-overlay")), a ? a._attachOverlay = c.bind(a) : e.$?.overlay && (e.$.overlay._attachOverlay = c.bind(e.$.overlay));
  });
}
function u() {
  return document.querySelector(`${h}main`).shadowRoot;
}
const v = () => Array.from(u().children).filter((e) => e._hasOverlayStackMixin && !e.hasAttribute("closing")).sort((e, a) => e.__zIndex - a.__zIndex || 0), y = (t) => t === v().pop();
function c() {
  const t = this;
  t._placeholder = document.createComment("vaadin-overlay-placeholder"), t.parentNode.insertBefore(t._placeholder, t), u().appendChild(t), t.hasOwnProperty("_last") || Object.defineProperty(t, "_last", {
    // Only returns odd die sides
    get() {
      return y(this);
    }
  }), t.bringToFront(), requestAnimationFrame(() => b(t));
}
export {
  p as e,
  b as m
};
