import { b as t, c, w as a } from "./copilot-BcASoA3D.js";
const e = window.Vaadin.copilot._machineState;
if (!e)
  throw new Error("Trying to use stored machine state before it was initialized");
const f = 5e3;
let n = 1;
function d(i) {
  t.notifications.includes(i) && (i.dontShowAgain && i.dismissId && u(i.dismissId, i.dismissTarget ?? "browser"), t.removeNotification(i), c.emit("notification-dismissed", i));
}
function r(i) {
  return a.dismissedNotifications.includes(i) ? !0 : e.getDismissedNotifications().includes(i);
}
function u(i, s) {
  r(i) || (s === "machine" ? e.addDismissedNotification(i) : a.addDismissedNotification(i));
}
function m(i) {
  return !(i.dismissId && (r(i.dismissId) || t.notifications.find((s) => s.dismissId === i.dismissId)));
}
function l(i) {
  m(i) && w(i);
}
function w(i) {
  const s = n;
  n += 1;
  const o = { ...i, id: s, dontShowAgain: !1, animatingOut: !1 };
  t.setNotifications([...t.notifications, o]), !i.link && !i.dismissId && setTimeout(() => {
    d(o);
  }, f);
}
const h = /* @__PURE__ */ Object.freeze(/* @__PURE__ */ Object.defineProperty({
  __proto__: null,
  dismissNotification: d,
  showNotification: l
}, Symbol.toStringTag, { value: "Module" }));
export {
  l as a,
  h as c,
  d,
  e as s
};
