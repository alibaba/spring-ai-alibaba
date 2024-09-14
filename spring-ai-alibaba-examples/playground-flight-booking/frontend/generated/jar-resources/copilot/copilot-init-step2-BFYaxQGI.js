import { t as C, M as S, C as B, e as l, f as Ae, g as K, k as T, b as c, O as pe, A as y, x as u, l as ke, P as ge, n as ve, o as $e, p as Q, T as w, q as Ce, u as J, c as m, s as Ee, w as I, y as Re, z as oe, B as De, D as Ie, E as Le, F as _e } from "./copilot-BcASoA3D.js";
import { n as O, r as E, i as L } from "./icons-C4lgWZRy.js";
import { s as ne, d as Me, a as Se } from "./copilot-notification-jGQjkdF7.js";
import { f as Te } from "./react-utils-C_cxT7DH.js";
import { a as Oe } from "./_commonjsHelpers-Dn0DSpot.js";
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const se = (e, t, i) => (i.configurable = !0, i.enumerable = !0, Reflect.decorate && typeof t != "object" && Object.defineProperty(e, t, i), i);
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
function z(e, t) {
  return (i, o, n) => {
    const s = (r) => r.renderRoot?.querySelector(e) ?? null;
    if (t) {
      const { get: r, set: a } = typeof o == "object" ? i : n ?? (() => {
        const d = Symbol();
        return { get() {
          return this[d];
        }, set(p) {
          this[d] = p;
        } };
      })();
      return se(i, o, { get() {
        let d = r.call(this);
        return d === void 0 && (d = s(this), (d !== null || this.hasUpdated) && a.call(this, d)), d;
      } });
    }
    return se(i, o, { get() {
      return s(this);
    } });
  };
}
const ee = "@keyframes bounce{0%{transform:scale(.8)}50%{transform:scale(1.5)}to{transform:scale(1)}}@keyframes pulse{0%{box-shadow:0 0 calc(var(--pulse-size) * 2) 0 transparent}25%{box-shadow:0 0 calc(var(--pulse-size) * 2) 0 var(--pulse-first-color, var(--selection-color))}50%{box-shadow:0 0 calc(var(--pulse-size) * 2) 0 transparent}75%{box-shadow:0 0 calc(var(--pulse-size) * 2) 0 var(--pulse-second-color, var(--accent-color))}to{box-shadow:0 0 calc(var(--pulse-size) * 2) 0 transparent}}@keyframes around-we-go-again{0%{background-position:0 0,0 0,calc(var(--glow-size) * -.5) calc(var(--glow-size) * -.5),calc(100% + calc(var(--glow-size) * .5)) calc(100% + calc(var(--glow-size) * .5))}25%{background-position:0 0,0 0,calc(100% + calc(var(--glow-size) * .5)) calc(var(--glow-size) * -.5),calc(var(--glow-size) * -.5) calc(100% + calc(var(--glow-size) * .5))}50%{background-position:0 0,0 0,calc(100% + calc(var(--glow-size) * .5)) calc(100% + calc(var(--glow-size) * .5)),calc(var(--glow-size) * -.5) calc(var(--glow-size) * -.5)}75%{background-position:0 0,0 0,calc(var(--glow-size) * -.5) calc(100% + calc(var(--glow-size) * .5)),calc(100% + calc(var(--glow-size) * .5)) calc(var(--glow-size) * -.5)}to{background-position:0 0,0 0,calc(var(--glow-size) * -.5) calc(var(--glow-size) * -.5),calc(100% + calc(var(--glow-size) * .5)) calc(100% + calc(var(--glow-size) * .5))}}@keyframes swirl{0%{rotate:0deg;filter:hue-rotate(20deg)}50%{filter:hue-rotate(-30deg)}to{rotate:360deg;filter:hue-rotate(20deg)}}";
var Ne = Object.defineProperty, Be = Object.getOwnPropertyDescriptor, R = (e, t, i, o) => {
  for (var n = o > 1 ? void 0 : o ? Be(t, i) : t, s = e.length - 1, r; s >= 0; s--)
    (r = e[s]) && (n = (o ? r(t, i, n) : r(n)) || n);
  return o && n && Ne(t, i, n), n;
};
const j = "data-drag-initial-index", F = "data-drag-final-index";
let x = class extends S {
  constructor() {
    super(...arguments), this.position = "right", this.opened = !1, this.keepOpen = !1, this.resizing = !1, this.closingForcefully = !1, this.draggingSectionPanel = null, this.resizingMouseMoveListener = (e) => {
      if (!this.resizing)
        return;
      const { x: t, y: i } = e;
      e.stopPropagation(), e.preventDefault(), requestAnimationFrame(() => {
        let o;
        if (this.position === "right") {
          const n = document.body.clientWidth - t;
          this.style.setProperty("--size", `${n}px`), B.saveDrawerSize(this.position, n), o = { width: n };
        } else if (this.position === "left") {
          const n = t;
          this.style.setProperty("--size", `${n}px`), B.saveDrawerSize(this.position, n), o = { width: n };
        } else if (this.position === "bottom") {
          const n = document.body.clientHeight - i;
          this.style.setProperty("--size", `${n}px`), B.saveDrawerSize(this.position, n), o = { height: n };
        }
        l.panels.filter((n) => !n.floating && n.panel === this.position).forEach((n) => {
          l.updatePanel(n.tag, o);
        });
      });
    }, this.sectionPanelDraggingStarted = (e, t) => {
      this.draggingSectionPanel = e, this.draggingSectionPointerStartY = t.clientY, e.toggleAttribute("dragging", !0), e.style.zIndex = "1000", Array.from(this.querySelectorAll("copilot-section-panel-wrapper")).forEach((i, o) => {
        i.setAttribute(j, `${o}`);
      }), document.addEventListener("mousemove", this.sectionPanelDragging), document.addEventListener("mouseup", this.sectionPanelDraggingFinished);
    }, this.sectionPanelDragging = (e) => {
      if (!this.draggingSectionPanel)
        return;
      const { clientX: t, clientY: i } = e;
      if (!Ae(this.getBoundingClientRect(), t, i)) {
        this.cleanUpDragging();
        return;
      }
      const o = i - this.draggingSectionPointerStartY;
      this.draggingSectionPanel.style.transform = `translateY(${o}px)`, this.updateSectionPanelPositionsWhileDragging();
    }, this.sectionPanelDraggingFinished = () => {
      if (!this.draggingSectionPanel)
        return;
      const e = this.getAllPanels().filter(
        (t) => t.panelInfo?.panelOrder !== Number.parseInt(t.getAttribute(F), 10)
      ).map((t) => ({
        tag: t.panelTag,
        order: Number.parseInt(t.getAttribute(F), 10)
      }));
      this.cleanUpDragging(), l.updateOrders(e), document.removeEventListener("mouseup", this.sectionPanelDraggingFinished), document.removeEventListener("mousemove", this.sectionPanelDragging);
    }, this.updateSectionPanelPositionsWhileDragging = () => {
      const e = this.draggingSectionPanel.getBoundingClientRect().height;
      this.getAllPanels().sort((t, i) => {
        const o = t.getBoundingClientRect(), n = i.getBoundingClientRect(), s = (o.top + o.bottom) / 2, r = (n.top + n.bottom) / 2;
        return s - r;
      }).forEach((t, i) => {
        if (t.setAttribute(F, `${i}`), t.panelTag !== this.draggingSectionPanel?.panelTag) {
          const o = Number.parseInt(t.getAttribute(j), 10);
          o > i ? t.style.transform = `translateY(${-e}px)` : o < i ? t.style.transform = `translateY(${e}px)` : t.style.removeProperty("transform");
        }
      });
    };
  }
  static get styles() {
    return [
      K(ee),
      T`
        :host {
          --size: 350px;
          --min-size: 20%;
          --max-size: 80%;
          --default-content-height: 300px;
          --transition-duration: var(--duration-2);
          --opening-delay: var(--duration-2);
          --closing-delay: var(--duration-3);
          --hover-size: 18px;
          --pulse-size: var(--hover-size);
          --pulse-animation-duration: 8s;
          position: absolute;
          z-index: var(--z-index-drawer);
          transition: translate var(--transition-duration) var(--closing-delay);
        }

        :host(:is([position='left'], [position='right'])) {
          width: var(--size);
          min-width: var(--min-size);
          max-width: var(--max-size);
          top: 0;
          bottom: 0;
        }

        :host([position='left']) {
          left: 0;
          translate: calc(-100% + var(--hover-size)) 0%;
          padding-right: var(--hover-size);
        }

        :host([position='right']) {
          right: 0;
          translate: calc(100% - var(--hover-size)) 0%;
          padding-left: var(--hover-size);
        }

        :host([position='bottom']) {
          height: var(--size);
          min-height: var(--min-size);
          max-height: var(--max-size);
          bottom: 0;
          left: 0;
          right: 0;
          translate: 0% calc(100% - var(--hover-size));
          padding-top: var(--hover-size);
        }

        /* The visible container. Needed to have extra space for hover and resize handle outside it. */

        .container {
          display: flex;
          flex-direction: column;
          box-sizing: border-box;
          height: 100%;
          background: var(--surface);
          -webkit-backdrop-filter: var(--surface-backdrop-filter);
          backdrop-filter: var(--surface-backdrop-filter);
          overflow-y: auto;
          overflow-x: hidden;
          box-shadow: var(--surface-box-shadow-2);
          transition:
            opacity var(--transition-duration) var(--closing-delay),
            visibility calc(var(--transition-duration) * 2) var(--closing-delay);
          opacity: 0;
          /* For accessibility (restored when open) */
          visibility: hidden;
        }

        :host([position='left']) .container {
          border-right: 1px solid var(--surface-border-color);
        }

        :host([position='right']) .container {
          border-left: 1px solid var(--surface-border-color);
        }

        :host([position='bottom']) .container {
          border-top: 1px solid var(--surface-border-color);
        }

        /* Opened state */

        :host(:is([opened], [keepopen])) {
          translate: 0% 0%;
          transition-delay: var(--opening-delay);
        }

        :host(:is([opened], [keepopen])) .container {
          transition-delay: var(--opening-delay);
          visibility: visible;
          opacity: 1;
        }

        .resize {
          position: absolute;
          z-index: 10;
          inset: 0;
        }

        :host(:is([position='left'], [position='right'])) .resize {
          width: var(--hover-size);
          cursor: col-resize;
        }

        :host([position='left']) .resize {
          left: auto;
          right: calc(var(--hover-size) * 0.5);
        }

        :host([position='right']) .resize {
          right: auto;
          left: calc(var(--hover-size) * 0.5);
        }

        :host([position='bottom']) .resize {
          height: var(--hover-size);
          bottom: auto;
          top: calc(var(--hover-size) * 0.5);
          cursor: row-resize;
        }

        :host([resizing]) .container {
          /* vaadin-grid (used in the outline) blocks the mouse events */
          pointer-events: none;
        }

        /* Visual indication of the drawer */

        :host::before {
          content: '';
          position: absolute;
          pointer-events: none;
          z-index: -1;
          inset: var(--hover-size);
          transition: opacity var(--transition-duration) var(--closing-delay);
          animation: pulse var(--pulse-animation-duration) infinite;
        }

        :host([attention-required]) {
          --pulse-animation-duration: 2s;
          --pulse-first-color: var(--red-500);
          --pulse-second-color: var(--red-800);
        }

        :host(:is([opened], [keepopen]))::before {
          transition-delay: var(--opening-delay);
          opacity: 0;
        }
      `
    ];
  }
  connectedCallback() {
    super.connectedCallback(), this.reaction(
      () => l.panels,
      () => this.requestUpdate()
    ), this.reaction(
      () => c.operationInProgress,
      (t) => {
        t === pe.DragAndDrop && !this.opened && !this.keepOpen ? this.style.setProperty("pointer-events", "none") : this.style.setProperty("pointer-events", "auto");
      }
    ), this.reaction(
      () => l.getAttentionRequiredPanelConfiguration(),
      () => {
        const t = l.getAttentionRequiredPanelConfiguration();
        t && !t.floating && this.toggleAttribute(y, t.panel === this.position);
      }
    ), document.addEventListener("mouseup", () => {
      this.resizing = !1, this.removeAttribute("resizing");
    });
    const e = B.getDrawerSize(this.position);
    e && this.style.setProperty("--size", `${e}px`), document.addEventListener("mousemove", this.resizingMouseMoveListener), this.addEventListener("mouseenter", this.mouseEnterListener);
  }
  firstUpdated(e) {
    super.firstUpdated(e), this.resizeElement.addEventListener("mousedown", (t) => {
      t.button === 0 && (this.resizing = !0, this.setAttribute("resizing", ""));
    });
  }
  updated(e) {
    super.updated(e), e.has("opened") && this.opened && this.hasAttribute(y) && (this.removeAttribute(y), l.clearAttention());
  }
  disconnectedCallback() {
    super.disconnectedCallback(), document.removeEventListener("mousemove", this.resizingMouseMoveListener), this.removeEventListener("mouseenter", this.mouseEnterListener);
  }
  /**
   * Cleans up attributes/styles etc... for dragging operations
   * @private
   */
  cleanUpDragging() {
    this.draggingSectionPanel && (c.setSectionPanelDragging(!1), this.draggingSectionPanel.style.zIndex = "", Array.from(this.querySelectorAll("copilot-section-panel-wrapper")).forEach((e) => {
      e.style.removeProperty("transform"), e.removeAttribute(F), e.removeAttribute(j);
    }), this.draggingSectionPanel.removeAttribute("dragging"), this.draggingSectionPanel = null);
  }
  getAllPanels() {
    return Array.from(this.querySelectorAll("copilot-section-panel-wrapper"));
  }
  /**
   * Closes the drawer and disables mouse enter event for a while.
   */
  forceClose() {
    this.closingForcefully = !0, this.opened = !1, setTimeout(() => {
      this.closingForcefully = !1;
    }, 0.5);
  }
  mouseEnterListener(e) {
    this.closingForcefully || c.sectionPanelResizing || (this.opened = !0);
  }
  render() {
    return u`
      <div class="container">
        <slot></slot>
      </div>
      <div class="resize"></div>
    `;
  }
};
R([
  O({ reflect: !0, attribute: !0 })
], x.prototype, "position", 2);
R([
  O({ reflect: !0, type: Boolean })
], x.prototype, "opened", 2);
R([
  O({ reflect: !0, type: Boolean })
], x.prototype, "keepOpen", 2);
R([
  z(".container")
], x.prototype, "container", 2);
R([
  z(".resize")
], x.prototype, "resizeElement", 2);
x = R([
  C("copilot-drawer-panel")
], x);
var Fe = Object.defineProperty, Ue = Object.getOwnPropertyDescriptor, fe = (e, t, i, o) => {
  for (var n = o > 1 ? void 0 : o ? Ue(t, i) : t, s = e.length - 1, r; s >= 0; s--)
    (r = e[s]) && (n = (o ? r(t, i, n) : r(n)) || n);
  return o && n && Fe(t, i, n), n;
};
let X = class extends ke {
  constructor() {
    super(...arguments), this.checked = !1;
  }
  static get styles() {
    return T`
      .switch {
        display: inline-flex;
        align-items: center;
        gap: var(--space-100);
      }

      .switch input {
        display: none;
      }

      .slider {
        background-color: var(--gray-300);
        border-radius: 9999px;
        cursor: pointer;
        inset: 0;
        position: absolute;
        transition: 0.4s;
        height: 0.75rem;
        position: relative;
        width: 1.5rem;
        min-width: 1.5rem;
      }

      .slider:before {
        background-color: white;
        border-radius: 50%;
        bottom: 1px;
        content: '';
        height: 0.625rem;
        left: 1px;
        position: absolute;
        transition: 0.4s;
        width: 0.625rem;
      }

      input:checked + .slider {
        background-color: var(--selection-color);
      }

      input:checked + .slider:before {
        transform: translateX(0.75rem);
      }

      label:has(input:focus) {
        outline: 2px solid var(--selection-color);
        outline-offset: 2px;
      }
    `;
  }
  render() {
    return u`
      <label class="switch">
        <input
          class="feature-toggle"
          id="feature-toggle-${this.id}"
          type="checkbox"
          ?checked="${this.checked}"
          @change=${(e) => {
      e.preventDefault(), this.checked = e.target.checked, this.dispatchEvent(new CustomEvent("on-change"));
    }} />
        <span class="slider"></span>
        ${this.title}
      </label>
    `;
  }
  //  @change=${(e: InputEvent) => this.toggleFeatureFlag(e, feature)}
};
fe([
  O({ reflect: !0 })
], X.prototype, "checked", 2);
X = fe([
  C("copilot-toggle-button")
], X);
function te(e) {
  e.querySelectorAll(
    "vaadin-context-menu, vaadin-menu-bar, vaadin-menu-bar-submenu, vaadin-select, vaadin-combo-box, vaadin-tooltip, vaadin-dialog"
  ).forEach((t) => {
    let i = t.shadowRoot?.querySelector(
      `${t.localName}-overlay, ${t.localName}-submenu, vaadin-menu-bar-overlay`
    );
    i?.localName === "vaadin-menu-bar-submenu" && (i = i.shadowRoot.querySelector("vaadin-menu-bar-overlay")), i ? i._attachOverlay = re.bind(i) : t.$?.overlay && (t.$.overlay._attachOverlay = re.bind(t.$.overlay));
  });
}
function me() {
  return document.querySelector(`${ge}main`).shadowRoot;
}
const qe = () => Array.from(me().children).filter((t) => t._hasOverlayStackMixin && !t.hasAttribute("closing")).sort((t, i) => t.__zIndex - i.__zIndex || 0), Ye = (e) => e === qe().pop();
function re() {
  const e = this;
  e._placeholder = document.createComment("vaadin-overlay-placeholder"), e.parentNode.insertBefore(e._placeholder, e), me().appendChild(e), e.hasOwnProperty("_last") || Object.defineProperty(e, "_last", {
    // Only returns odd die sides
    get() {
      return Ye(this);
    }
  }), e.bringToFront(), requestAnimationFrame(() => te(e));
}
function A(e, t) {
  const i = document.createElement(e);
  if (t.style && (i.className = t.style), t.icon) {
    const o = document.createElement("vaadin-icon");
    o.setAttribute("icon", t.icon), i.append(o);
  }
  if (t.label) {
    const o = document.createElement("span");
    o.className = "label", o.innerHTML = t.label, i.append(o);
  }
  if (t.hint) {
    const o = document.createElement("span");
    o.className = "hint", o.innerHTML = t.hint, i.append(o);
  }
  return i;
}
function Ve() {
  const e = window.navigator.userAgent;
  return e.indexOf("Windows") !== -1 ? "Windows" : e.indexOf("Mac") !== -1 ? "Mac" : e.indexOf("Linux") !== -1 ? "Linux" : null;
}
function je() {
  return Ve() === "Mac";
}
function He() {
  return je() ? "⌘" : "Ctrl";
}
class We {
  constructor() {
    this.offsetX = 0, this.offsetY = 0;
  }
  draggingStarts(t, i) {
    this.offsetX = i.clientX - t.getBoundingClientRect().left, this.offsetY = i.clientY - t.getBoundingClientRect().top;
  }
  dragging(t, i) {
    const o = i.clientX, n = i.clientY, s = o - this.offsetX, r = o - this.offsetX + t.getBoundingClientRect().width, a = n - this.offsetY, d = n - this.offsetY + t.getBoundingClientRect().height;
    return this.adjust(t, s, a, r, d);
  }
  adjust(t, i, o, n, s) {
    let r, a, d, p;
    const v = document.documentElement.getBoundingClientRect().width, h = document.documentElement.getBoundingClientRect().height;
    return (n + i) / 2 < v / 2 ? (t.style.setProperty("--left", `${i}px`), t.style.setProperty("--right", ""), p = void 0, r = Math.max(0, i)) : (t.style.removeProperty("--left"), t.style.setProperty("--right", `${v - n}px`), r = void 0, p = Math.max(0, v - n)), (o + s) / 2 < h / 2 ? (t.style.setProperty("--top", `${o}px`), t.style.setProperty("--bottom", ""), d = void 0, a = Math.max(0, o)) : (t.style.setProperty("--top", ""), t.style.setProperty("--bottom", `${h - s}px`), a = void 0, d = Math.max(0, h - s)), {
      left: r,
      right: p,
      top: a,
      bottom: d
    };
  }
  anchor(t) {
    const { left: i, top: o, bottom: n, right: s } = t.getBoundingClientRect();
    return this.adjust(t, i, o, s, n);
  }
  anchorLeftTop(t) {
    const { left: i, top: o } = t.getBoundingClientRect();
    return t.style.setProperty("--left", `${i}px`), t.style.setProperty("--right", ""), t.style.setProperty("--top", `${o}px`), t.style.setProperty("--bottom", ""), {
      left: i,
      top: o
    };
  }
}
const f = new We();
/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
let ae = 0, be = 0;
const k = [];
let G = !1;
function Xe() {
  G = !1;
  const e = k.length;
  for (let t = 0; t < e; t++) {
    const i = k[t];
    if (i)
      try {
        i();
      } catch (o) {
        setTimeout(() => {
          throw o;
        });
      }
  }
  k.splice(0, e), be += e;
}
const Ge = {
  /**
   * Enqueues a function called at microtask timing.
   *
   * @memberof microTask
   * @param {!Function=} callback Callback to run
   * @return {number} Handle used for canceling task
   */
  run(e) {
    G || (G = !0, queueMicrotask(() => Xe())), k.push(e);
    const t = ae;
    return ae += 1, t;
  },
  /**
   * Cancels a previously enqueued `microTask` callback.
   *
   * @memberof microTask
   * @param {number} handle Handle returned from `run` of callback to cancel
   * @return {void}
   */
  cancel(e) {
    const t = e - be;
    if (t >= 0) {
      if (!k[t])
        throw new Error(`invalid async handle: ${e}`);
      k[t] = null;
    }
  }
};
/**
@license
Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
Code distributed by Google as part of the polymer project is also
subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
*/
const le = /* @__PURE__ */ new Set();
class U {
  /**
   * Creates a debouncer if no debouncer is passed as a parameter
   * or it cancels an active debouncer otherwise. The following
   * example shows how a debouncer can be called multiple times within a
   * microtask and "debounced" such that the provided callback function is
   * called once. Add this method to a custom element:
   *
   * ```js
   * import {microTask} from '@vaadin/component-base/src/async.js';
   * import {Debouncer} from '@vaadin/component-base/src/debounce.js';
   * // ...
   *
   * _debounceWork() {
   *   this._debounceJob = Debouncer.debounce(this._debounceJob,
   *       microTask, () => this._doWork());
   * }
   * ```
   *
   * If the `_debounceWork` method is called multiple times within the same
   * microtask, the `_doWork` function will be called only once at the next
   * microtask checkpoint.
   *
   * Note: In testing it is often convenient to avoid asynchrony. To accomplish
   * this with a debouncer, you can use `enqueueDebouncer` and
   * `flush`. For example, extend the above example by adding
   * `enqueueDebouncer(this._debounceJob)` at the end of the
   * `_debounceWork` method. Then in a test, call `flush` to ensure
   * the debouncer has completed.
   *
   * @param {Debouncer?} debouncer Debouncer object.
   * @param {!AsyncInterface} asyncModule Object with Async interface
   * @param {function()} callback Callback to run.
   * @return {!Debouncer} Returns a debouncer object.
   */
  static debounce(t, i, o) {
    return t instanceof U ? t._cancelAsync() : t = new U(), t.setConfig(i, o), t;
  }
  constructor() {
    this._asyncModule = null, this._callback = null, this._timer = null;
  }
  /**
   * Sets the scheduler; that is, a module with the Async interface,
   * a callback and optional arguments to be passed to the run function
   * from the async module.
   *
   * @param {!AsyncInterface} asyncModule Object with Async interface.
   * @param {function()} callback Callback to run.
   * @return {void}
   */
  setConfig(t, i) {
    this._asyncModule = t, this._callback = i, this._timer = this._asyncModule.run(() => {
      this._timer = null, le.delete(this), this._callback();
    });
  }
  /**
   * Cancels an active debouncer and returns a reference to itself.
   *
   * @return {void}
   */
  cancel() {
    this.isActive() && (this._cancelAsync(), le.delete(this));
  }
  /**
   * Cancels a debouncer's async callback.
   *
   * @return {void}
   */
  _cancelAsync() {
    this.isActive() && (this._asyncModule.cancel(
      /** @type {number} */
      this._timer
    ), this._timer = null);
  }
  /**
   * Flushes an active debouncer and returns a reference to itself.
   *
   * @return {void}
   */
  flush() {
    this.isActive() && (this.cancel(), this._callback());
  }
  /**
   * Returns true if the debouncer is active.
   *
   * @return {boolean} True if active.
   */
  isActive() {
    return this._timer != null;
  }
}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const _ = (e, t) => {
  const i = e._$AN;
  if (i === void 0)
    return !1;
  for (const o of i)
    o._$AO?.(t, !1), _(o, t);
  return !0;
}, q = (e) => {
  let t, i;
  do {
    if ((t = e._$AM) === void 0)
      break;
    i = t._$AN, i.delete(e), e = t;
  } while (i?.size === 0);
}, ye = (e) => {
  for (let t; t = e._$AM; e = t) {
    let i = t._$AN;
    if (i === void 0)
      t._$AN = i = /* @__PURE__ */ new Set();
    else if (i.has(e))
      break;
    i.add(e), Qe(t);
  }
};
function Ze(e) {
  this._$AN !== void 0 ? (q(this), this._$AM = e, ye(this)) : this._$AM = e;
}
function Ke(e, t = !1, i = 0) {
  const o = this._$AH, n = this._$AN;
  if (n !== void 0 && n.size !== 0)
    if (t)
      if (Array.isArray(o))
        for (let s = i; s < o.length; s++)
          _(o[s], !1), q(o[s]);
      else
        o != null && (_(o, !1), q(o));
    else
      _(this, e);
}
const Qe = (e) => {
  e.type == Q.CHILD && (e._$AP ??= Ke, e._$AQ ??= Ze);
};
class Je extends ve {
  constructor() {
    super(...arguments), this._$AN = void 0;
  }
  _$AT(t, i, o) {
    super._$AT(t, i, o), ye(this), this.isConnected = t._$AU;
  }
  _$AO(t, i = !0) {
    t !== this.isConnected && (this.isConnected = t, t ? this.reconnected?.() : this.disconnected?.()), i && (_(this, t), q(this));
  }
  setValue(t) {
    if ($e(this._$Ct))
      this._$Ct._$AI(t, this);
    else {
      const i = [...this._$Ct._$AH];
      i[this._$Ci] = t, this._$Ct._$AI(i, this, 0);
    }
  }
  disconnected() {
  }
  reconnected() {
  }
}
/**
 * @license
 * Copyright (c) 2016 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
const de = Symbol("valueNotInitialized");
class et extends Je {
  constructor(t) {
    if (super(t), t.type !== Q.ELEMENT)
      throw new Error(`\`${this.constructor.name}\` must be bound to an element.`);
    this.previousValue = de;
  }
  /** @override */
  render(t, i) {
    return w;
  }
  /** @override */
  update(t, [i, o]) {
    return this.hasChanged(o) ? (this.host = t.options && t.options.host, this.element = t.element, this.renderer = i, this.previousValue === de ? this.addRenderer() : this.runRenderer(), this.previousValue = Array.isArray(o) ? [...o] : o, w) : w;
  }
  /** @override */
  reconnected() {
    this.addRenderer();
  }
  /** @override */
  disconnected() {
    this.removeRenderer();
  }
  /** @abstract */
  addRenderer() {
    throw new Error("The `addRenderer` method must be implemented.");
  }
  /** @abstract */
  runRenderer() {
    throw new Error("The `runRenderer` method must be implemented.");
  }
  /** @abstract */
  removeRenderer() {
    throw new Error("The `removeRenderer` method must be implemented.");
  }
  /** @protected */
  renderRenderer(t, ...i) {
    const o = this.renderer.call(this.host, ...i);
    Ce(o, t, { host: this.host });
  }
  /** @protected */
  hasChanged(t) {
    return Array.isArray(t) ? !Array.isArray(this.previousValue) || this.previousValue.length !== t.length ? !0 : t.some((i, o) => i !== this.previousValue[o]) : this.previousValue !== t;
  }
}
/**
 * @license
 * Copyright (c) 2017 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
const H = Symbol("contentUpdateDebouncer");
class we extends et {
  /**
   * A property to that the renderer callback will be assigned.
   *
   * @abstract
   */
  get rendererProperty() {
    throw new Error("The `rendererProperty` getter must be implemented.");
  }
  /**
   * Adds the renderer callback to the dialog.
   */
  addRenderer() {
    this.element[this.rendererProperty] = (t, i) => {
      this.renderRenderer(t, i);
    };
  }
  /**
   * Runs the renderer callback on the dialog.
   */
  runRenderer() {
    this.element[H] = U.debounce(
      this.element[H],
      Ge,
      () => {
        this.element.requestContentUpdate();
      }
    );
  }
  /**
   * Removes the renderer callback from the dialog.
   */
  removeRenderer() {
    this.element[this.rendererProperty] = null, delete this.element[H];
  }
}
class tt extends we {
  get rendererProperty() {
    return "renderer";
  }
}
class it extends we {
  get rendererProperty() {
    return "footerRenderer";
  }
}
const ot = J(tt), nt = J(it);
var st = Object.defineProperty, rt = Object.getOwnPropertyDescriptor, D = (e, t, i, o) => {
  for (var n = o > 1 ? void 0 : o ? rt(t, i) : t, s = e.length - 1, r; s >= 0; s--)
    (r = e[s]) && (n = (o ? r(t, i, n) : r(n)) || n);
  return o && n && st(t, i, n), n;
};
const ce = "https://github.com/vaadin/copilot/issues/new", at = "?template=feature_request.md&title=%5BFEATURE%5D", lt = "A short, concise description of the bug and why you consider it a bug. Any details like exceptions and logs can be helpful as well.", dt = "Please provide as many details as possible, this will help us deliver a fix as soon as possible.%0AThank you!%0A%0A%23%23%23 Description of the Bug%0A%0A{description}%0A%0A%23%23%23 Expected Behavior%0A%0AA description of what you would expect to happen. (Sometimes it is clear what the expected outcome is if something does not work, other times, it is not super clear.)%0A%0A%23%23%23 Minimal Reproducible Example%0A%0AWe would appreciate the minimum code with which we can reproduce the issue.%0A%0A%23%23%23 Versions%0A{versionsInfo}";
let P = class extends S {
  constructor() {
    super(), this.#e = [
      {
        label: "🐞 Report a Bug",
        value: "bug",
        ghTitle: "[BUG]"
      },
      {
        label: "❓ Ask a Question",
        value: "question",
        ghTitle: "[QUESTION]"
      },
      {
        label: "💡 Share an Idea",
        value: "idea",
        ghTitle: "[FEATURE]"
      }
    ], this.renderDialog = () => this.message === void 0 ? u`
          <vaadin-vertical-layout style="width: 40em; height: 30em; align-items: stretch;">
            <p>
              Your insights are incredibly valuable to us. Whether you’ve encountered a hiccup, have questions, or ideas
              to make our platform better, we're all ears! If you wish, leave your email and we’ll get back to you. You
              can even share your code snippet with us for a clearer picture.
            </p>
            <vaadin-select
              label="What's on your mind?"
              .items="${this.items}"
              .value="${this.items[0].value}"
              @value-changed=${(e) => {
      this.type = e.detail.value;
    }}>
            </vaadin-select>
            <vaadin-text-area
              .value="${this.description}"
              @keydown=${this.keyDown}
              @value-changed=${(e) => {
      this.description = e.detail.value;
    }}
              style="flex: 1; max-height: 100%; overflow-y: auto;"
              label="Tell Us More"
              helper-text="Describe what you're experiencing, wondering about, or envisioning. The more you share, the better we can understand and act on your feedback"></vaadin-text-area>
            <vaadin-text-field
              @keydown=${this.keyDown}
              @value-changed=${(e) => {
      this.email = e.detail.value;
    }}
              id="email"
              label="Your Email (Optional)"
              helper-text="Leave your email if you’d like us to follow up. Totally optional, but we’d love to keep the conversation going."></vaadin-text-field>
          </vaadin-vertical-layout>
        ` : u`<p>${this.message}</p>`, this.renderFooter = () => this.message === void 0 ? u`
          <vaadin-button
            @click="${() => m.emit("system-info-with-callback", {
      callback: this.openGithub,
      notify: !1
    })}">
            <span style="display: flex" slot="prefix">${L.github}</span>
            Create GitHub issue
          </vaadin-button>
          <vaadin-button @click="${this.close}">Cancel</vaadin-button>
          <vaadin-button theme="primary" @click="${this.submit}">Submit</vaadin-button>
        ` : u` <vaadin-button @click="${this.close}">Close</vaadin-button>`, this.description = "";
  }
  #e;
  get items() {
    return this.#e;
  }
  set items(e) {
    this.#e = e;
  }
  firstUpdated() {
    te(this.shadowRoot);
  }
  render() {
    return u` <vaadin-dialog
      header-title="Help Us improve!"
      draggable
      .opened="${c.feedbackOpened}"
      .noCloseOnOutsideClick="${!0}"
      @opened-changed="${(e) => {
      c.feedbackOpened && (this.message = void 0), c.setFeedbackOpened(e.detail.value);
    }}"
      ${ot(this.renderDialog, [this.message, this.description])}
      ${nt(this.renderFooter, [this.message])}></vaadin-dialog>`;
  }
  close() {
    c.setFeedbackOpened(!1);
  }
  submit() {
    const e = {
      description: this.description,
      email: this.email,
      type: this.type
    };
    Ee(`${ge}feedback`, e), this.message = "Thank you for sharing feedback.";
  }
  keyDown(e) {
    (e.key === "Backspace" || e.key === "Delete") && e.stopPropagation();
  }
  openGithub(e) {
    if (this.type === "idea") {
      window.open(`${ce}${at}`);
      return;
    }
    const t = e.replace(/\n/g, "%0A"), i = `${this.items.find((s) => s.value === this.type)?.ghTitle}`, o = this.description !== "" ? this.description : lt, n = dt.replace("{description}", o).replace("{versionsInfo}", t);
    window.open(`${ce}?title=${i}&body=${n}`, "_blank")?.focus();
  }
};
D([
  E()
], P.prototype, "description", 2);
D([
  E()
], P.prototype, "type", 2);
D([
  E()
], P.prototype, "email", 2);
D([
  E()
], P.prototype, "message", 2);
D([
  E()
], P.prototype, "items", 1);
P = D([
  C("copilot-feedback")
], P);
var ct = Object.defineProperty, ht = Object.getOwnPropertyDescriptor, Y = (e, t, i, o) => {
  for (var n = o > 1 ? void 0 : o ? ht(t, i) : t, s = e.length - 1, r; s >= 0; s--)
    (r = e[s]) && (n = (o ? r(t, i, n) : r(n)) || n);
  return o && n && ct(t, i, n), n;
};
const ut = 8;
let M = class extends S {
  constructor() {
    super(...arguments), this.initialMouseDownPosition = null, this.dragging = !1, this.mouseDownListener = (e) => {
      this.initialMouseDownPosition = { x: e.clientX, y: e.clientY }, f.draggingStarts(this, e), document.addEventListener("mousemove", this.documentDraggingMouseMoveEventListener);
    }, this.documentDraggingMouseMoveEventListener = (e) => {
      if (this.initialMouseDownPosition && !this.dragging) {
        const { clientX: t, clientY: i } = e;
        this.dragging = Math.abs(t - this.initialMouseDownPosition.x) + Math.abs(i - this.initialMouseDownPosition.y) > ut;
      }
      this.dragging && (this.setOverlayVisibility(!1), f.dragging(this, e));
    }, this.documentMouseUpListener = (e) => {
      if (this.dragging) {
        const t = f.dragging(this, e);
        ne.setActivationButtonPosition(t), this.setOverlayVisibility(!0);
      }
      this.dragging = !1, this.initialMouseDownPosition = null, document.removeEventListener("mousemove", this.documentDraggingMouseMoveEventListener), this.setMenuBarOnClick();
    }, this.dispatchSpotlightActivationEvent = (e) => {
      this.dispatchEvent(
        new CustomEvent("spotlight-activation-changed", {
          detail: e
        })
      );
    }, this.activationBtnClicked = (e) => {
      if (this.dragging) {
        e?.stopPropagation(), this.dragging = !1;
        return;
      }
      if (c.active && this.handleAttentionRequiredOnClick()) {
        e?.stopPropagation(), e?.preventDefault();
        return;
      }
      e?.stopPropagation(), this.dispatchEvent(new CustomEvent("activation-btn-clicked"));
    }, this.handleAttentionRequiredOnClick = () => {
      const e = l.getAttentionRequiredPanelConfiguration();
      return e ? e.panel && !e.floating ? (m.emit("open-attention-required-drawer", null), !0) : (l.clearAttention(), !0) : !1;
    }, this.setMenuBarOnClick = () => {
      const e = this.shadowRoot.querySelector("vaadin-menu-bar-button");
      e && (e.onclick = this.activationBtnClicked);
    };
  }
  static get styles() {
    return [
      K(ee),
      T`
        :host {
          --space: 8px;
          --height: 28px;
          --width: 28px;
          position: absolute;
          top: clamp(var(--space), var(--top), calc(100vh - var(--height) - var(--space)));
          left: clamp(var(--space), var(--left), calc(100vw - var(--width) - var(--space)));
          bottom: clamp(var(--space), var(--bottom), calc(100vh - var(--height) - var(--space)));
          right: clamp(var(--space), var(--right), calc(100vw - var(--width) - var(--space)));
          user-select: none;
          -ms-user-select: none;
          -moz-user-select: none;
          -webkit-user-select: none;
          /* Don't add a z-index or anything else that creates a stacking context */
        }

        .menu-button::part(container) {
          overflow: visible;
        }

        .menu-button vaadin-menu-bar-button {
          all: initial;
          display: block;
          position: relative;
          z-index: var(--z-index-activation-button);
          width: var(--width);
          height: var(--height);
          overflow: hidden;
          color: transparent;
          background: hsl(0 0% 0% / 0.25);
          border-radius: 8px;
          box-shadow: 0 0 0 1px hsl(0 0% 100% / 0.1);
          cursor: default;
          -webkit-backdrop-filter: blur(8px);
          backdrop-filter: blur(8px);
          transition:
            box-shadow 0.2s,
            background-color 0.2s;
        }

        /* pointer-events property is set when the menu is open */

        .menu-button[style*='pointer-events'] + .monkey-patch-close-on-hover {
          position: fixed; /* escapes the host positioning context */
          inset: 0;
          bottom: 40px;
          z-index: calc(var(--z-index-popover) - 1);
          pointer-events: auto;
        }

        /* visual effect when active */

        .menu-button vaadin-menu-bar-button::before {
          all: initial;
          content: '';
          position: absolute;
          inset: -6px;
          background-image: radial-gradient(circle at 50% -10%, hsl(221 100% 55% / 0.6) 0%, transparent 60%),
            radial-gradient(circle at 25% 40%, hsl(303 71% 64%) 0%, transparent 70%),
            radial-gradient(circle at 80% 10%, hsla(262, 38%, 9%, 0.5) 0%, transparent 80%),
            radial-gradient(circle at 110% 50%, hsla(147, 100%, 77%, 1) 20%, transparent 100%);
          animation: 5s swirl linear infinite;
          animation-play-state: paused;
          opacity: 0;
          transition: opacity 0.5s;
        }

        /* vaadin symbol */

        .menu-button vaadin-menu-bar-button::after {
          all: initial;
          content: '';
          position: absolute;
          inset: 1px;
          background: url('data:image/svg+xml;utf8,<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12.7407 9.70401C12.7407 9.74417 12.7378 9.77811 12.7335 9.81479C12.7111 10.207 12.3897 10.5195 11.9955 10.5195C11.6014 10.5195 11.2801 10.209 11.2577 9.8169C11.2534 9.7801 11.2504 9.74417 11.2504 9.70401C11.2504 9.31225 11.1572 8.90867 10.2102 8.90867H7.04307C5.61481 8.90867 5 8.22698 5 6.86345V5.70358C5 5.31505 5.29521 5 5.68008 5C6.06495 5 6.35683 5.31505 6.35683 5.70358V6.09547C6.35683 6.53423 6.655 6.85413 7.307 6.85413H10.4119C11.8248 6.85413 11.9334 7.91255 11.98 8.4729H12.0111C12.0577 7.91255 12.1663 6.85413 13.5791 6.85413H16.6841C17.3361 6.85413 17.614 6.54529 17.614 6.10641L17.6158 5.70358C17.6158 5.31505 17.9246 5 18.3095 5C18.6943 5 19 5.31505 19 5.70358V6.86345C19 8.22698 18.3763 8.90867 16.9481 8.90867H13.7809C12.8338 8.90867 12.7407 9.31225 12.7407 9.70401Z" fill="white"/><path d="M12.7536 17.7785C12.6267 18.0629 12.3469 18.2608 12.0211 18.2608C11.6907 18.2608 11.4072 18.0575 11.2831 17.7668C11.2817 17.7643 11.2803 17.7619 11.279 17.7595C11.2761 17.7544 11.2732 17.7495 11.2704 17.744L8.45986 12.4362C8.3821 12.2973 8.34106 12.1399 8.34106 11.9807C8.34106 11.4732 8.74546 11.0603 9.24238 11.0603C9.64162 11.0603 9.91294 11.2597 10.0985 11.6922L12.0216 15.3527L13.9468 11.6878C14.1301 11.2597 14.4014 11.0603 14.8008 11.0603C15.2978 11.0603 15.7021 11.4732 15.7021 11.9807C15.7021 12.1399 15.6611 12.2973 15.5826 12.4374L12.7724 17.7446C12.7683 17.7524 12.7642 17.7597 12.7601 17.767C12.7579 17.7708 12.7557 17.7746 12.7536 17.7785Z" fill="white"/></svg>');
          background-size: 100%;
        }

        .menu-button vaadin-menu-bar-button[focus-ring] {
          outline: 2px solid var(--selection-color);
          outline-offset: 2px;
        }

        .menu-button vaadin-menu-bar-button:hover {
          background: hsl(0 0% 0% / 0.8);
          box-shadow:
            0 0 0 1px hsl(0 0% 100% / 0.1),
            0 2px 8px -1px hsl(0 0% 0% / 0.3);
        }

        :host([active]) .menu-button vaadin-menu-bar-button {
          background-color: transparent;
          box-shadow:
            inset 0 0 0 1px hsl(0 0% 0% / 0.2),
            0 2px 8px -1px hsl(0 0% 0% / 0.3);
        }

        :host([active]) .menu-button vaadin-menu-bar-button::before {
          opacity: 1;
          animation-play-state: running;
        }

        :host([attention-required]) {
          animation: bounce 0.5s;
          animation-iteration-count: 2;
        }

        :host([attention-required]) [part='attention-required-indicator'] {
          top: -1px;
          right: -1px;
          width: 6px;
          height: 6px;
          box-sizing: border-box;
          border-radius: 100%;
          position: absolute;
          background: var(--red-500);
          z-index: calc(var(--z-index-activation-button) + 1);
        }
      `
    ];
  }
  connectedCallback() {
    super.connectedCallback(), this.reaction(
      () => l.attentionRequiredPanelTag,
      () => {
        this.toggleAttribute(y, l.attentionRequiredPanelTag !== null);
      }
    ), this.reaction(
      () => c.active,
      () => {
        this.toggleAttribute("active", c.active);
      },
      { fireImmediately: !0 }
    ), this.addEventListener("mousedown", this.mouseDownListener), document.addEventListener("mouseup", this.documentMouseUpListener);
    const e = ne.getActivationButtonPosition();
    e ? (this.style.setProperty("--left", `${e.left}px`), this.style.setProperty("--bottom", `${e.bottom}px`), this.style.setProperty("--right", `${e.right}px`), this.style.setProperty("--top", `${e.top}px`)) : (this.style.setProperty("--bottom", "var(--space)"), this.style.setProperty("--right", "var(--space)"));
  }
  disconnectedCallback() {
    super.disconnectedCallback(), this.removeEventListener("mousedown", this.mouseDownListener), document.removeEventListener("mouseup", this.documentMouseUpListener);
  }
  /**
   * To hide overlay while dragging
   * @param visible
   */
  setOverlayVisibility(e) {
    const t = this.shadowRoot.querySelector("vaadin-menu-bar-button").__overlay;
    e ? (t?.style.setProperty("display", "flex"), t?.style.setProperty("visibility", "visible")) : (t?.style.setProperty("display", "none"), t?.style.setProperty("visibility", "invisible"));
  }
  render() {
    const e = He(), t = [
      {
        text: "Vaadin Copilot",
        children: [
          {
            component: A("vaadin-menu-bar-item", {
              label: I.activationShortcutEnabled ? "Shortcut enabled" : "Shortcut disabled",
              hint: I.activationShortcutEnabled ? "✓" : void 0
            }),
            action: "shortcut"
          },
          {
            component: A("vaadin-menu-bar-item", {
              label: '<span class="deactivate">Deactivate</span><span class="activate">Activate</span> Copilot',
              hint: I.activationShortcutEnabled ? `<kbd>⇧</kbd> + <kbd>${e}</kbd> <kbd>${e}</kbd>` : void 0
            }),
            action: "copilot"
          },
          {
            component: A("vaadin-menu-bar-item", {
              label: "Toggle Spotlight",
              hint: "<kbd>⇧</kbd> + <kbd>Space</kbd>",
              style: "toggle-spotlight"
            }),
            action: "spotlight"
          }
        ]
      }
    ];
    return c.active && (c.idePluginState?.supportedActions?.find((i) => i === "undo") && (t[0].children = [
      {
        component: A("vaadin-menu-bar-item", {
          label: "Undo",
          hint: `<kbd>${e}</kbd> + <kbd>Z</kbd>`
        }),
        action: "undo"
      },
      {
        component: A("vaadin-menu-bar-item", {
          label: "Redo",
          hint: `<kbd>${e}</kbd> + <kbd>⇧</kbd> + <kbd>Z</kbd>`
        }),
        action: "redo"
      },
      ...t[0].children
    ]), t[0].children.push(
      {
        component: "hr"
      },
      {
        component: A("vaadin-menu-bar-item", {
          label: "Tell us what you think"
        }),
        action: "feedback"
      }
    )), u`
      <vaadin-menu-bar
        class="menu-button"
        .items="${t}"
        @item-selected="${(i) => {
      this.handleMenuItemClick(i.detail.value);
    }}"
        ?open-on-hover=${!this.dragging}
        overlay-class="activation-button-menu">
      </vaadin-menu-bar>
      <div class="monkey-patch-close-on-hover" @mouseenter="${this.closeMenu}"></div>
      <div part="attention-required-indicator"></div>
      <copilot-feedback></copilot-feedback>
    `;
  }
  closeMenu() {
    this.menubar._close();
  }
  handleMenuItemClick(e) {
    switch (e.action) {
      case "copilot":
        this.activationBtnClicked();
        break;
      case "spotlight":
        c.setSpotlightActive(!c.spotlightActive);
        break;
      case "shortcut":
        I.setActivationShortcutEnabled(!I.activationShortcutEnabled);
        break;
      case "undo":
      case "redo":
        m.emit("undoRedo", { undo: e.action === "undo" });
        break;
      case "feedback":
        c.setFeedbackOpened(!0);
        break;
    }
  }
  firstUpdated() {
    this.setMenuBarOnClick(), te(this.shadowRoot);
  }
};
Y([
  z("vaadin-menu-bar")
], M.prototype, "menubar", 2);
Y([
  E()
], M.prototype, "dragging", 2);
Y([
  z("copilot-feedback")
], M.prototype, "feedback", 2);
M = Y([
  C("copilot-activation-button")
], M);
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
class Z extends ve {
  constructor(t) {
    if (super(t), this.it = w, t.type !== Q.CHILD)
      throw Error(this.constructor.directiveName + "() can only be used in child bindings");
  }
  render(t) {
    if (t === w || t == null)
      return this._t = void 0, this.it = t;
    if (t === Re)
      return t;
    if (typeof t != "string")
      throw Error(this.constructor.directiveName + "() called with a non-string value");
    if (t === this.it)
      return this._t;
    this.it = t;
    const i = [t];
    return i.raw = i, this._t = { _$litType$: this.constructor.resultType, strings: i, values: [] };
  }
}
Z.directiveName = "unsafeHTML", Z.resultType = 1;
const pt = J(Z);
var gt = Object.defineProperty, vt = Object.getOwnPropertyDescriptor, N = (e, t, i, o) => {
  for (var n = o > 1 ? void 0 : o ? vt(t, i) : t, s = e.length - 1, r; s >= 0; s--)
    (r = e[s]) && (n = (o ? r(t, i, n) : r(n)) || n);
  return o && n && gt(t, i, n), n;
};
const g = "resize-dir", W = "floating-resizing-active";
let $ = class extends S {
  constructor() {
    super(...arguments), this.panelTag = "", this.floatingResizingStarted = !1, this.resizingInDrawerStarted = !1, this.toggling = !1, this.rectangleBeforeResizing = null, this.floatingResizeHandlerMouseMoveListener = (e) => {
      if (!this.panelInfo?.floating || this.floatingResizingStarted || !this.panelInfo?.expanded)
        return;
      const t = this.getBoundingClientRect(), i = Math.abs(e.clientX - t.x), o = Math.abs(t.x + t.width - e.clientX), n = Math.abs(e.clientY - t.y), s = Math.abs(t.y + t.height - e.clientY), r = 16;
      let a = "";
      if (i < r ? n < r ? (a = "nw-resize", this.setAttribute(g, "top left")) : s < r ? (a = "sw-resize", this.setAttribute(g, "bottom left")) : (a = "col-resize", this.setAttribute(g, "left")) : o < r ? n < r ? (a = "ne-resize", this.setAttribute(g, "top right")) : s < r ? (a = "se-resize", this.setAttribute(g, "bottom right")) : (a = "col-resize", this.setAttribute(g, "right")) : s < r ? (a = "row-resize", this.setAttribute(g, "bottom")) : n < r && (a = "row-resize", this.setAttribute(g, "top")), a !== "") {
        const d = window.getComputedStyle(this), p = Number.parseInt(d.borderTopWidth, 10), v = Number.parseInt(d.borderTopWidth, 10), h = Number.parseInt(d.borderLeftWidth, 10), b = Number.parseInt(d.borderRightWidth, 10);
        this.rectangleBeforeResizing = this.getBoundingClientRect(), this.rectangleBeforeResizing.width -= h + b, this.rectangleBeforeResizing.height -= p + v, this.style.setProperty("--resize-cursor", a);
      } else
        this.style.removeProperty("--resize-cursor"), this.removeAttribute(g);
      this.toggleAttribute(W, a !== "");
    }, this.floatingResizingMouseDownListener = (e) => {
      this.hasAttribute(W) && (e.stopPropagation(), e.preventDefault(), f.anchorLeftTop(this), this.floatingResizingStarted = !0, this.toggleAttribute("resizing", !0), oe(() => {
        c.sectionPanelResizing = !0;
      }));
    }, this.floatingResizingMouseLeaveListener = () => {
      this.panelInfo?.floating && (this.floatingResizingStarted || (this.removeAttribute("resizing"), this.removeAttribute(W), this.removeAttribute("dragging"), this.style.removeProperty("--resize-cursor"), this.removeAttribute(g)));
    }, this.floatingResizingMouseMoveListener = (e) => {
      if (!this.panelInfo?.floating || !this.floatingResizingStarted)
        return;
      const t = this.getAttribute(g);
      if (t === null)
        return;
      e.stopPropagation(), e.preventDefault();
      const { clientX: i, clientY: o } = e, n = t.split(" "), s = this.rectangleBeforeResizing;
      if (n.includes("left")) {
        const r = Math.max(0, i);
        this.setFloatingResizeDirectionProps("left", r, s.left - r + s.width);
      }
      if (n.includes("right")) {
        const r = Math.max(0, i);
        this.setFloatingResizeDirectionProps("right", r, r - s.right + s.width);
      }
      if (n.includes("top")) {
        const r = Math.max(0, o), a = s.top - r + s.height;
        this.setFloatingResizeDirectionProps("top", r, void 0, a);
      }
      if (n.includes("bottom")) {
        const r = Math.max(0, o), a = r - s.bottom + s.height;
        this.setFloatingResizeDirectionProps("bottom", r, void 0, a);
      }
    }, this.setFloatingResizeDirectionProps = (e, t, i, o) => {
      i && i > Number.parseFloat(window.getComputedStyle(this).getPropertyValue("--min-width")) && (this.style.setProperty(`--${e}`, `${t}px`), this.style.setProperty("width", `${i}px`)), o && o > Number.parseFloat(window.getComputedStyle(this).getPropertyValue("--header-height")) && (this.style.setProperty(`--${e}`, `${t}px`), this.style.setProperty("height", `${o}px`));
    }, this.floatingResizingMouseUpListener = (e) => {
      if (!this.floatingResizingStarted || !this.panelInfo?.floating)
        return;
      e.stopPropagation(), e.preventDefault(), this.floatingResizingStarted = !1, oe(() => {
        c.sectionPanelResizing = !1;
      });
      const { width: t, height: i } = this.getBoundingClientRect(), { left: o, top: n, bottom: s, right: r } = f.anchor(this), a = window.getComputedStyle(this), d = Number.parseInt(a.borderTopWidth, 10), p = Number.parseInt(a.borderTopWidth, 10), v = Number.parseInt(a.borderLeftWidth, 10), h = Number.parseInt(a.borderRightWidth, 10);
      l.updatePanel(this.panelInfo.tag, {
        width: t - (v + h),
        height: i - (d + p),
        floatingPosition: {
          ...this.panelInfo.floatingPosition,
          left: o,
          top: n,
          bottom: s,
          right: r
        }
      }), this.style.removeProperty("width"), this.style.removeProperty("height"), this.setCssSizePositionProperties(), this.toggleAttribute("dragging", !1);
    }, this.transitionEndEventListener = () => {
      this.toggling && (this.toggling = !1, f.anchor(this));
    }, this.resizeInDrawerMouseDownListener = (e) => {
      e.button === 0 && (this.resizingInDrawerStarted = !0, this.setAttribute("resizing", ""));
    }, this.resizeInDrawerMouseMoveListener = (e) => {
      if (!this.resizingInDrawerStarted)
        return;
      const { y: t } = e;
      e.stopPropagation(), e.preventDefault();
      const i = t - this.getBoundingClientRect().top;
      this.style.setProperty("--section-height", `${i}px`), l.updatePanel(this.panelInfo.tag, {
        height: i
      });
    }, this.resizeInDrawerMouseUpListener = () => {
      this.resizingInDrawerStarted && (this.panelInfo?.floating || (this.resizingInDrawerStarted = !1, this.removeAttribute("resizing"), this.style.setProperty("--section-height", `${this.getBoundingClientRect().height}px`)));
    }, this.sectionPanelMouseEnterListener = () => {
      this.hasAttribute(y) && (this.removeAttribute(y), l.clearAttention());
    }, this.contentAreaMouseDownListener = () => {
      l.addFocusedFloatingPanel(this.panelInfo);
    }, this.documentMouseUpEventListener = () => {
      document.removeEventListener("mousemove", this.draggingEventListener), this.panelInfo?.floating && (this.toggleAttribute("dragging", !1), c.setSectionPanelDragging(!1));
    }, this.panelHeaderMouseDownEventListener = (e) => {
      e.button === 0 && (e.target instanceof HTMLButtonElement && e.target.getAttribute("part") === "title-button" || (l.addFocusedFloatingPanel(this.panelInfo), !this.hasAttribute(g) && (f.draggingStarts(this, e), document.addEventListener("mousemove", this.draggingEventListener), c.setSectionPanelDragging(!0), this.panelInfo?.floating ? this.toggleAttribute("dragging", !0) : this.parentElement.sectionPanelDraggingStarted(this, e), e.preventDefault(), e.stopPropagation())));
    }, this.draggingEventListener = (e) => {
      const t = f.dragging(this, e);
      if (this.panelInfo?.floating && this.panelInfo?.floatingPosition) {
        e.preventDefault();
        const { left: i, top: o, bottom: n, right: s } = t;
        l.updatePanel(this.panelInfo.tag, {
          floatingPosition: {
            ...this.panelInfo.floatingPosition,
            left: i,
            top: o,
            bottom: n,
            right: s
          }
        });
      }
    }, this.setCssSizePositionProperties = () => {
      const e = l.getPanelByTag(this.panelTag);
      if (e && (e.height !== void 0 && (this.panelInfo?.floating || e.panel === "left" || e.panel === "right" ? this.style.setProperty("--section-height", `${e.height}px`) : this.style.removeProperty("--section-height")), e.width !== void 0 && (e.floating || e.panel === "bottom" ? this.style.setProperty("--section-width", `${e.width}px`) : this.style.removeProperty("--section-width")), e.floating && e.floatingPosition && !this.toggling)) {
        const { left: t, top: i, bottom: o, right: n } = e.floatingPosition;
        this.style.setProperty("--left", t !== void 0 ? `${t}px` : "auto"), this.style.setProperty("--top", i !== void 0 ? `${i}px` : "auto"), this.style.setProperty("--bottom", o !== void 0 ? `${o}px` : ""), this.style.setProperty("--right", n !== void 0 ? `${n}px` : "");
      }
    }, this.changePanelFloating = (e) => {
      if (this.panelInfo)
        if (e.stopPropagation(), De(this), this.panelInfo?.floating)
          l.updatePanel(this.panelInfo?.tag, { floating: !1 });
        else {
          let t;
          if (this.panelInfo.floatingPosition)
            t = this.panelInfo?.floatingPosition;
          else {
            const { left: n, top: s } = this.getBoundingClientRect();
            t = {
              left: n,
              top: s
            };
          }
          let i = this.panelInfo?.height;
          i === void 0 && this.panelInfo.expanded && (i = Number.parseInt(window.getComputedStyle(this).height, 10)), this.parentElement.forceClose(), l.updatePanel(this.panelInfo?.tag, {
            floating: !0,
            width: this.panelInfo?.width || Number.parseInt(window.getComputedStyle(this).width, 10),
            height: i,
            floatingPosition: t
          }), l.addFocusedFloatingPanel(this.panelInfo);
        }
    }, this.toggleExpand = (e) => {
      this.panelInfo && (e.stopPropagation(), f.anchorLeftTop(this), l.updatePanel(this.panelInfo.tag, {
        expanded: !this.panelInfo.expanded
      }), this.toggling = !0, this.toggleAttribute("expanded", this.panelInfo.expanded));
    };
  }
  static get styles() {
    return [
      K(ee),
      T`
        * {
          box-sizing: border-box;
        }

        :host {
          flex: none;
          display: grid;
          align-content: start;
          grid-template-rows: auto 1fr;
          transition: grid-template-rows var(--duration-2);
          overflow: hidden;
          position: relative;
          --min-width: 160px;
          --resize-div-size: 10px;
          --header-height: 37px;
          --content-height: calc(var(--section-height) - var(--header-height));
          --content-width: var(--content-width, 100%);
          --floating-border-width: 1px;
          cursor: var(--resize-cursor, default);
        }

        :host(:not([expanded])) {
          grid-template-rows: auto 0fr;
          --content-height: 0px !important;
        }

        [part='header'] {
          align-items: center;
          color: var(--color-high-contrast);
          display: flex;
          flex: none;
          font: var(--font-small-bold);
          justify-content: space-between;
          min-width: 100%;
          user-select: none;
          -webkit-user-select: none;
          width: var(--min-width);
          height: var(--header-height);
        }

        :host([floating]:not([expanded])) [part='header'] {
          --min-width: unset;
        }

        [part='header'] {
          border-bottom: 1px solid var(--border-color);
        }

        :host([floating]) [part='header'] {
          transition: border-color var(--duration-2);
        }

        :host([floating]:not([expanded])) [part='header'] {
          border-color: transparent;
        }

        [part='title'] {
          flex: auto;
          margin: 0;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        [part='content'] {
          height: var(--content-height);
          overflow: auto;
          transition:
            height var(--duration-2),
            width var(--duration-2),
            opacity var(--duration-2),
            visibility calc(var(--duration-2) * 2);
        }

        [part='drawer-resize'] {
          resize: vertical;
          cursor: row-resize;
          position: absolute;
          bottom: -5px;
          left: 0;
          width: 100%;
          height: 10px;
        }

        :host([floating]) [part='drawer-resize'] {
          display: none;
        }

        :host(:not([expanded])) [part='drawer-resize'] {
          display: none;
        }

        :host(:not([floating]):not(:last-child)) {
          border-bottom: 1px solid var(--border-color);
        }

        :host(:not([expanded])) [part='content'] {
          opacity: 0;
          visibility: hidden;
        }

        :host([floating]:not([expanded])) [part='content'] {
          width: 0;
          height: 0;
        }

        :host(:not([expanded])) [part='content'][style*='height'] {
          height: 0 !important;
        }

        :host(:not([expanded])) [part='content'][style*='width'] {
          width: 0 !important;
        }

        :host([floating]) {
          position: fixed;
          overflow: hidden;
          min-width: 0;
          min-height: 0;
          z-index: calc(var(--z-index-floating-panel) + var(--z-index-focus, 0));
          box-shadow: var(--surface-box-shadow-2);
          background: var(--surface);
          border: var(--floating-border-width) solid var(--surface-border-color);
          -webkit-backdrop-filter: var(--surface-backdrop-filter);
          backdrop-filter: var(--surface-backdrop-filter);
          border-radius: var(--radius-2);
          top: clamp(0px, var(--top), calc(100vh - var(--section-height) * 0.5));
          left: clamp(calc(var(--section-width) * -0.5), var(--left), calc(100vw - var(--section-width) * 0.5));
          bottom: clamp(calc(var(--section-height) * -0.5), var(--bottom), calc(100vh - var(--section-height) * 0.5));
          right: clamp(calc(var(--section-width) * -0.5), var(--right), calc(100vw - var(--section-width) * 0.5));
          width: var(--section-width);
        }

        :host([floating]:not([expanded])) {
          width: unset;
        }

        :host([floating]) .drag-handle {
          cursor: var(--resize-cursor, move);
        }

        :host([floating][expanded]) [part='content'] {
          min-width: var(--min-width);
          min-height: 0;
          max-height: 85vh;
          max-width: 90vw;
          width: var(--content-width);
        }

        /* :hover for Firefox, :active for others */

        :host([floating][expanded]) [part='content']:is(:hover, :active) {
          transition: none;
        }

        [part='header'] button {
          align-items: center;
          appearance: none;
          background: transparent;
          border: 0px;
          border-radius: var(--radius-1);
          color: var(--color);
          display: flex;
          flex: 0 0 auto;
          height: 2.25rem;
          justify-content: center;
          padding: 0px;
          width: 16px;
          margin-left: 10px;
          margin-right: 10px;
        }

        div.actions {
          width: auto;
        }

        :host(:not([expanded])) div.actions {
          display: none;
        }

        [part='title'] button {
          color: var(--color-high-contrast);
          font: var(--font-xsmall-strong);
          width: auto;
        }

        [part='header'] button:hover {
          color: var(--color-high-contrast);
        }

        [part='header'] button:focus-visible {
          outline: 2px solid var(--blue-500);
          outline-offset: -2px;
        }

        [part='header'] button svg {
          display: block;
        }

        [part='header'] .actions:empty {
          display: none;
        }

        ::slotted(*) {
          box-sizing: border-box;
          display: block;
          height: var(--content-height, var(--default-content-height, 100%));
          /* padding: var(--space-150); */
          width: 100%;
        }

        :host(:not([floating])) ::slotted(*) {
          /* padding-top: var(--space-50); */
        }

        :host([dragging]) {
          opacity: 0.4;
        }

        :host([dragging]) [part='content'] {
          pointer-events: none;
        }

        :host([attention-required]) {
          --pulse-animation-duration: 2s;
          --pulse-first-color: var(--red-500);
          --pulse-second-color: var(--red-800);
          --pulse-size: 12px;
          animation: pulse 2s infinite;
        }

        :host([resizing]),
        :host([resizing]) [part='content'] {
          transition: none;
        }

        :host([hiding-while-drag-and-drop]) {
          display: none;
        }

        // dragging in drawer

        :host(:not([floating])) .drag-handle {
          cursor: grab;
        }

        :host(:not([floating])[dragging]) .drag-handle {
          cursor: grabbing;
        }
      `
    ];
  }
  connectedCallback() {
    super.connectedCallback(), this.setAttribute("role", "region"), this.reaction(
      () => l.getAttentionRequiredPanelConfiguration(),
      () => {
        const e = l.getAttentionRequiredPanelConfiguration();
        this.toggleAttribute(y, e?.tag === this.panelTag && e?.floating);
      }
    ), this.addEventListener("mouseenter", this.sectionPanelMouseEnterListener), document.addEventListener("mousemove", this.resizeInDrawerMouseMoveListener), document.addEventListener("mouseup", this.resizeInDrawerMouseUpListener), this.reaction(
      () => c.operationInProgress,
      () => {
        requestAnimationFrame(() => {
          this.toggleAttribute(
            "hiding-while-drag-and-drop",
            c.operationInProgress === pe.DragAndDrop && this.panelInfo?.floating && !this.panelInfo.showWhileDragging
          );
        });
      }
    ), this.reaction(
      () => l.floatingPanelsZIndexOrder,
      () => {
        this.style.setProperty("--z-index-focus", `${l.getFloatingPanelZIndex(this.panelTag)}`);
      },
      { fireImmediately: !0 }
    ), this.addEventListener("transitionend", this.transitionEndEventListener), this.addEventListener("mousemove", this.floatingResizeHandlerMouseMoveListener), this.addEventListener("mousedown", this.floatingResizingMouseDownListener), this.addEventListener("mouseleave", this.floatingResizingMouseLeaveListener), document.addEventListener("mousemove", this.floatingResizingMouseMoveListener), document.addEventListener("mouseup", this.floatingResizingMouseUpListener);
  }
  disconnectedCallback() {
    super.disconnectedCallback(), this.removeEventListener("mouseenter", this.sectionPanelMouseEnterListener), this.drawerResizeElement.removeEventListener("mousedown", this.resizeInDrawerMouseDownListener), document.removeEventListener("mousemove", this.resizeInDrawerMouseMoveListener), document.removeEventListener("mouseup", this.resizeInDrawerMouseUpListener), this.removeEventListener("mousemove", this.floatingResizeHandlerMouseMoveListener), this.removeEventListener("mousedown", this.floatingResizingMouseDownListener), document.removeEventListener("mousemove", this.floatingResizingMouseMoveListener), document.removeEventListener("mouseup", this.floatingResizingMouseUpListener);
  }
  willUpdate(e) {
    super.willUpdate(e), e.has("panelTag") && (this.panelInfo = l.getPanelByTag(this.panelTag), this.setAttribute("aria-labelledby", this.panelInfo.tag.concat("-title"))), this.toggleAttribute("floating", this.panelInfo?.floating);
  }
  updated(e) {
    super.updated(e), this.setCssSizePositionProperties();
  }
  firstUpdated(e) {
    super.firstUpdated(e), document.addEventListener("mouseup", this.documentMouseUpEventListener), this.headerDraggableArea.addEventListener("mousedown", this.panelHeaderMouseDownEventListener), this.toggleAttribute("expanded", this.panelInfo?.expanded), Ie(this), this.setCssSizePositionProperties(), this.contentArea.addEventListener("mousedown", this.contentAreaMouseDownListener), this.drawerResizeElement.addEventListener("mousedown", this.resizeInDrawerMouseDownListener);
  }
  render() {
    return this.panelInfo ? u`
      <div part="header" class="drag-handle">
        <button
          part="toggle-button"
          @mousedown="${(e) => e.stopPropagation()}"
          @click="${(e) => this.toggleExpand(e)}"
          aria-expanded="${this.panelInfo.expanded}"
          aria-controls="content"
          aria-label="Expand ${this.panelInfo.header}">
          ${this.panelInfo.expanded ? L.chevronDown : L.chevronRight}
        </button>
        <h2 id="${this.panelInfo.tag}-title" part="title">
          <button part="title-button" @dblclick="${(e) => this.toggleExpand(e)}">
            ${this.panelInfo.header}
          </button>
        </h2>
        <div class="actions" @mousedown="${(e) => e.stopPropagation()}">${this.renderActions()}</div>
        <button
          part="popup-button"
          @click="${(e) => this.changePanelFloating(e)}"
          @mousedown="${(e) => e.stopPropagation()}"
          aria-label=${this.panelInfo.floating ? `Close the ${this.panelInfo.header} popup` : `Open ${this.panelInfo.header} as a popup`}>
          ${this.panelInfo.floating ? L.close : L.popup}
        </button>
      </div>
      <div part="content" id="content">
        <slot name="content"></slot>
      </div>
      <div part="drawer-resize"></div>
    ` : w;
  }
  renderActions() {
    if (!this.panelInfo?.actionsTag)
      return w;
    const e = this.panelInfo.actionsTag;
    return pt(`<${e}></${e}>`);
  }
};
N([
  O()
], $.prototype, "panelTag", 2);
N([
  z(".drag-handle")
], $.prototype, "headerDraggableArea", 2);
N([
  z("#content")
], $.prototype, "contentArea", 2);
N([
  z('[part="drawer-resize"]')
], $.prototype, "drawerResizeElement", 2);
$ = N([
  C("copilot-section-panel-wrapper")
], $);
m.on("undoRedo", (e) => {
  const t = e.detail.files ?? Te();
  e.detail.undo ? m.send("copilot-plugin-undo", { files: t }) : m.send("copilot-plugin-redo", { files: t });
});
var ft = Object.defineProperty, mt = Object.getOwnPropertyDescriptor, bt = (e, t, i, o) => {
  for (var n = o > 1 ? void 0 : o ? mt(t, i) : t, s = e.length - 1, r; s >= 0; s--)
    (r = e[s]) && (n = (o ? r(t, i, n) : r(n)) || n);
  return o && n && ft(t, i, n), n;
};
let he = class extends S {
  static get styles() {
    return T`
      :host {
        position: fixed;
        bottom: 2.5rem;
        right: 0rem;
        visibility: visible; /* Always show, even if copilot is off */
        user-select: none;
        z-index: 10000;

        --dev-tools-text-color: rgba(255, 255, 255, 0.8);

        --dev-tools-text-color-secondary: rgba(255, 255, 255, 0.65);
        --dev-tools-text-color-emphasis: rgba(255, 255, 255, 0.95);
        --dev-tools-text-color-active: rgba(255, 255, 255, 1);

        --dev-tools-background-color-inactive: rgba(45, 45, 45, 0.25);
        --dev-tools-background-color-active: rgba(45, 45, 45, 0.98);
        --dev-tools-background-color-active-blurred: rgba(45, 45, 45, 0.85);

        --dev-tools-border-radius: 0.5rem;
        --dev-tools-box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05), 0 4px 12px -2px rgba(0, 0, 0, 0.4);

        --dev-tools-blue-hsl: 206, 100%, 70%;
        --dev-tools-blue-color: hsl(var(--dev-tools-blue-hsl));
        --dev-tools-green-hsl: 145, 80%, 42%;
        --dev-tools-green-color: hsl(var(--dev-tools-green-hsl));
        --dev-tools-grey-hsl: 0, 0%, 50%;
        --dev-tools-grey-color: hsl(var(--dev-tools-grey-hsl));
        --dev-tools-yellow-hsl: 38, 98%, 64%;
        --dev-tools-yellow-color: hsl(var(--dev-tools-yellow-hsl));
        --dev-tools-red-hsl: 355, 100%, 68%;
        --dev-tools-red-color: hsl(var(--dev-tools-red-hsl));

        /* Needs to be in ms, used in JavaScript as well */
        --dev-tools-transition-duration: 180ms;
      }

      .notification-tray {
        display: flex;
        flex-direction: column-reverse;
        align-items: flex-end;
        margin: 0.5rem;
        flex: none;
      }

      @supports (backdrop-filter: blur(1px)) {
        .notification-tray div.message {
          backdrop-filter: blur(8px);
        }

        .notification-tray div.message {
          background-color: var(--dev-tools-background-color-active-blurred);
        }
      }

      .notification-tray .message {
        pointer-events: auto;
        background-color: var(--dev-tools-background-color-active);
        color: var(--dev-tools-text-color);
        max-width: 30rem;
        box-sizing: border-box;
        border-radius: var(--dev-tools-border-radius);
        margin-top: 0.5rem;
        transition: var(--dev-tools-transition-duration);
        transform-origin: bottom right;
        animation: slideIn var(--dev-tools-transition-duration);
        box-shadow: var(--dev-tools-box-shadow);
        padding-top: 0.25rem;
        padding-bottom: 0.25rem;
      }

      .notification-tray .message.animate-out {
        animation: slideOut forwards var(--dev-tools-transition-duration);
      }

      .notification-tray .message .message-details {
        max-height: 10em;
        overflow: hidden;
      }

      .message.information {
        --dev-tools-notification-color: var(--dev-tools-blue-color);
      }

      .message.warning {
        --dev-tools-notification-color: var(--dev-tools-yellow-color);
      }

      .message.error {
        --dev-tools-notification-color: var(--dev-tools-red-color);
      }

      .message {
        display: flex;
        padding: 0.1875rem 0.75rem 0.1875rem 2rem;
        background-clip: padding-box;
      }

      .message.log {
        padding-left: 0.75rem;
      }

      .message-content {
        margin-right: 0.5rem;
        -webkit-user-select: text;
        -moz-user-select: text;
        user-select: text;
      }

      .message-heading {
        position: relative;
        display: flex;
        align-items: center;
        margin: 0.125rem 0;
      }

      .message .message-details {
        font-weight: 400;
        color: var(--dev-tools-text-color-secondary);
        margin: 0.25rem 0;
      }

      .message .message-details[hidden] {
        display: none;
      }

      .message .message-details p {
        display: inline;
        margin: 0;
        margin-right: 0.375em;
        word-break: break-word;
      }

      .message .persist {
        color: var(--dev-tools-text-color-secondary);
        white-space: nowrap;
        margin: 0.375rem 0;
        display: flex;
        align-items: center;
        position: relative;
        -webkit-user-select: none;
        -moz-user-select: none;
        user-select: none;
      }

      .message .persist::before {
        content: '';
        width: 1em;
        height: 1em;
        border-radius: 0.2em;
        margin-right: 0.375em;
        background-color: rgba(255, 255, 255, 0.3);
      }

      .message .persist:hover::before {
        background-color: rgba(255, 255, 255, 0.4);
      }

      .message .persist.on::before {
        background-color: rgba(255, 255, 255, 0.9);
      }

      .message .persist.on::after {
        content: '';
        order: -1;
        position: absolute;
        width: 0.75em;
        height: 0.25em;
        border: 2px solid var(--dev-tools-background-color-active);
        border-width: 0 0 2px 2px;
        transform: translate(0.05em, -0.05em) rotate(-45deg) scale(0.8, 0.9);
      }

      .message .dismiss-message {
        font-weight: 600;
        align-self: stretch;
        display: flex;
        align-items: center;
        padding: 0 0.25rem;
        margin-left: 0.5rem;
        color: var(--dev-tools-text-color-secondary);
      }

      .message .dismiss-message:hover {
        color: var(--dev-tools-text-color);
      }

      .message.log {
        color: var(--dev-tools-text-color-secondary);
      }

      .message:not(.log) .message-heading {
        font-weight: 500;
      }

      .message.has-details .message-heading {
        color: var(--dev-tools-text-color-emphasis);
        font-weight: 600;
      }

      .message-heading::before {
        position: absolute;
        margin-left: -1.5rem;
        display: inline-block;
        text-align: center;
        font-size: 0.875em;
        font-weight: 600;
        line-height: calc(1.25em - 2px);
        width: 14px;
        height: 14px;
        box-sizing: border-box;
        border: 1px solid transparent;
        border-radius: 50%;
      }

      .message.information .message-heading::before {
        content: 'i';
        border-color: currentColor;
        color: var(--dev-tools-notification-color);
      }

      .message.warning .message-heading::before,
      .message.error .message-heading::before {
        content: '!';
        color: var(--dev-tools-background-color-active);
        background-color: var(--dev-tools-notification-color);
      }

      .ahreflike {
        font-weight: 500;
        color: var(--dev-tools-text-color-secondary);
        text-decoration: underline;
        cursor: pointer;
      }

      @keyframes slideIn {
        from {
          transform: translateX(100%);
          opacity: 0;
        }
        to {
          transform: translateX(0%);
          opacity: 1;
        }
      }

      @keyframes slideOut {
        from {
          transform: translateX(0%);
          opacity: 1;
        }
        to {
          transform: translateX(100%);
          opacity: 0;
        }
      }

      @keyframes fade-in {
        0% {
          opacity: 0;
        }
      }

      @keyframes bounce {
        0% {
          transform: scale(0.8);
        }
        50% {
          transform: scale(1.5);
          background-color: hsla(var(--dev-tools-red-hsl), 1);
        }
        100% {
          transform: scale(1);
        }
      }
    `;
  }
  render() {
    return u`<div class="notification-tray">
      ${c.notifications.map((e) => this.renderNotification(e))}
    </div>`;
  }
  renderNotification(e) {
    return u`
      <div
        class="message ${e.type} ${e.animatingOut ? "animate-out" : ""} ${e.details || e.link ? "has-details" : ""}"
        data-testid="message">
        <div class="message-content">
          <div class="message-heading">${e.message}</div>
          <div class="message-details" ?hidden="${!e.details && !e.link}">
            ${Le(e.details)}
            ${e.link ? u`<a class="ahreflike" href="${e.link}" target="_blank">Learn more</a>` : ""}
          </div>
          ${e.dismissId ? u`<div
                class="persist ${e.dontShowAgain ? "on" : "off"}"
                @click=${() => {
      this.toggleDontShowAgain(e);
    }}>
                ${yt(e)}
              </div>` : ""}
        </div>
        <div
          class="dismiss-message"
          @click=${(t) => {
      Me(e), t.stopPropagation();
    }}>
          Dismiss
        </div>
      </div>
    `;
  }
  toggleDontShowAgain(e) {
    e.dontShowAgain = !e.dontShowAgain, this.requestUpdate();
  }
};
he = bt([
  C("copilot-notifications-container")
], he);
function yt(e) {
  return e.dontShowAgainMessage ? e.dontShowAgainMessage : e.dismissTarget === "machine" ? "Do not show this again on this machine" : `Do not show this again for ${location.host}`;
}
Se({
  type: _e.WARNING,
  message: "Development Mode",
  details: "This application is running in development mode.",
  dismissId: "devmode",
  dismissTarget: "machine"
});
var ie = { exports: {} };
function xe(e, t = 100, i = {}) {
  if (typeof e != "function")
    throw new TypeError(`Expected the first parameter to be a function, got \`${typeof e}\`.`);
  if (t < 0)
    throw new RangeError("`wait` must not be negative.");
  const { immediate: o } = typeof i == "boolean" ? { immediate: i } : i;
  let n, s, r, a, d;
  function p() {
    const h = Date.now() - a;
    if (h < t && h >= 0)
      r = setTimeout(p, t - h);
    else if (r = void 0, !o) {
      const b = n, V = s;
      n = void 0, s = void 0, d = e.apply(b, V);
    }
  }
  const v = function(...h) {
    if (n && this !== n)
      throw new Error("Debounced method called with different contexts.");
    n = this, s = h, a = Date.now();
    const b = o && !r;
    if (r || (r = setTimeout(p, t)), b) {
      const V = n, ze = s;
      n = void 0, s = void 0, d = e.apply(V, ze);
    }
    return d;
  };
  return v.clear = () => {
    r && (clearTimeout(r), r = void 0);
  }, v.flush = () => {
    if (!r)
      return;
    const h = n, b = s;
    n = void 0, s = void 0, d = e.apply(h, b), clearTimeout(r), r = void 0;
  }, v;
}
ie.exports.debounce = xe;
ie.exports = xe;
var wt = ie.exports;
const xt = /* @__PURE__ */ Oe(wt), Pe = xt(() => {
  m.emit("component-tree-updated", {});
});
m.on("vite-after-update", () => {
  Pe();
});
const ue = window?.Vaadin?.connectionState?.stateChangeListeners;
ue ? ue.add((e, t) => {
  e === "loading" && t === "connected" && c.active && Pe();
}) : console.warn("Unable to add listener for connection state changes");
