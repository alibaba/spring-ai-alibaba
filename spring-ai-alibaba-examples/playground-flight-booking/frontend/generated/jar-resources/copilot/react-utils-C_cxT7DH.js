let o;
function F() {
  const e = /* @__PURE__ */ new Set();
  return Array.from(document.body.querySelectorAll("*")).flatMap(m).filter(f).forEach((t) => e.add(t.fileName)), Array.from(e);
}
function f(e) {
  return !!e && e.fileName;
}
function s(e) {
  return e?._debugSource || void 0;
}
function d(e) {
  if (e && e.type?.__debugSourceDefine)
    return e.type.__debugSourceDefine;
}
function m(e) {
  return s(a(e));
}
function g() {
  return `__reactFiber$${c()}`;
}
function y() {
  return `__reactContainer$${c()}`;
}
function c() {
  if (!(!o && (o = Array.from(document.querySelectorAll("*")).flatMap((e) => Object.keys(e)).filter((e) => e.startsWith("__reactFiber$")).map((e) => e.replace("__reactFiber$", "")).find((e) => e), !o)))
    return o;
}
function b() {
  const e = Array.from(document.querySelectorAll("body > *")).flatMap((t) => t[y()]).find((t) => t), n = u(e);
  return u(n?.child);
}
function S(e) {
  const n = [];
  let t = u(e.child);
  for (; t; )
    n.push(t), t = u(t.sibling);
  return n;
}
const p = (e) => {
  const n = S(e);
  if (n.length === 0)
    return [];
  const t = n.filter((r) => _(r) || h(r));
  return t.length === n.length ? n : n.flatMap((r) => t.includes(r) ? r : p(r));
};
function u(e) {
  const n = e?.stateNode?.current;
  if (n)
    return n;
  if (!e)
    return;
  if (!e.alternate)
    return e;
  const t = e.alternate, r = e?.actualStartTime, i = t?.actualStartTime;
  return i !== r && i > r ? t : e;
}
function a(e) {
  const n = g(), t = u(e[n]);
  if (t?._debugSource)
    return t;
  let r = t?.return || void 0;
  for (; r && !r._debugSource; )
    r = r.return || void 0;
  return r;
}
function l(e) {
  if (e.stateNode?.isConnected === !0)
    return e.stateNode;
  if (e.child)
    return l(e.child);
}
function _(e) {
  const n = l(e);
  return n && u(a(n)) === e;
}
function h(e) {
  return typeof e.type != "function" ? !1 : !!(e._debugSource || d(e));
}
export {
  l as a,
  p as b,
  b as c,
  u as d,
  a as e,
  F as f,
  m as g,
  _ as i
};
