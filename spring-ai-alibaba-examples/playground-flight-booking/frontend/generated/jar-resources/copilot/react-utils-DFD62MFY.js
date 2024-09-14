const d = Symbol.for("react.portal"), _ = Symbol.for("react.fragment"), y = Symbol.for("react.strict_mode"), S = Symbol.for("react.profiler"), E = Symbol.for("react.provider"), p = Symbol.for("react.context"), s = Symbol.for("react.forward_ref"), T = Symbol.for("react.suspense"), g = Symbol.for("react.suspense_list"), C = Symbol.for("react.memo"), R = Symbol.for("react.lazy");
function N(e, t, n) {
  const r = e.displayName;
  if (r)
    return r;
  const o = t.displayName || t.name || "";
  return o !== "" ? `${n}(${o})` : n;
}
function i(e) {
  return e.displayName || "Context";
}
function u(e) {
  if (e == null)
    return null;
  if (typeof e == "function")
    return e.displayName || e.name || null;
  if (typeof e == "string")
    return e;
  switch (e) {
    case _:
      return "Fragment";
    case d:
      return "Portal";
    case S:
      return "Profiler";
    case y:
      return "StrictMode";
    case T:
      return "Suspense";
    case g:
      return "SuspenseList";
  }
  if (typeof e == "object")
    switch (e.$$typeof) {
      case p:
        return `${i(e)}.Consumer`;
      case E:
        return `${i(e._context)}.Provider`;
      case s:
        return N(e, e.render, "ForwardRef");
      case C:
        const t = e.displayName || null;
        return t !== null ? t : u(e.type) || "Memo";
      case R: {
        const n = e, r = n._payload, o = n._init;
        try {
          return u(o(r));
        } catch {
          return null;
        }
      }
    }
  return null;
}
let a;
function K() {
  const e = /* @__PURE__ */ new Set();
  return Array.from(document.body.querySelectorAll("*")).flatMap(F).filter(P).forEach((n) => e.add(n.fileName)), Array.from(e);
}
function P(e) {
  return !!e && e.fileName;
}
function A(e) {
  return e?._debugSource || void 0;
}
function h(e) {
  if (e && e.type?.__debugSourceDefine)
    return e.type.__debugSourceDefine;
}
function F(e) {
  return A(f(e));
}
function b() {
  return `__reactFiber$${l()}`;
}
function I() {
  return `__reactContainer$${l()}`;
}
function l() {
  if (!(!a && (a = Array.from(document.querySelectorAll("*")).flatMap((e) => Object.keys(e)).filter((e) => e.startsWith("__reactFiber$")).map((e) => e.replace("__reactFiber$", "")).find((e) => e), !a)))
    return a;
}
function $(e) {
  const t = e.type;
  return t?.$$typeof === s && !t.displayName && e.child ? $(e.child) : u(e.type) ?? u(e.elementType) ?? "???";
}
function L() {
  const e = Array.from(document.querySelectorAll("body > *")).flatMap((n) => n[I()]).find((n) => n), t = c(e);
  return c(t?.child);
}
function O(e) {
  const t = [];
  let n = c(e.child);
  for (; n; )
    t.push(n), n = c(n.sibling);
  return t;
}
const Y = (e) => {
  const t = O(e);
  if (t.length === 0)
    return [];
  const n = t.filter((r) => M(r) || x(r));
  return n.length === t.length ? t : t.flatMap((r) => n.includes(r) ? r : Y(r));
};
function v(e) {
  return e.hasOwnProperty("entanglements") && e.hasOwnProperty("containerInfo");
}
function w(e) {
  return e.hasOwnProperty("stateNode") && e.hasOwnProperty("pendingProps");
}
function c(e) {
  const t = e?.stateNode;
  if (t?.current && (v(t) || w(t)))
    return t?.current;
  if (!e)
    return;
  if (!e.alternate)
    return e;
  const n = e.alternate, r = e?.actualStartTime, o = n?.actualStartTime;
  return o !== r && o > r ? n : e;
}
function f(e) {
  const t = b(), n = c(e[t]);
  if (n?._debugSource)
    return n;
  let r = n?.return || void 0;
  for (; r && !r._debugSource; )
    r = r.return || void 0;
  return r;
}
function m(e) {
  if (e.stateNode?.isConnected === !0)
    return e.stateNode;
  if (e.child)
    return m(e.child);
}
function M(e) {
  const t = m(e);
  return t && c(f(t)) === e;
}
function x(e) {
  return typeof e.type != "function" ? !1 : !!(e._debugSource || h(e));
}
export {
  m as a,
  Y as b,
  $ as c,
  L as d,
  c as e,
  f,
  F as g,
  K as h,
  M as i
};
