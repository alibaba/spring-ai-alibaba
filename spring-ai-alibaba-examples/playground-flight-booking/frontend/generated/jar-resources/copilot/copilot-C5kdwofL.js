class io extends EventTarget {
  constructor() {
    super(...arguments), this.eventBuffer = [], this.handledTypes = [];
  }
  on(t, r) {
    const n = r;
    return this.addEventListener(t, n), this.handledTypes.push(t), this.flush(t), () => this.off(t, n);
  }
  once(t, r) {
    this.addEventListener(t, r, { once: !0 });
  }
  off(t, r) {
    this.removeEventListener(t, r);
    const n = this.handledTypes.indexOf(t, 0);
    n > -1 && this.handledTypes.splice(n, 1);
  }
  emit(t, r) {
    const n = new CustomEvent(t, { detail: r, cancelable: !0 });
    return this.handledTypes.includes(t) || this.eventBuffer.push(n), this.dispatchEvent(n), n.defaultPrevented;
  }
  emitUnsafe({ type: t, data: r }) {
    return this.emit(t, r);
  }
  // Communication with server via eventbus
  send(t, r) {
    const n = new CustomEvent("copilot-send", { detail: { command: t, data: r } });
    this.dispatchEvent(n);
  }
  // Listeners for Copilot itself
  onSend(t) {
    this.on("copilot-send", t);
  }
  offSend(t) {
    this.off("copilot-send", t);
  }
  flush(t) {
    const r = [];
    this.eventBuffer.filter((n) => n.type === t).forEach((n) => {
      this.dispatchEvent(n), r.push(n);
    }), this.eventBuffer = this.eventBuffer.filter((n) => !r.includes(n));
  }
}
var oo = {
  0: "Invalid value for configuration 'enforceActions', expected 'never', 'always' or 'observed'",
  1: function(t, r) {
    return "Cannot apply '" + t + "' to '" + r.toString() + "': Field not found.";
  },
  /*
  2(prop) {
      return `invalid decorator for '${prop.toString()}'`
  },
  3(prop) {
      return `Cannot decorate '${prop.toString()}': action can only be used on properties with a function value.`
  },
  4(prop) {
      return `Cannot decorate '${prop.toString()}': computed can only be used on getter properties.`
  },
  */
  5: "'keys()' can only be used on observable objects, arrays, sets and maps",
  6: "'values()' can only be used on observable objects, arrays, sets and maps",
  7: "'entries()' can only be used on observable objects, arrays and maps",
  8: "'set()' can only be used on observable objects, arrays and maps",
  9: "'remove()' can only be used on observable objects, arrays and maps",
  10: "'has()' can only be used on observable objects, arrays and maps",
  11: "'get()' can only be used on observable objects, arrays and maps",
  12: "Invalid annotation",
  13: "Dynamic observable objects cannot be frozen. If you're passing observables to 3rd party component/function that calls Object.freeze, pass copy instead: toJS(observable)",
  14: "Intercept handlers should return nothing or a change object",
  15: "Observable arrays cannot be frozen. If you're passing observables to 3rd party component/function that calls Object.freeze, pass copy instead: toJS(observable)",
  16: "Modification exception: the internal structure of an observable array was changed.",
  17: function(t, r) {
    return "[mobx.array] Index out of bounds, " + t + " is larger than " + r;
  },
  18: "mobx.map requires Map polyfill for the current browser. Check babel-polyfill or core-js/es6/map.js",
  19: function(t) {
    return "Cannot initialize from classes that inherit from Map: " + t.constructor.name;
  },
  20: function(t) {
    return "Cannot initialize map from " + t;
  },
  21: function(t) {
    return "Cannot convert to map from '" + t + "'";
  },
  22: "mobx.set requires Set polyfill for the current browser. Check babel-polyfill or core-js/es6/set.js",
  23: "It is not possible to get index atoms from arrays",
  24: function(t) {
    return "Cannot obtain administration from " + t;
  },
  25: function(t, r) {
    return "the entry '" + t + "' does not exist in the observable map '" + r + "'";
  },
  26: "please specify a property",
  27: function(t, r) {
    return "no observable property '" + t.toString() + "' found on the observable object '" + r + "'";
  },
  28: function(t) {
    return "Cannot obtain atom from " + t;
  },
  29: "Expecting some object",
  30: "invalid action stack. did you forget to finish an action?",
  31: "missing option for computed: get",
  32: function(t, r) {
    return "Cycle detected in computation " + t + ": " + r;
  },
  33: function(t) {
    return "The setter of computed value '" + t + "' is trying to update itself. Did you intend to update an _observable_ value, instead of the computed property?";
  },
  34: function(t) {
    return "[ComputedValue '" + t + "'] It is not possible to assign a new value to a computed value.";
  },
  35: "There are multiple, different versions of MobX active. Make sure MobX is loaded only once or use `configure({ isolateGlobalState: true })`",
  36: "isolateGlobalState should be called before MobX is running any reactions",
  37: function(t) {
    return "[mobx] `observableArray." + t + "()` mutates the array in-place, which is not allowed inside a derivation. Use `array.slice()." + t + "()` instead";
  },
  38: "'ownKeys()' can only be used on observable objects",
  39: "'defineProperty()' can only be used on observable objects"
}, ao = process.env.NODE_ENV !== "production" ? oo : {};
function v(e) {
  for (var t = arguments.length, r = new Array(t > 1 ? t - 1 : 0), n = 1; n < t; n++)
    r[n - 1] = arguments[n];
  if (process.env.NODE_ENV !== "production") {
    var i = typeof e == "string" ? e : ao[e];
    throw typeof i == "function" && (i = i.apply(null, r)), new Error("[MobX] " + i);
  }
  throw new Error(typeof e == "number" ? "[MobX] minified error nr: " + e + (r.length ? " " + r.map(String).join(",") : "") + ". Find the full error at: https://github.com/mobxjs/mobx/blob/main/packages/mobx/src/errors.ts" : "[MobX] " + e);
}
var so = {};
function Nn() {
  return typeof globalThis < "u" ? globalThis : typeof window < "u" ? window : typeof global < "u" ? global : typeof self < "u" ? self : so;
}
var xn = Object.assign, Pt = Object.getOwnPropertyDescriptor, G = Object.defineProperty, zt = Object.prototype, Ct = [];
Object.freeze(Ct);
var Er = {};
Object.freeze(Er);
var lo = typeof Proxy < "u", co = /* @__PURE__ */ Object.toString();
function $n() {
  lo || v(process.env.NODE_ENV !== "production" ? "`Proxy` objects are not available in the current environment. Please configure MobX to enable a fallback implementation.`" : "Proxy not available");
}
function Be(e) {
  process.env.NODE_ENV !== "production" && h.verifyProxies && v("MobX is currently configured to be able to run in ES5 mode, but in ES5 MobX won't be able to " + e);
}
function I() {
  return ++h.mobxGuid;
}
function Or(e) {
  var t = !1;
  return function() {
    if (!t)
      return t = !0, e.apply(this, arguments);
  };
}
var Ce = function() {
};
function A(e) {
  return typeof e == "function";
}
function _e(e) {
  var t = typeof e;
  switch (t) {
    case "string":
    case "symbol":
    case "number":
      return !0;
  }
  return !1;
}
function Bt(e) {
  return e !== null && typeof e == "object";
}
function D(e) {
  if (!Bt(e))
    return !1;
  var t = Object.getPrototypeOf(e);
  if (t == null)
    return !0;
  var r = Object.hasOwnProperty.call(t, "constructor") && t.constructor;
  return typeof r == "function" && r.toString() === co;
}
function Dn(e) {
  var t = e?.constructor;
  return t ? t.name === "GeneratorFunction" || t.displayName === "GeneratorFunction" : !1;
}
function Kt(e, t, r) {
  G(e, t, {
    enumerable: !1,
    writable: !0,
    configurable: !0,
    value: r
  });
}
function Pn(e, t, r) {
  G(e, t, {
    enumerable: !1,
    writable: !1,
    configurable: !0,
    value: r
  });
}
function Se(e, t) {
  var r = "isMobX" + e;
  return t.prototype[r] = !0, function(n) {
    return Bt(n) && n[r] === !0;
  };
}
function Le(e) {
  return e != null && Object.prototype.toString.call(e) === "[object Map]";
}
function uo(e) {
  var t = Object.getPrototypeOf(e), r = Object.getPrototypeOf(t), n = Object.getPrototypeOf(r);
  return n === null;
}
function st(e) {
  return e != null && Object.prototype.toString.call(e) === "[object Set]";
}
var Cn = typeof Object.getOwnPropertySymbols < "u";
function ho(e) {
  var t = Object.keys(e);
  if (!Cn)
    return t;
  var r = Object.getOwnPropertySymbols(e);
  return r.length ? [].concat(t, r.filter(function(n) {
    return zt.propertyIsEnumerable.call(e, n);
  })) : t;
}
var Ze = typeof Reflect < "u" && Reflect.ownKeys ? Reflect.ownKeys : Cn ? function(e) {
  return Object.getOwnPropertyNames(e).concat(Object.getOwnPropertySymbols(e));
} : (
  /* istanbul ignore next */
  Object.getOwnPropertyNames
);
function ur(e) {
  return typeof e == "string" ? e : typeof e == "symbol" ? e.toString() : new String(e).toString();
}
function Tn(e) {
  return e === null ? null : typeof e == "object" ? "" + e : e;
}
function z(e, t) {
  return zt.hasOwnProperty.call(e, t);
}
var vo = Object.getOwnPropertyDescriptors || function(t) {
  var r = {};
  return Ze(t).forEach(function(n) {
    r[n] = Pt(t, n);
  }), r;
};
function Br(e, t) {
  for (var r = 0; r < t.length; r++) {
    var n = t[r];
    n.enumerable = n.enumerable || !1, n.configurable = !0, "value" in n && (n.writable = !0), Object.defineProperty(e, go(n.key), n);
  }
}
function Ht(e, t, r) {
  return t && Br(e.prototype, t), r && Br(e, r), Object.defineProperty(e, "prototype", {
    writable: !1
  }), e;
}
function ae() {
  return ae = Object.assign ? Object.assign.bind() : function(e) {
    for (var t = 1; t < arguments.length; t++) {
      var r = arguments[t];
      for (var n in r)
        Object.prototype.hasOwnProperty.call(r, n) && (e[n] = r[n]);
    }
    return e;
  }, ae.apply(this, arguments);
}
function Vn(e, t) {
  e.prototype = Object.create(t.prototype), e.prototype.constructor = e, dr(e, t);
}
function dr(e, t) {
  return dr = Object.setPrototypeOf ? Object.setPrototypeOf.bind() : function(n, i) {
    return n.__proto__ = i, n;
  }, dr(e, t);
}
function St(e) {
  if (e === void 0)
    throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
  return e;
}
function fo(e, t) {
  if (e) {
    if (typeof e == "string") return Kr(e, t);
    var r = Object.prototype.toString.call(e).slice(8, -1);
    if (r === "Object" && e.constructor && (r = e.constructor.name), r === "Map" || r === "Set") return Array.from(e);
    if (r === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(r)) return Kr(e, t);
  }
}
function Kr(e, t) {
  (t == null || t > e.length) && (t = e.length);
  for (var r = 0, n = new Array(t); r < t; r++) n[r] = e[r];
  return n;
}
function Te(e, t) {
  var r = typeof Symbol < "u" && e[Symbol.iterator] || e["@@iterator"];
  if (r) return (r = r.call(e)).next.bind(r);
  if (Array.isArray(e) || (r = fo(e)) || t && e && typeof e.length == "number") {
    r && (e = r);
    var n = 0;
    return function() {
      return n >= e.length ? {
        done: !0
      } : {
        done: !1,
        value: e[n++]
      };
    };
  }
  throw new TypeError(`Invalid attempt to iterate non-iterable instance.
In order to be iterable, non-array objects must have a [Symbol.iterator]() method.`);
}
function po(e, t) {
  if (typeof e != "object" || e === null) return e;
  var r = e[Symbol.toPrimitive];
  if (r !== void 0) {
    var n = r.call(e, t || "default");
    if (typeof n != "object") return n;
    throw new TypeError("@@toPrimitive must return a primitive value.");
  }
  return (t === "string" ? String : Number)(e);
}
function go(e) {
  var t = po(e, "string");
  return typeof t == "symbol" ? t : String(t);
}
var J = /* @__PURE__ */ Symbol("mobx-stored-annotations");
function X(e) {
  function t(r, n) {
    if (lt(n))
      return e.decorate_20223_(r, n);
    Me(r, n, e);
  }
  return Object.assign(t, e);
}
function Me(e, t, r) {
  if (z(e, J) || Kt(e, J, ae({}, e[J])), process.env.NODE_ENV !== "production" && Tt(r) && !z(e[J], t)) {
    var n = e.constructor.name + ".prototype." + t.toString();
    v("'" + n + "' is decorated with 'override', but no such decorated member was found on prototype.");
  }
  bo(e, r, t), Tt(r) || (e[J][t] = r);
}
function bo(e, t, r) {
  if (process.env.NODE_ENV !== "production" && !Tt(t) && z(e[J], r)) {
    var n = e.constructor.name + ".prototype." + r.toString(), i = e[J][r].annotationType_, o = t.annotationType_;
    v("Cannot apply '@" + o + "' to '" + n + "':" + (`
The field is already decorated with '@` + i + "'.") + `
Re-decorating fields is not allowed.
Use '@override' decorator for methods overridden by subclass.`);
  }
}
function lt(e) {
  return typeof e == "object" && typeof e.kind == "string";
}
function Ft(e, t) {
  process.env.NODE_ENV !== "production" && !t.includes(e.kind) && v("The decorator applied to '" + String(e.name) + "' cannot be used on a " + e.kind + " element");
}
var g = /* @__PURE__ */ Symbol("mobx administration"), ct = /* @__PURE__ */ function() {
  function e(r) {
    r === void 0 && (r = process.env.NODE_ENV !== "production" ? "Atom@" + I() : "Atom"), this.name_ = void 0, this.isPendingUnobservation = !1, this.isBeingObserved = !1, this.observers_ = /* @__PURE__ */ new Set(), this.diffValue_ = 0, this.lastAccessedBy_ = 0, this.lowestObserverState_ = _.NOT_TRACKING_, this.onBOL = void 0, this.onBUOL = void 0, this.name_ = r;
  }
  var t = e.prototype;
  return t.onBO = function() {
    this.onBOL && this.onBOL.forEach(function(n) {
      return n();
    });
  }, t.onBUO = function() {
    this.onBUOL && this.onBUOL.forEach(function(n) {
      return n();
    });
  }, t.reportObserved = function() {
    return Zn(this);
  }, t.reportChanged = function() {
    k(), Yn(this), L();
  }, t.toString = function() {
    return this.name_;
  }, e;
}(), Sr = /* @__PURE__ */ Se("Atom", ct);
function jn(e, t, r) {
  t === void 0 && (t = Ce), r === void 0 && (r = Ce);
  var n = new ct(e);
  return t !== Ce && $a(n, t), r !== Ce && li(n, r), n;
}
function _o(e, t) {
  return e === t;
}
function mo(e, t) {
  return Tr(e, t);
}
function yo(e, t) {
  return Tr(e, t, 1);
}
function wo(e, t) {
  return Object.is ? Object.is(e, t) : e === t ? e !== 0 || 1 / e === 1 / t : e !== e && t !== t;
}
var Ve = {
  identity: _o,
  structural: mo,
  default: wo,
  shallow: yo
};
function me(e, t, r) {
  return tt(e) ? e : Array.isArray(e) ? O.array(e, {
    name: r
  }) : D(e) ? O.object(e, void 0, {
    name: r
  }) : Le(e) ? O.map(e, {
    name: r
  }) : st(e) ? O.set(e, {
    name: r
  }) : typeof e == "function" && !dt(e) && !et(e) ? Dn(e) ? je(e) : Qe(r, e) : e;
}
function Ao(e, t, r) {
  if (e == null || Ae(e) || ft(e) || ee(e) || Ie(e))
    return e;
  if (Array.isArray(e))
    return O.array(e, {
      name: r,
      deep: !1
    });
  if (D(e))
    return O.object(e, void 0, {
      name: r,
      deep: !1
    });
  if (Le(e))
    return O.map(e, {
      name: r,
      deep: !1
    });
  if (st(e))
    return O.set(e, {
      name: r,
      deep: !1
    });
  process.env.NODE_ENV !== "production" && v("The shallow modifier / decorator can only used in combination with arrays, objects, maps and sets");
}
function qt(e) {
  return e;
}
function Eo(e, t) {
  return process.env.NODE_ENV !== "production" && tt(e) && v("observable.struct should not be used with observable values"), Tr(e, t) ? t : e;
}
var Oo = "override";
function Tt(e) {
  return e.annotationType_ === Oo;
}
function ut(e, t) {
  return {
    annotationType_: e,
    options_: t,
    make_: So,
    extend_: No,
    decorate_20223_: xo
  };
}
function So(e, t, r, n) {
  var i;
  if ((i = this.options_) != null && i.bound)
    return this.extend_(e, t, r, !1) === null ? 0 : 1;
  if (n === e.target_)
    return this.extend_(e, t, r, !1) === null ? 0 : 2;
  if (dt(r.value))
    return 1;
  var o = Rn(e, this, t, r, !1);
  return G(n, t, o), 2;
}
function No(e, t, r, n) {
  var i = Rn(e, this, t, r);
  return e.defineProperty_(t, i, n);
}
function xo(e, t) {
  process.env.NODE_ENV !== "production" && Ft(t, ["method", "field"]);
  var r = t.kind, n = t.name, i = t.addInitializer, o = this, a = function(c) {
    var d, u, f, p;
    return ye((d = (u = o.options_) == null ? void 0 : u.name) != null ? d : n.toString(), c, (f = (p = o.options_) == null ? void 0 : p.autoAction) != null ? f : !1);
  };
  if (r == "field") {
    i(function() {
      Me(this, n, o);
    });
    return;
  }
  if (r == "method") {
    var l;
    return dt(e) || (e = a(e)), (l = this.options_) != null && l.bound && i(function() {
      var s = this, c = s[n].bind(s);
      c.isMobxAction = !0, s[n] = c;
    }), e;
  }
  v("Cannot apply '" + o.annotationType_ + "' to '" + String(n) + "' (kind: " + r + "):" + (`
'` + o.annotationType_ + "' can only be used on properties with a function value."));
}
function $o(e, t, r, n) {
  var i = t.annotationType_, o = n.value;
  process.env.NODE_ENV !== "production" && !A(o) && v("Cannot apply '" + i + "' to '" + e.name_ + "." + r.toString() + "':" + (`
'` + i + "' can only be used on properties with a function value."));
}
function Rn(e, t, r, n, i) {
  var o, a, l, s, c, d, u;
  i === void 0 && (i = h.safeDescriptors), $o(e, t, r, n);
  var f = n.value;
  if ((o = t.options_) != null && o.bound) {
    var p;
    f = f.bind((p = e.proxy_) != null ? p : e.target_);
  }
  return {
    value: ye(
      (a = (l = t.options_) == null ? void 0 : l.name) != null ? a : r.toString(),
      f,
      (s = (c = t.options_) == null ? void 0 : c.autoAction) != null ? s : !1,
      // https://github.com/mobxjs/mobx/discussions/3140
      (d = t.options_) != null && d.bound ? (u = e.proxy_) != null ? u : e.target_ : void 0
    ),
    // Non-configurable for classes
    // prevents accidental field redefinition in subclass
    configurable: i ? e.isPlainObject_ : !0,
    // https://github.com/mobxjs/mobx/pull/2641#issuecomment-737292058
    enumerable: !1,
    // Non-obsevable, therefore non-writable
    // Also prevents rewriting in subclass constructor
    writable: !i
  };
}
function kn(e, t) {
  return {
    annotationType_: e,
    options_: t,
    make_: Do,
    extend_: Po,
    decorate_20223_: Co
  };
}
function Do(e, t, r, n) {
  var i;
  if (n === e.target_)
    return this.extend_(e, t, r, !1) === null ? 0 : 2;
  if ((i = this.options_) != null && i.bound && (!z(e.target_, t) || !et(e.target_[t])) && this.extend_(e, t, r, !1) === null)
    return 0;
  if (et(r.value))
    return 1;
  var o = Ln(e, this, t, r, !1, !1);
  return G(n, t, o), 2;
}
function Po(e, t, r, n) {
  var i, o = Ln(e, this, t, r, (i = this.options_) == null ? void 0 : i.bound);
  return e.defineProperty_(t, o, n);
}
function Co(e, t) {
  var r;
  process.env.NODE_ENV !== "production" && Ft(t, ["method"]);
  var n = t.name, i = t.addInitializer;
  return et(e) || (e = je(e)), (r = this.options_) != null && r.bound && i(function() {
    var o = this, a = o[n].bind(o);
    a.isMobXFlow = !0, o[n] = a;
  }), e;
}
function To(e, t, r, n) {
  var i = t.annotationType_, o = n.value;
  process.env.NODE_ENV !== "production" && !A(o) && v("Cannot apply '" + i + "' to '" + e.name_ + "." + r.toString() + "':" + (`
'` + i + "' can only be used on properties with a generator function value."));
}
function Ln(e, t, r, n, i, o) {
  o === void 0 && (o = h.safeDescriptors), To(e, t, r, n);
  var a = n.value;
  if (et(a) || (a = je(a)), i) {
    var l;
    a = a.bind((l = e.proxy_) != null ? l : e.target_), a.isMobXFlow = !0;
  }
  return {
    value: a,
    // Non-configurable for classes
    // prevents accidental field redefinition in subclass
    configurable: o ? e.isPlainObject_ : !0,
    // https://github.com/mobxjs/mobx/pull/2641#issuecomment-737292058
    enumerable: !1,
    // Non-obsevable, therefore non-writable
    // Also prevents rewriting in subclass constructor
    writable: !o
  };
}
function Nr(e, t) {
  return {
    annotationType_: e,
    options_: t,
    make_: Vo,
    extend_: jo,
    decorate_20223_: Ro
  };
}
function Vo(e, t, r) {
  return this.extend_(e, t, r, !1) === null ? 0 : 1;
}
function jo(e, t, r, n) {
  return ko(e, this, t, r), e.defineComputedProperty_(t, ae({}, this.options_, {
    get: r.get,
    set: r.set
  }), n);
}
function Ro(e, t) {
  process.env.NODE_ENV !== "production" && Ft(t, ["getter"]);
  var r = this, n = t.name, i = t.addInitializer;
  return i(function() {
    var o = Ue(this)[g], a = ae({}, r.options_, {
      get: e,
      context: this
    });
    a.name || (a.name = process.env.NODE_ENV !== "production" ? o.name_ + "." + n.toString() : "ObservableObject." + n.toString()), o.values_.set(n, new H(a));
  }), function() {
    return this[g].getObservablePropValue_(n);
  };
}
function ko(e, t, r, n) {
  var i = t.annotationType_, o = n.get;
  process.env.NODE_ENV !== "production" && !o && v("Cannot apply '" + i + "' to '" + e.name_ + "." + r.toString() + "':" + (`
'` + i + "' can only be used on getter(+setter) properties."));
}
function Wt(e, t) {
  return {
    annotationType_: e,
    options_: t,
    make_: Lo,
    extend_: Mo,
    decorate_20223_: Io
  };
}
function Lo(e, t, r) {
  return this.extend_(e, t, r, !1) === null ? 0 : 1;
}
function Mo(e, t, r, n) {
  var i, o;
  return Uo(e, this, t, r), e.defineObservableProperty_(t, r.value, (i = (o = this.options_) == null ? void 0 : o.enhancer) != null ? i : me, n);
}
function Io(e, t) {
  if (process.env.NODE_ENV !== "production") {
    if (t.kind === "field")
      throw v("Please use `@observable accessor " + String(t.name) + "` instead of `@observable " + String(t.name) + "`");
    Ft(t, ["accessor"]);
  }
  var r = this, n = t.kind, i = t.name, o = /* @__PURE__ */ new WeakSet();
  function a(l, s) {
    var c, d, u = Ue(l)[g], f = new ge(s, (c = (d = r.options_) == null ? void 0 : d.enhancer) != null ? c : me, process.env.NODE_ENV !== "production" ? u.name_ + "." + i.toString() : "ObservableObject." + i.toString(), !1);
    u.values_.set(i, f), o.add(l);
  }
  if (n == "accessor")
    return {
      get: function() {
        return o.has(this) || a(this, e.get.call(this)), this[g].getObservablePropValue_(i);
      },
      set: function(s) {
        return o.has(this) || a(this, s), this[g].setObservablePropValue_(i, s);
      },
      init: function(s) {
        return o.has(this) || a(this, s), s;
      }
    };
}
function Uo(e, t, r, n) {
  var i = t.annotationType_;
  process.env.NODE_ENV !== "production" && !("value" in n) && v("Cannot apply '" + i + "' to '" + e.name_ + "." + r.toString() + "':" + (`
'` + i + "' cannot be used on getter/setter properties"));
}
var zo = "true", Bo = /* @__PURE__ */ Mn();
function Mn(e) {
  return {
    annotationType_: zo,
    options_: e,
    make_: Ko,
    extend_: Ho,
    decorate_20223_: Fo
  };
}
function Ko(e, t, r, n) {
  var i, o;
  if (r.get)
    return Gt.make_(e, t, r, n);
  if (r.set) {
    var a = ye(t.toString(), r.set);
    return n === e.target_ ? e.defineProperty_(t, {
      configurable: h.safeDescriptors ? e.isPlainObject_ : !0,
      set: a
    }) === null ? 0 : 2 : (G(n, t, {
      configurable: !0,
      set: a
    }), 2);
  }
  if (n !== e.target_ && typeof r.value == "function") {
    var l;
    if (Dn(r.value)) {
      var s, c = (s = this.options_) != null && s.autoBind ? je.bound : je;
      return c.make_(e, t, r, n);
    }
    var d = (l = this.options_) != null && l.autoBind ? Qe.bound : Qe;
    return d.make_(e, t, r, n);
  }
  var u = ((i = this.options_) == null ? void 0 : i.deep) === !1 ? O.ref : O;
  if (typeof r.value == "function" && (o = this.options_) != null && o.autoBind) {
    var f;
    r.value = r.value.bind((f = e.proxy_) != null ? f : e.target_);
  }
  return u.make_(e, t, r, n);
}
function Ho(e, t, r, n) {
  var i, o;
  if (r.get)
    return Gt.extend_(e, t, r, n);
  if (r.set)
    return e.defineProperty_(t, {
      configurable: h.safeDescriptors ? e.isPlainObject_ : !0,
      set: ye(t.toString(), r.set)
    }, n);
  if (typeof r.value == "function" && (i = this.options_) != null && i.autoBind) {
    var a;
    r.value = r.value.bind((a = e.proxy_) != null ? a : e.target_);
  }
  var l = ((o = this.options_) == null ? void 0 : o.deep) === !1 ? O.ref : O;
  return l.extend_(e, t, r, n);
}
function Fo(e, t) {
  v("'" + this.annotationType_ + "' cannot be used as a decorator");
}
var qo = "observable", Wo = "observable.ref", Go = "observable.shallow", Xo = "observable.struct", In = {
  deep: !0,
  name: void 0,
  defaultDecorator: void 0,
  proxy: !0
};
Object.freeze(In);
function gt(e) {
  return e || In;
}
var hr = /* @__PURE__ */ Wt(qo), Jo = /* @__PURE__ */ Wt(Wo, {
  enhancer: qt
}), Zo = /* @__PURE__ */ Wt(Go, {
  enhancer: Ao
}), Yo = /* @__PURE__ */ Wt(Xo, {
  enhancer: Eo
}), Un = /* @__PURE__ */ X(hr);
function bt(e) {
  return e.deep === !0 ? me : e.deep === !1 ? qt : ea(e.defaultDecorator);
}
function Qo(e) {
  var t;
  return e ? (t = e.defaultDecorator) != null ? t : Mn(e) : void 0;
}
function ea(e) {
  var t, r;
  return e && (t = (r = e.options_) == null ? void 0 : r.enhancer) != null ? t : me;
}
function zn(e, t, r) {
  if (lt(t))
    return hr.decorate_20223_(e, t);
  if (_e(t)) {
    Me(e, t, hr);
    return;
  }
  return tt(e) ? e : D(e) ? O.object(e, t, r) : Array.isArray(e) ? O.array(e, t) : Le(e) ? O.map(e, t) : st(e) ? O.set(e, t) : typeof e == "object" && e !== null ? e : O.box(e, t);
}
xn(zn, Un);
var ta = {
  box: function(t, r) {
    var n = gt(r);
    return new ge(t, bt(n), n.name, !0, n.equals);
  },
  array: function(t, r) {
    var n = gt(r);
    return (h.useProxies === !1 || n.proxy === !1 ? Ja : Ua)(t, bt(n), n.name);
  },
  map: function(t, r) {
    var n = gt(r);
    return new gi(t, bt(n), n.name);
  },
  set: function(t, r) {
    var n = gt(r);
    return new mi(t, bt(n), n.name);
  },
  object: function(t, r, n) {
    return xe(function() {
      return ui(h.useProxies === !1 || n?.proxy === !1 ? Ue({}, n) : La({}, n), t, r);
    });
  },
  ref: /* @__PURE__ */ X(Jo),
  shallow: /* @__PURE__ */ X(Zo),
  deep: Un,
  struct: /* @__PURE__ */ X(Yo)
}, O = /* @__PURE__ */ xn(zn, ta), Bn = "computed", ra = "computed.struct", vr = /* @__PURE__ */ Nr(Bn), na = /* @__PURE__ */ Nr(ra, {
  equals: Ve.structural
}), Gt = function(t, r) {
  if (lt(r))
    return vr.decorate_20223_(t, r);
  if (_e(r))
    return Me(t, r, vr);
  if (D(t))
    return X(Nr(Bn, t));
  process.env.NODE_ENV !== "production" && (A(t) || v("First argument to `computed` should be an expression."), A(r) && v("A setter as second argument is no longer supported, use `{ set: fn }` option instead"));
  var n = D(r) ? r : {};
  return n.get = t, n.name || (n.name = t.name || ""), new H(n);
};
Object.assign(Gt, vr);
Gt.struct = /* @__PURE__ */ X(na);
var Hr, Fr, Vt = 0, ia = 1, oa = (Hr = (Fr = /* @__PURE__ */ Pt(function() {
}, "name")) == null ? void 0 : Fr.configurable) != null ? Hr : !1, qr = {
  value: "action",
  configurable: !0,
  writable: !1,
  enumerable: !1
};
function ye(e, t, r, n) {
  r === void 0 && (r = !1), process.env.NODE_ENV !== "production" && (A(t) || v("`action` can only be invoked on functions"), (typeof e != "string" || !e) && v("actions should have valid names, got: '" + e + "'"));
  function i() {
    return Kn(e, r, t, n || this, arguments);
  }
  return i.isMobxAction = !0, i.toString = function() {
    return t.toString();
  }, oa && (qr.value = e, G(i, "name", qr)), i;
}
function Kn(e, t, r, n, i) {
  var o = aa(e, t, n, i);
  try {
    return r.apply(n, i);
  } catch (a) {
    throw o.error_ = a, a;
  } finally {
    sa(o);
  }
}
function aa(e, t, r, n) {
  var i = process.env.NODE_ENV !== "production" && x() && !!e, o = 0;
  if (process.env.NODE_ENV !== "production" && i) {
    o = Date.now();
    var a = n ? Array.from(n) : Ct;
    C({
      type: $r,
      name: e,
      object: r,
      arguments: a
    });
  }
  var l = h.trackingDerivation, s = !t || !l;
  k();
  var c = h.allowStateChanges;
  s && (Ne(), c = Xt(!0));
  var d = xr(!0), u = {
    runAsAction_: s,
    prevDerivation_: l,
    prevAllowStateChanges_: c,
    prevAllowStateReads_: d,
    notifySpy_: i,
    startTime_: o,
    actionId_: ia++,
    parentActionId_: Vt
  };
  return Vt = u.actionId_, u;
}
function sa(e) {
  Vt !== e.actionId_ && v(30), Vt = e.parentActionId_, e.error_ !== void 0 && (h.suppressReactionErrors = !0), Jt(e.prevAllowStateChanges_), Ge(e.prevAllowStateReads_), L(), e.runAsAction_ && Q(e.prevDerivation_), process.env.NODE_ENV !== "production" && e.notifySpy_ && T({
    time: Date.now() - e.startTime_
  }), h.suppressReactionErrors = !1;
}
function la(e, t) {
  var r = Xt(e);
  try {
    return t();
  } finally {
    Jt(r);
  }
}
function Xt(e) {
  var t = h.allowStateChanges;
  return h.allowStateChanges = e, t;
}
function Jt(e) {
  h.allowStateChanges = e;
}
var Hn, ca = "create";
Hn = Symbol.toPrimitive;
var ge = /* @__PURE__ */ function(e) {
  Vn(t, e);
  function t(n, i, o, a, l) {
    var s;
    return o === void 0 && (o = process.env.NODE_ENV !== "production" ? "ObservableValue@" + I() : "ObservableValue"), a === void 0 && (a = !0), l === void 0 && (l = Ve.default), s = e.call(this, o) || this, s.enhancer = void 0, s.name_ = void 0, s.equals = void 0, s.hasUnreportedChange_ = !1, s.interceptors_ = void 0, s.changeListeners_ = void 0, s.value_ = void 0, s.dehancer = void 0, s.enhancer = i, s.name_ = o, s.equals = l, s.value_ = i(n, void 0, o), process.env.NODE_ENV !== "production" && a && x() && we({
      type: ca,
      object: St(s),
      observableKind: "value",
      debugObjectName: s.name_,
      newValue: "" + s.value_
    }), s;
  }
  var r = t.prototype;
  return r.dehanceValue = function(i) {
    return this.dehancer !== void 0 ? this.dehancer(i) : i;
  }, r.set = function(i) {
    var o = this.value_;
    if (i = this.prepareNewValue_(i), i !== h.UNCHANGED) {
      var a = x();
      process.env.NODE_ENV !== "production" && a && C({
        type: U,
        object: this,
        observableKind: "value",
        debugObjectName: this.name_,
        newValue: i,
        oldValue: o
      }), this.setNewValue_(i), process.env.NODE_ENV !== "production" && a && T();
    }
  }, r.prepareNewValue_ = function(i) {
    if (W(this), j(this)) {
      var o = R(this, {
        object: this,
        type: U,
        newValue: i
      });
      if (!o)
        return h.UNCHANGED;
      i = o.newValue;
    }
    return i = this.enhancer(i, this.value_, this.name_), this.equals(this.value_, i) ? h.UNCHANGED : i;
  }, r.setNewValue_ = function(i) {
    var o = this.value_;
    this.value_ = i, this.reportChanged(), B(this) && K(this, {
      type: U,
      object: this,
      newValue: i,
      oldValue: o
    });
  }, r.get = function() {
    return this.reportObserved(), this.dehanceValue(this.value_);
  }, r.intercept_ = function(i) {
    return ht(this, i);
  }, r.observe_ = function(i, o) {
    return o && i({
      observableKind: "value",
      debugObjectName: this.name_,
      object: this,
      type: U,
      newValue: this.value_,
      oldValue: void 0
    }), vt(this, i);
  }, r.raw = function() {
    return this.value_;
  }, r.toJSON = function() {
    return this.get();
  }, r.toString = function() {
    return this.name_ + "[" + this.value_ + "]";
  }, r.valueOf = function() {
    return Tn(this.get());
  }, r[Hn] = function() {
    return this.valueOf();
  }, t;
}(ct), Fn;
function _t(e, t) {
  return !!(e & t);
}
function mt(e, t, r) {
  return r ? e |= t : e &= ~t, e;
}
Fn = Symbol.toPrimitive;
var H = /* @__PURE__ */ function() {
  function e(r) {
    this.dependenciesState_ = _.NOT_TRACKING_, this.observing_ = [], this.newObserving_ = null, this.observers_ = /* @__PURE__ */ new Set(), this.diffValue_ = 0, this.runId_ = 0, this.lastAccessedBy_ = 0, this.lowestObserverState_ = _.UP_TO_DATE_, this.unboundDepsCount_ = 0, this.value_ = new jt(null), this.name_ = void 0, this.triggeredBy_ = void 0, this.flags_ = 0, this.derivation = void 0, this.setter_ = void 0, this.isTracing_ = M.NONE, this.scope_ = void 0, this.equals_ = void 0, this.requiresReaction_ = void 0, this.keepAlive_ = void 0, this.onBOL = void 0, this.onBUOL = void 0, r.get || v(31), this.derivation = r.get, this.name_ = r.name || (process.env.NODE_ENV !== "production" ? "ComputedValue@" + I() : "ComputedValue"), r.set && (this.setter_ = ye(process.env.NODE_ENV !== "production" ? this.name_ + "-setter" : "ComputedValue-setter", r.set)), this.equals_ = r.equals || (r.compareStructural || r.struct ? Ve.structural : Ve.default), this.scope_ = r.context, this.requiresReaction_ = r.requiresReaction, this.keepAlive_ = !!r.keepAlive;
  }
  var t = e.prototype;
  return t.onBecomeStale_ = function() {
    pa(this);
  }, t.onBO = function() {
    this.onBOL && this.onBOL.forEach(function(n) {
      return n();
    });
  }, t.onBUO = function() {
    this.onBUOL && this.onBUOL.forEach(function(n) {
      return n();
    });
  }, t.get = function() {
    if (this.isComputing && v(32, this.name_, this.derivation), h.inBatch === 0 && // !globalState.trackingDerivatpion &&
    this.observers_.size === 0 && !this.keepAlive_)
      fr(this) && (this.warnAboutUntrackedRead_(), k(), this.value_ = this.computeValue_(!1), L());
    else if (Zn(this), fr(this)) {
      var n = h.trackingContext;
      this.keepAlive_ && !n && (h.trackingContext = this), this.trackAndCompute() && fa(this), h.trackingContext = n;
    }
    var i = this.value_;
    if (Nt(i))
      throw i.cause;
    return i;
  }, t.set = function(n) {
    if (this.setter_) {
      this.isRunningSetter && v(33, this.name_), this.isRunningSetter = !0;
      try {
        this.setter_.call(this.scope_, n);
      } finally {
        this.isRunningSetter = !1;
      }
    } else
      v(34, this.name_);
  }, t.trackAndCompute = function() {
    var n = this.value_, i = (
      /* see #1208 */
      this.dependenciesState_ === _.NOT_TRACKING_
    ), o = this.computeValue_(!0), a = i || Nt(n) || Nt(o) || !this.equals_(n, o);
    return a && (this.value_ = o, process.env.NODE_ENV !== "production" && x() && we({
      observableKind: "computed",
      debugObjectName: this.name_,
      object: this.scope_,
      type: "update",
      oldValue: n,
      newValue: o
    })), a;
  }, t.computeValue_ = function(n) {
    this.isComputing = !0;
    var i = Xt(!1), o;
    if (n)
      o = qn(this, this.derivation, this.scope_);
    else if (h.disableErrorBoundaries === !0)
      o = this.derivation.call(this.scope_);
    else
      try {
        o = this.derivation.call(this.scope_);
      } catch (a) {
        o = new jt(a);
      }
    return Jt(i), this.isComputing = !1, o;
  }, t.suspend_ = function() {
    this.keepAlive_ || (pr(this), this.value_ = void 0, process.env.NODE_ENV !== "production" && this.isTracing_ !== M.NONE && console.log("[mobx.trace] Computed value '" + this.name_ + "' was suspended and it will recompute on the next access."));
  }, t.observe_ = function(n, i) {
    var o = this, a = !0, l = void 0;
    return oi(function() {
      var s = o.get();
      if (!a || i) {
        var c = Ne();
        n({
          observableKind: "computed",
          debugObjectName: o.name_,
          type: U,
          object: o,
          newValue: s,
          oldValue: l
        }), Q(c);
      }
      a = !1, l = s;
    });
  }, t.warnAboutUntrackedRead_ = function() {
    process.env.NODE_ENV !== "production" && (this.isTracing_ !== M.NONE && console.log("[mobx.trace] Computed value '" + this.name_ + "' is being read outside a reactive context. Doing a full recompute."), (typeof this.requiresReaction_ == "boolean" ? this.requiresReaction_ : h.computedRequiresReaction) && console.warn("[mobx] Computed value '" + this.name_ + "' is being read outside a reactive context. Doing a full recompute."));
  }, t.toString = function() {
    return this.name_ + "[" + this.derivation.toString() + "]";
  }, t.valueOf = function() {
    return Tn(this.get());
  }, t[Fn] = function() {
    return this.valueOf();
  }, Ht(e, [{
    key: "isComputing",
    get: function() {
      return _t(this.flags_, e.isComputingMask_);
    },
    set: function(n) {
      this.flags_ = mt(this.flags_, e.isComputingMask_, n);
    }
  }, {
    key: "isRunningSetter",
    get: function() {
      return _t(this.flags_, e.isRunningSetterMask_);
    },
    set: function(n) {
      this.flags_ = mt(this.flags_, e.isRunningSetterMask_, n);
    }
  }, {
    key: "isBeingObserved",
    get: function() {
      return _t(this.flags_, e.isBeingObservedMask_);
    },
    set: function(n) {
      this.flags_ = mt(this.flags_, e.isBeingObservedMask_, n);
    }
  }, {
    key: "isPendingUnobservation",
    get: function() {
      return _t(this.flags_, e.isPendingUnobservationMask_);
    },
    set: function(n) {
      this.flags_ = mt(this.flags_, e.isPendingUnobservationMask_, n);
    }
  }]), e;
}();
H.isComputingMask_ = 1;
H.isRunningSetterMask_ = 2;
H.isBeingObservedMask_ = 4;
H.isPendingUnobservationMask_ = 8;
var Zt = /* @__PURE__ */ Se("ComputedValue", H), _;
(function(e) {
  e[e.NOT_TRACKING_ = -1] = "NOT_TRACKING_", e[e.UP_TO_DATE_ = 0] = "UP_TO_DATE_", e[e.POSSIBLY_STALE_ = 1] = "POSSIBLY_STALE_", e[e.STALE_ = 2] = "STALE_";
})(_ || (_ = {}));
var M;
(function(e) {
  e[e.NONE = 0] = "NONE", e[e.LOG = 1] = "LOG", e[e.BREAK = 2] = "BREAK";
})(M || (M = {}));
var jt = function(t) {
  this.cause = void 0, this.cause = t;
};
function Nt(e) {
  return e instanceof jt;
}
function fr(e) {
  switch (e.dependenciesState_) {
    case _.UP_TO_DATE_:
      return !1;
    case _.NOT_TRACKING_:
    case _.STALE_:
      return !0;
    case _.POSSIBLY_STALE_: {
      for (var t = xr(!0), r = Ne(), n = e.observing_, i = n.length, o = 0; o < i; o++) {
        var a = n[o];
        if (Zt(a)) {
          if (h.disableErrorBoundaries)
            a.get();
          else
            try {
              a.get();
            } catch {
              return Q(r), Ge(t), !0;
            }
          if (e.dependenciesState_ === _.STALE_)
            return Q(r), Ge(t), !0;
        }
      }
      return Gn(e), Q(r), Ge(t), !1;
    }
  }
}
function W(e) {
  if (process.env.NODE_ENV !== "production") {
    var t = e.observers_.size > 0;
    !h.allowStateChanges && (t || h.enforceActions === "always") && console.warn("[MobX] " + (h.enforceActions ? "Since strict-mode is enabled, changing (observed) observable values without using an action is not allowed. Tried to modify: " : "Side effects like changing state are not allowed at this point. Are you trying to modify state from, for example, a computed value or the render function of a React component? You can wrap side effects in 'runInAction' (or decorate functions with 'action') if needed. Tried to modify: ") + e.name_);
  }
}
function ua(e) {
  process.env.NODE_ENV !== "production" && !h.allowStateReads && h.observableRequiresReaction && console.warn("[mobx] Observable '" + e.name_ + "' being read outside a reactive context.");
}
function qn(e, t, r) {
  var n = xr(!0);
  Gn(e), e.newObserving_ = new Array(
    // Reserve constant space for initial dependencies, dynamic space otherwise.
    // See https://github.com/mobxjs/mobx/pull/3833
    e.runId_ === 0 ? 100 : e.observing_.length
  ), e.unboundDepsCount_ = 0, e.runId_ = ++h.runId;
  var i = h.trackingDerivation;
  h.trackingDerivation = e, h.inBatch++;
  var o;
  if (h.disableErrorBoundaries === !0)
    o = t.call(r);
  else
    try {
      o = t.call(r);
    } catch (a) {
      o = new jt(a);
    }
  return h.inBatch--, h.trackingDerivation = i, ha(e), da(e), Ge(n), o;
}
function da(e) {
  process.env.NODE_ENV !== "production" && e.observing_.length === 0 && (typeof e.requiresObservable_ == "boolean" ? e.requiresObservable_ : h.reactionRequiresObservable) && console.warn("[mobx] Derivation '" + e.name_ + "' is created/updated without reading any observable value.");
}
function ha(e) {
  for (var t = e.observing_, r = e.observing_ = e.newObserving_, n = _.UP_TO_DATE_, i = 0, o = e.unboundDepsCount_, a = 0; a < o; a++) {
    var l = r[a];
    l.diffValue_ === 0 && (l.diffValue_ = 1, i !== a && (r[i] = l), i++), l.dependenciesState_ > n && (n = l.dependenciesState_);
  }
  for (r.length = i, e.newObserving_ = null, o = t.length; o--; ) {
    var s = t[o];
    s.diffValue_ === 0 && Xn(s, e), s.diffValue_ = 0;
  }
  for (; i--; ) {
    var c = r[i];
    c.diffValue_ === 1 && (c.diffValue_ = 0, va(c, e));
  }
  n !== _.UP_TO_DATE_ && (e.dependenciesState_ = n, e.onBecomeStale_());
}
function pr(e) {
  var t = e.observing_;
  e.observing_ = [];
  for (var r = t.length; r--; )
    Xn(t[r], e);
  e.dependenciesState_ = _.NOT_TRACKING_;
}
function Wn(e) {
  var t = Ne();
  try {
    return e();
  } finally {
    Q(t);
  }
}
function Ne() {
  var e = h.trackingDerivation;
  return h.trackingDerivation = null, e;
}
function Q(e) {
  h.trackingDerivation = e;
}
function xr(e) {
  var t = h.allowStateReads;
  return h.allowStateReads = e, t;
}
function Ge(e) {
  h.allowStateReads = e;
}
function Gn(e) {
  if (e.dependenciesState_ !== _.UP_TO_DATE_) {
    e.dependenciesState_ = _.UP_TO_DATE_;
    for (var t = e.observing_, r = t.length; r--; )
      t[r].lowestObserverState_ = _.UP_TO_DATE_;
  }
}
var tr = function() {
  this.version = 6, this.UNCHANGED = {}, this.trackingDerivation = null, this.trackingContext = null, this.runId = 0, this.mobxGuid = 0, this.inBatch = 0, this.pendingUnobservations = [], this.pendingReactions = [], this.isRunningReactions = !1, this.allowStateChanges = !1, this.allowStateReads = !0, this.enforceActions = !0, this.spyListeners = [], this.globalReactionErrorHandlers = [], this.computedRequiresReaction = !1, this.reactionRequiresObservable = !1, this.observableRequiresReaction = !1, this.disableErrorBoundaries = !1, this.suppressReactionErrors = !1, this.useProxies = !0, this.verifyProxies = !1, this.safeDescriptors = !0;
}, rr = !0, h = /* @__PURE__ */ function() {
  var e = /* @__PURE__ */ Nn();
  return e.__mobxInstanceCount > 0 && !e.__mobxGlobals && (rr = !1), e.__mobxGlobals && e.__mobxGlobals.version !== new tr().version && (rr = !1), rr ? e.__mobxGlobals ? (e.__mobxInstanceCount += 1, e.__mobxGlobals.UNCHANGED || (e.__mobxGlobals.UNCHANGED = {}), e.__mobxGlobals) : (e.__mobxInstanceCount = 1, e.__mobxGlobals = /* @__PURE__ */ new tr()) : (setTimeout(function() {
    v(35);
  }, 1), new tr());
}();
function va(e, t) {
  e.observers_.add(t), e.lowestObserverState_ > t.dependenciesState_ && (e.lowestObserverState_ = t.dependenciesState_);
}
function Xn(e, t) {
  e.observers_.delete(t), e.observers_.size === 0 && Jn(e);
}
function Jn(e) {
  e.isPendingUnobservation === !1 && (e.isPendingUnobservation = !0, h.pendingUnobservations.push(e));
}
function k() {
  h.inBatch++;
}
function L() {
  if (--h.inBatch === 0) {
    ti();
    for (var e = h.pendingUnobservations, t = 0; t < e.length; t++) {
      var r = e[t];
      r.isPendingUnobservation = !1, r.observers_.size === 0 && (r.isBeingObserved && (r.isBeingObserved = !1, r.onBUO()), r instanceof H && r.suspend_());
    }
    h.pendingUnobservations = [];
  }
}
function Zn(e) {
  ua(e);
  var t = h.trackingDerivation;
  return t !== null ? (t.runId_ !== e.lastAccessedBy_ && (e.lastAccessedBy_ = t.runId_, t.newObserving_[t.unboundDepsCount_++] = e, !e.isBeingObserved && h.trackingContext && (e.isBeingObserved = !0, e.onBO())), e.isBeingObserved) : (e.observers_.size === 0 && h.inBatch > 0 && Jn(e), !1);
}
function Yn(e) {
  e.lowestObserverState_ !== _.STALE_ && (e.lowestObserverState_ = _.STALE_, e.observers_.forEach(function(t) {
    t.dependenciesState_ === _.UP_TO_DATE_ && (process.env.NODE_ENV !== "production" && t.isTracing_ !== M.NONE && Qn(t, e), t.onBecomeStale_()), t.dependenciesState_ = _.STALE_;
  }));
}
function fa(e) {
  e.lowestObserverState_ !== _.STALE_ && (e.lowestObserverState_ = _.STALE_, e.observers_.forEach(function(t) {
    t.dependenciesState_ === _.POSSIBLY_STALE_ ? (t.dependenciesState_ = _.STALE_, process.env.NODE_ENV !== "production" && t.isTracing_ !== M.NONE && Qn(t, e)) : t.dependenciesState_ === _.UP_TO_DATE_ && (e.lowestObserverState_ = _.UP_TO_DATE_);
  }));
}
function pa(e) {
  e.lowestObserverState_ === _.UP_TO_DATE_ && (e.lowestObserverState_ = _.POSSIBLY_STALE_, e.observers_.forEach(function(t) {
    t.dependenciesState_ === _.UP_TO_DATE_ && (t.dependenciesState_ = _.POSSIBLY_STALE_, t.onBecomeStale_());
  }));
}
function Qn(e, t) {
  if (console.log("[mobx.trace] '" + e.name_ + "' is invalidated due to a change in: '" + t.name_ + "'"), e.isTracing_ === M.BREAK) {
    var r = [];
    ei(Da(e), r, 1), new Function(`debugger;
/*
Tracing '` + e.name_ + `'

You are entering this break point because derivation '` + e.name_ + "' is being traced and '" + t.name_ + `' is now forcing it to update.
Just follow the stacktrace you should now see in the devtools to see precisely what piece of your code is causing this update
The stackframe you are looking for is at least ~6-8 stack-frames up.

` + (e instanceof H ? e.derivation.toString().replace(/[*]\//g, "/") : "") + `

The dependencies for this derivation are:

` + r.join(`
`) + `
*/
    `)();
  }
}
function ei(e, t, r) {
  if (t.length >= 1e3) {
    t.push("(and many more)");
    return;
  }
  t.push("" + "	".repeat(r - 1) + e.name), e.dependencies && e.dependencies.forEach(function(n) {
    return ei(n, t, r + 1);
  });
}
var Ye = /* @__PURE__ */ function() {
  function e(r, n, i, o) {
    r === void 0 && (r = process.env.NODE_ENV !== "production" ? "Reaction@" + I() : "Reaction"), this.name_ = void 0, this.onInvalidate_ = void 0, this.errorHandler_ = void 0, this.requiresObservable_ = void 0, this.observing_ = [], this.newObserving_ = [], this.dependenciesState_ = _.NOT_TRACKING_, this.diffValue_ = 0, this.runId_ = 0, this.unboundDepsCount_ = 0, this.isDisposed_ = !1, this.isScheduled_ = !1, this.isTrackPending_ = !1, this.isRunning_ = !1, this.isTracing_ = M.NONE, this.name_ = r, this.onInvalidate_ = n, this.errorHandler_ = i, this.requiresObservable_ = o;
  }
  var t = e.prototype;
  return t.onBecomeStale_ = function() {
    this.schedule_();
  }, t.schedule_ = function() {
    this.isScheduled_ || (this.isScheduled_ = !0, h.pendingReactions.push(this), ti());
  }, t.isScheduled = function() {
    return this.isScheduled_;
  }, t.runReaction_ = function() {
    if (!this.isDisposed_) {
      k(), this.isScheduled_ = !1;
      var n = h.trackingContext;
      if (h.trackingContext = this, fr(this)) {
        this.isTrackPending_ = !0;
        try {
          this.onInvalidate_(), process.env.NODE_ENV !== "production" && this.isTrackPending_ && x() && we({
            name: this.name_,
            type: "scheduled-reaction"
          });
        } catch (i) {
          this.reportExceptionInDerivation_(i);
        }
      }
      h.trackingContext = n, L();
    }
  }, t.track = function(n) {
    if (!this.isDisposed_) {
      k();
      var i = x(), o;
      process.env.NODE_ENV !== "production" && i && (o = Date.now(), C({
        name: this.name_,
        type: "reaction"
      })), this.isRunning_ = !0;
      var a = h.trackingContext;
      h.trackingContext = this;
      var l = qn(this, n, void 0);
      h.trackingContext = a, this.isRunning_ = !1, this.isTrackPending_ = !1, this.isDisposed_ && pr(this), Nt(l) && this.reportExceptionInDerivation_(l.cause), process.env.NODE_ENV !== "production" && i && T({
        time: Date.now() - o
      }), L();
    }
  }, t.reportExceptionInDerivation_ = function(n) {
    var i = this;
    if (this.errorHandler_) {
      this.errorHandler_(n, this);
      return;
    }
    if (h.disableErrorBoundaries)
      throw n;
    var o = process.env.NODE_ENV !== "production" ? "[mobx] Encountered an uncaught exception that was thrown by a reaction or observer component, in: '" + this + "'" : "[mobx] uncaught error in '" + this + "'";
    h.suppressReactionErrors ? process.env.NODE_ENV !== "production" && console.warn("[mobx] (error in reaction '" + this.name_ + "' suppressed, fix error of causing action below)") : console.error(o, n), process.env.NODE_ENV !== "production" && x() && we({
      type: "error",
      name: this.name_,
      message: o,
      error: "" + n
    }), h.globalReactionErrorHandlers.forEach(function(a) {
      return a(n, i);
    });
  }, t.dispose = function() {
    this.isDisposed_ || (this.isDisposed_ = !0, this.isRunning_ || (k(), pr(this), L()));
  }, t.getDisposer_ = function(n) {
    var i = this, o = function a() {
      i.dispose(), n == null || n.removeEventListener == null || n.removeEventListener("abort", a);
    };
    return n == null || n.addEventListener == null || n.addEventListener("abort", o), o[g] = this, o;
  }, t.toString = function() {
    return "Reaction[" + this.name_ + "]";
  }, t.trace = function(n) {
    n === void 0 && (n = !1), ja(this, n);
  }, e;
}(), Wr = 100, ga = function(t) {
  return t();
};
function ti() {
  h.inBatch > 0 || h.isRunningReactions || ga(ba);
}
function ba() {
  h.isRunningReactions = !0;
  for (var e = h.pendingReactions, t = 0; e.length > 0; ) {
    ++t === Wr && (console.error(process.env.NODE_ENV !== "production" ? "Reaction doesn't converge to a stable state after " + Wr + " iterations." + (" Probably there is a cycle in the reactive function: " + e[0]) : "[mobx] cycle in reaction: " + e[0]), e.splice(0));
    for (var r = e.splice(0), n = 0, i = r.length; n < i; n++)
      r[n].runReaction_();
  }
  h.isRunningReactions = !1;
}
var Rt = /* @__PURE__ */ Se("Reaction", Ye);
function x() {
  return process.env.NODE_ENV !== "production" && !!h.spyListeners.length;
}
function we(e) {
  if (process.env.NODE_ENV !== "production" && h.spyListeners.length)
    for (var t = h.spyListeners, r = 0, n = t.length; r < n; r++)
      t[r](e);
}
function C(e) {
  if (process.env.NODE_ENV !== "production") {
    var t = ae({}, e, {
      spyReportStart: !0
    });
    we(t);
  }
}
var _a = {
  type: "report-end",
  spyReportEnd: !0
};
function T(e) {
  process.env.NODE_ENV !== "production" && we(e ? ae({}, e, {
    type: "report-end",
    spyReportEnd: !0
  }) : _a);
}
function ma(e) {
  return process.env.NODE_ENV === "production" ? (console.warn("[mobx.spy] Is a no-op in production builds"), function() {
  }) : (h.spyListeners.push(e), Or(function() {
    h.spyListeners = h.spyListeners.filter(function(t) {
      return t !== e;
    });
  }));
}
var $r = "action", ya = "action.bound", ri = "autoAction", wa = "autoAction.bound", ni = "<unnamed action>", gr = /* @__PURE__ */ ut($r), Aa = /* @__PURE__ */ ut(ya, {
  bound: !0
}), br = /* @__PURE__ */ ut(ri, {
  autoAction: !0
}), Ea = /* @__PURE__ */ ut(wa, {
  autoAction: !0,
  bound: !0
});
function ii(e) {
  var t = function(n, i) {
    if (A(n))
      return ye(n.name || ni, n, e);
    if (A(i))
      return ye(n, i, e);
    if (lt(i))
      return (e ? br : gr).decorate_20223_(n, i);
    if (_e(i))
      return Me(n, i, e ? br : gr);
    if (_e(n))
      return X(ut(e ? ri : $r, {
        name: n,
        autoAction: e
      }));
    process.env.NODE_ENV !== "production" && v("Invalid arguments for `action`");
  };
  return t;
}
var fe = /* @__PURE__ */ ii(!1);
Object.assign(fe, gr);
var Qe = /* @__PURE__ */ ii(!0);
Object.assign(Qe, br);
fe.bound = /* @__PURE__ */ X(Aa);
Qe.bound = /* @__PURE__ */ X(Ea);
function wl(e) {
  return Kn(e.name || ni, !1, e, this, void 0);
}
function dt(e) {
  return A(e) && e.isMobxAction === !0;
}
function oi(e, t) {
  var r, n, i, o, a;
  t === void 0 && (t = Er), process.env.NODE_ENV !== "production" && (A(e) || v("Autorun expects a function as first argument"), dt(e) && v("Autorun does not accept actions since actions are untrackable"));
  var l = (r = (n = t) == null ? void 0 : n.name) != null ? r : process.env.NODE_ENV !== "production" ? e.name || "Autorun@" + I() : "Autorun", s = !t.scheduler && !t.delay, c;
  if (s)
    c = new Ye(l, function() {
      this.track(f);
    }, t.onError, t.requiresObservable);
  else {
    var d = ai(t), u = !1;
    c = new Ye(l, function() {
      u || (u = !0, d(function() {
        u = !1, c.isDisposed_ || c.track(f);
      }));
    }, t.onError, t.requiresObservable);
  }
  function f() {
    e(c);
  }
  return (i = t) != null && (o = i.signal) != null && o.aborted || c.schedule_(), c.getDisposer_((a = t) == null ? void 0 : a.signal);
}
var Oa = function(t) {
  return t();
};
function ai(e) {
  return e.scheduler ? e.scheduler : e.delay ? function(t) {
    return setTimeout(t, e.delay);
  } : Oa;
}
function si(e, t, r) {
  var n, i, o, a;
  r === void 0 && (r = Er), process.env.NODE_ENV !== "production" && ((!A(e) || !A(t)) && v("First and second argument to reaction should be functions"), D(r) || v("Third argument of reactions should be an object"));
  var l = (n = r.name) != null ? n : process.env.NODE_ENV !== "production" ? "Reaction@" + I() : "Reaction", s = fe(l, r.onError ? Sa(r.onError, t) : t), c = !r.scheduler && !r.delay, d = ai(r), u = !0, f = !1, p, y = r.compareStructural ? Ve.structural : r.equals || Ve.default, m = new Ye(l, function() {
    u || c ? S() : f || (f = !0, d(S));
  }, r.onError, r.requiresObservable);
  function S() {
    if (f = !1, !m.isDisposed_) {
      var q = !1, $e = p;
      m.track(function() {
        var te = la(!1, function() {
          return e(m);
        });
        q = u || !y(p, te), p = te;
      }), (u && r.fireImmediately || !u && q) && s(p, $e, m), u = !1;
    }
  }
  return (i = r) != null && (o = i.signal) != null && o.aborted || m.schedule_(), m.getDisposer_((a = r) == null ? void 0 : a.signal);
}
function Sa(e, t) {
  return function() {
    try {
      return t.apply(this, arguments);
    } catch (r) {
      e.call(this, r);
    }
  };
}
var Na = "onBO", xa = "onBUO";
function $a(e, t, r) {
  return ci(Na, e, t, r);
}
function li(e, t, r) {
  return ci(xa, e, t, r);
}
function ci(e, t, r, n) {
  var i = typeof n == "function" ? se(t, r) : se(t), o = A(n) ? n : r, a = e + "L";
  return i[a] ? i[a].add(o) : i[a] = /* @__PURE__ */ new Set([o]), function() {
    var l = i[a];
    l && (l.delete(o), l.size === 0 && delete i[a]);
  };
}
function ui(e, t, r, n) {
  process.env.NODE_ENV !== "production" && (arguments.length > 4 && v("'extendObservable' expected 2-4 arguments"), typeof e != "object" && v("'extendObservable' expects an object as first argument"), ee(e) && v("'extendObservable' should not be used on maps, use map.merge instead"), D(t) || v("'extendObservable' only accepts plain objects as second argument"), (tt(t) || tt(r)) && v("Extending an object with another observable (object) is not supported"));
  var i = vo(t);
  return xe(function() {
    var o = Ue(e, n)[g];
    Ze(i).forEach(function(a) {
      o.extend_(
        a,
        i[a],
        // must pass "undefined" for { key: undefined }
        r && a in r ? r[a] : !0
      );
    });
  }), e;
}
function Da(e, t) {
  return di(se(e, t));
}
function di(e) {
  var t = {
    name: e.name_
  };
  return e.observing_ && e.observing_.length > 0 && (t.dependencies = Pa(e.observing_).map(di)), t;
}
function Pa(e) {
  return Array.from(new Set(e));
}
var Ca = 0;
function hi() {
  this.message = "FLOW_CANCELLED";
}
hi.prototype = /* @__PURE__ */ Object.create(Error.prototype);
var nr = /* @__PURE__ */ kn("flow"), Ta = /* @__PURE__ */ kn("flow.bound", {
  bound: !0
}), je = /* @__PURE__ */ Object.assign(function(t, r) {
  if (lt(r))
    return nr.decorate_20223_(t, r);
  if (_e(r))
    return Me(t, r, nr);
  process.env.NODE_ENV !== "production" && arguments.length !== 1 && v("Flow expects single argument with generator function");
  var n = t, i = n.name || "<unnamed flow>", o = function() {
    var l = this, s = arguments, c = ++Ca, d = fe(i + " - runid: " + c + " - init", n).apply(l, s), u, f = void 0, p = new Promise(function(y, m) {
      var S = 0;
      u = m;
      function q(P) {
        f = void 0;
        var re;
        try {
          re = fe(i + " - runid: " + c + " - yield " + S++, d.next).call(d, P);
        } catch (ce) {
          return m(ce);
        }
        te(re);
      }
      function $e(P) {
        f = void 0;
        var re;
        try {
          re = fe(i + " - runid: " + c + " - yield " + S++, d.throw).call(d, P);
        } catch (ce) {
          return m(ce);
        }
        te(re);
      }
      function te(P) {
        if (A(P?.then)) {
          P.then(te, m);
          return;
        }
        return P.done ? y(P.value) : (f = Promise.resolve(P.value), f.then(q, $e));
      }
      q(void 0);
    });
    return p.cancel = fe(i + " - runid: " + c + " - cancel", function() {
      try {
        f && Gr(f);
        var y = d.return(void 0), m = Promise.resolve(y.value);
        m.then(Ce, Ce), Gr(m), u(new hi());
      } catch (S) {
        u(S);
      }
    }), p;
  };
  return o.isMobXFlow = !0, o;
}, nr);
je.bound = /* @__PURE__ */ X(Ta);
function Gr(e) {
  A(e.cancel) && e.cancel();
}
function et(e) {
  return e?.isMobXFlow === !0;
}
function Va(e, t) {
  return e ? t !== void 0 ? process.env.NODE_ENV !== "production" && (ee(e) || ft(e)) ? v("isObservable(object, propertyName) is not supported for arrays and maps. Use map.has or array.length instead.") : Ae(e) ? e[g].values_.has(t) : !1 : Ae(e) || !!e[g] || Sr(e) || Rt(e) || Zt(e) : !1;
}
function tt(e) {
  return process.env.NODE_ENV !== "production" && arguments.length !== 1 && v("isObservable expects only 1 argument. Use isObservableProp to inspect the observability of a property"), Va(e);
}
function ja() {
  if (process.env.NODE_ENV !== "production") {
    for (var e = !1, t = arguments.length, r = new Array(t), n = 0; n < t; n++)
      r[n] = arguments[n];
    typeof r[r.length - 1] == "boolean" && (e = r.pop());
    var i = Ra(r);
    if (!i)
      return v("'trace(break?)' can only be used inside a tracked computed value or a Reaction. Consider passing in the computed value or reaction explicitly");
    i.isTracing_ === M.NONE && console.log("[mobx.trace] '" + i.name_ + "' tracing enabled"), i.isTracing_ = e ? M.BREAK : M.LOG;
  }
}
function Ra(e) {
  switch (e.length) {
    case 0:
      return h.trackingDerivation;
    case 1:
      return se(e[0]);
    case 2:
      return se(e[0], e[1]);
  }
}
function Z(e, t) {
  t === void 0 && (t = void 0), k();
  try {
    return e.apply(t);
  } finally {
    L();
  }
}
function ue(e) {
  return e[g];
}
var ka = {
  has: function(t, r) {
    return process.env.NODE_ENV !== "production" && h.trackingDerivation && Be("detect new properties using the 'in' operator. Use 'has' from 'mobx' instead."), ue(t).has_(r);
  },
  get: function(t, r) {
    return ue(t).get_(r);
  },
  set: function(t, r, n) {
    var i;
    return _e(r) ? (process.env.NODE_ENV !== "production" && !ue(t).values_.has(r) && Be("add a new observable property through direct assignment. Use 'set' from 'mobx' instead."), (i = ue(t).set_(r, n, !0)) != null ? i : !0) : !1;
  },
  deleteProperty: function(t, r) {
    var n;
    return process.env.NODE_ENV !== "production" && Be("delete properties from an observable object. Use 'remove' from 'mobx' instead."), _e(r) ? (n = ue(t).delete_(r, !0)) != null ? n : !0 : !1;
  },
  defineProperty: function(t, r, n) {
    var i;
    return process.env.NODE_ENV !== "production" && Be("define property on an observable object. Use 'defineProperty' from 'mobx' instead."), (i = ue(t).defineProperty_(r, n)) != null ? i : !0;
  },
  ownKeys: function(t) {
    return process.env.NODE_ENV !== "production" && h.trackingDerivation && Be("iterate keys to detect added / removed properties. Use 'keys' from 'mobx' instead."), ue(t).ownKeys_();
  },
  preventExtensions: function(t) {
    v(13);
  }
};
function La(e, t) {
  var r, n;
  return $n(), e = Ue(e, t), (n = (r = e[g]).proxy_) != null ? n : r.proxy_ = new Proxy(e, ka);
}
function j(e) {
  return e.interceptors_ !== void 0 && e.interceptors_.length > 0;
}
function ht(e, t) {
  var r = e.interceptors_ || (e.interceptors_ = []);
  return r.push(t), Or(function() {
    var n = r.indexOf(t);
    n !== -1 && r.splice(n, 1);
  });
}
function R(e, t) {
  var r = Ne();
  try {
    for (var n = [].concat(e.interceptors_ || []), i = 0, o = n.length; i < o && (t = n[i](t), t && !t.type && v(14), !!t); i++)
      ;
    return t;
  } finally {
    Q(r);
  }
}
function B(e) {
  return e.changeListeners_ !== void 0 && e.changeListeners_.length > 0;
}
function vt(e, t) {
  var r = e.changeListeners_ || (e.changeListeners_ = []);
  return r.push(t), Or(function() {
    var n = r.indexOf(t);
    n !== -1 && r.splice(n, 1);
  });
}
function K(e, t) {
  var r = Ne(), n = e.changeListeners_;
  if (n) {
    n = n.slice();
    for (var i = 0, o = n.length; i < o; i++)
      n[i](t);
    Q(r);
  }
}
var ir = /* @__PURE__ */ Symbol("mobx-keys");
function Yt(e, t, r) {
  return process.env.NODE_ENV !== "production" && (!D(e) && !D(Object.getPrototypeOf(e)) && v("'makeAutoObservable' can only be used for classes that don't have a superclass"), Ae(e) && v("makeAutoObservable can only be used on objects not already made observable")), D(e) ? ui(e, e, t, r) : (xe(function() {
    var n = Ue(e, r)[g];
    if (!e[ir]) {
      var i = Object.getPrototypeOf(e), o = new Set([].concat(Ze(e), Ze(i)));
      o.delete("constructor"), o.delete(g), Kt(i, ir, o);
    }
    e[ir].forEach(function(a) {
      return n.make_(
        a,
        // must pass "undefined" for { key: undefined }
        t && a in t ? t[a] : !0
      );
    });
  }), e);
}
var Xr = "splice", U = "update", Ma = 1e4, Ia = {
  get: function(t, r) {
    var n = t[g];
    return r === g ? n : r === "length" ? n.getArrayLength_() : typeof r == "string" && !isNaN(r) ? n.get_(parseInt(r)) : z(kt, r) ? kt[r] : t[r];
  },
  set: function(t, r, n) {
    var i = t[g];
    return r === "length" && i.setArrayLength_(n), typeof r == "symbol" || isNaN(r) ? t[r] = n : i.set_(parseInt(r), n), !0;
  },
  preventExtensions: function() {
    v(15);
  }
}, Dr = /* @__PURE__ */ function() {
  function e(r, n, i, o) {
    r === void 0 && (r = process.env.NODE_ENV !== "production" ? "ObservableArray@" + I() : "ObservableArray"), this.owned_ = void 0, this.legacyMode_ = void 0, this.atom_ = void 0, this.values_ = [], this.interceptors_ = void 0, this.changeListeners_ = void 0, this.enhancer_ = void 0, this.dehancer = void 0, this.proxy_ = void 0, this.lastKnownLength_ = 0, this.owned_ = i, this.legacyMode_ = o, this.atom_ = new ct(r), this.enhancer_ = function(a, l) {
      return n(a, l, process.env.NODE_ENV !== "production" ? r + "[..]" : "ObservableArray[..]");
    };
  }
  var t = e.prototype;
  return t.dehanceValue_ = function(n) {
    return this.dehancer !== void 0 ? this.dehancer(n) : n;
  }, t.dehanceValues_ = function(n) {
    return this.dehancer !== void 0 && n.length > 0 ? n.map(this.dehancer) : n;
  }, t.intercept_ = function(n) {
    return ht(this, n);
  }, t.observe_ = function(n, i) {
    return i === void 0 && (i = !1), i && n({
      observableKind: "array",
      object: this.proxy_,
      debugObjectName: this.atom_.name_,
      type: "splice",
      index: 0,
      added: this.values_.slice(),
      addedCount: this.values_.length,
      removed: [],
      removedCount: 0
    }), vt(this, n);
  }, t.getArrayLength_ = function() {
    return this.atom_.reportObserved(), this.values_.length;
  }, t.setArrayLength_ = function(n) {
    (typeof n != "number" || isNaN(n) || n < 0) && v("Out of range: " + n);
    var i = this.values_.length;
    if (n !== i)
      if (n > i) {
        for (var o = new Array(n - i), a = 0; a < n - i; a++)
          o[a] = void 0;
        this.spliceWithArray_(i, 0, o);
      } else
        this.spliceWithArray_(n, i - n);
  }, t.updateArrayLength_ = function(n, i) {
    n !== this.lastKnownLength_ && v(16), this.lastKnownLength_ += i, this.legacyMode_ && i > 0 && Ai(n + i + 1);
  }, t.spliceWithArray_ = function(n, i, o) {
    var a = this;
    W(this.atom_);
    var l = this.values_.length;
    if (n === void 0 ? n = 0 : n > l ? n = l : n < 0 && (n = Math.max(0, l + n)), arguments.length === 1 ? i = l - n : i == null ? i = 0 : i = Math.max(0, Math.min(i, l - n)), o === void 0 && (o = Ct), j(this)) {
      var s = R(this, {
        object: this.proxy_,
        type: Xr,
        index: n,
        removedCount: i,
        added: o
      });
      if (!s)
        return Ct;
      i = s.removedCount, o = s.added;
    }
    if (o = o.length === 0 ? o : o.map(function(u) {
      return a.enhancer_(u, void 0);
    }), this.legacyMode_ || process.env.NODE_ENV !== "production") {
      var c = o.length - i;
      this.updateArrayLength_(l, c);
    }
    var d = this.spliceItemsIntoValues_(n, i, o);
    return (i !== 0 || o.length !== 0) && this.notifyArraySplice_(n, o, d), this.dehanceValues_(d);
  }, t.spliceItemsIntoValues_ = function(n, i, o) {
    if (o.length < Ma) {
      var a;
      return (a = this.values_).splice.apply(a, [n, i].concat(o));
    } else {
      var l = this.values_.slice(n, n + i), s = this.values_.slice(n + i);
      this.values_.length += o.length - i;
      for (var c = 0; c < o.length; c++)
        this.values_[n + c] = o[c];
      for (var d = 0; d < s.length; d++)
        this.values_[n + o.length + d] = s[d];
      return l;
    }
  }, t.notifyArrayChildUpdate_ = function(n, i, o) {
    var a = !this.owned_ && x(), l = B(this), s = l || a ? {
      observableKind: "array",
      object: this.proxy_,
      type: U,
      debugObjectName: this.atom_.name_,
      index: n,
      newValue: i,
      oldValue: o
    } : null;
    process.env.NODE_ENV !== "production" && a && C(s), this.atom_.reportChanged(), l && K(this, s), process.env.NODE_ENV !== "production" && a && T();
  }, t.notifyArraySplice_ = function(n, i, o) {
    var a = !this.owned_ && x(), l = B(this), s = l || a ? {
      observableKind: "array",
      object: this.proxy_,
      debugObjectName: this.atom_.name_,
      type: Xr,
      index: n,
      removed: o,
      added: i,
      removedCount: o.length,
      addedCount: i.length
    } : null;
    process.env.NODE_ENV !== "production" && a && C(s), this.atom_.reportChanged(), l && K(this, s), process.env.NODE_ENV !== "production" && a && T();
  }, t.get_ = function(n) {
    if (this.legacyMode_ && n >= this.values_.length) {
      console.warn(process.env.NODE_ENV !== "production" ? "[mobx.array] Attempt to read an array index (" + n + ") that is out of bounds (" + this.values_.length + "). Please check length first. Out of bound indices will not be tracked by MobX" : "[mobx] Out of bounds read: " + n);
      return;
    }
    return this.atom_.reportObserved(), this.dehanceValue_(this.values_[n]);
  }, t.set_ = function(n, i) {
    var o = this.values_;
    if (this.legacyMode_ && n > o.length && v(17, n, o.length), n < o.length) {
      W(this.atom_);
      var a = o[n];
      if (j(this)) {
        var l = R(this, {
          type: U,
          object: this.proxy_,
          index: n,
          newValue: i
        });
        if (!l)
          return;
        i = l.newValue;
      }
      i = this.enhancer_(i, a);
      var s = i !== a;
      s && (o[n] = i, this.notifyArrayChildUpdate_(n, i, a));
    } else {
      for (var c = new Array(n + 1 - o.length), d = 0; d < c.length - 1; d++)
        c[d] = void 0;
      c[c.length - 1] = i, this.spliceWithArray_(o.length, 0, c);
    }
  }, e;
}();
function Ua(e, t, r, n) {
  return r === void 0 && (r = process.env.NODE_ENV !== "production" ? "ObservableArray@" + I() : "ObservableArray"), n === void 0 && (n = !1), $n(), xe(function() {
    var i = new Dr(r, t, n, !1);
    Pn(i.values_, g, i);
    var o = new Proxy(i.values_, Ia);
    return i.proxy_ = o, e && e.length && i.spliceWithArray_(0, 0, e), o;
  });
}
var kt = {
  clear: function() {
    return this.splice(0);
  },
  replace: function(t) {
    var r = this[g];
    return r.spliceWithArray_(0, r.values_.length, t);
  },
  // Used by JSON.stringify
  toJSON: function() {
    return this.slice();
  },
  /*
   * functions that do alter the internal structure of the array, (based on lib.es6.d.ts)
   * since these functions alter the inner structure of the array, the have side effects.
   * Because the have side effects, they should not be used in computed function,
   * and for that reason the do not call dependencyState.notifyObserved
   */
  splice: function(t, r) {
    for (var n = arguments.length, i = new Array(n > 2 ? n - 2 : 0), o = 2; o < n; o++)
      i[o - 2] = arguments[o];
    var a = this[g];
    switch (arguments.length) {
      case 0:
        return [];
      case 1:
        return a.spliceWithArray_(t);
      case 2:
        return a.spliceWithArray_(t, r);
    }
    return a.spliceWithArray_(t, r, i);
  },
  spliceWithArray: function(t, r, n) {
    return this[g].spliceWithArray_(t, r, n);
  },
  push: function() {
    for (var t = this[g], r = arguments.length, n = new Array(r), i = 0; i < r; i++)
      n[i] = arguments[i];
    return t.spliceWithArray_(t.values_.length, 0, n), t.values_.length;
  },
  pop: function() {
    return this.splice(Math.max(this[g].values_.length - 1, 0), 1)[0];
  },
  shift: function() {
    return this.splice(0, 1)[0];
  },
  unshift: function() {
    for (var t = this[g], r = arguments.length, n = new Array(r), i = 0; i < r; i++)
      n[i] = arguments[i];
    return t.spliceWithArray_(0, 0, n), t.values_.length;
  },
  reverse: function() {
    return h.trackingDerivation && v(37, "reverse"), this.replace(this.slice().reverse()), this;
  },
  sort: function() {
    h.trackingDerivation && v(37, "sort");
    var t = this.slice();
    return t.sort.apply(t, arguments), this.replace(t), this;
  },
  remove: function(t) {
    var r = this[g], n = r.dehanceValues_(r.values_).indexOf(t);
    return n > -1 ? (this.splice(n, 1), !0) : !1;
  }
};
w("at", V);
w("concat", V);
w("flat", V);
w("includes", V);
w("indexOf", V);
w("join", V);
w("lastIndexOf", V);
w("slice", V);
w("toString", V);
w("toLocaleString", V);
w("toSorted", V);
w("toSpliced", V);
w("with", V);
w("every", F);
w("filter", F);
w("find", F);
w("findIndex", F);
w("findLast", F);
w("findLastIndex", F);
w("flatMap", F);
w("forEach", F);
w("map", F);
w("some", F);
w("toReversed", F);
w("reduce", vi);
w("reduceRight", vi);
function w(e, t) {
  typeof Array.prototype[e] == "function" && (kt[e] = t(e));
}
function V(e) {
  return function() {
    var t = this[g];
    t.atom_.reportObserved();
    var r = t.dehanceValues_(t.values_);
    return r[e].apply(r, arguments);
  };
}
function F(e) {
  return function(t, r) {
    var n = this, i = this[g];
    i.atom_.reportObserved();
    var o = i.dehanceValues_(i.values_);
    return o[e](function(a, l) {
      return t.call(r, a, l, n);
    });
  };
}
function vi(e) {
  return function() {
    var t = this, r = this[g];
    r.atom_.reportObserved();
    var n = r.dehanceValues_(r.values_), i = arguments[0];
    return arguments[0] = function(o, a, l) {
      return i(o, a, l, t);
    }, n[e].apply(n, arguments);
  };
}
var za = /* @__PURE__ */ Se("ObservableArrayAdministration", Dr);
function ft(e) {
  return Bt(e) && za(e[g]);
}
var fi, pi, Ba = {}, oe = "add", Lt = "delete";
fi = Symbol.iterator;
pi = Symbol.toStringTag;
var gi = /* @__PURE__ */ function() {
  function e(r, n, i) {
    var o = this;
    n === void 0 && (n = me), i === void 0 && (i = process.env.NODE_ENV !== "production" ? "ObservableMap@" + I() : "ObservableMap"), this.enhancer_ = void 0, this.name_ = void 0, this[g] = Ba, this.data_ = void 0, this.hasMap_ = void 0, this.keysAtom_ = void 0, this.interceptors_ = void 0, this.changeListeners_ = void 0, this.dehancer = void 0, this.enhancer_ = n, this.name_ = i, A(Map) || v(18), xe(function() {
      o.keysAtom_ = jn(process.env.NODE_ENV !== "production" ? o.name_ + ".keys()" : "ObservableMap.keys()"), o.data_ = /* @__PURE__ */ new Map(), o.hasMap_ = /* @__PURE__ */ new Map(), r && o.merge(r);
    });
  }
  var t = e.prototype;
  return t.has_ = function(n) {
    return this.data_.has(n);
  }, t.has = function(n) {
    var i = this;
    if (!h.trackingDerivation)
      return this.has_(n);
    var o = this.hasMap_.get(n);
    if (!o) {
      var a = o = new ge(this.has_(n), qt, process.env.NODE_ENV !== "production" ? this.name_ + "." + ur(n) + "?" : "ObservableMap.key?", !1);
      this.hasMap_.set(n, a), li(a, function() {
        return i.hasMap_.delete(n);
      });
    }
    return o.get();
  }, t.set = function(n, i) {
    var o = this.has_(n);
    if (j(this)) {
      var a = R(this, {
        type: o ? U : oe,
        object: this,
        newValue: i,
        name: n
      });
      if (!a)
        return this;
      i = a.newValue;
    }
    return o ? this.updateValue_(n, i) : this.addValue_(n, i), this;
  }, t.delete = function(n) {
    var i = this;
    if (W(this.keysAtom_), j(this)) {
      var o = R(this, {
        type: Lt,
        object: this,
        name: n
      });
      if (!o)
        return !1;
    }
    if (this.has_(n)) {
      var a = x(), l = B(this), s = l || a ? {
        observableKind: "map",
        debugObjectName: this.name_,
        type: Lt,
        object: this,
        oldValue: this.data_.get(n).value_,
        name: n
      } : null;
      return process.env.NODE_ENV !== "production" && a && C(s), Z(function() {
        var c;
        i.keysAtom_.reportChanged(), (c = i.hasMap_.get(n)) == null || c.setNewValue_(!1);
        var d = i.data_.get(n);
        d.setNewValue_(void 0), i.data_.delete(n);
      }), l && K(this, s), process.env.NODE_ENV !== "production" && a && T(), !0;
    }
    return !1;
  }, t.updateValue_ = function(n, i) {
    var o = this.data_.get(n);
    if (i = o.prepareNewValue_(i), i !== h.UNCHANGED) {
      var a = x(), l = B(this), s = l || a ? {
        observableKind: "map",
        debugObjectName: this.name_,
        type: U,
        object: this,
        oldValue: o.value_,
        name: n,
        newValue: i
      } : null;
      process.env.NODE_ENV !== "production" && a && C(s), o.setNewValue_(i), l && K(this, s), process.env.NODE_ENV !== "production" && a && T();
    }
  }, t.addValue_ = function(n, i) {
    var o = this;
    W(this.keysAtom_), Z(function() {
      var c, d = new ge(i, o.enhancer_, process.env.NODE_ENV !== "production" ? o.name_ + "." + ur(n) : "ObservableMap.key", !1);
      o.data_.set(n, d), i = d.value_, (c = o.hasMap_.get(n)) == null || c.setNewValue_(!0), o.keysAtom_.reportChanged();
    });
    var a = x(), l = B(this), s = l || a ? {
      observableKind: "map",
      debugObjectName: this.name_,
      type: oe,
      object: this,
      name: n,
      newValue: i
    } : null;
    process.env.NODE_ENV !== "production" && a && C(s), l && K(this, s), process.env.NODE_ENV !== "production" && a && T();
  }, t.get = function(n) {
    return this.has(n) ? this.dehanceValue_(this.data_.get(n).get()) : this.dehanceValue_(void 0);
  }, t.dehanceValue_ = function(n) {
    return this.dehancer !== void 0 ? this.dehancer(n) : n;
  }, t.keys = function() {
    return this.keysAtom_.reportObserved(), this.data_.keys();
  }, t.values = function() {
    var n = this, i = this.keys();
    return rt({
      next: function() {
        var a = i.next(), l = a.done, s = a.value;
        return {
          done: l,
          value: l ? void 0 : n.get(s)
        };
      }
    });
  }, t.entries = function() {
    var n = this, i = this.keys();
    return rt({
      next: function() {
        var a = i.next(), l = a.done, s = a.value;
        return {
          done: l,
          value: l ? void 0 : [s, n.get(s)]
        };
      }
    });
  }, t[fi] = function() {
    return this.entries();
  }, t.forEach = function(n, i) {
    for (var o = Te(this), a; !(a = o()).done; ) {
      var l = a.value, s = l[0], c = l[1];
      n.call(i, c, s, this);
    }
  }, t.merge = function(n) {
    var i = this;
    return ee(n) && (n = new Map(n)), Z(function() {
      D(n) ? ho(n).forEach(function(o) {
        return i.set(o, n[o]);
      }) : Array.isArray(n) ? n.forEach(function(o) {
        var a = o[0], l = o[1];
        return i.set(a, l);
      }) : Le(n) ? (uo(n) || v(19, n), n.forEach(function(o, a) {
        return i.set(a, o);
      })) : n != null && v(20, n);
    }), this;
  }, t.clear = function() {
    var n = this;
    Z(function() {
      Wn(function() {
        for (var i = Te(n.keys()), o; !(o = i()).done; ) {
          var a = o.value;
          n.delete(a);
        }
      });
    });
  }, t.replace = function(n) {
    var i = this;
    return Z(function() {
      for (var o = Ka(n), a = /* @__PURE__ */ new Map(), l = !1, s = Te(i.data_.keys()), c; !(c = s()).done; ) {
        var d = c.value;
        if (!o.has(d)) {
          var u = i.delete(d);
          if (u)
            l = !0;
          else {
            var f = i.data_.get(d);
            a.set(d, f);
          }
        }
      }
      for (var p = Te(o.entries()), y; !(y = p()).done; ) {
        var m = y.value, S = m[0], q = m[1], $e = i.data_.has(S);
        if (i.set(S, q), i.data_.has(S)) {
          var te = i.data_.get(S);
          a.set(S, te), $e || (l = !0);
        }
      }
      if (!l)
        if (i.data_.size !== a.size)
          i.keysAtom_.reportChanged();
        else
          for (var P = i.data_.keys(), re = a.keys(), ce = P.next(), zr = re.next(); !ce.done; ) {
            if (ce.value !== zr.value) {
              i.keysAtom_.reportChanged();
              break;
            }
            ce = P.next(), zr = re.next();
          }
      i.data_ = a;
    }), this;
  }, t.toString = function() {
    return "[object ObservableMap]";
  }, t.toJSON = function() {
    return Array.from(this);
  }, t.observe_ = function(n, i) {
    return process.env.NODE_ENV !== "production" && i === !0 && v("`observe` doesn't support fireImmediately=true in combination with maps."), vt(this, n);
  }, t.intercept_ = function(n) {
    return ht(this, n);
  }, Ht(e, [{
    key: "size",
    get: function() {
      return this.keysAtom_.reportObserved(), this.data_.size;
    }
  }, {
    key: pi,
    get: function() {
      return "Map";
    }
  }]), e;
}(), ee = /* @__PURE__ */ Se("ObservableMap", gi);
function Ka(e) {
  if (Le(e) || ee(e))
    return e;
  if (Array.isArray(e))
    return new Map(e);
  if (D(e)) {
    var t = /* @__PURE__ */ new Map();
    for (var r in e)
      t.set(r, e[r]);
    return t;
  } else
    return v(21, e);
}
var bi, _i, Ha = {};
bi = Symbol.iterator;
_i = Symbol.toStringTag;
var mi = /* @__PURE__ */ function() {
  function e(r, n, i) {
    var o = this;
    n === void 0 && (n = me), i === void 0 && (i = process.env.NODE_ENV !== "production" ? "ObservableSet@" + I() : "ObservableSet"), this.name_ = void 0, this[g] = Ha, this.data_ = /* @__PURE__ */ new Set(), this.atom_ = void 0, this.changeListeners_ = void 0, this.interceptors_ = void 0, this.dehancer = void 0, this.enhancer_ = void 0, this.name_ = i, A(Set) || v(22), this.enhancer_ = function(a, l) {
      return n(a, l, i);
    }, xe(function() {
      o.atom_ = jn(o.name_), r && o.replace(r);
    });
  }
  var t = e.prototype;
  return t.dehanceValue_ = function(n) {
    return this.dehancer !== void 0 ? this.dehancer(n) : n;
  }, t.clear = function() {
    var n = this;
    Z(function() {
      Wn(function() {
        for (var i = Te(n.data_.values()), o; !(o = i()).done; ) {
          var a = o.value;
          n.delete(a);
        }
      });
    });
  }, t.forEach = function(n, i) {
    for (var o = Te(this), a; !(a = o()).done; ) {
      var l = a.value;
      n.call(i, l, l, this);
    }
  }, t.add = function(n) {
    var i = this;
    if (W(this.atom_), j(this)) {
      var o = R(this, {
        type: oe,
        object: this,
        newValue: n
      });
      if (!o)
        return this;
    }
    if (!this.has(n)) {
      Z(function() {
        i.data_.add(i.enhancer_(n, void 0)), i.atom_.reportChanged();
      });
      var a = process.env.NODE_ENV !== "production" && x(), l = B(this), s = l || a ? {
        observableKind: "set",
        debugObjectName: this.name_,
        type: oe,
        object: this,
        newValue: n
      } : null;
      a && process.env.NODE_ENV !== "production" && C(s), l && K(this, s), a && process.env.NODE_ENV !== "production" && T();
    }
    return this;
  }, t.delete = function(n) {
    var i = this;
    if (j(this)) {
      var o = R(this, {
        type: Lt,
        object: this,
        oldValue: n
      });
      if (!o)
        return !1;
    }
    if (this.has(n)) {
      var a = process.env.NODE_ENV !== "production" && x(), l = B(this), s = l || a ? {
        observableKind: "set",
        debugObjectName: this.name_,
        type: Lt,
        object: this,
        oldValue: n
      } : null;
      return a && process.env.NODE_ENV !== "production" && C(s), Z(function() {
        i.atom_.reportChanged(), i.data_.delete(n);
      }), l && K(this, s), a && process.env.NODE_ENV !== "production" && T(), !0;
    }
    return !1;
  }, t.has = function(n) {
    return this.atom_.reportObserved(), this.data_.has(this.dehanceValue_(n));
  }, t.entries = function() {
    var n = 0, i = Array.from(this.keys()), o = Array.from(this.values());
    return rt({
      next: function() {
        var l = n;
        return n += 1, l < o.length ? {
          value: [i[l], o[l]],
          done: !1
        } : {
          done: !0
        };
      }
    });
  }, t.keys = function() {
    return this.values();
  }, t.values = function() {
    this.atom_.reportObserved();
    var n = this, i = 0, o = Array.from(this.data_.values());
    return rt({
      next: function() {
        return i < o.length ? {
          value: n.dehanceValue_(o[i++]),
          done: !1
        } : {
          done: !0
        };
      }
    });
  }, t.replace = function(n) {
    var i = this;
    return Ie(n) && (n = new Set(n)), Z(function() {
      Array.isArray(n) ? (i.clear(), n.forEach(function(o) {
        return i.add(o);
      })) : st(n) ? (i.clear(), n.forEach(function(o) {
        return i.add(o);
      })) : n != null && v("Cannot initialize set from " + n);
    }), this;
  }, t.observe_ = function(n, i) {
    return process.env.NODE_ENV !== "production" && i === !0 && v("`observe` doesn't support fireImmediately=true in combination with sets."), vt(this, n);
  }, t.intercept_ = function(n) {
    return ht(this, n);
  }, t.toJSON = function() {
    return Array.from(this);
  }, t.toString = function() {
    return "[object ObservableSet]";
  }, t[bi] = function() {
    return this.values();
  }, Ht(e, [{
    key: "size",
    get: function() {
      return this.atom_.reportObserved(), this.data_.size;
    }
  }, {
    key: _i,
    get: function() {
      return "Set";
    }
  }]), e;
}(), Ie = /* @__PURE__ */ Se("ObservableSet", mi), Jr = /* @__PURE__ */ Object.create(null), Zr = "remove", _r = /* @__PURE__ */ function() {
  function e(r, n, i, o) {
    n === void 0 && (n = /* @__PURE__ */ new Map()), o === void 0 && (o = Bo), this.target_ = void 0, this.values_ = void 0, this.name_ = void 0, this.defaultAnnotation_ = void 0, this.keysAtom_ = void 0, this.changeListeners_ = void 0, this.interceptors_ = void 0, this.proxy_ = void 0, this.isPlainObject_ = void 0, this.appliedAnnotations_ = void 0, this.pendingKeys_ = void 0, this.target_ = r, this.values_ = n, this.name_ = i, this.defaultAnnotation_ = o, this.keysAtom_ = new ct(process.env.NODE_ENV !== "production" ? this.name_ + ".keys" : "ObservableObject.keys"), this.isPlainObject_ = D(this.target_), process.env.NODE_ENV !== "production" && !Ei(this.defaultAnnotation_) && v("defaultAnnotation must be valid annotation"), process.env.NODE_ENV !== "production" && (this.appliedAnnotations_ = {});
  }
  var t = e.prototype;
  return t.getObservablePropValue_ = function(n) {
    return this.values_.get(n).get();
  }, t.setObservablePropValue_ = function(n, i) {
    var o = this.values_.get(n);
    if (o instanceof H)
      return o.set(i), !0;
    if (j(this)) {
      var a = R(this, {
        type: U,
        object: this.proxy_ || this.target_,
        name: n,
        newValue: i
      });
      if (!a)
        return null;
      i = a.newValue;
    }
    if (i = o.prepareNewValue_(i), i !== h.UNCHANGED) {
      var l = B(this), s = process.env.NODE_ENV !== "production" && x(), c = l || s ? {
        type: U,
        observableKind: "object",
        debugObjectName: this.name_,
        object: this.proxy_ || this.target_,
        oldValue: o.value_,
        name: n,
        newValue: i
      } : null;
      process.env.NODE_ENV !== "production" && s && C(c), o.setNewValue_(i), l && K(this, c), process.env.NODE_ENV !== "production" && s && T();
    }
    return !0;
  }, t.get_ = function(n) {
    return h.trackingDerivation && !z(this.target_, n) && this.has_(n), this.target_[n];
  }, t.set_ = function(n, i, o) {
    return o === void 0 && (o = !1), z(this.target_, n) ? this.values_.has(n) ? this.setObservablePropValue_(n, i) : o ? Reflect.set(this.target_, n, i) : (this.target_[n] = i, !0) : this.extend_(n, {
      value: i,
      enumerable: !0,
      writable: !0,
      configurable: !0
    }, this.defaultAnnotation_, o);
  }, t.has_ = function(n) {
    if (!h.trackingDerivation)
      return n in this.target_;
    this.pendingKeys_ || (this.pendingKeys_ = /* @__PURE__ */ new Map());
    var i = this.pendingKeys_.get(n);
    return i || (i = new ge(n in this.target_, qt, process.env.NODE_ENV !== "production" ? this.name_ + "." + ur(n) + "?" : "ObservableObject.key?", !1), this.pendingKeys_.set(n, i)), i.get();
  }, t.make_ = function(n, i) {
    if (i === !0 && (i = this.defaultAnnotation_), i !== !1) {
      if (en(this, i, n), !(n in this.target_)) {
        var o;
        if ((o = this.target_[J]) != null && o[n])
          return;
        v(1, i.annotationType_, this.name_ + "." + n.toString());
      }
      for (var a = this.target_; a && a !== zt; ) {
        var l = Pt(a, n);
        if (l) {
          var s = i.make_(this, n, l, a);
          if (s === 0)
            return;
          if (s === 1)
            break;
        }
        a = Object.getPrototypeOf(a);
      }
      Qr(this, i, n);
    }
  }, t.extend_ = function(n, i, o, a) {
    if (a === void 0 && (a = !1), o === !0 && (o = this.defaultAnnotation_), o === !1)
      return this.defineProperty_(n, i, a);
    en(this, o, n);
    var l = o.extend_(this, n, i, a);
    return l && Qr(this, o, n), l;
  }, t.defineProperty_ = function(n, i, o) {
    o === void 0 && (o = !1), W(this.keysAtom_);
    try {
      k();
      var a = this.delete_(n);
      if (!a)
        return a;
      if (j(this)) {
        var l = R(this, {
          object: this.proxy_ || this.target_,
          name: n,
          type: oe,
          newValue: i.value
        });
        if (!l)
          return null;
        var s = l.newValue;
        i.value !== s && (i = ae({}, i, {
          value: s
        }));
      }
      if (o) {
        if (!Reflect.defineProperty(this.target_, n, i))
          return !1;
      } else
        G(this.target_, n, i);
      this.notifyPropertyAddition_(n, i.value);
    } finally {
      L();
    }
    return !0;
  }, t.defineObservableProperty_ = function(n, i, o, a) {
    a === void 0 && (a = !1), W(this.keysAtom_);
    try {
      k();
      var l = this.delete_(n);
      if (!l)
        return l;
      if (j(this)) {
        var s = R(this, {
          object: this.proxy_ || this.target_,
          name: n,
          type: oe,
          newValue: i
        });
        if (!s)
          return null;
        i = s.newValue;
      }
      var c = Yr(n), d = {
        configurable: h.safeDescriptors ? this.isPlainObject_ : !0,
        enumerable: !0,
        get: c.get,
        set: c.set
      };
      if (a) {
        if (!Reflect.defineProperty(this.target_, n, d))
          return !1;
      } else
        G(this.target_, n, d);
      var u = new ge(i, o, process.env.NODE_ENV !== "production" ? this.name_ + "." + n.toString() : "ObservableObject.key", !1);
      this.values_.set(n, u), this.notifyPropertyAddition_(n, u.value_);
    } finally {
      L();
    }
    return !0;
  }, t.defineComputedProperty_ = function(n, i, o) {
    o === void 0 && (o = !1), W(this.keysAtom_);
    try {
      k();
      var a = this.delete_(n);
      if (!a)
        return a;
      if (j(this)) {
        var l = R(this, {
          object: this.proxy_ || this.target_,
          name: n,
          type: oe,
          newValue: void 0
        });
        if (!l)
          return null;
      }
      i.name || (i.name = process.env.NODE_ENV !== "production" ? this.name_ + "." + n.toString() : "ObservableObject.key"), i.context = this.proxy_ || this.target_;
      var s = Yr(n), c = {
        configurable: h.safeDescriptors ? this.isPlainObject_ : !0,
        enumerable: !1,
        get: s.get,
        set: s.set
      };
      if (o) {
        if (!Reflect.defineProperty(this.target_, n, c))
          return !1;
      } else
        G(this.target_, n, c);
      this.values_.set(n, new H(i)), this.notifyPropertyAddition_(n, void 0);
    } finally {
      L();
    }
    return !0;
  }, t.delete_ = function(n, i) {
    if (i === void 0 && (i = !1), W(this.keysAtom_), !z(this.target_, n))
      return !0;
    if (j(this)) {
      var o = R(this, {
        object: this.proxy_ || this.target_,
        name: n,
        type: Zr
      });
      if (!o)
        return null;
    }
    try {
      var a, l;
      k();
      var s = B(this), c = process.env.NODE_ENV !== "production" && x(), d = this.values_.get(n), u = void 0;
      if (!d && (s || c)) {
        var f;
        u = (f = Pt(this.target_, n)) == null ? void 0 : f.value;
      }
      if (i) {
        if (!Reflect.deleteProperty(this.target_, n))
          return !1;
      } else
        delete this.target_[n];
      if (process.env.NODE_ENV !== "production" && delete this.appliedAnnotations_[n], d && (this.values_.delete(n), d instanceof ge && (u = d.value_), Yn(d)), this.keysAtom_.reportChanged(), (a = this.pendingKeys_) == null || (l = a.get(n)) == null || l.set(n in this.target_), s || c) {
        var p = {
          type: Zr,
          observableKind: "object",
          object: this.proxy_ || this.target_,
          debugObjectName: this.name_,
          oldValue: u,
          name: n
        };
        process.env.NODE_ENV !== "production" && c && C(p), s && K(this, p), process.env.NODE_ENV !== "production" && c && T();
      }
    } finally {
      L();
    }
    return !0;
  }, t.observe_ = function(n, i) {
    return process.env.NODE_ENV !== "production" && i === !0 && v("`observe` doesn't support the fire immediately property for observable objects."), vt(this, n);
  }, t.intercept_ = function(n) {
    return ht(this, n);
  }, t.notifyPropertyAddition_ = function(n, i) {
    var o, a, l = B(this), s = process.env.NODE_ENV !== "production" && x();
    if (l || s) {
      var c = l || s ? {
        type: oe,
        observableKind: "object",
        debugObjectName: this.name_,
        object: this.proxy_ || this.target_,
        name: n,
        newValue: i
      } : null;
      process.env.NODE_ENV !== "production" && s && C(c), l && K(this, c), process.env.NODE_ENV !== "production" && s && T();
    }
    (o = this.pendingKeys_) == null || (a = o.get(n)) == null || a.set(!0), this.keysAtom_.reportChanged();
  }, t.ownKeys_ = function() {
    return this.keysAtom_.reportObserved(), Ze(this.target_);
  }, t.keys_ = function() {
    return this.keysAtom_.reportObserved(), Object.keys(this.target_);
  }, e;
}();
function Ue(e, t) {
  var r;
  if (process.env.NODE_ENV !== "production" && t && Ae(e) && v("Options can't be provided for already observable objects."), z(e, g))
    return process.env.NODE_ENV !== "production" && !(Cr(e) instanceof _r) && v("Cannot convert '" + Mt(e) + `' into observable object:
The target is already observable of different type.
Extending builtins is not supported.`), e;
  process.env.NODE_ENV !== "production" && !Object.isExtensible(e) && v("Cannot make the designated object observable; it is not extensible");
  var n = (r = t?.name) != null ? r : process.env.NODE_ENV !== "production" ? (D(e) ? "ObservableObject" : e.constructor.name) + "@" + I() : "ObservableObject", i = new _r(e, /* @__PURE__ */ new Map(), String(n), Qo(t));
  return Kt(e, g, i), e;
}
var Fa = /* @__PURE__ */ Se("ObservableObjectAdministration", _r);
function Yr(e) {
  return Jr[e] || (Jr[e] = {
    get: function() {
      return this[g].getObservablePropValue_(e);
    },
    set: function(r) {
      return this[g].setObservablePropValue_(e, r);
    }
  });
}
function Ae(e) {
  return Bt(e) ? Fa(e[g]) : !1;
}
function Qr(e, t, r) {
  var n;
  process.env.NODE_ENV !== "production" && (e.appliedAnnotations_[r] = t), (n = e.target_[J]) == null || delete n[r];
}
function en(e, t, r) {
  if (process.env.NODE_ENV !== "production" && !Ei(t) && v("Cannot annotate '" + e.name_ + "." + r.toString() + "': Invalid annotation."), process.env.NODE_ENV !== "production" && !Tt(t) && z(e.appliedAnnotations_, r)) {
    var n = e.name_ + "." + r.toString(), i = e.appliedAnnotations_[r].annotationType_, o = t.annotationType_;
    v("Cannot apply '" + o + "' to '" + n + "':" + (`
The field is already annotated with '` + i + "'.") + `
Re-annotating fields is not allowed.
Use 'override' annotation for methods overridden by subclass.`);
  }
}
var qa = /* @__PURE__ */ wi(0), Wa = /* @__PURE__ */ function() {
  var e = !1, t = {};
  return Object.defineProperty(t, "0", {
    set: function() {
      e = !0;
    }
  }), Object.create(t)[0] = 1, e === !1;
}(), or = 0, yi = function() {
};
function Ga(e, t) {
  Object.setPrototypeOf ? Object.setPrototypeOf(e.prototype, t) : e.prototype.__proto__ !== void 0 ? e.prototype.__proto__ = t : e.prototype = t;
}
Ga(yi, Array.prototype);
var Pr = /* @__PURE__ */ function(e, t, r) {
  Vn(n, e);
  function n(o, a, l, s) {
    var c;
    return l === void 0 && (l = process.env.NODE_ENV !== "production" ? "ObservableArray@" + I() : "ObservableArray"), s === void 0 && (s = !1), c = e.call(this) || this, xe(function() {
      var d = new Dr(l, a, s, !0);
      d.proxy_ = St(c), Pn(St(c), g, d), o && o.length && c.spliceWithArray(0, 0, o), Wa && Object.defineProperty(St(c), "0", qa);
    }), c;
  }
  var i = n.prototype;
  return i.concat = function() {
    this[g].atom_.reportObserved();
    for (var a = arguments.length, l = new Array(a), s = 0; s < a; s++)
      l[s] = arguments[s];
    return Array.prototype.concat.apply(
      this.slice(),
      //@ts-ignore
      l.map(function(c) {
        return ft(c) ? c.slice() : c;
      })
    );
  }, i[r] = function() {
    var o = this, a = 0;
    return rt({
      next: function() {
        return a < o.length ? {
          value: o[a++],
          done: !1
        } : {
          done: !0,
          value: void 0
        };
      }
    });
  }, Ht(n, [{
    key: "length",
    get: function() {
      return this[g].getArrayLength_();
    },
    set: function(a) {
      this[g].setArrayLength_(a);
    }
  }, {
    key: t,
    get: function() {
      return "Array";
    }
  }]), n;
}(yi, Symbol.toStringTag, Symbol.iterator);
Object.entries(kt).forEach(function(e) {
  var t = e[0], r = e[1];
  t !== "concat" && Kt(Pr.prototype, t, r);
});
function wi(e) {
  return {
    enumerable: !1,
    configurable: !0,
    get: function() {
      return this[g].get_(e);
    },
    set: function(r) {
      this[g].set_(e, r);
    }
  };
}
function Xa(e) {
  G(Pr.prototype, "" + e, wi(e));
}
function Ai(e) {
  if (e > or) {
    for (var t = or; t < e + 100; t++)
      Xa(t);
    or = e;
  }
}
Ai(1e3);
function Ja(e, t, r) {
  return new Pr(e, t, r);
}
function se(e, t) {
  if (typeof e == "object" && e !== null) {
    if (ft(e))
      return t !== void 0 && v(23), e[g].atom_;
    if (Ie(e))
      return e.atom_;
    if (ee(e)) {
      if (t === void 0)
        return e.keysAtom_;
      var r = e.data_.get(t) || e.hasMap_.get(t);
      return r || v(25, t, Mt(e)), r;
    }
    if (Ae(e)) {
      if (!t)
        return v(26);
      var n = e[g].values_.get(t);
      return n || v(27, t, Mt(e)), n;
    }
    if (Sr(e) || Zt(e) || Rt(e))
      return e;
  } else if (A(e) && Rt(e[g]))
    return e[g];
  v(28);
}
function Cr(e, t) {
  if (e || v(29), t !== void 0)
    return Cr(se(e, t));
  if (Sr(e) || Zt(e) || Rt(e) || ee(e) || Ie(e))
    return e;
  if (e[g])
    return e[g];
  v(24, e);
}
function Mt(e, t) {
  var r;
  if (t !== void 0)
    r = se(e, t);
  else {
    if (dt(e))
      return e.name;
    Ae(e) || ee(e) || Ie(e) ? r = Cr(e) : r = se(e);
  }
  return r.name_;
}
function xe(e) {
  var t = Ne(), r = Xt(!0);
  k();
  try {
    return e();
  } finally {
    L(), Jt(r), Q(t);
  }
}
var tn = zt.toString;
function Tr(e, t, r) {
  return r === void 0 && (r = -1), mr(e, t, r);
}
function mr(e, t, r, n, i) {
  if (e === t)
    return e !== 0 || 1 / e === 1 / t;
  if (e == null || t == null)
    return !1;
  if (e !== e)
    return t !== t;
  var o = typeof e;
  if (o !== "function" && o !== "object" && typeof t != "object")
    return !1;
  var a = tn.call(e);
  if (a !== tn.call(t))
    return !1;
  switch (a) {
    case "[object RegExp]":
    case "[object String]":
      return "" + e == "" + t;
    case "[object Number]":
      return +e != +e ? +t != +t : +e == 0 ? 1 / +e === 1 / t : +e == +t;
    case "[object Date]":
    case "[object Boolean]":
      return +e == +t;
    case "[object Symbol]":
      return typeof Symbol < "u" && Symbol.valueOf.call(e) === Symbol.valueOf.call(t);
    case "[object Map]":
    case "[object Set]":
      r >= 0 && r++;
      break;
  }
  e = rn(e), t = rn(t);
  var l = a === "[object Array]";
  if (!l) {
    if (typeof e != "object" || typeof t != "object")
      return !1;
    var s = e.constructor, c = t.constructor;
    if (s !== c && !(A(s) && s instanceof s && A(c) && c instanceof c) && "constructor" in e && "constructor" in t)
      return !1;
  }
  if (r === 0)
    return !1;
  r < 0 && (r = -1), n = n || [], i = i || [];
  for (var d = n.length; d--; )
    if (n[d] === e)
      return i[d] === t;
  if (n.push(e), i.push(t), l) {
    if (d = e.length, d !== t.length)
      return !1;
    for (; d--; )
      if (!mr(e[d], t[d], r - 1, n, i))
        return !1;
  } else {
    var u = Object.keys(e), f;
    if (d = u.length, Object.keys(t).length !== d)
      return !1;
    for (; d--; )
      if (f = u[d], !(z(t, f) && mr(e[f], t[f], r - 1, n, i)))
        return !1;
  }
  return n.pop(), i.pop(), !0;
}
function rn(e) {
  return ft(e) ? e.slice() : Le(e) || ee(e) || st(e) || Ie(e) ? Array.from(e.entries()) : e;
}
function rt(e) {
  return e[Symbol.iterator] = Za, e;
}
function Za() {
  return this;
}
function Ei(e) {
  return (
    // Can be function
    e instanceof Object && typeof e.annotationType_ == "string" && A(e.make_) && A(e.extend_)
  );
}
["Symbol", "Map", "Set"].forEach(function(e) {
  var t = Nn();
  typeof t[e] > "u" && v("MobX requires global '" + e + "' to be available or polyfilled");
});
typeof __MOBX_DEVTOOLS_GLOBAL_HOOK__ == "object" && __MOBX_DEVTOOLS_GLOBAL_HOOK__.injectMobx({
  spy: ma,
  extras: {
    getDebugName: Mt
  },
  $mobx: g
});
const nn = "copilot-conf";
class be {
  static get sessionConfiguration() {
    const t = sessionStorage.getItem(nn);
    return t ? JSON.parse(t) : {};
  }
  static saveCopilotActivation(t) {
    const r = this.sessionConfiguration;
    r.active = t, this.persist(r);
  }
  static getCopilotActivation() {
    return this.sessionConfiguration.active;
  }
  static saveSpotlightActivation(t) {
    const r = this.sessionConfiguration;
    r.spotlightActive = t, this.persist(r);
  }
  static getSpotlightActivation() {
    return this.sessionConfiguration.spotlightActive;
  }
  static saveSpotlightPosition(t, r, n, i) {
    const o = this.sessionConfiguration;
    o.spotlightPosition = { left: t, top: r, right: n, bottom: i }, this.persist(o);
  }
  static getSpotlightPosition() {
    return this.sessionConfiguration.spotlightPosition;
  }
  static saveDrawerSize(t, r) {
    const n = this.sessionConfiguration;
    n.drawerSizes = n.drawerSizes ?? {}, n.drawerSizes[t] = r, this.persist(n);
  }
  static getDrawerSize(t) {
    const r = this.sessionConfiguration;
    if (r.drawerSizes)
      return r.drawerSizes[t];
  }
  static savePanelConfigurations(t) {
    const r = this.sessionConfiguration;
    r.sectionPanelState = t, this.persist(r);
  }
  static getPanelConfigurations() {
    return this.sessionConfiguration.sectionPanelState;
  }
  static persist(t) {
    sessionStorage.setItem(nn, JSON.stringify(t));
  }
  static savePrompts(t) {
    const r = this.sessionConfiguration;
    r.prompts = t, this.persist(r);
  }
  static getPrompts() {
    return this.sessionConfiguration.prompts || [];
  }
}
class Ya {
  constructor() {
    this.spotlightActive = !1, this.welcomeActive = !1, this.loginCheckActive = !1, this.userInfo = void 0, this.active = !1, this.activatedFrom = null, this.activatedAtLeastOnce = !1, this.operationInProgress = void 0, this.operationWaitsHmrUpdate = void 0, this.idePluginState = void 0, this.notifications = [], this.infoTooltip = null, this.sectionPanelDragging = !1, this.spotlightDragging = !1, this.sectionPanelResizing = !1, this.drawerResizing = !1, this.jdkInfo = void 0, Yt(this, {
      notifications: O.shallow
    }), this.spotlightActive = be.getSpotlightActivation() ?? !1;
  }
  setActive(t, r) {
    this.active = t, t && (this.activatedAtLeastOnce = !0), this.activatedFrom = r ?? null;
  }
  setSpotlightActive(t) {
    this.spotlightActive = t;
  }
  setWelcomeActive(t) {
    this.welcomeActive = t;
  }
  setLoginCheckActive(t) {
    this.loginCheckActive = t;
  }
  setUserInfo(t) {
    this.userInfo = t;
  }
  startOperation(t) {
    if (this.operationInProgress)
      throw new Error(`An ${t} operation is already in progress`);
    if (this.operationWaitsHmrUpdate)
      throw new Error("Wait for files to be updated to start a new operation");
    this.operationInProgress = t;
  }
  stopOperation(t) {
    if (this.operationInProgress) {
      if (this.operationInProgress !== t)
        return;
    } else return;
    this.operationInProgress = void 0;
  }
  setIdePluginState(t) {
    this.idePluginState = t;
  }
  toggleActive(t) {
    this.setActive(!this.active, this.active ? null : t ?? null);
  }
  reset() {
    this.active = !1, this.activatedAtLeastOnce = !1;
  }
  setNotifications(t) {
    this.notifications = t;
  }
  removeNotification(t) {
    t.animatingOut = !0, setTimeout(() => {
      this.reallyRemoveNotification(t);
    }, 180);
  }
  reallyRemoveNotification(t) {
    const r = this.notifications.indexOf(t);
    r > -1 && this.notifications.splice(r, 1);
  }
  setTooltip(t, r) {
    this.infoTooltip = {
      text: t,
      loader: r
    };
  }
  clearTooltip() {
    this.infoTooltip = null;
  }
  setSectionPanelDragging(t) {
    this.sectionPanelDragging = t;
  }
  setSpotlightDragging(t) {
    this.spotlightDragging = t;
  }
  setSectionPanelResizing(t) {
    this.sectionPanelResizing = t;
  }
  setDrawerResizing(t) {
    this.drawerResizing = t;
  }
}
const Re = "copilot-", Qa = "24.4.8", Al = "attention-required", El = "https://plugins.jetbrains.com/plugin/23758-vaadin", Ol = "https://marketplace.visualstudio.com/items?itemName=vaadin.vaadin-vscode", Sl = (e, t, r) => t >= e.left && t <= e.right && r >= e.top && r <= e.bottom, es = (e) => {
  const t = [];
  let r = rs(e);
  for (; r; )
    t.push(r), r = r.parentElement;
  return t;
}, ts = (e, t) => {
  let r = e;
  for (; !(r instanceof HTMLElement && r.localName === `${Re}main`); ) {
    if (!r.isConnected)
      return null;
    if (r.parentNode ? r = r.parentNode : r.host && (r = r.host), r instanceof HTMLElement && r.localName === t)
      return r;
  }
  return null;
};
function rs(e) {
  return e.parentElement ?? e.parentNode?.host;
}
function nt(e) {
  return !e || !(e instanceof HTMLElement) ? !1 : [...es(e), e].map((t) => t.localName).some((t) => t.startsWith(Re));
}
function Nl(e) {
  return e instanceof Element;
}
function xl(e) {
  return e.startsWith("vaadin-") ? e.substring(7).split("-").map((n) => n.charAt(0).toUpperCase() + n.slice(1)).join(" ") : e;
}
function $l(e) {
  if (!e)
    return;
  if (e.id)
    return `#${e.id}`;
  if (!e.children)
    return;
  const t = Array.from(e.children).find((n) => n.localName === "label");
  if (t)
    return t.outerText.trim();
  const r = Array.from(e.childNodes).find(
    (n) => n.nodeType === Node.TEXT_NODE && n.textContent && n.textContent.trim().length > 0
  );
  if (r && r.textContent)
    return r.textContent.trim();
}
var Oi = /* @__PURE__ */ ((e) => (e["vaadin-combo-box"] = "vaadin-combo-box", e["vaadin-date-picker"] = "vaadin-date-picker", e["vaadin-dialog"] = "vaadin-dialog", e["vaadin-multi-select-combo-box"] = "vaadin-multi-select-combo-box", e["vaadin-select"] = "vaadin-select", e["vaadin-time-picker"] = "vaadin-time-picker", e))(Oi || {});
const Ke = {
  "vaadin-combo-box": {
    hideOnActivation: !0,
    open: (e) => yt(e),
    close: (e) => wt(e)
  },
  "vaadin-select": {
    hideOnActivation: !0,
    open: (e) => {
      const t = e;
      Ni(t, t._overlayElement), t.opened = !0;
    },
    close: (e) => {
      const t = e;
      xi(t, t._overlayElement), t.opened = !1;
    }
  },
  "vaadin-multi-select-combo-box": {
    hideOnActivation: !0,
    open: (e) => yt(e.$.comboBox),
    close: (e) => {
      wt(e.$.comboBox), e.removeAttribute("focused");
    }
  },
  "vaadin-date-picker": {
    hideOnActivation: !0,
    open: (e) => yt(e),
    close: (e) => wt(e)
  },
  "vaadin-time-picker": {
    hideOnActivation: !0,
    open: (e) => yt(e.$.comboBox),
    close: (e) => {
      wt(e.$.comboBox), e.removeAttribute("focused");
    }
  },
  "vaadin-dialog": {
    hideOnActivation: !1
  }
}, Si = (e) => {
  e.preventDefault(), e.stopImmediatePropagation();
}, yt = (e) => {
  e.addEventListener("focusout", Si, { capture: !0 }), Ni(e), e.opened = !0;
}, wt = (e) => {
  xi(e), e.removeAttribute("focused"), e.removeEventListener("focusout", Si, { capture: !0 }), e.opened = !1;
}, Ni = (e, t) => {
  const r = t ?? e.$.overlay;
  r.__oldModeless = r.modeless, r.modeless = !0;
}, xi = (e, t) => {
  const r = t ?? e.$.overlay;
  r.modeless = r.__oldModeless !== void 0 ? r.__oldModeless : r.modeless, delete r.__oldModeless;
};
class ns {
  constructor() {
    this.openedOverlayOwners = /* @__PURE__ */ new Set(), this.overlayCloseEventListener = (t) => {
      nt(t.target?.owner) || (window.Vaadin.copilot._uiState.active || nt(t.detail.sourceEvent.target)) && (t.preventDefault(), t.stopImmediatePropagation());
    };
  }
  /**
   * Modifies pointer-events property to auto if dialog overlay is present on body element. <br/>
   * Overriding closeOnOutsideClick method in order to keep overlay present while copilot is active
   * @private
   */
  onCopilotActivation() {
    const t = Array.from(document.body.children).find(
      (n) => n.localName.startsWith("vaadin") && n.localName.endsWith("-overlay")
    );
    if (!t)
      return;
    const r = this.getOwner(t);
    if (r) {
      const n = Ke[r.localName];
      if (!n)
        return;
      n.hideOnActivation && n.close ? n.close(r) : document.body.style.getPropertyValue("pointer-events") === "none" && document.body.style.removeProperty("pointer-events");
    }
  }
  /**
   * Restores pointer-events state on deactivation. <br/>
   * Closes opened overlays while using copilot.
   * @private
   */
  onCopilotDeactivation() {
    this.openedOverlayOwners.forEach((r) => {
      const n = Ke[r.localName];
      n && n.close && n.close(r);
    }), document.body.querySelector("vaadin-dialog-overlay") && document.body.style.setProperty("pointer-events", "none");
  }
  getOwner(t) {
    const r = t;
    return r.owner ?? r.__dataHost;
  }
  addOverlayOutsideClickEvent() {
    document.documentElement.addEventListener("vaadin-overlay-outside-click", this.overlayCloseEventListener, {
      capture: !0
    }), document.documentElement.addEventListener("vaadin-overlay-escape-press", this.overlayCloseEventListener, {
      capture: !0
    });
  }
  removeOverlayOutsideClickEvent() {
    document.documentElement.removeEventListener("vaadin-overlay-outside-click", this.overlayCloseEventListener), document.documentElement.removeEventListener("vaadin-overlay-escape-press", this.overlayCloseEventListener);
  }
  toggle(t) {
    const r = Ke[t.localName];
    this.isOverlayActive(t) ? (r.close(t), this.openedOverlayOwners.delete(t)) : (r.open(t), this.openedOverlayOwners.add(t));
  }
  isOverlayActive(t) {
    const r = Ke[t.localName];
    return r.active ? r.active(t) : t.hasAttribute("opened");
  }
  overlayStatus(t) {
    if (!t)
      return { visible: !1 };
    const r = t.localName;
    let n = Object.keys(Oi).includes(r);
    if (!n)
      return { visible: !1 };
    const i = Ke[t.localName];
    i.hasOverlay && (n = i.hasOverlay(t));
    const o = this.isOverlayActive(t);
    return { visible: n, active: o };
  }
}
function $i(e, t) {
  const r = e();
  r ? t(r) : setTimeout(() => $i(e, t), 50);
}
async function Di(e) {
  const t = e();
  if (t)
    return t;
  let r;
  const n = new Promise((o) => {
    r = o;
  }), i = setInterval(() => {
    const o = e();
    o && (clearInterval(i), r(o));
  }, 10);
  return n;
}
function is(e) {
  return e && typeof e.lastAccessedBy_ == "number";
}
function Dl(e) {
  if (e) {
    if (typeof e == "string")
      return e;
    if (!is(e))
      throw new Error(`Expected message to be a string or an observable value but was ${JSON.stringify(e)}`);
    return e.get();
  }
}
function Pl(e, t) {
  return e.length > t ? `${e.substring(0, t - 3)}...` : e;
}
const os = {
  userAgent: navigator.userAgent,
  locale: navigator.language,
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
};
async function Vr() {
  return Di(() => {
    const e = window.Vaadin.devTools, t = e?.frontendConnection && e?.frontendConnection.status === "active";
    return e !== void 0 && t && e?.frontendConnection;
  });
}
function ke(e, t) {
  Vr().then((r) => r.send(e, { ...t, context: os }));
}
async function Cl() {
  return await Vr(), !!window.Vaadin.devTools.conf.backend;
}
class as {
  constructor() {
    this.promise = new Promise((t) => {
      this.resolveInit = t;
    });
  }
  done(t) {
    this.resolveInit(t);
  }
}
class ss {
  constructor() {
    this.dismissedNotifications = [], this.termsSummaryDismissed = !1, this.activationButtonPosition = null, this.paletteState = null, this.activationShortcut = !0, this.activationAnimation = !0, Yt(this), this.initializer = new as(), this.initializer.promise.then(() => {
      si(
        () => JSON.stringify(this),
        () => {
          ke("copilot-set-machine-configuration", { conf: JSON.stringify(on(this)) });
        }
      );
    }), window.Vaadin.copilot.eventbus.on("copilot-machine-configuration", (t) => {
      const r = t.detail.conf;
      Object.assign(this, on(r)), this.initializer.done(!0), t.preventDefault();
    }), this.loadData();
  }
  loadData() {
    ke("copilot-get-machine-configuration", {});
  }
  addDismissedNotification(t) {
    this.dismissedNotifications.push(t);
  }
  getDismissedNotifications() {
    return this.dismissedNotifications;
  }
  setTermsSummaryDismissed(t) {
    this.termsSummaryDismissed = t;
  }
  isTermsSummaryDismissed() {
    return this.termsSummaryDismissed;
  }
  getActivationButtonPosition() {
    return this.activationButtonPosition;
  }
  setActivationButtonPosition(t) {
    this.activationButtonPosition = t;
  }
  getPaletteState() {
    return this.paletteState;
  }
  setPaletteState(t) {
    this.paletteState = t;
  }
  isActivationShortcut() {
    return this.activationShortcut;
  }
  setActivationShortcut(t) {
    this.activationShortcut = t;
  }
  isActivationAnimation() {
    return this.activationAnimation;
  }
  setActivationAnimation(t) {
    this.activationAnimation = t;
  }
}
function on(e) {
  const t = { ...e };
  return delete t.initializer, t;
}
window.Vaadin ??= {};
window.Vaadin.copilot ??= {};
window.Vaadin.copilot.plugins = [];
window.Vaadin.copilot._uiState = new Ya();
window.Vaadin.copilot.eventbus = new io();
window.Vaadin.copilot.overlayManager = new ns();
window.Vaadin.copilot._machineState = new ss();
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const ls = (e) => (t, r) => {
  r !== void 0 ? r.addInitializer(() => {
    customElements.define(e, t);
  }) : customElements.define(e, t);
};
/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const xt = globalThis, jr = xt.ShadowRoot && (xt.ShadyCSS === void 0 || xt.ShadyCSS.nativeShadow) && "adoptedStyleSheets" in Document.prototype && "replace" in CSSStyleSheet.prototype, Rr = Symbol(), an = /* @__PURE__ */ new WeakMap();
let Pi = class {
  constructor(t, r, n) {
    if (this._$cssResult$ = !0, n !== Rr) throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");
    this.cssText = t, this.t = r;
  }
  get styleSheet() {
    let t = this.o;
    const r = this.t;
    if (jr && t === void 0) {
      const n = r !== void 0 && r.length === 1;
      n && (t = an.get(r)), t === void 0 && ((this.o = t = new CSSStyleSheet()).replaceSync(this.cssText), n && an.set(r, t));
    }
    return t;
  }
  toString() {
    return this.cssText;
  }
};
const ie = (e) => new Pi(typeof e == "string" ? e : e + "", void 0, Rr), cs = (e, ...t) => {
  const r = e.length === 1 ? e[0] : t.reduce((n, i, o) => n + ((a) => {
    if (a._$cssResult$ === !0) return a.cssText;
    if (typeof a == "number") return a;
    throw Error("Value passed to 'css' function must be a 'css' function result: " + a + ". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.");
  })(i) + e[o + 1], e[0]);
  return new Pi(r, e, Rr);
}, us = (e, t) => {
  if (jr) e.adoptedStyleSheets = t.map((r) => r instanceof CSSStyleSheet ? r : r.styleSheet);
  else for (const r of t) {
    const n = document.createElement("style"), i = xt.litNonce;
    i !== void 0 && n.setAttribute("nonce", i), n.textContent = r.cssText, e.appendChild(n);
  }
}, sn = jr ? (e) => e : (e) => e instanceof CSSStyleSheet ? ((t) => {
  let r = "";
  for (const n of t.cssRules) r += n.cssText;
  return ie(r);
})(e) : e;
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const { is: ds, defineProperty: hs, getOwnPropertyDescriptor: vs, getOwnPropertyNames: fs, getOwnPropertySymbols: ps, getPrototypeOf: gs } = Object, Qt = globalThis, ln = Qt.trustedTypes, bs = ln ? ln.emptyScript : "", _s = Qt.reactiveElementPolyfillSupport, Xe = (e, t) => e, yr = { toAttribute(e, t) {
  switch (t) {
    case Boolean:
      e = e ? bs : null;
      break;
    case Object:
    case Array:
      e = e == null ? e : JSON.stringify(e);
  }
  return e;
}, fromAttribute(e, t) {
  let r = e;
  switch (t) {
    case Boolean:
      r = e !== null;
      break;
    case Number:
      r = e === null ? null : Number(e);
      break;
    case Object:
    case Array:
      try {
        r = JSON.parse(e);
      } catch {
        r = null;
      }
  }
  return r;
} }, Ci = (e, t) => !ds(e, t), cn = { attribute: !0, type: String, converter: yr, reflect: !1, hasChanged: Ci };
Symbol.metadata ??= Symbol("metadata"), Qt.litPropertyMetadata ??= /* @__PURE__ */ new WeakMap();
let Pe = class extends HTMLElement {
  static addInitializer(t) {
    this._$Ei(), (this.l ??= []).push(t);
  }
  static get observedAttributes() {
    return this.finalize(), this._$Eh && [...this._$Eh.keys()];
  }
  static createProperty(t, r = cn) {
    if (r.state && (r.attribute = !1), this._$Ei(), this.elementProperties.set(t, r), !r.noAccessor) {
      const n = Symbol(), i = this.getPropertyDescriptor(t, n, r);
      i !== void 0 && hs(this.prototype, t, i);
    }
  }
  static getPropertyDescriptor(t, r, n) {
    const { get: i, set: o } = vs(this.prototype, t) ?? { get() {
      return this[r];
    }, set(a) {
      this[r] = a;
    } };
    return { get() {
      return i?.call(this);
    }, set(a) {
      const l = i?.call(this);
      o.call(this, a), this.requestUpdate(t, l, n);
    }, configurable: !0, enumerable: !0 };
  }
  static getPropertyOptions(t) {
    return this.elementProperties.get(t) ?? cn;
  }
  static _$Ei() {
    if (this.hasOwnProperty(Xe("elementProperties"))) return;
    const t = gs(this);
    t.finalize(), t.l !== void 0 && (this.l = [...t.l]), this.elementProperties = new Map(t.elementProperties);
  }
  static finalize() {
    if (this.hasOwnProperty(Xe("finalized"))) return;
    if (this.finalized = !0, this._$Ei(), this.hasOwnProperty(Xe("properties"))) {
      const r = this.properties, n = [...fs(r), ...ps(r)];
      for (const i of n) this.createProperty(i, r[i]);
    }
    const t = this[Symbol.metadata];
    if (t !== null) {
      const r = litPropertyMetadata.get(t);
      if (r !== void 0) for (const [n, i] of r) this.elementProperties.set(n, i);
    }
    this._$Eh = /* @__PURE__ */ new Map();
    for (const [r, n] of this.elementProperties) {
      const i = this._$Eu(r, n);
      i !== void 0 && this._$Eh.set(i, r);
    }
    this.elementStyles = this.finalizeStyles(this.styles);
  }
  static finalizeStyles(t) {
    const r = [];
    if (Array.isArray(t)) {
      const n = new Set(t.flat(1 / 0).reverse());
      for (const i of n) r.unshift(sn(i));
    } else t !== void 0 && r.push(sn(t));
    return r;
  }
  static _$Eu(t, r) {
    const n = r.attribute;
    return n === !1 ? void 0 : typeof n == "string" ? n : typeof t == "string" ? t.toLowerCase() : void 0;
  }
  constructor() {
    super(), this._$Ep = void 0, this.isUpdatePending = !1, this.hasUpdated = !1, this._$Em = null, this._$Ev();
  }
  _$Ev() {
    this._$ES = new Promise((t) => this.enableUpdating = t), this._$AL = /* @__PURE__ */ new Map(), this._$E_(), this.requestUpdate(), this.constructor.l?.forEach((t) => t(this));
  }
  addController(t) {
    (this._$EO ??= /* @__PURE__ */ new Set()).add(t), this.renderRoot !== void 0 && this.isConnected && t.hostConnected?.();
  }
  removeController(t) {
    this._$EO?.delete(t);
  }
  _$E_() {
    const t = /* @__PURE__ */ new Map(), r = this.constructor.elementProperties;
    for (const n of r.keys()) this.hasOwnProperty(n) && (t.set(n, this[n]), delete this[n]);
    t.size > 0 && (this._$Ep = t);
  }
  createRenderRoot() {
    const t = this.shadowRoot ?? this.attachShadow(this.constructor.shadowRootOptions);
    return us(t, this.constructor.elementStyles), t;
  }
  connectedCallback() {
    this.renderRoot ??= this.createRenderRoot(), this.enableUpdating(!0), this._$EO?.forEach((t) => t.hostConnected?.());
  }
  enableUpdating(t) {
  }
  disconnectedCallback() {
    this._$EO?.forEach((t) => t.hostDisconnected?.());
  }
  attributeChangedCallback(t, r, n) {
    this._$AK(t, n);
  }
  _$EC(t, r) {
    const n = this.constructor.elementProperties.get(t), i = this.constructor._$Eu(t, n);
    if (i !== void 0 && n.reflect === !0) {
      const o = (n.converter?.toAttribute !== void 0 ? n.converter : yr).toAttribute(r, n.type);
      this._$Em = t, o == null ? this.removeAttribute(i) : this.setAttribute(i, o), this._$Em = null;
    }
  }
  _$AK(t, r) {
    const n = this.constructor, i = n._$Eh.get(t);
    if (i !== void 0 && this._$Em !== i) {
      const o = n.getPropertyOptions(i), a = typeof o.converter == "function" ? { fromAttribute: o.converter } : o.converter?.fromAttribute !== void 0 ? o.converter : yr;
      this._$Em = i, this[i] = a.fromAttribute(r, o.type), this._$Em = null;
    }
  }
  requestUpdate(t, r, n) {
    if (t !== void 0) {
      if (n ??= this.constructor.getPropertyOptions(t), !(n.hasChanged ?? Ci)(this[t], r)) return;
      this.P(t, r, n);
    }
    this.isUpdatePending === !1 && (this._$ES = this._$ET());
  }
  P(t, r, n) {
    this._$AL.has(t) || this._$AL.set(t, r), n.reflect === !0 && this._$Em !== t && (this._$Ej ??= /* @__PURE__ */ new Set()).add(t);
  }
  async _$ET() {
    this.isUpdatePending = !0;
    try {
      await this._$ES;
    } catch (r) {
      Promise.reject(r);
    }
    const t = this.scheduleUpdate();
    return t != null && await t, !this.isUpdatePending;
  }
  scheduleUpdate() {
    return this.performUpdate();
  }
  performUpdate() {
    if (!this.isUpdatePending) return;
    if (!this.hasUpdated) {
      if (this.renderRoot ??= this.createRenderRoot(), this._$Ep) {
        for (const [i, o] of this._$Ep) this[i] = o;
        this._$Ep = void 0;
      }
      const n = this.constructor.elementProperties;
      if (n.size > 0) for (const [i, o] of n) o.wrapped !== !0 || this._$AL.has(i) || this[i] === void 0 || this.P(i, this[i], o);
    }
    let t = !1;
    const r = this._$AL;
    try {
      t = this.shouldUpdate(r), t ? (this.willUpdate(r), this._$EO?.forEach((n) => n.hostUpdate?.()), this.update(r)) : this._$EU();
    } catch (n) {
      throw t = !1, this._$EU(), n;
    }
    t && this._$AE(r);
  }
  willUpdate(t) {
  }
  _$AE(t) {
    this._$EO?.forEach((r) => r.hostUpdated?.()), this.hasUpdated || (this.hasUpdated = !0, this.firstUpdated(t)), this.updated(t);
  }
  _$EU() {
    this._$AL = /* @__PURE__ */ new Map(), this.isUpdatePending = !1;
  }
  get updateComplete() {
    return this.getUpdateComplete();
  }
  getUpdateComplete() {
    return this._$ES;
  }
  shouldUpdate(t) {
    return !0;
  }
  update(t) {
    this._$Ej &&= this._$Ej.forEach((r) => this._$EC(r, this[r])), this._$EU();
  }
  updated(t) {
  }
  firstUpdated(t) {
  }
};
Pe.elementStyles = [], Pe.shadowRootOptions = { mode: "open" }, Pe[Xe("elementProperties")] = /* @__PURE__ */ new Map(), Pe[Xe("finalized")] = /* @__PURE__ */ new Map(), _s?.({ ReactiveElement: Pe }), (Qt.reactiveElementVersions ??= []).push("2.0.4");
const De = Symbol("LitMobxRenderReaction"), un = Symbol("LitMobxRequestUpdate");
function ms(e, t) {
  var r, n;
  return n = class extends e {
    constructor() {
      super(...arguments), this[r] = () => {
        this.requestUpdate();
      };
    }
    connectedCallback() {
      super.connectedCallback();
      const o = this.constructor.name || this.nodeName;
      this[De] = new t(`${o}.update()`, this[un]), this.hasUpdated && this.requestUpdate();
    }
    disconnectedCallback() {
      super.disconnectedCallback(), this[De] && (this[De].dispose(), this[De] = void 0);
    }
    update(o) {
      this[De] ? this[De].track(super.update.bind(this, o)) : super.update(o);
    }
  }, r = un, n;
}
function ys(e) {
  return ms(e, Ye);
}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const kr = globalThis, It = kr.trustedTypes, dn = It ? It.createPolicy("lit-html", { createHTML: (e) => e }) : void 0, Lr = "$lit$", Y = `lit$${(Math.random() + "").slice(9)}$`, Mr = "?" + Y, ws = `<${Mr}>`, Ee = document, it = () => Ee.createComment(""), ot = (e) => e === null || typeof e != "object" && typeof e != "function", Ti = Array.isArray, Vi = (e) => Ti(e) || typeof e?.[Symbol.iterator] == "function", ar = `[ 	
\f\r]`, He = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g, hn = /-->/g, vn = />/g, de = RegExp(`>|${ar}(?:([^\\s"'>=/]+)(${ar}*=${ar}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g"), fn = /'/g, pn = /"/g, ji = /^(?:script|style|textarea|title)$/i, Ri = (e) => (t, ...r) => ({ _$litType$: e, strings: t, values: r }), wr = Ri(1), Rl = Ri(2), le = Symbol.for("lit-noChange"), E = Symbol.for("lit-nothing"), gn = /* @__PURE__ */ new WeakMap(), pe = Ee.createTreeWalker(Ee, 129);
function ki(e, t) {
  if (!Array.isArray(e) || !e.hasOwnProperty("raw")) throw Error("invalid template strings array");
  return dn !== void 0 ? dn.createHTML(t) : t;
}
const Li = (e, t) => {
  const r = e.length - 1, n = [];
  let i, o = t === 2 ? "<svg>" : "", a = He;
  for (let l = 0; l < r; l++) {
    const s = e[l];
    let c, d, u = -1, f = 0;
    for (; f < s.length && (a.lastIndex = f, d = a.exec(s), d !== null); ) f = a.lastIndex, a === He ? d[1] === "!--" ? a = hn : d[1] !== void 0 ? a = vn : d[2] !== void 0 ? (ji.test(d[2]) && (i = RegExp("</" + d[2], "g")), a = de) : d[3] !== void 0 && (a = de) : a === de ? d[0] === ">" ? (a = i ?? He, u = -1) : d[1] === void 0 ? u = -2 : (u = a.lastIndex - d[2].length, c = d[1], a = d[3] === void 0 ? de : d[3] === '"' ? pn : fn) : a === pn || a === fn ? a = de : a === hn || a === vn ? a = He : (a = de, i = void 0);
    const p = a === de && e[l + 1].startsWith("/>") ? " " : "";
    o += a === He ? s + ws : u >= 0 ? (n.push(c), s.slice(0, u) + Lr + s.slice(u) + Y + p) : s + Y + (u === -2 ? l : p);
  }
  return [ki(e, o + (e[r] || "<?>") + (t === 2 ? "</svg>" : "")), n];
};
class at {
  constructor({ strings: t, _$litType$: r }, n) {
    let i;
    this.parts = [];
    let o = 0, a = 0;
    const l = t.length - 1, s = this.parts, [c, d] = Li(t, r);
    if (this.el = at.createElement(c, n), pe.currentNode = this.el.content, r === 2) {
      const u = this.el.content.firstChild;
      u.replaceWith(...u.childNodes);
    }
    for (; (i = pe.nextNode()) !== null && s.length < l; ) {
      if (i.nodeType === 1) {
        if (i.hasAttributes()) for (const u of i.getAttributeNames()) if (u.endsWith(Lr)) {
          const f = d[a++], p = i.getAttribute(u).split(Y), y = /([.?@])?(.*)/.exec(f);
          s.push({ type: 1, index: o, name: y[2], strings: p, ctor: y[1] === "." ? Ii : y[1] === "?" ? Ui : y[1] === "@" ? zi : pt }), i.removeAttribute(u);
        } else u.startsWith(Y) && (s.push({ type: 6, index: o }), i.removeAttribute(u));
        if (ji.test(i.tagName)) {
          const u = i.textContent.split(Y), f = u.length - 1;
          if (f > 0) {
            i.textContent = It ? It.emptyScript : "";
            for (let p = 0; p < f; p++) i.append(u[p], it()), pe.nextNode(), s.push({ type: 2, index: ++o });
            i.append(u[f], it());
          }
        }
      } else if (i.nodeType === 8) if (i.data === Mr) s.push({ type: 2, index: o });
      else {
        let u = -1;
        for (; (u = i.data.indexOf(Y, u + 1)) !== -1; ) s.push({ type: 7, index: o }), u += Y.length - 1;
      }
      o++;
    }
  }
  static createElement(t, r) {
    const n = Ee.createElement("template");
    return n.innerHTML = t, n;
  }
}
function Oe(e, t, r = e, n) {
  if (t === le) return t;
  let i = n !== void 0 ? r._$Co?.[n] : r._$Cl;
  const o = ot(t) ? void 0 : t._$litDirective$;
  return i?.constructor !== o && (i?._$AO?.(!1), o === void 0 ? i = void 0 : (i = new o(e), i._$AT(e, r, n)), n !== void 0 ? (r._$Co ??= [])[n] = i : r._$Cl = i), i !== void 0 && (t = Oe(e, i._$AS(e, t.values), i, n)), t;
}
class Mi {
  constructor(t, r) {
    this._$AV = [], this._$AN = void 0, this._$AD = t, this._$AM = r;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t) {
    const { el: { content: r }, parts: n } = this._$AD, i = (t?.creationScope ?? Ee).importNode(r, !0);
    pe.currentNode = i;
    let o = pe.nextNode(), a = 0, l = 0, s = n[0];
    for (; s !== void 0; ) {
      if (a === s.index) {
        let c;
        s.type === 2 ? c = new ze(o, o.nextSibling, this, t) : s.type === 1 ? c = new s.ctor(o, s.name, s.strings, this, t) : s.type === 6 && (c = new Bi(o, this, t)), this._$AV.push(c), s = n[++l];
      }
      a !== s?.index && (o = pe.nextNode(), a++);
    }
    return pe.currentNode = Ee, i;
  }
  p(t) {
    let r = 0;
    for (const n of this._$AV) n !== void 0 && (n.strings !== void 0 ? (n._$AI(t, n, r), r += n.strings.length - 2) : n._$AI(t[r])), r++;
  }
}
class ze {
  get _$AU() {
    return this._$AM?._$AU ?? this._$Cv;
  }
  constructor(t, r, n, i) {
    this.type = 2, this._$AH = E, this._$AN = void 0, this._$AA = t, this._$AB = r, this._$AM = n, this.options = i, this._$Cv = i?.isConnected ?? !0;
  }
  get parentNode() {
    let t = this._$AA.parentNode;
    const r = this._$AM;
    return r !== void 0 && t?.nodeType === 11 && (t = r.parentNode), t;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t, r = this) {
    t = Oe(this, t, r), ot(t) ? t === E || t == null || t === "" ? (this._$AH !== E && this._$AR(), this._$AH = E) : t !== this._$AH && t !== le && this._(t) : t._$litType$ !== void 0 ? this.$(t) : t.nodeType !== void 0 ? this.T(t) : Vi(t) ? this.k(t) : this._(t);
  }
  S(t) {
    return this._$AA.parentNode.insertBefore(t, this._$AB);
  }
  T(t) {
    this._$AH !== t && (this._$AR(), this._$AH = this.S(t));
  }
  _(t) {
    this._$AH !== E && ot(this._$AH) ? this._$AA.nextSibling.data = t : this.T(Ee.createTextNode(t)), this._$AH = t;
  }
  $(t) {
    const { values: r, _$litType$: n } = t, i = typeof n == "number" ? this._$AC(t) : (n.el === void 0 && (n.el = at.createElement(ki(n.h, n.h[0]), this.options)), n);
    if (this._$AH?._$AD === i) this._$AH.p(r);
    else {
      const o = new Mi(i, this), a = o.u(this.options);
      o.p(r), this.T(a), this._$AH = o;
    }
  }
  _$AC(t) {
    let r = gn.get(t.strings);
    return r === void 0 && gn.set(t.strings, r = new at(t)), r;
  }
  k(t) {
    Ti(this._$AH) || (this._$AH = [], this._$AR());
    const r = this._$AH;
    let n, i = 0;
    for (const o of t) i === r.length ? r.push(n = new ze(this.S(it()), this.S(it()), this, this.options)) : n = r[i], n._$AI(o), i++;
    i < r.length && (this._$AR(n && n._$AB.nextSibling, i), r.length = i);
  }
  _$AR(t = this._$AA.nextSibling, r) {
    for (this._$AP?.(!1, !0, r); t && t !== this._$AB; ) {
      const n = t.nextSibling;
      t.remove(), t = n;
    }
  }
  setConnected(t) {
    this._$AM === void 0 && (this._$Cv = t, this._$AP?.(t));
  }
}
class pt {
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  constructor(t, r, n, i, o) {
    this.type = 1, this._$AH = E, this._$AN = void 0, this.element = t, this.name = r, this._$AM = i, this.options = o, n.length > 2 || n[0] !== "" || n[1] !== "" ? (this._$AH = Array(n.length - 1).fill(new String()), this.strings = n) : this._$AH = E;
  }
  _$AI(t, r = this, n, i) {
    const o = this.strings;
    let a = !1;
    if (o === void 0) t = Oe(this, t, r, 0), a = !ot(t) || t !== this._$AH && t !== le, a && (this._$AH = t);
    else {
      const l = t;
      let s, c;
      for (t = o[0], s = 0; s < o.length - 1; s++) c = Oe(this, l[n + s], r, s), c === le && (c = this._$AH[s]), a ||= !ot(c) || c !== this._$AH[s], c === E ? t = E : t !== E && (t += (c ?? "") + o[s + 1]), this._$AH[s] = c;
    }
    a && !i && this.j(t);
  }
  j(t) {
    t === E ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t ?? "");
  }
}
class Ii extends pt {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t) {
    this.element[this.name] = t === E ? void 0 : t;
  }
}
class Ui extends pt {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t) {
    this.element.toggleAttribute(this.name, !!t && t !== E);
  }
}
class zi extends pt {
  constructor(t, r, n, i, o) {
    super(t, r, n, i, o), this.type = 5;
  }
  _$AI(t, r = this) {
    if ((t = Oe(this, t, r, 0) ?? E) === le) return;
    const n = this._$AH, i = t === E && n !== E || t.capture !== n.capture || t.once !== n.once || t.passive !== n.passive, o = t !== E && (n === E || i);
    i && this.element.removeEventListener(this.name, this, n), o && this.element.addEventListener(this.name, this, t), this._$AH = t;
  }
  handleEvent(t) {
    typeof this._$AH == "function" ? this._$AH.call(this.options?.host ?? this.element, t) : this._$AH.handleEvent(t);
  }
}
class Bi {
  constructor(t, r, n) {
    this.element = t, this.type = 6, this._$AN = void 0, this._$AM = r, this.options = n;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t) {
    Oe(this, t);
  }
}
const As = { P: Lr, A: Y, C: Mr, M: 1, L: Li, R: Mi, D: Vi, V: Oe, I: ze, H: pt, N: Ui, U: zi, B: Ii, F: Bi }, Es = kr.litHtmlPolyfillSupport;
Es?.(at, ze), (kr.litHtmlVersions ??= []).push("3.1.2");
const Os = (e, t, r) => {
  const n = r?.renderBefore ?? t;
  let i = n._$litPart$;
  if (i === void 0) {
    const o = r?.renderBefore ?? null;
    n._$litPart$ = i = new ze(t.insertBefore(it(), o), o, void 0, r ?? {});
  }
  return i._$AI(e), i;
};
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
let Je = class extends Pe {
  constructor() {
    super(...arguments), this.renderOptions = { host: this }, this._$Do = void 0;
  }
  createRenderRoot() {
    const t = super.createRenderRoot();
    return this.renderOptions.renderBefore ??= t.firstChild, t;
  }
  update(t) {
    const r = this.render();
    this.hasUpdated || (this.renderOptions.isConnected = this.isConnected), super.update(t), this._$Do = Os(r, this.renderRoot, this.renderOptions);
  }
  connectedCallback() {
    super.connectedCallback(), this._$Do?.setConnected(!0);
  }
  disconnectedCallback() {
    super.disconnectedCallback(), this._$Do?.setConnected(!1);
  }
  render() {
    return le;
  }
};
Je._$litElement$ = !0, Je.finalized = !0, globalThis.litElementHydrateSupport?.({ LitElement: Je });
const Ss = globalThis.litElementPolyfillSupport;
Ss?.({ LitElement: Je });
(globalThis.litElementVersions ??= []).push("4.0.4");
class Ns extends ys(Je) {
}
class xs extends Ns {
  constructor() {
    super(...arguments), this.disposers = [];
  }
  /**
   * Creates a MobX reaction using the given parameters and disposes it when this element is detached.
   *
   * This should be called from `connectedCallback` to ensure that the reaction is active also if the element is attached again later.
   */
  reaction(t, r, n) {
    this.disposers.push(si(t, r, n));
  }
  /**
   * Creates a MobX autorun using the given parameters and disposes it when this element is detached.
   *
   * This should be called from `connectedCallback` to ensure that the reaction is active also if the element is attached again later.
   */
  autorun(t, r) {
    this.disposers.push(oi(t, r));
  }
  disconnectedCallback() {
    super.disconnectedCallback(), this.disposers.forEach((t) => {
      t();
    }), this.disposers = [];
  }
}
class $s {
  constructor() {
    this._panels = [], this._attentionRequiredPanelTag = null, this._floatingPanelsZIndexOrder = [], Yt(this), this.restorePositions();
  }
  restorePositions() {
    const t = be.getPanelConfigurations();
    t && (this._panels = this._panels.map((r) => {
      const n = t.find((i) => i.tag === r.tag);
      return n && (r = Object.assign(r, { ...n })), r;
    }));
  }
  /**
   * Adds panelTag as last element -focused- to list.
   * @param panelConfiguration
   */
  addFocusedFloatingPanel(t) {
    this._floatingPanelsZIndexOrder = this._floatingPanelsZIndexOrder.filter((r) => r !== t.tag), t.floating && this._floatingPanelsZIndexOrder.push(t.tag);
  }
  /**
   * Returns the focused z-index of floating panel as following order
   * <ul>
   *     <li>Returns 50 for last(focused) element </li>
   *     <li>Returns the index of element in list(starting from 0) </li>
   *     <li>Returns 0 if panel is not in the list</li>
   * </ul>
   * @param panelTag
   */
  getFloatingPanelZIndex(t) {
    const r = this._floatingPanelsZIndexOrder.findIndex((n) => n === t);
    return r === this._floatingPanelsZIndexOrder.length - 1 ? 50 : r === -1 ? 0 : r;
  }
  get floatingPanelsZIndexOrder() {
    return this._floatingPanelsZIndexOrder;
  }
  get attentionRequiredPanelTag() {
    return this._attentionRequiredPanelTag;
  }
  set attentionRequiredPanelTag(t) {
    this._attentionRequiredPanelTag = t;
  }
  getAttentionRequiredPanelConfiguration() {
    return this._panels.find((t) => t.tag === this._attentionRequiredPanelTag);
  }
  clearAttention() {
    this._attentionRequiredPanelTag = null;
  }
  get panels() {
    return this._panels;
  }
  addPanel(t) {
    this._panels.push(t), this.restorePositions();
  }
  getPanelByTag(t) {
    return this._panels.find((r) => r.tag === t);
  }
  updatePanel(t, r) {
    const n = [...this._panels], i = n.find((o) => o.tag === t);
    if (i) {
      for (const o in r)
        i[o] = r[o];
      r.floating === !1 && (this._floatingPanelsZIndexOrder = this._floatingPanelsZIndexOrder.filter((o) => o !== t)), this._panels = n, be.savePanelConfigurations(this._panels);
    }
  }
  updateOrders(t) {
    const r = [...this._panels];
    r.forEach((n) => {
      const i = t.find((o) => o.tag === n.tag);
      i && (n.panelOrder = i.order);
    }), this._panels = r, be.savePanelConfigurations(r);
  }
}
const er = new $s();
let ve = [];
const bn = [];
function _n(e) {
  e.init({
    addPanel: (t) => {
      er.addPanel(t);
    },
    send(t, r) {
      ke(t, r);
    }
  });
}
function Ds() {
  ve.push(import("./copilot-log-plugin-2obnS2ov.js")), ve.push(import("./copilot-info-plugin-BD4JTXcX.js")), ve.push(import("./copilot-features-plugin-C5ocHpy1.js")), ve.push(import("./copilot-feedback-plugin-B1vtqacG.js")), ve.push(import("./copilot-shortcuts-plugin-D7SmGfb0.js"));
}
function Ps() {
  {
    const e = `https://cdn.vaadin.com/copilot/${Qa}/copilot-plugins.js`;
    import(
      /* @vite-ignore */
      e
    ).catch((t) => {
      console.warn(`Unable to load plugins from ${e}. Some Copilot features are unavailable.`, t);
    });
  }
}
function Cs() {
  Promise.all(ve).then(() => {
    const e = window.Vaadin;
    if (e.copilot.plugins) {
      const t = e.copilot.plugins;
      e.copilot.plugins.push = (r) => _n(r), Array.from(t).forEach((r) => {
        bn.includes(r) || (_n(r), bn.push(r));
      });
    }
  }), ve = [];
}
class Ts {
  constructor() {
    this.active = !1, this.activate = () => {
      this.active = !0, this.blurActiveApplicationElement();
    }, this.deactivate = () => {
      this.active = !1;
    }, this.focusInEventListener = (t) => {
      this.active && (t.preventDefault(), t.stopPropagation(), nt(t.target) || requestAnimationFrame(() => {
        t.target.blur && t.target.blur(), document.body.querySelector("copilot-main")?.focus();
      }));
    };
  }
  hostConnectedCallback() {
    const t = this.getApplicationRootElement();
    t && t instanceof HTMLElement && t.addEventListener("focusin", this.focusInEventListener);
  }
  hostDisconnectedCallback() {
    const t = this.getApplicationRootElement();
    t && t instanceof HTMLElement && t.removeEventListener("focusin", this.focusInEventListener);
  }
  getApplicationRootElement() {
    return document.body.firstElementChild;
  }
  blurActiveApplicationElement() {
    document.activeElement && document.activeElement.blur && document.activeElement.blur();
  }
}
const At = new Ts(), $ = window.Vaadin.copilot.eventbus;
if (!$)
  throw new Error("Tried to access copilot eventbus before it was initialized.");
const Fe = window.Vaadin.copilot.overlayManager, Ll = {
  AddClickListener: "Add Click Listener",
  AI: "AI",
  Delete: "Delete",
  DragAndDrop: "Drag and Drop",
  Duplicate: "Duplicate",
  SetLabel: "Set label",
  SetText: "Set text",
  SetHelper: "Set helper text",
  WrapWithTag: "Wrapping with tag",
  Alignment: "Alignment",
  Padding: "Padding",
  ModifyComponentSource: "Modify component source",
  Gap: "Gap"
}, b = window.Vaadin.copilot._uiState;
if (!b)
  throw new Error("Tried to access copilot ui state before it was initialized.");
const Ki = (e, t) => {
  ke("copilot-track-event", { event: e, value: t });
}, Hi = async (e, t, r) => window.Vaadin.copilot.comm(e, t, r);
var Fi = /* @__PURE__ */ ((e) => (e.INFORMATION = "information", e.WARNING = "warning", e.ERROR = "error", e))(Fi || {});
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const qi = { ATTRIBUTE: 1, CHILD: 2, PROPERTY: 3, BOOLEAN_ATTRIBUTE: 4, EVENT: 5, ELEMENT: 6 }, Wi = (e) => (...t) => ({ _$litDirective$: e, values: t });
let Gi = class {
  constructor(t) {
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AT(t, r, n) {
    this._$Ct = t, this._$AM = r, this._$Ci = n;
  }
  _$AS(t, r) {
    return this.update(t, r);
  }
  update(t, r) {
    return this.render(...r);
  }
};
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
let Ar = class extends Gi {
  constructor(t) {
    if (super(t), this.it = E, t.type !== qi.CHILD) throw Error(this.constructor.directiveName + "() can only be used in child bindings");
  }
  render(t) {
    if (t === E || t == null) return this._t = void 0, this.it = t;
    if (t === le) return t;
    if (typeof t != "string") throw Error(this.constructor.directiveName + "() called with a non-string value");
    if (t === this.it) return this._t;
    this.it = t;
    const r = [t];
    return r.raw = r, this._t = { _$litType$: this.constructor.resultType, strings: r, values: [] };
  }
};
Ar.directiveName = "unsafeHTML", Ar.resultType = 1;
const Ul = Wi(Ar);
function Vs() {
  return import("./copilot-notification-COJP6ks5.js");
}
function Ir(e, t) {
  Vs().then(({ showNotification: r }) => {
    r({
      type: Fi.ERROR,
      message: "Copilot internal error",
      details: e + (t ? `
${t}` : "")
    });
  }), Ki("error", `${e}
\`\`\`${t}\`\`\``);
}
class js {
  constructor() {
    this._previewActivated = !1, this._remainingTimeInMillis = -1, this._active = !1, this._configurationLoaded = !1, Yt(this);
  }
  setConfiguration(t) {
    this._previewActivated = t.previewActivated, t.previewActivated ? this._remainingTimeInMillis = t.remainingTimeInMillis : this._remainingTimeInMillis = -1, this._active = t.active, this._configurationLoaded = !0;
  }
  get previewActivated() {
    return this._previewActivated;
  }
  get remainingTimeInMillis() {
    return this._remainingTimeInMillis;
  }
  get active() {
    return this._active;
  }
  get configurationLoaded() {
    return this._configurationLoaded;
  }
  get expired() {
    return this.previewActivated && !this.active;
  }
  reset() {
    this._previewActivated = !1, this._active = !1, this._configurationLoaded = !1, this._remainingTimeInMillis = -1;
  }
  loadPreviewConfiguration() {
    Hi(`${Re}get-preview`, {}, (t) => {
      const r = t.data;
      Xi.setConfiguration(r);
    }).catch((t) => Ir("Load preview configuration failed", t));
  }
}
const Xi = new js(), Ji = () => {
  Rs().then((e) => b.setUserInfo(e)).catch((e) => Ir("Failed to load userInfo", e));
}, Rs = async () => Hi(`${Re}get-user-info`, {}, (e) => (delete e.data.reqId, e.data)), ks = async () => Di(() => b.userInfo), zl = async () => (await ks()).vaadiner;
$.on("copilot-prokey-received", (e) => {
  Ji(), e.preventDefault();
});
function Ls() {
  const e = window.navigator.userAgent;
  return e.indexOf("Windows") !== -1 ? "Windows" : e.indexOf("Mac") !== -1 ? "Mac" : e.indexOf("Linux") !== -1 ? "Linux" : null;
}
function Ms() {
  return Ls() === "Mac";
}
function Is() {
  return Ms() ? "⌘" : "Ctrl";
}
const Zi = window.Vaadin.copilot._machineState;
if (!Zi)
  throw new Error("Trying to use stored machine state before it was initialized");
function Us(e) {
  return e.composed && e.composedPath().map((t) => t.localName).some((t) => t === "copilot-spotlight");
}
function zs(e) {
  return e.composed && e.composedPath().map((t) => t.localName).some((t) => t === "copilot-drawer-panel" || t === "copilot-section-panel-wrapper");
}
let sr = !1, Et = 0;
const mn = (e) => {
  if (Zi.isActivationShortcut())
    if (e.key === "Shift" && !e.ctrlKey && !e.altKey && !e.metaKey)
      sr = !0;
    else if (sr && e.shiftKey && (e.key === "Control" || e.key === "Meta")) {
      if (Et++, Et === 2) {
        b.toggleActive("shortcut");
        return;
      }
      setTimeout(() => {
        Et = 0;
      }, 500);
    } else
      sr = !1, Et = 0;
  b.active && Bs(e);
}, Bs = (e) => {
  const t = Us(e);
  if (e.shiftKey && e.code === "Space")
    b.setSpotlightActive(!b.spotlightActive), e.stopPropagation(), e.preventDefault();
  else if (e.key === "Escape") {
    if (e.stopPropagation(), b.loginCheckActive) {
      b.setLoginCheckActive(!1);
      return;
    }
    $.emit("close-drawers", {}), b.setSpotlightActive(!1);
  } else !zs(e) && !t && Ks(e) ? ($.emit("delete-selected", {}), e.preventDefault(), e.stopPropagation()) : (e.ctrlKey || e.metaKey) && e.key === "d" && !t ? ($.emit("duplicate-selected", {}), e.preventDefault(), e.stopPropagation()) : (e.ctrlKey || e.metaKey) && e.key === "b" && !t ? ($.emit("show-selected-in-ide", {}), e.preventDefault(), e.stopPropagation()) : (e.ctrlKey || e.metaKey) && e.key === "z" && b.idePluginState?.supportedActions?.find((r) => r === "undo") && ($.emit("undoRedo", { undo: !e.shiftKey }), e.preventDefault(), e.stopPropagation());
}, Ks = (e) => (e.key === "Backspace" || e.key === "Delete") && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey, ne = Is(), Bl = {
  toggleCopilot: `<kbd>⇧</kbd> + <kbd>${ne}</kbd> <kbd>${ne}</kbd>`,
  toggleCommandWindow: "<kbd>⇧</kbd> + <kbd>Space</kbd>",
  undo: `<kbd>${ne}</kbd> + <kbd>Z</kbd>`,
  redo: `<kbd>${ne}</kbd> + <kbd>⇧</kbd> + <kbd>Z</kbd>`,
  duplicate: `<kbd>${ne}</kbd> + <kbd>D</kbd>`,
  goToSource: `<kbd>${ne}</kbd> + <kbd>B</kbd>`,
  selectParent: "<kbd>←</kbd>",
  selectPreviousSibling: "<kbd>↑</kbd>",
  selectNextSibling: "<kbd>↓</kbd>",
  delete: "<kbd>DEL</kbd>",
  copy: `<kbd>${ne}</kbd> + <kbd>C</kbd>`,
  paste: `<kbd>${ne}</kbd> + <kbd>V</kbd>`
};
/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const Yi = Symbol.for(""), Hs = (e) => {
  if (e?.r === Yi) return e?._$litStatic$;
}, Qi = (e) => ({ _$litStatic$: e, r: Yi }), yn = /* @__PURE__ */ new Map(), Fs = (e) => (t, ...r) => {
  const n = r.length;
  let i, o;
  const a = [], l = [];
  let s, c = 0, d = !1;
  for (; c < n; ) {
    for (s = t[c]; c < n && (o = r[c], (i = Hs(o)) !== void 0); ) s += i + t[++c], d = !0;
    c !== n && l.push(o), a.push(s), c++;
  }
  if (c === n && a.push(t[n]), d) {
    const u = a.join("$$lit$$");
    (t = yn.get(u)) === void 0 && (a.raw = a, yn.set(u, t = a)), r = l;
  }
  return e(t, ...r);
}, Ut = Fs(wr);
/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const { I: qs } = As, wn = () => document.createComment(""), qe = (e, t, r) => {
  const n = e._$AA.parentNode, i = t === void 0 ? e._$AB : t._$AA;
  if (r === void 0) {
    const o = n.insertBefore(wn(), i), a = n.insertBefore(wn(), i);
    r = new qs(o, a, e, e.options);
  } else {
    const o = r._$AB.nextSibling, a = r._$AM, l = a !== e;
    if (l) {
      let s;
      r._$AQ?.(e), r._$AM = e, r._$AP !== void 0 && (s = e._$AU) !== a._$AU && r._$AP(s);
    }
    if (o !== i || l) {
      let s = r._$AA;
      for (; s !== o; ) {
        const c = s.nextSibling;
        n.insertBefore(s, i), s = c;
      }
    }
  }
  return r;
}, he = (e, t, r = e) => (e._$AI(t, r), e), Ws = {}, Gs = (e, t = Ws) => e._$AH = t, Xs = (e) => e._$AH, lr = (e) => {
  e._$AP?.(!1, !0);
  let t = e._$AA;
  const r = e._$AB.nextSibling;
  for (; t !== r; ) {
    const n = t.nextSibling;
    t.remove(), t = n;
  }
};
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const An = (e, t, r) => {
  const n = /* @__PURE__ */ new Map();
  for (let i = t; i <= r; i++) n.set(e[i], i);
  return n;
}, eo = Wi(class extends Gi {
  constructor(e) {
    if (super(e), e.type !== qi.CHILD) throw Error("repeat() can only be used in text expressions");
  }
  dt(e, t, r) {
    let n;
    r === void 0 ? r = t : t !== void 0 && (n = t);
    const i = [], o = [];
    let a = 0;
    for (const l of e) i[a] = n ? n(l, a) : a, o[a] = r(l, a), a++;
    return { values: o, keys: i };
  }
  render(e, t, r) {
    return this.dt(e, t, r).values;
  }
  update(e, [t, r, n]) {
    const i = Xs(e), { values: o, keys: a } = this.dt(t, r, n);
    if (!Array.isArray(i)) return this.ut = a, o;
    const l = this.ut ??= [], s = [];
    let c, d, u = 0, f = i.length - 1, p = 0, y = o.length - 1;
    for (; u <= f && p <= y; ) if (i[u] === null) u++;
    else if (i[f] === null) f--;
    else if (l[u] === a[p]) s[p] = he(i[u], o[p]), u++, p++;
    else if (l[f] === a[y]) s[y] = he(i[f], o[y]), f--, y--;
    else if (l[u] === a[y]) s[y] = he(i[u], o[y]), qe(e, s[y + 1], i[u]), u++, y--;
    else if (l[f] === a[p]) s[p] = he(i[f], o[p]), qe(e, i[u], i[f]), f--, p++;
    else if (c === void 0 && (c = An(a, p, y), d = An(l, u, f)), c.has(l[u])) if (c.has(l[f])) {
      const m = d.get(a[p]), S = m !== void 0 ? i[m] : null;
      if (S === null) {
        const q = qe(e, i[u]);
        he(q, o[p]), s[p] = q;
      } else s[p] = he(S, o[p]), qe(e, i[u], S), i[m] = null;
      p++;
    } else lr(i[f]), f--;
    else lr(i[u]), u++;
    for (; p <= y; ) {
      const m = qe(e, s[y + 1]);
      he(m, o[p]), s[p++] = m;
    }
    for (; u <= f; ) {
      const m = i[u++];
      m !== null && lr(m);
    }
    return this.ut = a, Gs(e, s), le;
  }
}), $t = /* @__PURE__ */ new Map(), Js = (e) => {
  const r = er.panels.filter((n) => !n.floating && n.panel === e).sort((n, i) => n.panelOrder - i.panelOrder);
  return Ut`
    ${eo(
    r,
    (n) => n.tag,
    (n) => {
      const i = Qi(n.tag);
      return Ut`
                        <copilot-section-panel-wrapper panelTag="${i}">
                            <${i} slot="content"></${i}>
                        </copilot-section-panel-wrapper>`;
    }
  )}
  `;
}, Zs = () => {
  const e = er.panels;
  return Ut`
    ${eo(
    e.filter((t) => t.floating),
    (t) => t.tag,
    (t) => {
      const r = Qi(t.tag);
      return Ut`
                        <copilot-section-panel-wrapper panelTag="${r}">
                            <${r} slot="content"></${r}>
                        </copilot-section-panel-wrapper>`;
    }
  )}
  `;
}, Kl = (e) => {
  const t = e.panelTag, r = e.querySelector('[slot="content"]');
  r && $t.set(t, r);
}, Hl = (e) => {
  if ($t.has(e.panelTag)) {
    const t = $t.get(e.panelTag);
    e.querySelector('[slot="content"]').replaceWith(t);
  }
  $t.delete(e.panelTag);
};
var N = [];
for (var cr = 0; cr < 256; ++cr)
  N.push((cr + 256).toString(16).slice(1));
function Ys(e, t = 0) {
  return (N[e[t + 0]] + N[e[t + 1]] + N[e[t + 2]] + N[e[t + 3]] + "-" + N[e[t + 4]] + N[e[t + 5]] + "-" + N[e[t + 6]] + N[e[t + 7]] + "-" + N[e[t + 8]] + N[e[t + 9]] + "-" + N[e[t + 10]] + N[e[t + 11]] + N[e[t + 12]] + N[e[t + 13]] + N[e[t + 14]] + N[e[t + 15]]).toLowerCase();
}
var Ot, Qs = new Uint8Array(16);
function el() {
  if (!Ot && (Ot = typeof crypto < "u" && crypto.getRandomValues && crypto.getRandomValues.bind(crypto), !Ot))
    throw new Error("crypto.getRandomValues() not supported. See https://github.com/uuidjs/uuid#getrandomvalues-not-supported");
  return Ot(Qs);
}
var tl = typeof crypto < "u" && crypto.randomUUID && crypto.randomUUID.bind(crypto);
const En = {
  randomUUID: tl
};
function rl(e, t, r) {
  if (En.randomUUID && !t && !e)
    return En.randomUUID();
  e = e || {};
  var n = e.random || (e.rng || el)();
  if (n[6] = n[6] & 15 | 64, n[8] = n[8] & 63 | 128, t) {
    r = r || 0;
    for (var i = 0; i < 16; ++i)
      t[r + i] = n[i];
    return t;
  }
  return Ys(n);
}
const Dt = [], We = [], Fl = async (e, t, r) => {
  let n, i;
  t.reqId = rl();
  const o = new Promise((a, l) => {
    n = a, i = l;
  });
  return Dt.push({
    handleMessage(a) {
      if (a?.data?.reqId !== t.reqId)
        return !1;
      try {
        n(r(a));
      } catch (l) {
        i(l.toString());
      }
      return !0;
    }
  }), ke(e, t), o;
};
function nl(e) {
  for (const t of Dt)
    if (t.handleMessage(e))
      return Dt.splice(Dt.indexOf(t), 1), !0;
  if ($.emitUnsafe({ type: e.command, data: e.data }))
    return !0;
  for (const t of ro())
    if (to(t, e))
      return !0;
  return We.push(e), !1;
}
function to(e, t) {
  return e.handleMessage?.call(e, t);
}
function il() {
  if (We.length)
    for (const e of ro())
      for (let t = 0; t < We.length; t++)
        to(e, We[t]) && (We.splice(t, 1), t--);
}
function ro() {
  const e = document.querySelector("copilot-main");
  return e ? e.renderRoot.querySelectorAll("copilot-section-panel-wrapper *") : [];
}
const ol = ":host{--gray-h: 220;--gray-s: 30%;--gray-l: 30%;--gray-hsl: var(--gray-h) var(--gray-s) var(--gray-l);--gray: hsl(var(--gray-hsl));--gray-50: hsl(var(--gray-hsl) / .05);--gray-100: hsl(var(--gray-hsl) / .1);--gray-150: hsl(var(--gray-hsl) / .16);--gray-200: hsl(var(--gray-hsl) / .24);--gray-250: hsl(var(--gray-hsl) / .34);--gray-300: hsl(var(--gray-hsl) / .46);--gray-350: hsl(var(--gray-hsl) / .6);--gray-400: hsl(var(--gray-hsl) / .7);--gray-450: hsl(var(--gray-hsl) / .8);--gray-500: hsl(var(--gray-hsl) / .9);--gray-550: hsl(var(--gray-hsl));--gray-600: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 2%));--gray-650: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 4%));--gray-700: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 8%));--gray-750: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 12%));--gray-800: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 20%));--gray-850: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 23%));--gray-900: hsl(var(--gray-h) var(--gray-s) calc(var(--gray-l) - 30%));--blue-h: 220;--blue-s: 90%;--blue-l: 53%;--blue-hsl: var(--blue-h) var(--blue-s) var(--blue-l);--blue: hsl(var(--blue-hsl));--blue-50: hsl(var(--blue-hsl) / .05);--blue-100: hsl(var(--blue-hsl) / .1);--blue-150: hsl(var(--blue-hsl) / .2);--blue-200: hsl(var(--blue-hsl) / .3);--blue-250: hsl(var(--blue-hsl) / .4);--blue-300: hsl(var(--blue-hsl) / .5);--blue-350: hsl(var(--blue-hsl) / .6);--blue-400: hsl(var(--blue-hsl) / .7);--blue-450: hsl(var(--blue-hsl) / .8);--blue-500: hsl(var(--blue-hsl) / .9);--blue-550: hsl(var(--blue-hsl));--blue-600: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 4%));--blue-650: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 8%));--blue-700: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 12%));--blue-750: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 15%));--blue-800: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 18%));--blue-850: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 24%));--blue-900: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) - 27%));--purple-h: 246;--purple-s: 90%;--purple-l: 60%;--purple-hsl: var(--purple-h) var(--purple-s) var(--purple-l);--purple: hsl(var(--purple-hsl));--purple-50: hsl(var(--purple-hsl) / .05);--purple-100: hsl(var(--purple-hsl) / .1);--purple-150: hsl(var(--purple-hsl) / .2);--purple-200: hsl(var(--purple-hsl) / .3);--purple-250: hsl(var(--purple-hsl) / .4);--purple-300: hsl(var(--purple-hsl) / .5);--purple-350: hsl(var(--purple-hsl) / .6);--purple-400: hsl(var(--purple-hsl) / .7);--purple-450: hsl(var(--purple-hsl) / .8);--purple-500: hsl(var(--purple-hsl) / .9);--purple-550: hsl(var(--purple-hsl));--purple-600: hsl(var(--purple-h) calc(var(--purple-s) - 4%) calc(var(--purple-l) - 2%));--purple-650: hsl(var(--purple-h) calc(var(--purple-s) - 8%) calc(var(--purple-l) - 4%));--purple-700: hsl(var(--purple-h) calc(var(--purple-s) - 15%) calc(var(--purple-l) - 7%));--purple-750: hsl(var(--purple-h) calc(var(--purple-s) - 23%) calc(var(--purple-l) - 11%));--purple-800: hsl(var(--purple-h) calc(var(--purple-s) - 24%) calc(var(--purple-l) - 15%));--purple-850: hsl(var(--purple-h) calc(var(--purple-s) - 24%) calc(var(--purple-l) - 19%));--purple-900: hsl(var(--purple-h) calc(var(--purple-s) - 27%) calc(var(--purple-l) - 23%));--green-h: 150;--green-s: 80%;--green-l: 42%;--green-hsl: var(--green-h) var(--green-s) var(--green-l);--green: hsl(var(--green-hsl));--green-50: hsl(var(--green-hsl) / .05);--green-100: hsl(var(--green-hsl) / .1);--green-150: hsl(var(--green-hsl) / .2);--green-200: hsl(var(--green-hsl) / .3);--green-250: hsl(var(--green-hsl) / .4);--green-300: hsl(var(--green-hsl) / .5);--green-350: hsl(var(--green-hsl) / .6);--green-400: hsl(var(--green-hsl) / .7);--green-450: hsl(var(--green-hsl) / .8);--green-500: hsl(var(--green-hsl) / .9);--green-550: hsl(var(--green-hsl));--green-600: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 2%));--green-650: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 4%));--green-700: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 8%));--green-750: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 12%));--green-800: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 15%));--green-850: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 19%));--green-900: hsl(var(--green-h) var(--green-s) calc(var(--green-l) - 23%));--yellow-h: 38;--yellow-s: 98%;--yellow-l: 64%;--yellow-hsl: var(--yellow-h) var(--yellow-s) var(--yellow-l);--yellow: hsl(var(--yellow-hsl));--yellow-50: hsl(var(--yellow-hsl) / .07);--yellow-100: hsl(var(--yellow-hsl) / .12);--yellow-150: hsl(var(--yellow-hsl) / .2);--yellow-200: hsl(var(--yellow-hsl) / .3);--yellow-250: hsl(var(--yellow-hsl) / .4);--yellow-300: hsl(var(--yellow-hsl) / .5);--yellow-350: hsl(var(--yellow-hsl) / .6);--yellow-400: hsl(var(--yellow-hsl) / .7);--yellow-450: hsl(var(--yellow-hsl) / .8);--yellow-500: hsl(var(--yellow-hsl) / .9);--yellow-550: hsl(var(--yellow-hsl));--yellow-600: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 5%));--yellow-650: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 10%));--yellow-700: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 15%));--yellow-750: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 20%));--yellow-800: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 25%));--yellow-850: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 30%));--yellow-900: hsl(var(--yellow-h) var(--yellow-s) calc(var(--yellow-l) - 35%));--red-h: 355;--red-s: 75%;--red-l: 55%;--red-hsl: var(--red-h) var(--red-s) var(--red-l);--red: hsl(var(--red-hsl));--red-50: hsl(var(--red-hsl) / .05);--red-100: hsl(var(--red-hsl) / .1);--red-150: hsl(var(--red-hsl) / .2);--red-200: hsl(var(--red-hsl) / .3);--red-250: hsl(var(--red-hsl) / .4);--red-300: hsl(var(--red-hsl) / .5);--red-350: hsl(var(--red-hsl) / .6);--red-400: hsl(var(--red-hsl) / .7);--red-450: hsl(var(--red-hsl) / .8);--red-500: hsl(var(--red-hsl) / .9);--red-550: hsl(var(--red-hsl));--red-600: hsl(var(--red-h) calc(var(--red-s) - 5%) calc(var(--red-l) - 2%));--red-650: hsl(var(--red-h) calc(var(--red-s) - 10%) calc(var(--red-l) - 4%));--red-700: hsl(var(--red-h) calc(var(--red-s) - 15%) calc(var(--red-l) - 8%));--red-750: hsl(var(--red-h) calc(var(--red-s) - 20%) calc(var(--red-l) - 12%));--red-800: hsl(var(--red-h) calc(var(--red-s) - 25%) calc(var(--red-l) - 15%));--red-850: hsl(var(--red-h) calc(var(--red-s) - 30%) calc(var(--red-l) - 19%));--red-900: hsl(var(--red-h) calc(var(--red-s) - 35%) calc(var(--red-l) - 23%));--codeblock-bg: #f4f4f4;--vaadin-logo-blue: #00B4F0}:host(.dark){--gray-s: 15%;--gray-l: 70%;--gray-600: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 6%));--gray-650: hsl(var(--gray-h) calc(var(--gray-s) - 5%) calc(var(--gray-l) + 14%));--gray-700: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 26%));--gray-750: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 36%));--gray-800: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 48%));--gray-850: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 62%));--gray-900: hsl(var(--gray-h) calc(var(--gray-s) - 2%) calc(var(--gray-l) + 70%));--blue-s: 90%;--blue-l: 58%;--blue-600: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 6%));--blue-650: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 12%));--blue-700: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 17%));--blue-750: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 22%));--blue-800: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 28%));--blue-850: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 35%));--blue-900: hsl(var(--blue-h) var(--blue-s) calc(var(--blue-l) + 43%));--purple-600: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 4%));--purple-650: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 9%));--purple-700: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 12%));--purple-750: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 18%));--purple-800: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 24%));--purple-850: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 29%));--purple-900: hsl(var(--purple-h) var(--purple-s) calc(var(--purple-l) + 33%));--green-600: hsl(calc(var(--green-h) - 1) calc(var(--green-s) - 5%) calc(var(--green-l) + 5%));--green-650: hsl(calc(var(--green-h) - 2) calc(var(--green-s) - 10%) calc(var(--green-l) + 12%));--green-700: hsl(calc(var(--green-h) - 4) calc(var(--green-s) - 15%) calc(var(--green-l) + 20%));--green-750: hsl(calc(var(--green-h) - 6) calc(var(--green-s) - 20%) calc(var(--green-l) + 29%));--green-800: hsl(calc(var(--green-h) - 8) calc(var(--green-s) - 25%) calc(var(--green-l) + 37%));--green-850: hsl(calc(var(--green-h) - 10) calc(var(--green-s) - 30%) calc(var(--green-l) + 42%));--green-900: hsl(calc(var(--green-h) - 12) calc(var(--green-s) - 35%) calc(var(--green-l) + 48%));--yellow-600: hsl(calc(var(--yellow-h) + 1) var(--yellow-s) calc(var(--yellow-l) + 4%));--yellow-650: hsl(calc(var(--yellow-h) + 2) var(--yellow-s) calc(var(--yellow-l) + 7%));--yellow-700: hsl(calc(var(--yellow-h) + 4) var(--yellow-s) calc(var(--yellow-l) + 11%));--yellow-750: hsl(calc(var(--yellow-h) + 6) var(--yellow-s) calc(var(--yellow-l) + 16%));--yellow-800: hsl(calc(var(--yellow-h) + 8) var(--yellow-s) calc(var(--yellow-l) + 20%));--yellow-850: hsl(calc(var(--yellow-h) + 10) var(--yellow-s) calc(var(--yellow-l) + 24%));--yellow-900: hsl(calc(var(--yellow-h) + 12) var(--yellow-s) calc(var(--yellow-l) + 29%));--red-600: hsl(calc(var(--red-h) - 1) calc(var(--red-s) - 5%) calc(var(--red-l) + 3%));--red-650: hsl(calc(var(--red-h) - 2) calc(var(--red-s) - 10%) calc(var(--red-l) + 7%));--red-700: hsl(calc(var(--red-h) - 4) calc(var(--red-s) - 15%) calc(var(--red-l) + 14%));--red-750: hsl(calc(var(--red-h) - 6) calc(var(--red-s) - 20%) calc(var(--red-l) + 19%));--red-800: hsl(calc(var(--red-h) - 8) calc(var(--red-s) - 25%) calc(var(--red-l) + 24%));--red-850: hsl(calc(var(--red-h) - 10) calc(var(--red-s) - 30%) calc(var(--red-l) + 30%));--red-900: hsl(calc(var(--red-h) - 12) calc(var(--red-s) - 35%) calc(var(--red-l) + 36%));--codeblock-bg: var(--gray-100)}", al = ":host{--font-family: Inter, system-ui, ui-sans-serif, -apple-system, BlinkMacSystemFont, sans-serif;--monospace-font-family: Inconsolata, Monaco, Consolas, Courier New, Courier, monospace;--font-size-0: .6875rem;--font-size-1: .75rem;--font-size-2: .875rem;--font-size-3: 1rem;--font-size-4: 1.125rem;--font-size-5: 1.25rem;--font-size-6: 1.375rem;--font-size-7: 1.5rem;--line-height-1: 1.125rem;--line-height-2: 1.25rem;--line-height-3: 1.5rem;--line-height-4: 1.75rem;--line-height-5: 2rem;--line-height-6: 2.25rem;--line-height-7: 2.5rem;--font-weight-bold: 500;--font-weight-strong: 600;--font: normal 400 var(--font-size-3) / var(--line-height-3) var(--font-family);--font-bold: normal var(--font-weight-bold) var(--font-size-3) / var(--line-height-3) var(--font-family);--font-strong: normal var(--font-weight-strong) var(--font-size-3) / var(--line-height-3) var(--font-family);--font-small: normal 400 var(--font-size-2) / var(--line-height-2) var(--font-family);--font-small-bold: normal var(--font-weight-bold) var(--font-size-2) / var(--line-height-2) var(--font-family);--font-small-strong: normal var(--font-weight-strong) var(--font-size-2) / var(--line-height-2) var(--font-family);--font-xsmall: normal 400 var(--font-size-1) / var(--line-height-1) var(--font-family);--font-xsmall-bold: normal var(--font-weight-bold) var(--font-size-1) / var(--line-height-1) var(--font-family);--font-xsmall-strong: normal var(--font-weight-strong) var(--font-size-1) / var(--line-height-1) var(--font-family);--font-button: normal var(--font-weight-bold) var(--font-size-1) / var(--line-height-1) var(--font-family);--font-tooltip: normal var(--font-weight-bold) var(--font-size-1) / var(--line-height-2) var(--font-family);--radius-1: .1875rem;--radius-2: .375rem;--radius-3: .75rem;--space-25: 2px;--space-50: 4px;--space-75: 6px;--space-100: 8px;--space-150: 12px;--space-200: 16px;--space-300: 24px;--space-400: 32px;--space-500: 40px;--space-600: 48px;--space-700: 56px;--space-800: 64px;--space-900: 72px;--z-index-component-selector: 100;--z-index-floating-panel: 101;--z-index-drawer: 150;--z-index-opened-drawer: 151;--z-index-spotlight: 200;--z-index-popover: 300;--z-index-activation-button: 1000;--duration-1: .1s;--duration-2: .2s;--duration-3: .3s;--duration-4: .4s;--button-background: var(--gray-100);--button-background-hover: var(--gray-150)}:host{--lumo-font-family: var(--font-family);--lumo-font-size-xs: var(--font-size-1);--lumo-font-size-s: var(--font-size-2);--lumo-font-size-m: var(--font-size-3);--lumo-font-size-l: var(--font-size-4);--lumo-font-size-xl: var(--font-size-5);--lumo-font-size-xxl: var(--font-size-6);--lumo-font-size-xxxl: var(--font-size-7);--lumo-line-height-s: var(--line-height-2);--lumo-line-height-m: var(--line-height-3);--lumo-line-height-l: var(--line-height-4);--lumo-border-radius-s: var(--radius-1);--lumo-border-radius-m: var(--radius-2);--lumo-border-radius-l: var(--radius-3);--lumo-base-color: var(--surface-0);--lumo-body-text-color: var(--color-high-contrast);--lumo-header-text-color: var(--color-high-contrast);--lumo-secondary-text-color: var(--color);--lumo-tertiary-text-color: var(--color);--lumo-error-text-color: var(--color-danger);--lumo-primary-text-color: var(--color-high-contrast);--lumo-primary-color: var(--background-button-primary);--lumo-primary-color-50pct: var(--color-accent);--lumo-primary-contrast-color: var(--lumo-secondary-text-color);--lumo-space-xs: var(--space-50);--lumo-space-s: var(--space-100);--lumo-space-m: var(--space-200);--lumo-space-l: var(--space-300);--lumo-space-xl: var(--space-500);--lumo-icon-size-xs: var(--font-size-1);--lumo-icon-size-s: var(--font-size-2);--lumo-icon-size-m: var(--font-size-3);--lumo-icon-size-l: var(--font-size-4);--lumo-icon-size-xl: var(--font-size-5)}:host{color-scheme:light;--surface-0: hsl(var(--gray-h) var(--gray-s) 90% / .8);--surface-1: hsl(var(--gray-h) var(--gray-s) 95% / .8);--surface-2: hsl(var(--gray-h) var(--gray-s) 100% / .8);--surface-background: linear-gradient( hsl(var(--gray-h) var(--gray-s) 95% / .7), hsl(var(--gray-h) var(--gray-s) 95% / .65) );--surface-glow: radial-gradient(circle at 30% 0%, hsl(var(--gray-h) var(--gray-s) 98% / .7), transparent 50%);--surface-border-glow: radial-gradient(at 50% 50%, hsl(var(--purple-h) 90% 90% / .8) 0, transparent 50%);--surface: var(--surface-glow) no-repeat border-box, var(--surface-background) no-repeat padding-box, hsl(var(--gray-h) var(--gray-s) 98% / .2);--surface-with-border-glow: var(--surface-glow) no-repeat border-box, var(--surface-background) no-repeat padding-box, var(--surface-border-glow) no-repeat border-box 0 0 / var(--glow-size, 600px) var(--glow-size, 600px);--surface-border-color: hsl(var(--gray-h) var(--gray-s) 100% / .7);--surface-backdrop-filter: blur(10px);--surface-box-shadow-1: 0 0 0 .5px hsl(var(--gray-h) var(--gray-s) 5% / .15), 0 6px 12px -1px hsl(var(--shadow-hsl) / .3);--surface-box-shadow-2: 0 0 0 .5px hsl(var(--gray-h) var(--gray-s) 5% / .15), 0 24px 40px -4px hsl(var(--shadow-hsl) / .4);--background-button: linear-gradient( hsl(var(--gray-h) var(--gray-s) 98% / .4), hsl(var(--gray-h) var(--gray-s) 90% / .2) );--background-button-active: hsl(var(--gray-h) var(--gray-s) 80% / .2);--color: var(--gray-500);--color-high-contrast: var(--gray-900);--color-accent: var(--purple-700);--color-danger: var(--red-700);--border-color: var(--gray-150);--border-color-high-contrast: var(--gray-300);--border-color-button: var(--gray-350);--border-color-popover: hsl(var(--gray-hsl) / .08);--border-color-dialog: hsl(var(--gray-hsl) / .08);--accent-color: var(--purple-600);--selection-color: hsl(var(--blue-hsl));--shadow-hsl: var(--gray-h) var(--gray-s) 20%;--lumo-contrast-5pct: var(--gray-100);--lumo-contrast-10pct: var(--gray-200);--lumo-contrast-60pct: var(--gray-400);--lumo-contrast-80pct: var(--gray-600);--lumo-contrast-90pct: var(--gray-800);--card-bg: rgba(255, 255, 255, .5);--card-hover-bg: rgba(255, 255, 255, .65);--card-open-bg: rgba(255, 255, 255, .8);--card-border: 1px solid rgba(0, 50, 100, .15);--card-open-shadow: 0px 1px 4px -1px rgba(28, 52, 84, .26);--card-section-border: var(--card-border);--card-field-bg: var(--lumo-contrast-5pct)}:host(.dark){color-scheme:dark;--surface-0: hsl(var(--gray-h) var(--gray-s) 10% / .85);--surface-1: hsl(var(--gray-h) var(--gray-s) 14% / .85);--surface-2: hsl(var(--gray-h) var(--gray-s) 18% / .85);--surface-background: linear-gradient( hsl(var(--gray-h) var(--gray-s) 8% / .65), hsl(var(--gray-h) var(--gray-s) 8% / .7) );--surface-glow: radial-gradient( circle at 30% 0%, hsl(var(--gray-h) calc(var(--gray-s) * 2) 90% / .12), transparent 50% );--surface: var(--surface-glow) no-repeat border-box, var(--surface-background) no-repeat padding-box, hsl(var(--gray-h) var(--gray-s) 20% / .4);--surface-border-glow: hsl(var(--gray-h) var(--gray-s) 20% / .4) radial-gradient(at 50% 50%, hsl(250 40% 80% / .4) 0, transparent 50%);--surface-border-color: hsl(var(--gray-h) var(--gray-s) 50% / .2);--surface-box-shadow-1: 0 0 0 .5px hsl(var(--purple-h) 40% 5% / .4), 0 6px 12px -1px hsl(var(--shadow-hsl) / .4);--surface-box-shadow-2: 0 0 0 .5px hsl(var(--purple-h) 40% 5% / .4), 0 24px 40px -4px hsl(var(--shadow-hsl) / .5);--color: var(--gray-650);--background-button: linear-gradient( hsl(var(--gray-h) calc(var(--gray-s) * 2) 80% / .1), hsl(var(--gray-h) calc(var(--gray-s) * 2) 80% / 0) );--background-button-active: hsl(var(--gray-h) var(--gray-s) 10% / .1);--border-color-popover: hsl(var(--gray-h) var(--gray-s) 90% / .1);--border-color-dialog: hsl(var(--gray-h) var(--gray-s) 90% / .1);--shadow-hsl: 0 0% 0%;--lumo-disabled-text-color: var(--lumo-contrast-60pct);--card-bg: rgba(255, 255, 255, .05);--card-hover-bg: rgba(255, 255, 255, .065);--card-open-bg: rgba(255, 255, 255, .1);--card-border: 1px solid rgba(255, 255, 255, .11);--card-open-shadow: 0px 1px 4px -1px rgba(0, 0, 0, .26);--card-section-border: var(--card-border);--card-field-bg: var(--lumo-contrast-10pct)}", sl = "button{-webkit-appearance:none;appearance:none;background:var(--background-button);background-origin:border-box;font:var(--font-button);color:var(--color-high-contrast);border:1px solid var(--border-color);border-radius:var(--radius-2);padding:var(--space-25) var(--space-100)}button:focus-visible{outline:2px solid var(--blue-500);outline-offset:2px}button:active:not(:disabled){background:var(--background-button-active)}button:disabled{color:var(--gray-400);background:transparent}", ll = ":is(vaadin-context-menu-overlay,vaadin-select-overlay,vaadin-menu-bar-overlay){z-index:var(--z-index-popover)}:is(vaadin-context-menu-overlay,vaadin-select-overlay,vaadin-menu-bar-overlay):first-of-type{padding-top:0}:is(vaadin-context-menu-overlay,vaadin-select-overlay,vaadin-menu-bar-overlay)::part(overlay){color:inherit;font:inherit;background:var(--surface);-webkit-backdrop-filter:var(--surface-backdrop-filter);backdrop-filter:var(--surface-backdrop-filter);border-radius:var(--radius-2);border:1px solid var(--surface-border-color);box-shadow:var(--surface-box-shadow-1)}:is(vaadin-context-menu-overlay,vaadin-select-overlay,vaadin-menu-bar-overlay)::part(content){padding:var(--space-50)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item){color:var(--color-high-contrast);font:var(--font-small);display:flex;align-items:center;cursor:default;padding:var(--space-75) var(--space-100);min-height:0;border-radius:var(--radius-1);--_lumo-item-selected-icon-display: none}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item)[disabled],:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item)[disabled] .hint,:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item)[disabled] vaadin-icon{color:var(--lumo-disabled-text-color)}:is(vaadin-context-menu-item,vaadin-menu-bar-item)[expanded]{background:var(--gray-200)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item):not([disabled]):hover{background:var(--color-high-contrast);color:var(--surface-2);--lumo-tertiary-text-color: var(--surface-2);--color: currentColor;--border-color: var(--surface-0)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item)[focus-ring]{outline:2px solid var(--selection-color);outline-offset:-2px}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item):is([aria-haspopup=true]):after{margin-inline-end:calc(var(--space-200) * -1);margin-right:unset}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item).danger{color:var(--color-danger);--color: currentColor}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item).danger:not([disabled]):hover{background-color:var(--color-danger)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item)::part(content){display:flex;align-items:center;gap:var(--space-100)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item,vaadin-combo-box-item) vaadin-icon{width:1em;height:1em;padding:0;color:var(--color)}:is(vaadin-context-menu-overlay,vaadin-select-overlay,vaadin-menu-bar-overlay) hr{margin:var(--space-50)}:is(vaadin-context-menu-item,vaadin-select-item,vaadin-menu-bar-item) .label{padding-inline-end:var(--space-300)}:is(vaadin-context-menu-item,vaadin-select-item,vaadin-menu-bar-item) .hint{margin-inline-start:auto;color:var(--color)}:is(vaadin-context-menu-item,vaadin-menu-bar-item,vaadin-select-item) kbd{display:inline-block;border-radius:var(--radius-1);border:1px solid var(--border-color);min-width:1em;min-height:1em;text-align:center;margin:0 .1em;padding:.1em .25em;box-sizing:border-box;font-size:var(--font-size-1);font-family:var(--font-family);line-height:1}:is(copilot-alignment-overlay)::part(content){padding:0}:is(.padding-values-overlay){--lumo-base-color: var(--selection-color);--color-high-contrast: white}:is(.padding-values-overlay) vaadin-combo-box-item:hover{color:#272c35d9}", cl = "code.codeblock{background:var(--codeblock-bg);border-radius:var(--radius-2);display:block;font-family:var(--monospace-font-family);font-size:var(--font-size-1);line-height:var(--line-height-1);overflow:hidden;padding:.3125rem 1.75rem .3125rem var(--space-100);position:relative;text-overflow:ellipsis;white-space:pre}copilot-copy{position:absolute;right:0;top:0}copilot-copy button{align-items:center;background:none;border:1px solid transparent;border-radius:var(--radius-2);color:var(--color);display:flex;font:var(--font-button);height:1.75rem;justify-content:center;padding:0;width:1.75rem}copilot-copy button:hover{color:var(--color-high-contrast)}", ul = "vaadin-dialog-overlay::part(overlay){background:#fff}vaadin-dialog-overlay::part(content){background:var(--surface);font:var(--font-xsmall);padding:var(--space-300)}vaadin-dialog-overlay::part(header){background:var(--surface);font:var(--font-xsmall-strong);border-bottom:1px solid var(--border-color);padding:var(--space-100) var(--space-150)}vaadin-dialog-overlay::part(footer){background:var(--surface);padding:var(--space-150)}vaadin-dialog-overlay::part(header-content){display:flex;line-height:normal;justify-content:space-between;width:100%;align-items:center}vaadin-dialog-overlay [slot=header-content] h2{margin:0;padding:0;font:var(--font-small-bold)}vaadin-dialog-overlay [slot=header-content] .close{line-height:0}vaadin-dialog-overlay{--vaadin-button-font-size: var(--font-size-1);--vaadin-button-height: var(--line-height-4)}vaadin-dialog-overlay vaadin-button[theme~=primary]{background-color:hsl(var(--blue-hsl))}vaadin-dialog-overlay a svg{height:12px;width:12px}.dialog-footer vaadin-button{--vaadin-button-primary-background: var(--button-background);--vaadin-button-border-radius: var(--radius-1);--vaadin-button-primary-text-color: var(--color-high-contrast);--vaadin-button-height: var(--line-height-5);font:var(--font-small-bold)}.dialog-footer vaadin-button span[slot=suffix]{display:flex}.dialog-footer vaadin-button span[slot=suffix] svg{height:14px;width:14px}", dl = ":host{--vaadin-input-field-label-font-size: var(--font-size-1);--vaadin-select-label-font-size: var(--font-size-1);--vaadin-input-field-helper-font-size: var(--font-size-0);--vaadin-button-font-size: var(--font-size-2);--vaadin-checkbox-label-font-size: var(--font-size-1);--vaadin-input-field-background: var(--lumo-contrast-10pct);--vaadin-input-field-height: 26px;--vaadin-input-field-value-font-size: var(--font-xsmall)}";
var ql = typeof globalThis < "u" ? globalThis : typeof window < "u" ? window : typeof global < "u" ? global : typeof self < "u" ? self : {};
function hl(e) {
  return e && e.__esModule && Object.prototype.hasOwnProperty.call(e, "default") ? e.default : e;
}
function Wl(e) {
  if (e.__esModule) return e;
  var t = e.default;
  if (typeof t == "function") {
    var r = function n() {
      return this instanceof n ? Reflect.construct(t, arguments, this.constructor) : t.apply(this, arguments);
    };
    r.prototype = t.prototype;
  } else r = {};
  return Object.defineProperty(r, "__esModule", { value: !0 }), Object.keys(e).forEach(function(n) {
    var i = Object.getOwnPropertyDescriptor(e, n);
    Object.defineProperty(r, n, i.get ? i : {
      enumerable: !0,
      get: function() {
        return e[n];
      }
    });
  }), r;
}
var Ur = { exports: {} };
function no(e, t = 100, r = {}) {
  if (typeof e != "function")
    throw new TypeError(`Expected the first parameter to be a function, got \`${typeof e}\`.`);
  if (t < 0)
    throw new RangeError("`wait` must not be negative.");
  const { immediate: n } = typeof r == "boolean" ? { immediate: r } : r;
  let i, o, a, l, s;
  function c() {
    const f = i, p = o;
    return i = void 0, o = void 0, s = e.apply(f, p), s;
  }
  function d() {
    const f = Date.now() - l;
    f < t && f >= 0 ? a = setTimeout(d, t - f) : (a = void 0, n || (s = c()));
  }
  const u = function(...f) {
    if (i && this !== i)
      throw new Error("Debounced method called with different contexts.");
    i = this, o = f, l = Date.now();
    const p = n && !a;
    return a || (a = setTimeout(d, t)), p && (s = c()), s;
  };
  return u.clear = () => {
    a && (clearTimeout(a), a = void 0);
  }, u.flush = () => {
    a && u.trigger();
  }, u.trigger = () => {
    s = c(), u.clear();
  }, u;
}
Ur.exports.debounce = no;
Ur.exports = no;
var vl = Ur.exports;
const fl = /* @__PURE__ */ hl(vl);
class pl {
  constructor() {
    this.documentActive = !0, this.addListeners = () => {
      window.addEventListener("pageshow", this.handleWindowVisibilityChange), window.addEventListener("pagehide", this.handleWindowVisibilityChange), window.addEventListener("focus", this.handleWindowFocusChange), window.addEventListener("blur", this.handleWindowFocusChange), document.addEventListener("visibilitychange", this.handleDocumentVisibilityChange);
    }, this.removeListeners = () => {
      window.removeEventListener("pageshow", this.handleWindowVisibilityChange), window.removeEventListener("pagehide", this.handleWindowVisibilityChange), window.removeEventListener("focus", this.handleWindowFocusChange), window.removeEventListener("blur", this.handleWindowFocusChange), document.removeEventListener("visibilitychange", this.handleDocumentVisibilityChange);
    }, this.handleWindowVisibilityChange = (t) => {
      t.type === "pageshow" ? this.dispatch(!0) : this.dispatch(!1);
    }, this.handleWindowFocusChange = (t) => {
      t.type === "focus" ? this.dispatch(!0) : this.dispatch(!1);
    }, this.handleDocumentVisibilityChange = () => {
      this.dispatch(!document.hidden);
    }, this.dispatch = (t) => {
      if (t !== this.documentActive) {
        const r = window.Vaadin.copilot.eventbus;
        this.documentActive = t, r.emit("document-activation-change", { active: this.documentActive });
      }
    };
  }
  copilotActivated() {
    this.addListeners();
  }
  copilotDeactivated() {
    this.removeListeners();
  }
}
const On = new pl();
var gl = Object.defineProperty, bl = Object.getOwnPropertyDescriptor, _l = (e, t, r, n) => {
  for (var i = n > 1 ? void 0 : n ? bl(t, r) : t, o = e.length - 1, a; o >= 0; o--)
    (a = e[o]) && (i = (n ? a(t, r, i) : a(i)) || i);
  return n && i && gl(t, r, i), i;
};
let Sn = class extends xs {
  constructor() {
    super(...arguments), this.removers = [], this.initialized = !1, this.toggleOperationInProgressAttr = () => {
      this.toggleAttribute("operation-in-progress", b.operationWaitsHmrUpdate !== void 0);
    }, this.operationInProgressCursorUpdateDebounceFunc = fl(this.toggleOperationInProgressAttr, 500), this.overlayOutsideClickListener = (e) => {
      nt(e.target?.owner) || (b.active || nt(e.detail.sourceEvent.target)) && e.preventDefault();
    };
  }
  static get styles() {
    return [
      ie(ol),
      ie(al),
      ie(sl),
      ie(ll),
      ie(cl),
      ie(ul),
      ie(dl),
      cs`
        :host {
          position: fixed;
          inset: 0;
          z-index: 9999;
          contain: strict;
          font: var(--font-small);
          color: var(--color);
          pointer-events: all;
          cursor: var(--cursor, default);
        }

        :host([operation-in-progress]) {
          --cursor: wait;
          --lumo-clickable-cursor: wait;
        }

        :host(:not([active])) {
          visibility: hidden !important;
          pointer-events: none;
        }

        /* Hide floating panels when not active */

        :host(:not([active])) > copilot-section-panel-wrapper {
          display: none !important;
        }

        /* Keep activation button and menu visible */

        copilot-activation-button,
        .activation-button-menu {
          visibility: visible;
        }

        copilot-activation-button {
          pointer-events: auto;
        }

        a {
          color: var(--blue-600);
          text-decoration-color: var(--blue-200);
        }

        :host([user-select-none]) {
          -webkit-touch-callout: none;
          -webkit-user-select: none;
          -moz-user-select: none;
          -ms-user-select: none;
          user-select: none;
        }

        /* Needed to prevent a JS error because of monkey patched '_attachOverlay'. It is some scope issue, */
        /* where 'this._placeholder.parentNode' is undefined - the scope if 'this' gets messed up at some point. */
        /* We also don't want animations on the overlays to make the feel faster, so this is fine. */

        :is(
            vaadin-context-menu-overlay,
            vaadin-menu-bar-overlay,
            vaadin-select-overlay,
            vaadin-combo-box-overlay,
            vaadin-tooltip-overlay
          ):is([opening], [closing]),
        :is(
            vaadin-context-menu-overlay,
            vaadin-menu-bar-overlay,
            vaadin-select-overlay,
            vaadin-combo-box-overlay,
            vaadin-tooltip-overlay
          )::part(overlay) {
          animation: none !important;
        }

        :host(:not([active])) copilot-drawer-panel::before {
          animation: none;
        }

        /* Workaround for https://github.com/vaadin/web-components/issues/5400 */

        :host([active]) .activation-button-menu .activate,
        :host(:not([active])) .activation-button-menu .deactivate,
        :host(:not([active])) .activation-button-menu .toggle-spotlight {
          display: none;
        }
      `
    ];
  }
  connectedCallback() {
    super.connectedCallback(), this.init().catch((e) => Ir("Unable to initialize copilot", e));
  }
  async init() {
    if (this.initialized)
      return;
    await window.Vaadin.copilot._machineState.initializer.promise, document.body.style.setProperty("--dev-tools-button-display", "none"), await import("./copilot-global-vars-later-Bg313HPk.js"), await import("./copilot-init-step2-wwj70Eqk.js"), Ds(), this.tabIndex = 0, At.hostConnectedCallback(), window.addEventListener("keydown", mn), $.onSend(this.handleSendEvent), this.removers.push($.on("close-drawers", this.closeDrawers.bind(this))), this.removers.push(
      $.on("open-attention-required-drawer", this.openDrawerIfPanelRequiresAttention.bind(this))
    ), this.removers.push(
      $.on("set-pointer-events", (t) => {
        this.style.pointerEvents = t.detail.enable ? "" : "none";
      })
    ), this.addEventListener("mousemove", this.mouseMoveListener), this.addEventListener("dragover", this.mouseMoveListener), Fe.addOverlayOutsideClickEvent();
    const e = window.matchMedia("(prefers-color-scheme: dark)");
    this.classList.toggle("dark", e.matches), e.addEventListener("change", (t) => {
      this.classList.toggle("dark", e.matches);
    }), this.reaction(
      () => b.spotlightActive,
      () => {
        be.saveSpotlightActivation(b.spotlightActive), Array.from(this.shadowRoot.querySelectorAll("copilot-section-panel-wrapper")).filter((t) => t.panelInfo?.floating === !0).forEach((t) => {
          b.spotlightActive ? t.style.setProperty("display", "none") : t.style.removeProperty("display");
        });
      }
    ), this.reaction(
      () => b.active,
      () => {
        this.toggleAttribute("active", b.active), b.active ? this.activate() : this.deactivate(), be.saveCopilotActivation(b.active);
      }
    ), this.reaction(
      () => b.activatedAtLeastOnce,
      () => {
        Ji(), Ps();
      }
    ), this.reaction(
      () => b.sectionPanelDragging,
      () => {
        b.sectionPanelDragging && Array.from(this.shadowRoot.children).filter((r) => r.localName.endsWith("-overlay")).forEach((r) => {
          r.close && r.close();
        });
      }
    ), this.reaction(
      () => b.operationWaitsHmrUpdate,
      () => {
        b.operationWaitsHmrUpdate ? this.operationInProgressCursorUpdateDebounceFunc() : (this.operationInProgressCursorUpdateDebounceFunc.clear(), this.toggleOperationInProgressAttr());
      }
    ), be.getCopilotActivation() && Vr().then(() => {
      b.setActive(!0, "restore");
    }), this.removers.push(
      $.on("user-select", (t) => {
        const { allowSelection: r } = t.detail;
        this.toggleAttribute("user-select-none", !r);
      })
    ), this.initialized = !0;
  }
  /**
   * Called when Copilot is activated. Good place to start attach listeners etc.
   */
  activate() {
    Ki("activate"), At.activate(), On.copilotActivated(), Cs(), this.openDrawerIfPanelRequiresAttention(), document.documentElement.addEventListener("mouseleave", this.mouseLeaveListener), Fe.onCopilotActivation(), $.emit("component-tree-updated", {}), Xi.loadPreviewConfiguration();
  }
  /**
   * Called when Copilot is deactivated. Good place to remove listeners etc.
   */
  deactivate() {
    this.closeDrawers(), At.deactivate(), On.copilotDeactivated(), document.documentElement.removeEventListener("mouseleave", this.mouseLeaveListener), Fe.onCopilotDeactivation();
  }
  disconnectedCallback() {
    super.disconnectedCallback(), At.hostDisconnectedCallback(), window.removeEventListener("keydown", mn), $.offSend(this.handleSendEvent), this.removers.forEach((e) => e()), this.removeEventListener("mousemove", this.mouseMoveListener), this.removeEventListener("dragover", this.mouseMoveListener), Fe.removeOverlayOutsideClickEvent(), document.documentElement.removeEventListener("vaadin-overlay-outside-click", this.overlayOutsideClickListener);
  }
  handleSendEvent(e) {
    const t = e.detail.command, r = e.detail.data;
    ke(t, r);
  }
  /**
   * Opens the attention required drawer if there is any.
   */
  openDrawerIfPanelRequiresAttention() {
    const e = er.getAttentionRequiredPanelConfiguration();
    if (!e)
      return;
    const t = e.panel;
    if (!t || e.floating)
      return;
    const r = this.shadowRoot.querySelector(`copilot-drawer-panel[position="${t}"]`);
    r.opened = !0;
  }
  render() {
    return wr`
      <copilot-activation-button
        @activation-btn-clicked="${() => {
      b.toggleActive("button"), b.setLoginCheckActive(!1);
    }}"
        @spotlight-activation-changed="${(e) => {
      b.setSpotlightActive(e.detail);
    }}"
        .spotlightOn="${b.spotlightActive}">
      </copilot-activation-button>
      <copilot-component-selector></copilot-component-selector>
      <copilot-label-editor-container></copilot-label-editor-container>
      <copilot-info-tooltip></copilot-info-tooltip>
      ${this.renderDrawer("left")} ${this.renderDrawer("right")} ${this.renderDrawer("bottom")} ${Zs()}
      <copilot-spotlight ?active=${b.spotlightActive && b.active}></copilot-spotlight>
      <copilot-login-check ?active=${b.loginCheckActive && b.active}></copilot-login-check>
      <copilot-notifications-container></copilot-notifications-container>
    `;
  }
  renderDrawer(e) {
    return wr` <copilot-drawer-panel no-transition position=${e}>
      ${Js(e)}
    </copilot-drawer-panel>`;
  }
  /**
   * Closes the open drawers if any opened unless an overlay is opened from drawer.
   */
  closeDrawers() {
    const e = this.shadowRoot.querySelectorAll(`${Re}drawer-panel`);
    if (!Array.from(e).some((o) => o.opened))
      return;
    const r = Array.from(this.shadowRoot.children).find(
      (o) => o.localName.endsWith("overlay")
    ), n = r && Fe.getOwner(r);
    if (!n) {
      e.forEach((o) => {
        o.opened = !1;
      });
      return;
    }
    const i = ts(n, "copilot-drawer-panel");
    if (!i) {
      e.forEach((o) => {
        o.opened = !1;
      });
      return;
    }
    Array.from(e).filter((o) => o.position !== i.position).forEach((o) => {
      o.opened = !1;
    });
  }
  updated(e) {
    super.updated(e), this.attachActivationButtonToBody(), il();
  }
  attachActivationButtonToBody() {
    const e = document.body.querySelectorAll("copilot-activation-button");
    e.length > 1 && e[0].remove();
  }
  mouseMoveListener(e) {
    e.composedPath().find((t) => t.localName === `${Re}drawer-panel`) || this.closeDrawers();
  }
  mouseLeaveListener() {
    $.emit("close-drawers", {});
  }
};
Sn = _l([
  ls("copilot-main")
], Sn);
const ml = window.Vaadin, yl = {
  init(e) {
    $i(
      () => window.Vaadin.devTools,
      (t) => {
        const r = t.handleFrontendMessage;
        t.handleFrontendMessage = (n) => {
          nl(n) || r.call(t, n);
        };
      }
    );
  }
};
ml.devToolsPlugins.push(yl);
customElements.whenDefined("vaadin-dev-tools").then(() => {
  const e = window, t = e.Vaadin.devTools.frontendConnection.onReload;
  e.Vaadin.devTools.frontendConnection.onReload = () => {
    t(), e.Vaadin.copilot.eventbus.emit("java-after-update", {});
  };
});
export {
  Al as A,
  Hl as B,
  be as C,
  Ul as D,
  cl as E,
  sl as F,
  Dl as G,
  Fi as H,
  Ki as I,
  Pl as J,
  is as K,
  Yt as L,
  xs as M,
  zl as N,
  Ll as O,
  Re as P,
  El as Q,
  yr as R,
  Ci as S,
  E as T,
  Rl as U,
  Ol as V,
  hl as a,
  $ as b,
  ql as c,
  Hi as d,
  b as e,
  $l as f,
  Wl as g,
  Ir as h,
  Nl as i,
  Cl as j,
  Fl as k,
  er as l,
  Sl as m,
  cs as n,
  Zi as o,
  xl as p,
  Je as q,
  ie as r,
  ke as s,
  ls as t,
  Bl as u,
  rl as v,
  wl as w,
  wr as x,
  fl as y,
  Kl as z
};
